/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractThingHandlerDiscoveryService} extends the {@link AbstractDiscoveryService} for thing-based
 * discovery services.
 *
 * It handles the injection of the {@link ThingHandler}
 *
 * @author Jan N. Klug - Initial contribution
 * @author Laurent Garnier - Added discovery with an optional input parameter
 */
@NonNullByDefault
public abstract class AbstractThingHandlerDiscoveryService<T extends ThingHandler> extends AbstractDiscoveryService
        implements ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(AbstractThingHandlerDiscoveryService.class);
    private final Class<T> thingClazz;
    private boolean backgroundDiscoveryEnabled = false;

    // this works around a bug in ecj: @NonNullByDefault({}) complains about the field not being
    // initialized when the type is generic, so we have to initialize it with "something"
    protected @NonNullByDefault({}) T thingHandler = (@NonNull T) null;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param thingClazz the {@link ThingHandler} class.
     * @param supportedThingTypes the list of Thing types which are supported (can be {@code null}).
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @param backgroundDiscoveryEnabledByDefault defines, whether the default for this discovery service is to
     *            enable background discovery or not.
     * @param scanInputLabel the label of the optional input parameter to start the discovery or null if no input
     *            parameter supported.
     * @param scanInputDescription the description of the optional input parameter to start the discovery or null if no
     *            input parameter supported.
     * @throws IllegalArgumentException if {@code timeout < 0}.
     */
    protected AbstractThingHandlerDiscoveryService(Class<T> thingClazz, @Nullable Set<ThingTypeUID> supportedThingTypes,
            int timeout, boolean backgroundDiscoveryEnabledByDefault, @Nullable String scanInputLabel,
            @Nullable String scanInputDescription) throws IllegalArgumentException {
        super(supportedThingTypes, timeout, backgroundDiscoveryEnabledByDefault, scanInputLabel, scanInputDescription);
        this.thingClazz = thingClazz;
        this.backgroundDiscoveryEnabled = backgroundDiscoveryEnabledByDefault;
    }

    /**
     * Creates a new instance of this class with the specified parameters and with {@code scanInputLabel} and
     * {@code scanInputDescription} set to {@code null}.
     *
     * @param thingClazz the {@link ThingHandler} class.
     * @param supportedThingTypes the list of Thing types which are supported (can be {@code null}).
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @param backgroundDiscoveryEnabledByDefault defines, whether the default for this discovery service is to
     *            enable background discovery or not.
     * @throws IllegalArgumentException if {@code timeout < 0}.
     */
    protected AbstractThingHandlerDiscoveryService(Class<T> thingClazz, @Nullable Set<ThingTypeUID> supportedThingTypes,
            int timeout, boolean backgroundDiscoveryEnabledByDefault) throws IllegalArgumentException {
        this(thingClazz, supportedThingTypes, timeout, backgroundDiscoveryEnabledByDefault, null, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters and with {@code scanInputLabel} and
     * {@code scanInputDescription} set to {@code null}, and {@code backgroundDiscoveryEnabledByDefault} enabled.
     *
     * @param thingClazz the {@link ThingHandler} class.
     * @param supportedThingTypes the list of Thing types which are supported (can be {@code null}).
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @throws IllegalArgumentException if {@code timeout < 0}.
     */
    protected AbstractThingHandlerDiscoveryService(Class<T> thingClazz, @Nullable Set<ThingTypeUID> supportedThingTypes,
            int timeout) throws IllegalArgumentException {
        this(thingClazz, supportedThingTypes, timeout, true);
    }

    /**
     * Creates a new instance of this class with the specified parameters and with {@code scanInputLabel} and
     * {@code scanInputDescription} set to {@code null}, without any {@code supportedThingTypes}, and
     * {@code backgroundDiscoveryEnabledByDefault} enabled.
     *
     * @param thingClazz the {@link ThingHandler} class.
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @throws IllegalArgumentException if {@code timeout < 0}.
     */
    protected AbstractThingHandlerDiscoveryService(Class<T> thingClazz, int timeout) throws IllegalArgumentException {
        this(thingClazz, null, timeout);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     * <p>
     * <b>For use by tests only</b>, allows setting a different {@link ScheduledExecutorService} like
     * {@link org.openhab.core.util.SameThreadExecutorService} for synchronous behavior during testing.
     *
     * @param thingClazz the {@link ThingHandler} class.
     * @param supportedThingTypes the list of Thing types which are supported (can be {@code null}).
     * @param timeout the discovery timeout in seconds after which the discovery
     *            service automatically stops its forced discovery process (>= 0).
     * @param backgroundDiscoveryEnabledByDefault defines, whether the default for this discovery service is to
     *            enable background discovery or not.
     * @param scanInputLabel the label of the optional input parameter to start the discovery or null if no input
     *            parameter supported.
     * @param scanInputDescription the description of the optional input parameter to start the discovery or null if no
     *            input parameter supported.
     * @throws IllegalArgumentException if {@code timeout < 0}.
     */
    protected AbstractThingHandlerDiscoveryService(ScheduledExecutorService scheduler, Class<T> thingClazz,
            @Nullable Set<ThingTypeUID> supportedThingTypes, int timeout, boolean backgroundDiscoveryEnabledByDefault,
            @Nullable String scanInputLabel, @Nullable String scanInputDescription) throws IllegalArgumentException {
        super(scheduler, supportedThingTypes, timeout, backgroundDiscoveryEnabledByDefault, scanInputLabel,
                scanInputDescription);
        this.thingClazz = thingClazz;
        this.backgroundDiscoveryEnabled = backgroundDiscoveryEnabledByDefault;
    }

    @Override
    protected abstract void startScan();

    @Override
    @SuppressWarnings("unchecked")
    public void setThingHandler(ThingHandler handler) {
        if (thingClazz.isAssignableFrom(handler.getClass())) {
            this.thingHandler = (T) handler;
        } else {
            throw new IllegalArgumentException(
                    "Expected class is " + thingClazz + " but the parameter has class " + handler.getClass());
        }
    }

    @Override
    public @Nullable T getThingHandler() {
        return thingHandler;
    }

    @Override
    public void activate(@Nullable Map<String, Object> config) {
        // do not call super.activate here, otherwise the scan might be background scan might be started before the
        // thing handler is set. This is correctly handled in initialize
        if (config != null) {
            backgroundDiscoveryEnabled = ConfigParser.valueAsOrElse(
                    config.get(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY), Boolean.class,
                    backgroundDiscoveryEnabled);
        }
    }

    @Override
    public void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            boolean enabled = ConfigParser.valueAsOrElse(
                    config.get(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY), Boolean.class,
                    backgroundDiscoveryEnabled);

            if (backgroundDiscoveryEnabled && !enabled) {
                stopBackgroundDiscovery();
                logger.debug("Background discovery for discovery service '{}' disabled.", getClass().getName());
            } else if (!backgroundDiscoveryEnabled && enabled) {
                startBackgroundDiscovery();
                logger.debug("Background discovery for discovery service '{}' enabled.", getClass().getName());
            }
            backgroundDiscoveryEnabled = enabled;
        }
    }

    @Override
    public void deactivate() {
        // do not call super.deactivate here, background scan is already handled in dispose
    }

    @Override
    public void initialize() {
        if (backgroundDiscoveryEnabled) {
            startBackgroundDiscovery();
            logger.debug("Background discovery for discovery service '{}' enabled.", getClass().getName());
        }
    }

    @Override
    public void dispose() {
        if (backgroundDiscoveryEnabled) {
            stopBackgroundDiscovery();
        }
    }
}
