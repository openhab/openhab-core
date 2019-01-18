/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.internal.firmware;

import static org.eclipse.smarthome.core.thing.firmware.FirmwareStatusInfo.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.validation.ConfigDescriptionValidator;
import org.eclipse.smarthome.config.core.validation.ConfigValidationException;
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareUpdateBackgroundTransferHandler;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.eclipse.smarthome.core.thing.events.ThingStatusInfoChangedEvent;
import org.eclipse.smarthome.core.thing.firmware.FirmwareEventFactory;
import org.eclipse.smarthome.core.thing.firmware.FirmwareRegistry;
import org.eclipse.smarthome.core.thing.firmware.FirmwareStatus;
import org.eclipse.smarthome.core.thing.firmware.FirmwareStatusInfo;
import org.eclipse.smarthome.core.thing.firmware.FirmwareUpdateService;
import org.eclipse.smarthome.core.util.BundleResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link FirmwareUpdateService}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - update and cancel operations are run with different safe caller identifiers in order to
 *         execute asynchronously; Firmware update is done for thing
 */
@Component(immediate = true, service = { EventSubscriber.class, FirmwareUpdateService.class })
@NonNullByDefault
public final class FirmwareUpdateServiceImpl implements FirmwareUpdateService, EventSubscriber {
    private static final String THREAD_POOL_NAME = FirmwareUpdateServiceImpl.class.getSimpleName();
    private static final Set<String> SUPPORTED_TIME_UNITS = Collections.unmodifiableSet(
            Stream.of(TimeUnit.SECONDS.name(), TimeUnit.MINUTES.name(), TimeUnit.HOURS.name(), TimeUnit.DAYS.name())
                    .collect(Collectors.toSet()));
    protected static final String PERIOD_CONFIG_KEY = "period";
    protected static final String DELAY_CONFIG_KEY = "delay";
    protected static final String TIME_UNIT_CONFIG_KEY = "timeUnit";
    private static final String CONFIG_DESC_URI_KEY = "system:firmware-status-info-job";

    private final Logger logger = LoggerFactory.getLogger(FirmwareUpdateServiceImpl.class);

    private int firmwareStatusInfoJobPeriod = 3600;
    private int firmwareStatusInfoJobDelay = 3600;
    private TimeUnit firmwareStatusInfoJobTimeUnit = TimeUnit.SECONDS;

    private @Nullable ScheduledFuture<?> firmwareStatusInfoJob;

    protected int timeout = 30 * 60 * 1000;

    private final Set<String> subscribedEventTypes = Collections.singleton(ThingStatusInfoChangedEvent.TYPE);

    private final Map<ThingUID, FirmwareStatusInfo> firmwareStatusInfoMap = new ConcurrentHashMap<>();
    private final Map<ThingUID, ProgressCallbackImpl> progressCallbackMap = new ConcurrentHashMap<>();

