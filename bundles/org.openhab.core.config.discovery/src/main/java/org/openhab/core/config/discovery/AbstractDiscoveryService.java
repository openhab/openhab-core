/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.config.discovery;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractDiscoveryService} provides methods which handle the {@link DiscoveryListener}s.
 *
 * Subclasses do not have to care about adding and removing those listeners.
 * They can use the protected methods {@link #thingDiscovered(DiscoveryResult)} and {@link #thingRemoved(ThingUID)} in
 * order to notify the registered {@link DiscoveryListener}s.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Kai Kreuzer - Refactored API
 * @author Dennis Nobel - Added background discovery configuration through Configuration Admin
 * @author Andre Fuechsel - Added removeOlderResults
 * @author Laurent Garnier - Added discovery with an optional input parameter
 */
@NonNullByDefault
public abstract class AbstractDiscoveryService implements DiscoveryService {

    private static final String DISCOVERY_THREADPOOL_NAME = "discovery";

    private final Logger logger = LoggerFactory.getLogger(AbstractDiscoveryService.class);

    protected final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(DISCOVERY_THREADPOOL_NAME);

    private final Set<DiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    protected @Nullable ScanListener scanListener = null;

    private boolean backgroundDiscoveryEnabled;

    private @Nullable String scanInputLabel;
    private @Nullable String scanInputDescription;

    private final Map<ThingUID, DiscoveryResult> cachedResults = new HashMap<>();

    private final Set<ThingTypeUID> supportedThingTypes;
    private final int timeout;

    private Instant timestampOfLastScan = Instant.MIN;

    private @Nullable ScheduledFuture<?> scheduledStop;

    protected @NonNullByDefault({}) TranslationProvider i18nProvider;
    protected @NonNullByDefault({}) LocaleProvider localeProvider;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param supportedThingTypes the list of Thing types which are supported (can be null)
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @param backgroundDiscoveryEnabledByDefault defines, whether the default for this discovery service is to
     *            enable background discovery or not.
     * @param scanInputLabel the label of the optional input parameter to start the discovery or null if no input
     *            parameter supported
     * @param scanInputDescription the description of the optional input parameter to start the discovery or null if no
     *            input parameter supported
     * @throws IllegalArgumentException if {@code timeout < 0}
     */
    protected AbstractDiscoveryService(@Nullable Set<ThingTypeUID> supportedThingTypes, int timeout,
            boolean backgroundDiscoveryEnabledByDefault, @Nullable String scanInputLabel,
            @Nullable String scanInputDescription) throws IllegalArgumentException {
        if (timeout < 0) {
            throw new IllegalArgumentException("The timeout must be >= 0!");
        }
        this.supportedThingTypes = supportedThingTypes == null ? Set.of() : Set.copyOf(supportedThingTypes);
        this.timeout = timeout;
        this.backgroundDiscoveryEnabled = backgroundDiscoveryEnabledByDefault;
        this.scanInputLabel = scanInputLabel;
        this.scanInputDescription = scanInputDescription;
    }

    /**
     * Creates a new instance of this class with the specified parameters and no input parameter supported to start the
     * discovery.
     *
     * @param supportedThingTypes the list of Thing types which are supported (can be null)
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @param backgroundDiscoveryEnabledByDefault defines, whether the default for this discovery service is to
     *            enable background discovery or not.
     * @throws IllegalArgumentException if {@code timeout < 0}
     */
    protected AbstractDiscoveryService(@Nullable Set<ThingTypeUID> supportedThingTypes, int timeout,
            boolean backgroundDiscoveryEnabledByDefault) throws IllegalArgumentException {
        this(supportedThingTypes, timeout, backgroundDiscoveryEnabledByDefault, null, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters and background discovery enabled
     * and no input parameter supported to start the discovery.
     *
     * @param supportedThingTypes the list of Thing types which are supported (can be null)
     * @param timeout the discovery timeout in seconds after which the discovery service
     *            automatically stops its forced discovery process (>= 0).
     *            If set to 0, disables the automatic stop.
     * @throws IllegalArgumentException if {@code timeout < 0}
     */
    protected AbstractDiscoveryService(@Nullable Set<ThingTypeUID> supportedThingTypes, int timeout)
            throws IllegalArgumentException {
        this(supportedThingTypes, timeout, true);
    }

    /**
     * Creates a new instance of this class with the specified parameters and background discovery enabled
     * and no input parameter supported to start the discovery.
     *
     * @param timeout the discovery timeout in seconds after which the discovery service
     *            automatically stops its forced discovery process (>= 0).
     *            If set to 0, disables the automatic stop.
     * @throws IllegalArgumentException if {@code timeout < 0}
     */
    protected AbstractDiscoveryService(int timeout) throws IllegalArgumentException {
        this(null, timeout);
    }

    /**
     * Returns the list of {@code Thing} types which are supported by the {@link DiscoveryService}.
     *
     * @return the list of Thing types which are supported by the discovery service
     *         (not null, could be empty)
     */
    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return supportedThingTypes;
    }

    @Override
    public boolean isScanInputSupported() {
        return getScanInputLabel() != null && getScanInputDescription() != null;
    }

    @Override
    public @Nullable String getScanInputLabel() {
        return scanInputLabel;
    }

    @Override
    public @Nullable String getScanInputDescription() {
        return scanInputDescription;
    }

    /**
     * Returns the amount of time in seconds after which the discovery service automatically
     * stops its forced discovery process.
     *
     * @return the discovery timeout in seconds (>= 0).
     */
    @Override
    public int getScanTimeout() {
        return timeout;
    }

    @Override
    public boolean isBackgroundDiscoveryEnabled() {
        return backgroundDiscoveryEnabled;
    }

    @Override
    public void addDiscoveryListener(@Nullable DiscoveryListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (cachedResults) {
            for (DiscoveryResult cachedResult : cachedResults.values()) {
                listener.thingDiscovered(this, cachedResult);
            }
        }
        discoveryListeners.add(listener);
    }

    @Override
    public void removeDiscoveryListener(@Nullable DiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    @Override
    public void startScan(@Nullable ScanListener listener) {
        startScanInternal(null, listener);
    }

    @Override
    public void startScan(String input, @Nullable ScanListener listener) {
        startScanInternal(input, listener);
    }

    private synchronized void startScanInternal(@Nullable String input, @Nullable ScanListener listener) {
        synchronized (this) {
            // we first stop any currently running scan and its scheduled stop
            // call
            stopScan();
            ScheduledFuture<?> scheduledStop = this.scheduledStop;
            if (scheduledStop != null) {
                scheduledStop.cancel(false);
                this.scheduledStop = null;
            }

            scanListener = listener;

            // schedule an automatic call of stopScan when timeout is reached
            if (getScanTimeout() > 0) {
                scheduledStop = scheduler.schedule(() -> {
                    try {
                        stopScan();
                    } catch (Exception e) {
                        logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    }
                }, getScanTimeout(), TimeUnit.SECONDS);
            }
            timestampOfLastScan = Instant.now();

            try {
                if (isScanInputSupported() && input != null) {
                    startScan(input);
                } else {
                    startScan();
                }
            } catch (Exception ex) {
                scheduledStop = this.scheduledStop;
                if (scheduledStop != null) {
                    scheduledStop.cancel(false);
                    this.scheduledStop = null;
                }
                scanListener = null;
                throw ex;
            }
        }
    }

    @Override
    public synchronized void abortScan() {
        synchronized (this) {
            ScheduledFuture<?> scheduledStop = this.scheduledStop;
            if (scheduledStop != null) {
                scheduledStop.cancel(false);
                this.scheduledStop = null;
            }
            final ScanListener scanListener = this.scanListener;
            if (scanListener != null) {
                Exception e = new CancellationException("Scan has been aborted.");
                scanListener.onErrorOccurred(e);
                this.scanListener = null;
            }
        }
    }

    /**
     * This method is called by the {@link #startScan(ScanListener)} implementation of the
     * {@link AbstractDiscoveryService}.
     * The abstract class schedules a call of {@link #stopScan()} after {@link #getScanTimeout()} seconds. If this
     * behavior is not appropriate, the {@link #startScan(ScanListener)} method should be overridden.
     */
    protected abstract void startScan();

    // An abstract method would have required a change in all existing bindings implementing a DiscoveryService
    protected void startScan(String input) {
        logger.warn("Discovery with input parameter not implemented by service '{}'!", this.getClass().getName());
    }

    /**
     * This method cleans up after a scan, i.e. it removes listeners and other required operations.
     */
    protected synchronized void stopScan() {
        ScanListener scanListener = this.scanListener;
        if (scanListener != null) {
            scanListener.onFinished();
            this.scanListener = null;
        }
    }

    /**
     * Notifies the registered {@link DiscoveryListener}s about a discovered device.
     *
     * @param discoveryResult Holds the information needed to identify the discovered device.
     */
    protected void thingDiscovered(final DiscoveryResult discoveryResult) {
        final DiscoveryResult discoveryResultNew = getLocalizedDiscoveryResult(discoveryResult,
                FrameworkUtil.getBundle(this.getClass()));
        for (DiscoveryListener discoveryListener : discoveryListeners) {
            try {
                discoveryListener.thingDiscovered(this, discoveryResultNew);
            } catch (Exception e) {
                logger.error("An error occurred while calling the discovery listener {}.",
                        discoveryListener.getClass().getName(), e);
            }
        }
        synchronized (cachedResults) {
            cachedResults.put(discoveryResultNew.getThingUID(), discoveryResultNew);
        }
    }

    /**
     * Notifies the registered {@link DiscoveryListener}s about a removed device.
     *
     * @param thingUID The UID of the removed thing.
     */
    protected void thingRemoved(ThingUID thingUID) {
        for (DiscoveryListener discoveryListener : discoveryListeners) {
            try {
                discoveryListener.thingRemoved(this, thingUID);
            } catch (Exception e) {
                logger.error("An error occurred while calling the discovery listener {}.",
                        discoveryListener.getClass().getName(), e);
            }
        }
        synchronized (cachedResults) {
            cachedResults.remove(thingUID);
        }
    }

    /**
     * Call to remove all results of all {@link #supportedThingTypes} that are
     * older than the given timestamp. To remove all left over results after a
     * full scan, this method could be called {@link #getTimestampOfLastScan()}
     * as timestamp.
     *
     * @param timestamp timestamp, older results will be removed
     */
    protected void removeOlderResults(long timestamp) {
        removeOlderResults(timestamp, null, null);
    }

    /**
     * Call to remove all results of all {@link #supportedThingTypes} that are
     * older than the given timestamp. To remove all left over results after a
     * full scan, this method could be called {@link #getTimestampOfLastScan()}
     * as timestamp.
     *
     * @param timestamp timestamp, older results will be removed
     * @param bridgeUID if not {@code null} only results of that bridge are being removed
     */
    protected void removeOlderResults(long timestamp, @Nullable ThingUID bridgeUID) {
        removeOlderResults(timestamp, null, bridgeUID);
    }

    /**
     * Call to remove all results of the given types that are older than the
     * given timestamp. To remove all left over results after a full scan, this
     * method could be called {@link #getTimestampOfLastScan()} as timestamp.
     *
     * @param timestamp timestamp, older results will be removed
     * @param thingTypeUIDs collection of {@code ThingType}s, only results of these
     *            {@code ThingType}s will be removed; if {@code null} then
     *            {@link DiscoveryService#getSupportedThingTypes()} will be used
     *            instead
     * @param bridgeUID if not {@code null} only results of that bridge are being removed
     */
    protected void removeOlderResults(long timestamp, @Nullable Collection<ThingTypeUID> thingTypeUIDs,
            @Nullable ThingUID bridgeUID) {
        Collection<ThingUID> removedThings = null;

        Collection<ThingTypeUID> toBeRemoved = thingTypeUIDs != null ? thingTypeUIDs : getSupportedThingTypes();
        for (DiscoveryListener discoveryListener : discoveryListeners) {
            try {
                removedThings = discoveryListener.removeOlderResults(this, timestamp, toBeRemoved, bridgeUID);
            } catch (Exception e) {
                logger.error("An error occurred while calling the discovery listener {}.",
                        discoveryListener.getClass().getName(), e);
            }
        }
        if (removedThings != null) {
            synchronized (cachedResults) {
                for (ThingUID uid : removedThings) {
                    cachedResults.remove(uid);
                }
            }
        }
    }

    /**
     * Called on component activation, if the implementation of this class is an
     * OSGi declarative service and does not override the method. The method
     * implementation calls {@link AbstractDiscoveryService#startBackgroundDiscovery()} if background
     * discovery is enabled by default and not overridden by the configuration.
     *
     * @param configProperties configuration properties
     */
    protected void activate(@Nullable Map<String, Object> configProperties) {
        if (configProperties != null) {
            backgroundDiscoveryEnabled = ConfigParser.valueAsOrElse(
                    configProperties.get(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY), Boolean.class,
                    backgroundDiscoveryEnabled);
        }
        if (backgroundDiscoveryEnabled) {
            startBackgroundDiscovery();
            logger.debug("Background discovery for discovery service '{}' enabled.", this.getClass().getName());
        }
    }

    /**
     * Called when the configuration for the discovery service is changed. If
     * background discovery should be enabled and is currently disabled, the
     * method {@link AbstractDiscoveryService#startBackgroundDiscovery()} is
     * called. If background discovery should be disabled and is currently
     * enabled, the method {@link AbstractDiscoveryService#stopBackgroundDiscovery()} is called. In
     * all other cases, nothing happens.
     *
     * @param configProperties configuration properties
     */
    protected void modified(@Nullable Map<String, Object> configProperties) {
        if (configProperties != null) {
            boolean enabled = ConfigParser.valueAsOrElse(
                    configProperties.get(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY), Boolean.class,
                    backgroundDiscoveryEnabled);

            if (backgroundDiscoveryEnabled && !enabled) {
                stopBackgroundDiscovery();
                logger.debug("Background discovery for discovery service '{}' disabled.", this.getClass().getName());
            } else if (!backgroundDiscoveryEnabled && enabled) {
                startBackgroundDiscovery();
                logger.debug("Background discovery for discovery service '{}' enabled.", this.getClass().getName());
            }
            backgroundDiscoveryEnabled = enabled;
        }
    }

    /**
     * Called on component deactivation, if the implementation of this class is
     * an OSGi declarative service and does not override the method. The method
     * implementation calls {@link AbstractDiscoveryService#stopBackgroundDiscovery()} if background
     * discovery is enabled at the time of component deactivation.
     */
    protected void deactivate() {
        if (backgroundDiscoveryEnabled) {
            stopBackgroundDiscovery();
        }
    }

    /**
     * Can be overridden to start background discovery logic. This method is
     * called if background discovery is enabled when the component is being
     * activated (see {@link AbstractDiscoveryService#activate}.
     */
    protected void startBackgroundDiscovery() {
        // can be overridden
    }

    /**
     * Can be overridden to stop background discovery logic. This method is
     * called if background discovery is enabled when the component is being
     * deactivated (see {@link AbstractDiscoveryService#deactivate()}.
     */
    protected void stopBackgroundDiscovery() {
        // can be overridden
    }

    /**
     * Get the timestamp of the last call of {@link #startScan()}.
     *
     * @return timestamp as long
     */
    protected long getTimestampOfLastScan() {
        return Instant.MIN.equals(timestampOfLastScan) ? 0 : timestampOfLastScan.toEpochMilli();
    }

    private String inferKey(DiscoveryResult discoveryResult, String lastSegment) {
        return "discovery." + discoveryResult.getThingUID().getAsString().replace(":", ".") + "." + lastSegment;
    }

    protected DiscoveryResult getLocalizedDiscoveryResult(final DiscoveryResult discoveryResult,
            @Nullable Bundle bundle) {
        if (i18nProvider != null && localeProvider != null) {
            String currentLabel = discoveryResult.getLabel();

            String key = I18nUtil.stripConstantOr(currentLabel, () -> inferKey(discoveryResult, "label"));

            ParsedKey parsedKey = new ParsedKey(key);

            String label = i18nProvider.getText(bundle, parsedKey.key, currentLabel, localeProvider.getLocale(),
                    parsedKey.args);

            if (currentLabel.equals(label)) {
                return discoveryResult;
            } else {
                return DiscoveryResultBuilder.create(discoveryResult.getThingUID())
                        .withThingType(discoveryResult.getThingTypeUID()).withBridge(discoveryResult.getBridgeUID())
                        .withProperties(discoveryResult.getProperties())
                        .withRepresentationProperty(discoveryResult.getRepresentationProperty()).withLabel(label)
                        .withTTL(discoveryResult.getTimeToLive()).build();
            }
        } else {
            return discoveryResult;
        }
    }

    /**
     * Utility class to parse the key with parameters into the key and optional arguments.
     */
    private static final class ParsedKey {

        private static final int LIMIT = 2;

        private final String key;
        private final Object @Nullable [] args;

        private ParsedKey(String label) {
            String[] parts = label.split("\\s+", LIMIT);
            this.key = parts[0];

            if (parts.length == 1) {
                this.args = null;
            } else {
                this.args = Arrays.stream(parts[1].replaceAll("\\[|\\]|\"", "").split(",")).filter(s -> !s.isBlank())
                        .map(String::trim).toArray(Object[]::new);
            }
        }
    }
}