    private final List<FirmwareUpdateHandler> firmwareUpdateHandlers = new CopyOnWriteArrayList<>();
    private @NonNullByDefault({}) FirmwareRegistry firmwareRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) TranslationProvider i18nProvider;
    private @NonNullByDefault({}) LocaleProvider localeProvider;
    private @NonNullByDefault({}) SafeCaller safeCaller;
    private @NonNullByDefault({}) ConfigDescriptionValidator configDescriptionValidator;
    private @NonNullByDefault({}) BundleResolver bundleResolver;

    private final Runnable firmwareStatusRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("Running firmware status check.");
            for (FirmwareUpdateHandler firmwareUpdateHandler : firmwareUpdateHandlers) {
                try {
                    logger.debug("Executing firmware status check for thing with UID {}.",
                            firmwareUpdateHandler.getThing().getUID());

                    Firmware latestFirmware = getLatestSuitableFirmware(firmwareUpdateHandler.getThing());

                    FirmwareStatusInfo newFirmwareStatusInfo = getFirmwareStatusInfo(firmwareUpdateHandler,
                            latestFirmware);

                    processFirmwareStatusInfo(firmwareUpdateHandler, newFirmwareStatusInfo, latestFirmware);
                } catch (Exception e) {
                    logger.debug("Exception occurred during firmware status check.", e);
                }
            }
        }
    };

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected synchronized void modified(Map<String, Object> config) {
        logger.debug("Modifying the configuration of the firmware update service.");

        if (!isValid(config)) {
            return;
        }

        cancelFirmwareUpdateStatusInfoJob();

        firmwareStatusInfoJobPeriod = config.containsKey(PERIOD_CONFIG_KEY) ? (Integer) config.get(PERIOD_CONFIG_KEY)
                : firmwareStatusInfoJobPeriod;
        firmwareStatusInfoJobDelay = config.containsKey(DELAY_CONFIG_KEY) ? (Integer) config.get(DELAY_CONFIG_KEY)
                : firmwareStatusInfoJobDelay;
        firmwareStatusInfoJobTimeUnit = config.containsKey(TIME_UNIT_CONFIG_KEY)
                ? TimeUnit.valueOf((String) config.get(TIME_UNIT_CONFIG_KEY))
                : firmwareStatusInfoJobTimeUnit;

        if (!firmwareUpdateHandlers.isEmpty()) {
            createFirmwareUpdateStatusInfoJob();
        }
    }

    @Deactivate
    protected void deactivate() {
        cancelFirmwareUpdateStatusInfoJob();
        firmwareStatusInfoMap.clear();
        progressCallbackMap.clear();
    }

    @Override
    @Nullable
    public FirmwareStatusInfo getFirmwareStatusInfo(ThingUID thingUID) {
        ParameterChecks.checkNotNull(thingUID, "Thing UID");
        FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);

        if (firmwareUpdateHandler == null) {
            logger.trace("No firmware update handler available for thing with UID {}.", thingUID);
            return null;
        }

        Firmware latestFirmware = getLatestSuitableFirmware(firmwareUpdateHandler.getThing());

        FirmwareStatusInfo firmwareStatusInfo = getFirmwareStatusInfo(firmwareUpdateHandler, latestFirmware);

        processFirmwareStatusInfo(firmwareUpdateHandler, firmwareStatusInfo, latestFirmware);

        return firmwareStatusInfo;
    }

    @Override
    public void updateFirmware(final ThingUID thingUID, final String firmwareVersion, final @Nullable Locale locale) {
        ParameterChecks.checkNotNull(thingUID, "Thing UID");
        ParameterChecks.checkNotNullOrEmpty(firmwareVersion, "Firmware version");

        final FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);

        if (firmwareUpdateHandler == null) {
            throw new IllegalArgumentException(
                    String.format("There is no firmware update handler for thing with UID %s.", thingUID));
        }

        final Thing thing = firmwareUpdateHandler.getThing();
        final Firmware firmware = getFirmware(thing, firmwareVersion);

        validateFirmwareUpdateConditions(firmwareUpdateHandler, firmware);

        final Locale currentLocale = locale != null ? locale : localeProvider.getLocale();

        final ProgressCallbackImpl progressCallback = new ProgressCallbackImpl(firmwareUpdateHandler, eventPublisher,
                i18nProvider, bundleResolver, thingUID, firmware, currentLocale);
        progressCallbackMap.put(thingUID, progressCallback);

        logger.debug("Starting firmware update for thing with UID {} and firmware {}", thingUID, firmware);

        safeCaller.create(firmwareUpdateHandler, FirmwareUpdateHandler.class).withTimeout(timeout).withAsync()
                .onTimeout(() -> {
                    logger.error("Timeout occurred for firmware update of thing with UID {} and firmware {}.", thingUID,
                            firmware);
                    progressCallback.failedInternal("timeout-error");
                }).onException(e -> {
                    logger.error(
                            "Unexpected exception occurred for firmware update of thing with UID {} and firmware {}.",
                            thingUID, firmware, e.getCause());
                    progressCallback.failedInternal("unexpected-handler-error");
                }).build().updateFirmware(firmware, progressCallback);
    }

    @Override
    public void cancelFirmwareUpdate(final ThingUID thingUID) {
        ParameterChecks.checkNotNull(thingUID, "Thing UID");
        final FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);
        if (firmwareUpdateHandler == null) {
            throw new IllegalArgumentException(
                    String.format("There is no firmware update handler for thing with UID %s.", thingUID));
        }
        final ProgressCallbackImpl progressCallback = getProgressCallback(thingUID);

        logger.debug("Cancelling firmware update for thing with UID {}.", thingUID);
        safeCaller.create(firmwareUpdateHandler, FirmwareUpdateHandler.class).withTimeout(timeout).withAsync()
                .onTimeout(() -> {
                    logger.error("Timeout occurred while cancelling firmware update of thing with UID {}.", thingUID);
                    progressCallback.failedInternal("timeout-error-during-cancel");
                }).onException(e -> {
                    logger.error("Unexpected exception occurred while cancelling firmware update of thing with UID {}.",
                            thingUID, e.getCause());
                    progressCallback.failedInternal("unexpected-handler-error-during-cancel");
                }).withIdentifier(new Object()).build().cancel();
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Override
    @Nullable
    public EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ThingStatusInfoChangedEvent) {
            ThingStatusInfoChangedEvent changedEvent = (ThingStatusInfoChangedEvent) event;
            if (changedEvent.getStatusInfo().getStatus() != ThingStatus.ONLINE) {
                return;
            }

            ThingUID thingUID = changedEvent.getThingUID();
            FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);
            if (firmwareUpdateHandler != null && !firmwareStatusInfoMap.containsKey(thingUID)) {
                initializeFirmwareStatus(firmwareUpdateHandler);
            }
        }
    }

    protected ProgressCallbackImpl getProgressCallback(ThingUID thingUID) {
        if (!progressCallbackMap.containsKey(thingUID)) {
            throw new IllegalStateException(
                    String.format("No ProgressCallback available for thing with UID %s.", thingUID));
        }
        return progressCallbackMap.get(thingUID);
    }

    @Nullable
    private Firmware getLatestSuitableFirmware(Thing thing) {
        Collection<Firmware> firmwares = firmwareRegistry.getFirmwares(thing);
        if (firmwares != null) {
            Optional<Firmware> first = firmwares.stream().findFirst();
            if (first.isPresent()) {
                return first.get();
            }
        }
        return null;
    }

    private FirmwareStatusInfo getFirmwareStatusInfo(FirmwareUpdateHandler firmwareUpdateHandler,
            @Nullable Firmware latestFirmware) {
        String thingFirmwareVersion = getThingFirmwareVersion(firmwareUpdateHandler);
        ThingUID thingUID = firmwareUpdateHandler.getThing().getUID();

        if (latestFirmware == null || thingFirmwareVersion == null) {
            return createUnknownInfo(thingUID);
        }

        if (latestFirmware.isSuccessorVersion(thingFirmwareVersion)) {
            if (firmwareUpdateHandler.isUpdateExecutable()) {
                return createUpdateExecutableInfo(thingUID, latestFirmware.getVersion());
            }
            return createUpdateAvailableInfo(thingUID);
        }

        return createUpToDateInfo(thingUID);
    }

    private synchronized void processFirmwareStatusInfo(FirmwareUpdateHandler firmwareUpdateHandler,
            FirmwareStatusInfo newFirmwareStatusInfo, @Nullable Firmware latestFirmware) {
        FirmwareStatusInfo previousFirmwareStatusInfo = firmwareStatusInfoMap.put(newFirmwareStatusInfo.getThingUID(),
                newFirmwareStatusInfo);

        if (previousFirmwareStatusInfo == null || !previousFirmwareStatusInfo.equals(newFirmwareStatusInfo)) {
            eventPublisher.post(FirmwareEventFactory.createFirmwareStatusInfoEvent(newFirmwareStatusInfo));

            if (newFirmwareStatusInfo.getFirmwareStatus() == FirmwareStatus.UPDATE_AVAILABLE
                    && firmwareUpdateHandler instanceof FirmwareUpdateBackgroundTransferHandler
                    && !firmwareUpdateHandler.isUpdateExecutable()) {
                if (latestFirmware != null) {
                    transferLatestFirmware((FirmwareUpdateBackgroundTransferHandler) firmwareUpdateHandler,
                            latestFirmware, previousFirmwareStatusInfo);
                }
            }
        }
    }

    private void transferLatestFirmware(final FirmwareUpdateBackgroundTransferHandler fubtHandler,
            final Firmware latestFirmware, final FirmwareStatusInfo previousFirmwareStatusInfo) {
        getPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    fubtHandler.transferFirmware(latestFirmware);
                } catch (Exception e) {
                    logger.error("Exception occurred during background firmware transfer.", e);
                    synchronized (this) {
                        // restore previous firmware status info in order that transfer can be re-triggered
                        if (previousFirmwareStatusInfo == null) {
                            firmwareStatusInfoMap.remove(fubtHandler.getThing().getUID());
                        } else {
                            firmwareStatusInfoMap.put(fubtHandler.getThing().getUID(), previousFirmwareStatusInfo);
                        }
                    }
                }
            }
        });
    }

    private void validateFirmwareUpdateConditions(FirmwareUpdateHandler firmwareUpdateHandler, Firmware firmware) {
        if (!firmwareUpdateHandler.isUpdateExecutable()) {
            throw new IllegalStateException(String.format("The firmware update of thing with UID %s is not executable.",
                    firmwareUpdateHandler.getThing().getUID()));
        }
        validateFirmwareSuitability(firmware, firmwareUpdateHandler);
    }

    private void validateFirmwareSuitability(Firmware firmware, FirmwareUpdateHandler firmwareUpdateHandler) {
        Thing thing = firmwareUpdateHandler.getThing();

        if (!firmware.isSuitableFor(thing)) {
            throw new IllegalArgumentException(
                    String.format("Firmware %s is not suitable for thing with UID %s.", firmware, thing.getUID()));
        }
    }

    private Firmware getFirmware(Thing thing, String firmwareVersion) {
        Firmware firmware = firmwareRegistry.getFirmware(thing, firmwareVersion);
        if (firmware == null) {
            throw new IllegalArgumentException(String.format(
                    "Firmware with version %s for thing with UID %s was not found.", firmwareVersion, thing.getUID()));
        }
        return firmware;
    }

    @Nullable
    private FirmwareUpdateHandler getFirmwareUpdateHandler(ThingUID thingUID) {
        for (FirmwareUpdateHandler firmwareUpdateHandler : firmwareUpdateHandlers) {
            if (thingUID.equals(firmwareUpdateHandler.getThing().getUID())) {
                return firmwareUpdateHandler;
            }
        }
        return null;
    }

    @Nullable
    private String getThingFirmwareVersion(FirmwareUpdateHandler firmwareUpdateHandler) {
        return firmwareUpdateHandler.getThing().getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION);
    }

    private void createFirmwareUpdateStatusInfoJob() {
        if (firmwareStatusInfoJob == null || firmwareStatusInfoJob.isCancelled()) {
            logger.debug("Creating firmware status info job. [delay:{}, period:{}, time unit: {}]",
                    firmwareStatusInfoJobDelay, firmwareStatusInfoJobPeriod, firmwareStatusInfoJobTimeUnit);

            firmwareStatusInfoJob = getPool().scheduleWithFixedDelay(firmwareStatusRunnable, firmwareStatusInfoJobDelay,
                    firmwareStatusInfoJobPeriod, firmwareStatusInfoJobTimeUnit);
        }
    }

    private void cancelFirmwareUpdateStatusInfoJob() {
        if (firmwareStatusInfoJob != null && !firmwareStatusInfoJob.isCancelled()) {
            logger.debug("Cancelling firmware status info job.");
            firmwareStatusInfoJob.cancel(true);
            firmwareStatusInfoJob = null;
        }
    }

    private boolean isValid(Map<String, Object> config) {
        // the config description validator does not support option value validation at the moment; so we will validate
        // the time unit here
        if (!SUPPORTED_TIME_UNITS.contains(config.get(TIME_UNIT_CONFIG_KEY))) {
            logger.debug("Given time unit {} is not supported. Will keep current configuration.",
                    config.get(TIME_UNIT_CONFIG_KEY));
            return false;
        }

        try {
            configDescriptionValidator.validate(config, new URI(CONFIG_DESC_URI_KEY));
        } catch (URISyntaxException | ConfigValidationException e) {
            logger.debug("Validation of new configuration values failed. Will keep current configuration.", e);
            return false;
        }

        return true;
    }

    private void initializeFirmwareStatus(final FirmwareUpdateHandler firmwareUpdateHandler) {
        getPool().submit(new Runnable() {
            @Override
            public void run() {
                ThingUID thingUID = firmwareUpdateHandler.getThing().getUID();
                FirmwareStatusInfo info = getFirmwareStatusInfo(thingUID);
                logger.debug("Firmware status {} for thing {} initialized.", info.getFirmwareStatus(), thingUID);
                firmwareStatusInfoMap.put(thingUID, info);
            }
        });
    }

    private static ScheduledExecutorService getPool() {
        return ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
    }

    public int getFirmwareStatusInfoJobPeriod() {
        return firmwareStatusInfoJobPeriod;
    }

    public int getFirmwareStatusInfoJobDelay() {
        return firmwareStatusInfoJobDelay;
    }

    public TimeUnit getFirmwareStatusInfoJobTimeUnit() {
        return firmwareStatusInfoJobTimeUnit;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        if (firmwareUpdateHandlers.isEmpty()) {
            createFirmwareUpdateStatusInfoJob();
        }
        firmwareUpdateHandlers.add(firmwareUpdateHandler);
    }

    protected synchronized void removeFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        firmwareStatusInfoMap.remove(firmwareUpdateHandler.getThing().getUID());
        firmwareUpdateHandlers.remove(firmwareUpdateHandler);
        if (firmwareUpdateHandlers.isEmpty()) {
            cancelFirmwareUpdateStatusInfoJob();
        }
        progressCallbackMap.remove(firmwareUpdateHandler.getThing().getUID());
    }

    @Reference
    protected void setFirmwareRegistry(FirmwareRegistry firmwareRegistry) {
        this.firmwareRegistry = firmwareRegistry;
    }

    protected void unsetFirmwareRegistry(FirmwareRegistry firmwareRegistry) {
        this.firmwareRegistry = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
    }

    @Reference
    protected void setLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    @Reference
    protected void setSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = safeCaller;
    }

    protected void unsetSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = null;
    }

    @Reference
    protected void setConfigDescriptionValidator(ConfigDescriptionValidator configDescriptionValidator) {
        this.configDescriptionValidator = configDescriptionValidator;
    }

    protected void unsetConfigDescriptionValidator(ConfigDescriptionValidator configDescriptionValidator) {
        this.configDescriptionValidator = null;
    }

    @Reference
    protected void setBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    protected void unsetBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

}
