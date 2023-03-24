/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.config.core.validation.ConfigValidationMessage;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeMigrationService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.internal.update.ThingUpdateInstruction;
import org.openhab.core.thing.internal.update.ThingUpdateInstructionReader;
import org.openhab.core.thing.internal.update.ThingUpdateInstructionReader.UpdateInstructionKey;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.AbstractDescriptionType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ThingManagerImpl} tracks all things in the {@link ThingRegistry} and mediates the communication between the
 * {@link Thing} and the {@link ThingHandler} from the binding. It tracks {@link ThingHandlerFactory}s and
 * calls {@link ThingHandlerFactory#registerHandler(Thing)} for each thing, that was added to the {@link ThingRegistry}.
 * In addition, the {@link ThingManagerImpl} acts as an {@link org.openhab.core.internal.events.EventHandler}
 * and subscribes to update and command events.
 * Finally, the {@link ThingManagerImpl} implements the {@link ThingTypeMigrationService} to offer a way to change the
 * thing-type of a {@link Thing}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Added dynamic configuration update
 * @author Stefan Bußweiler - Added new thing status handling, migration to new event mechanism,
 *         refactorings due to thing/bridge life cycle
 * @author Simon Kaufmann - Added remove handling, type conversion
 * @author Kai Kreuzer - Removed usage of itemRegistry and thingLinkRegistry, fixed vetoing mechanism
 * @author Andre Fuechsel - Added the {@link ThingTypeMigrationService}
 * @author Thomas Höfer - Added localization of thing status info
 * @author Christoph Weitkamp - Moved OSGI ServiceTracker from BaseThingHandler to ThingHandlerCallback
 * @author Henning Sudbrock - Consider thing type properties when migrating to new thing type
 * @author Christoph Weitkamp - Added preconfigured ChannelGroupBuilder
 * @author Yordan Zhelev - Added thing disabling mechanism
 * @author Björn Lange - Ignore illegal thing status transitions instead of throwing IllegalArgumentException
 * @author Jan N. Klug - Add thing update mechanism
 */
@NonNullByDefault
@Component(immediate = true, service = { ThingTypeMigrationService.class, ThingManager.class })
public class ThingManagerImpl implements ReadyTracker, ThingManager, ThingTracker, ThingTypeMigrationService {
    public static final String PROPERTY_THING_TYPE_VERSION = "thingTypeVersion";

    // interval to check if thing prerequisites are met (in s)
    private static final int CHECK_INTERVAL = 2;

    // time after we try to initialize a thing even if the thing-type is not registered (in s)
    private static final int MAX_CHECK_PREREQUISITE_TIME = 120;
    private static final ReadyMarker READY_MARKER_THINGS_LOADED = new ReadyMarker("things", "handler");
    private static final String THING_STATUS_STORAGE_NAME = "thing_status_storage";
    private static final String FORCE_REMOVE_THREAD_POOL_NAME = "forceRemove";
    private static final String THING_MANAGER_THREAD_POOL_NAME = "thingManager";

    private final Logger logger = LoggerFactory.getLogger(ThingManagerImpl.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(THING_MANAGER_THREAD_POOL_NAME);

    private final List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<>();
    private final Map<ThingUID, ThingHandler> thingHandlers = new ConcurrentHashMap<>();
    private final Map<ThingHandlerFactory, Set<ThingHandler>> thingHandlersByFactory = new ConcurrentHashMap<>();
    private final Map<UpdateInstructionKey, List<ThingUpdateInstruction>> updateInstructions = new ConcurrentHashMap<>();
    private final Map<ThingUID, ThingPrerequisites> missingPrerequisites = new ConcurrentHashMap<>();

    private final Map<ThingUID, Thing> things = new ConcurrentHashMap<>();
    private final Map<ThingUID, Lock> thingLocks = new ConcurrentHashMap<>();
    private final Set<ThingUID> thingUpdatedLock = ConcurrentHashMap.newKeySet();

    protected final ChannelGroupTypeRegistry channelGroupTypeRegistry;
    protected final ChannelTypeRegistry channelTypeRegistry;
    protected final CommunicationManager communicationManager;
    protected final ConfigDescriptionRegistry configDescriptionRegistry;
    protected final ConfigDescriptionValidator configDescriptionValidator;
    private final EventPublisher eventPublisher;
    protected final ThingTypeRegistry thingTypeRegistry;
    protected final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ReadyService readyService;
    private final SafeCaller safeCaller;
    private final Storage<String> disabledStorage;
    protected final ThingRegistryImpl thingRegistry;
    private final TranslationProvider translationProvider;
    private final BundleContext bundleContext;

    private final ThingUpdateInstructionReader thingUpdateInstructionReader;

    private final ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;

    private @Nullable ScheduledFuture<?> startLevelSetterJob = null;
    private @Nullable ScheduledFuture<?> prerequisiteCheckerJob = null;

    private final ThingHandlerCallback thingHandlerCallback = new ThingHandlerCallbackImpl(this);

    @Activate
    public ThingManagerImpl( //
            final @Reference ChannelGroupTypeRegistry channelGroupTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference CommunicationManager communicationManager,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference ConfigDescriptionValidator configDescriptionValidator,
            final @Reference EventPublisher eventPublisher,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ReadyService readyService, //
            final @Reference SafeCaller safeCaller, //
            final @Reference StorageService storageService, //
            final @Reference ThingRegistry thingRegistry,
            final @Reference ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService,
            final @Reference ThingTypeRegistry thingTypeRegistry, final @Reference BundleResolver bundleResolver,
            final @Reference TranslationProvider translationProvider, final BundleContext bundleContext) {
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.communicationManager = communicationManager;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.configDescriptionValidator = configDescriptionValidator;
        this.eventPublisher = eventPublisher;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.readyService = readyService;
        this.safeCaller = safeCaller;
        this.thingRegistry = (ThingRegistryImpl) thingRegistry;
        this.thingUpdateInstructionReader = new ThingUpdateInstructionReader(bundleResolver);
        this.thingStatusInfoI18nLocalizationService = thingStatusInfoI18nLocalizationService;
        this.thingTypeRegistry = thingTypeRegistry;
        this.translationProvider = translationProvider;
        this.bundleContext = bundleContext;
        this.disabledStorage = storageService.getStorage(THING_STATUS_STORAGE_NAME, this.getClass().getClassLoader());

        this.thingRegistry.addThingTracker(this);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    protected synchronized void deactivate() {
        thingRegistry.removeThingTracker(this);
        for (ThingHandlerFactory factory : thingHandlerFactories) {
            removeThingHandlerFactory(factory);
        }
        readyService.unregisterTracker(this);
        ScheduledFuture<?> startLevelSetterJob = this.startLevelSetterJob;
        if (startLevelSetterJob != null) {
            startLevelSetterJob.cancel(true);
            this.startLevelSetterJob = null;
        }
        ScheduledFuture<?> prerequisiteCheckerJob = this.prerequisiteCheckerJob;
        if (prerequisiteCheckerJob != null) {
            prerequisiteCheckerJob.cancel(true);
            this.prerequisiteCheckerJob = null;
        }
    }

    protected void thingUpdated(final Thing thing) {
        thingUpdatedLock.add(thing.getUID());
        final Thing oldThing = thingRegistry.get(thing.getUID());
        if (oldThing == null) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Cannot update thing {0} because it is not known to the registry", thing.getUID().getAsString()));
        }
        final Provider<Thing> provider = thingRegistry.getProvider(thing);
        if (provider == null) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Provider for thing {0} cannot be determined because it is not known to the registry",
                    thing.getUID().getAsString()));
        }
        if (provider instanceof ManagedProvider<Thing, ThingUID> managedProvider) {
            managedProvider.update(thing);
        } else {
            logger.debug("Only updating thing {} in the registry because provider {} is not managed.",
                    thing.getUID().getAsString(), provider);
            thingRegistry.updated(provider, oldThing, thing);
        }
        thingUpdatedLock.remove(thing.getUID());
    }

    @Override
    public void migrateThingType(final Thing thing, final ThingTypeUID thingTypeUID,
            final @Nullable Configuration configuration) {
        final ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            throw new IllegalStateException(
                    MessageFormat.format("No thing type {0} registered, cannot change thing type for thing {1}",
                            thingTypeUID.getAsString(), thing.getUID().getAsString()));
        }
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                ThingUID thingUID = thing.getUID();
                Lock lock = getLockForThing(thingUID);
                lock.lock();
                try {
                    // Remove the ThingHandler, if any
                    final ThingHandlerFactory oldThingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
                    if (oldThingHandlerFactory != null) {
                        ThingHandler thingHandler = thing.getHandler();
                        if (thingHandler != null) {
                            unregisterAndDisposeHandler(oldThingHandlerFactory, thing, thingHandler);
                            waitUntilHandlerUnregistered(thing, 60 * 1000);
                        } else {
                            logger.debug("No ThingHandler to dispose for {}", thing.getUID());
                        }
                    } else {
                        logger.debug("No ThingHandlerFactory available that can handle {}", thing.getThingTypeUID());
                    }

                    // Set the new channels
                    List<Channel> channels = ThingFactoryHelper.createChannels(thingType, thingUID,
                            configDescriptionRegistry);
                    ((ThingImpl) thing).setChannels(channels);

                    // Set the given configuration
                    Configuration editConfiguration = configuration != null ? configuration : new Configuration();
                    ThingFactoryHelper.applyDefaultConfiguration(editConfiguration, thingType,
                            configDescriptionRegistry);
                    ((ThingImpl) thing).setConfiguration(editConfiguration);

                    // Set the new properties (keeping old properties, unless they have the same name as a new property)
                    for (Entry<String, String> entry : thingType.getProperties().entrySet()) {
                        thing.setProperty(entry.getKey(), entry.getValue());
                    }

                    // set the new ThingTypeUID
                    ((ThingImpl) thing).setThingTypeUID(thingTypeUID);

                    // update the thing and register a new handler
                    thingUpdated(thing);
                    final ThingHandlerFactory newThingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
                    if (newThingHandlerFactory != null) {
                        registerAndInitializeHandler(thing, newThingHandlerFactory);
                    }

                    ThingHandler handler = thing.getHandler();
                    logger.debug("Changed ThingType of Thing {} to {}. New ThingHandler is {}.", thingUID,
                            thing.getThingTypeUID(), handler == null ? "NO HANDLER" : handler);
                } finally {
                    lock.unlock();
                }
            }

            private void waitUntilHandlerUnregistered(final Thing thing, int timeout) {
                for (int i = 0; i < timeout / 100; i++) {
                    if (thing.getHandler() == null && thingHandlers.get(thing.getUID()) == null) {
                        return;
                    }
                    try {
                        Thread.sleep(100);
                        logger.debug("Waiting for handler deregistration to complete for thing {}. Took already {}ms.",
                                thing.getUID().getAsString(), (i + 1) * 100);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                String message = MessageFormat.format(
                        "Thing type migration failed for {0}. The handler deregistration did not complete within {1}ms.",
                        thing.getUID().getAsString(), timeout);
                logger.error(message);
                throw new IllegalStateException(message);
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void thingAdded(Thing thing, ThingTrackerEvent thingTrackerEvent) {
        if (things.get(thing.getUID()) != null) {
            logger.error("A thing with UID '{}' is already tracked by ThingManager. This is a bug.", thing.getUID());
        }

        things.put(thing.getUID(), thing);
        ThingPrerequisites thingPrerequisites = new ThingPrerequisites(thing);
        if (!thingPrerequisites.isReady()) {
            missingPrerequisites.put(thing.getUID(), thingPrerequisites);
        }

        eventPublisher.post(ThingEventFactory.createStatusInfoEvent(thing.getUID(), thing.getStatusInfo()));
        logger.debug("Thing '{}' is tracked by ThingManager.", thing.getUID());
        if (!isHandlerRegistered(thing)) {
            registerAndInitializeHandler(thing, getThingHandlerFactory(thing));
        } else {
            logger.debug("Handler of tracked thing '{}' already registered.", thing.getUID());
        }
    }

    @Override
    public void thingRemoving(Thing thing, ThingTrackerEvent thingTrackerEvent) {
        setThingStatus(thing, buildStatusInfo(ThingStatus.REMOVING, ThingStatusDetail.NONE));
        notifyThingHandlerAboutRemoval(thing);
    }

    @Override
    public void thingRemoved(final Thing thing, ThingTrackerEvent thingTrackerEvent) {
        logger.debug("Thing '{}' is no longer tracked by ThingManager.", thing.getUID());

        ThingHandler thingHandler = thingHandlers.get(thing.getUID());
        if (thingHandler != null) {
            final ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
            if (thingHandlerFactory != null) {
                unregisterAndDisposeHandler(thingHandlerFactory, thing, thingHandler);
                if (thingTrackerEvent == ThingTrackerEvent.THING_REMOVED) {
                    safeCaller.create(thingHandlerFactory, ThingHandlerFactory.class).build()
                            .removeThing(thing.getUID());
                }
            } else {
                logger.warn("Cannot unregister handler. No handler factory for thing '{}' found.", thing.getUID());
            }
        }

        if (!things.containsKey(thing.getUID())) {
            logger.error("Trying to remove thing '{}', but is not tracked by ThingManager. This is a bug.",
                    thing.getUID());
        } else {
            if (!thing.equals(things.remove(thing.getUID()))) {
                logger.error(
                        "Trying to remove thing '{}', but it is different from the thing with the same UID tracked by ThingManager. This is a bug.",
                        thing.getUID());
            }
        }

        missingPrerequisites.remove(thing.getUID());
    }

    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void thingUpdated(Thing oldThing, Thing newThing, ThingTrackerEvent thingTrackerEvent) {
        ThingUID thingUID = newThing.getUID();
        try {
            normalizeThingConfiguration(oldThing);
        } catch (ConfigValidationException e) {
            logger.warn("Failed to normalize configuration for thing '{}': {}", oldThing.getUID(),
                    e.getValidationMessages(null));
        }
        try {
            normalizeThingConfiguration(newThing);
        } catch (ConfigValidationException e) {
            logger.warn("Failed to normalize configuration´for thing '{}': {}", newThing.getUID(),
                    e.getValidationMessages(null));
        }
        if (thingUpdatedLock.contains(thingUID)) {
            // called from the thing handler itself or during thing structure update, therefore it exists
            // and either is initializing/initialized and must not be informed (in order to prevent infinite loops)
            // or will be initialized after the update is done by the thing type update method itself
            replaceThing(oldThing, newThing);
        } else {
            Lock lock = getLockForThing(newThing.getUID());
            lock.lock();
            try {
                ThingHandler thingHandler = replaceThing(oldThing, newThing);
                if (thingHandler != null) {
                    if (ThingHandlerHelper.isHandlerInitialized(newThing)
                            || newThing.getStatus() == ThingStatus.INITIALIZING) {
                        oldThing.setHandler(null);
                        newThing.setHandler(thingHandler);

                        try {
                            validate(newThing, thingTypeRegistry.getThingType(newThing.getThingTypeUID()));
                            safeCaller.create(thingHandler, ThingHandler.class).build().thingUpdated(newThing);
                        } catch (ConfigValidationException e) {
                            final ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(
                                    newThing.getThingTypeUID());
                            if (thingHandlerFactory != null) {
                                if (newThing instanceof Bridge bridge) {
                                    unregisterAndDisposeChildHandlers(bridge, thingHandlerFactory);
                                }
                                disposeHandler(newThing, thingHandler);
                                setThingStatus(newThing,
                                        buildStatusInfo(ThingStatus.UNINITIALIZED,
                                                ThingStatusDetail.HANDLER_CONFIGURATION_PENDING,
                                                e.getValidationMessages(null).toString()));
                            }
                        }
                    } else {
                        logger.debug(
                                "Cannot notify handler about updated thing '{}', because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE).",
                                newThing.getThingTypeUID());
                        if (thingHandler.getThing() == newThing) {
                            logger.debug("Initializing handler of thing '{}'", newThing.getThingTypeUID());
                            oldThing.setHandler(null);
                            newThing.setHandler(thingHandler);
                            initializeHandler(newThing);
                        } else {
                            logger.debug("Replacing uninitialized handler for updated thing '{}'",
                                    newThing.getThingTypeUID());
                            ThingHandlerFactory thingHandlerFactory = getThingHandlerFactory(newThing);
                            if (thingHandlerFactory != null) {
                                unregisterHandler(thingHandler.getThing(), thingHandlerFactory);
                            } else {
                                logger.debug("No ThingHandlerFactory available that can handle {}",
                                        newThing.getThingTypeUID());
                            }
                            registerAndInitializeHandler(newThing, thingHandlerFactory);
                        }
                    }
                } else {
                    registerAndInitializeHandler(newThing, getThingHandlerFactory(newThing));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private @Nullable ThingHandler replaceThing(Thing oldThing, Thing newThing) {
        final ThingHandler thingHandler = thingHandlers.get(newThing.getUID());
        if (oldThing != newThing) {
            if (!oldThing.equals(things.remove(oldThing.getUID()))) {
                logger.error("Thing '{}' is different from thing tracked by ThingManager. This is a bug.",
                        oldThing.getUID());
            }
            things.put(newThing.getUID(), newThing);
        }
        return thingHandler;
    }

    private @Nullable ThingHandlerFactory findThingHandlerFactory(ThingTypeUID thingTypeUID) {
        return thingHandlerFactories.stream().filter(factory -> factory.supportsThingType(thingTypeUID)).findFirst()
                .orElse(null);
    }

    private void registerHandler(Thing thing, ThingHandlerFactory thingHandlerFactory) {
        Lock lock = getLockForThing(thing.getUID());
        lock.lock();
        try {
            if (isHandlerRegistered(thing)) {
                logger.debug("Attempt to register a handler twice for thing {} at the same time will be ignored.",
                        thing.getUID());
                return;
            }

            if (thing.getBridgeUID() == null) {
                doRegisterHandler(thing, thingHandlerFactory);
            } else {
                Bridge bridge = getBridge(thing.getBridgeUID());
                if (bridge == null || !ThingHandlerHelper.isHandlerInitialized(bridge)) {
                    setThingStatus(thing,
                            buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED));
                } else {
                    doRegisterHandler(thing, thingHandlerFactory);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void doRegisterHandler(final Thing thing, final ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Calling '{}.registerHandler()' for thing '{}'.", thingHandlerFactory.getClass().getSimpleName(),
                thing.getUID());
        try {
            ThingHandler thingHandler = thingHandlerFactory.registerHandler(thing);
            thingHandler.setCallback(thingHandlerCallback);
            thing.setHandler(thingHandler);
            thingHandlers.put(thing.getUID(), thingHandler);
            thingHandlersByFactory.computeIfAbsent(thingHandlerFactory, unused -> new HashSet<>()).add(thingHandler);
        } catch (Exception ex) {
            ThingStatusInfo statusInfo = buildStatusInfo(ThingStatus.UNINITIALIZED,
                    ThingStatusDetail.HANDLER_REGISTERING_ERROR,
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            setThingStatus(thing, statusInfo);
            logger.error("Exception occurred while calling thing handler factory '{}': {}", thingHandlerFactory,
                    ex.getMessage(), ex);
        }
    }

    protected void registerChildHandlers(final Bridge bridge) {
        for (final Thing child : bridge.getThings()) {
            logger.debug("Register and initialize child '{}' of bridge '{}'.", child.getUID(), bridge.getUID());
            scheduler.execute(() -> {
                try {
                    registerAndInitializeHandler(child, getThingHandlerFactory(child));
                } catch (Exception ex) {
                    logger.error("Registration resp. initialization of child '{}' of bridge '{}' has been failed: {}",
                            child.getUID(), bridge.getUID(), ex.getMessage(), ex);
                }
            });
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    protected void initializeHandler(Thing thing) {
        ThingUID thingUID = thing.getUID();
        if (disabledStorage.containsKey(thingUID.getAsString())) {
            setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.DISABLED));
            logger.debug("Thing '{}' will not be initialized. It is marked as disabled.", thing.getUID());
            return;
        }
        if (!isHandlerRegistered(thing)) {
            return;
        }

        Lock lock = getLockForThing(thing.getUID());
        lock.lock();
        try {
            if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                logger.debug("Attempt to initialize the already initialized thing '{}' will be ignored.",
                        thing.getUID());
                return;
            }
            if (thing.getStatus() == ThingStatus.INITIALIZING) {
                logger.debug("Attempt to initialize a handler twice for thing '{}' at the same time will be ignored.",
                        thing.getUID());
                return;
            }
            ThingHandler handler = thing.getHandler();
            if (handler == null) {
                throw new IllegalStateException("Handler should not be null here");
            }
            if (handler.getThing() != thing) {
                logger.warn("The model of {} is inconsistent [thing.getHandler().getThing() != thing]", thing.getUID());
            }
            ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
            if (thingType != null) {
                ThingFactoryHelper.applyDefaultConfiguration(thing.getConfiguration(), thingType,
                        configDescriptionRegistry);
            }

            try {
                validate(thing, thingType);

                if (ThingStatus.REMOVING.equals(thing.getStatus())) {
                    // preserve REMOVING state so the callback can later decide to remove the thing after it has been
                    // initialized
                    logger.debug("Not setting status to INITIALIZING because thing '{}' is in REMOVING status.",
                            thing.getUID());
                } else {
                    setThingStatus(thing, buildStatusInfo(ThingStatus.INITIALIZING, ThingStatusDetail.NONE));
                }
                doInitializeHandler(handler);
            } catch (ConfigValidationException e) {
                setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED,
                        ThingStatusDetail.HANDLER_CONFIGURATION_PENDING, e.getValidationMessages(null).toString()));
            }
        } finally {
            lock.unlock();
        }
    }

    private void validate(Thing thing, @Nullable ThingType thingType) throws ConfigValidationException {
        validate(thingType, thing.getUID(), thing.getConfiguration());

        // validate a bridge is set when it is mandatory
        if (thingType != null && thing.getBridgeUID() == null && !thingType.getSupportedBridgeTypeUIDs().isEmpty()) {
            ConfigValidationMessage message = new ConfigValidationMessage("bridge",
                    "Configuring a bridge is mandatory.", "bridge_not_configured");
            throw new ConfigValidationException(bundleContext.getBundle(), translationProvider, List.of(message));
        }

        for (Channel channel : thing.getChannels()) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
            validate(channelType, channel.getUID(), channel.getConfiguration());
        }
    }

    /**
     * Determines if all 'required' configuration parameters are available in the configuration
     *
     * @param prototype the "prototype", i.e. thing type or channel type
     * @param targetUID the UID of the thing or channel entity
     * @param configuration the current configuration
     * @throws ConfigValidationException if validation failed
     */
    private void validate(@Nullable AbstractDescriptionType prototype, UID targetUID, Configuration configuration)
            throws ConfigValidationException {
        if (prototype == null) {
            logger.debug("Prototype for '{}' is not known, assuming it can be initialized", targetUID);
            return;
        }

        URI configDescriptionURI = prototype.getConfigDescriptionURI();
        if (configDescriptionURI == null) {
            logger.debug("Config description URI for '{}' not found, assuming '{}' can be initialized",
                    prototype.getUID(), targetUID);
            return;
        }

        configDescriptionValidator.validate(configuration.getProperties(), configDescriptionURI);
    }

    private void normalizeThingConfiguration(Thing thing) throws ConfigValidationException {
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType == null) {
            logger.warn("Could not normalize configuration for '{}' because the thing type was not found in registry.",
                    thing.getUID());
            return;
        }
        normalizeConfiguration(thingType, thing.getUID(), thing.getConfiguration());

        for (Channel channel : thing.getChannels()) {
            ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
            if (channelTypeUID != null) {
                ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
                normalizeConfiguration(channelType, channel.getUID(), channel.getConfiguration());
            }
        }
    }

    private void normalizeConfiguration(@Nullable AbstractDescriptionType prototype, UID targetUID,
            Configuration configuration) throws ConfigValidationException {
        if (prototype == null) {
            ConfigValidationMessage message = new ConfigValidationMessage("thing/channel",
                    "Type description for '{0}' not found although we checked the presence before.",
                    "type_description_missing", targetUID.toString());
            throw new ConfigValidationException(bundleContext.getBundle(), translationProvider, List.of(message));
        }

        URI configDescriptionURI = prototype.getConfigDescriptionURI();
        if (configDescriptionURI == null) {
            logger.debug("Config description URI for '{}' not found, assuming '{}' is normalized", prototype.getUID(),
                    targetUID);
            return;
        }

        ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(configDescriptionURI);
        if (configDescription == null) {
            ConfigValidationMessage message = new ConfigValidationMessage("thing/channel",
                    "Config description for '{0}' not found also we checked the presence before.",
                    "config_description_missing", targetUID);
            throw new ConfigValidationException(bundleContext.getBundle(), translationProvider, List.of(message));
        }

        Objects.requireNonNull(ConfigUtil.normalizeTypes(configuration.getProperties(), List.of(configDescription)))
                .forEach(configuration::put);
    }

    private void doInitializeHandler(final ThingHandler thingHandler) {
        logger.debug("Calling initialize handler for thing '{}' at '{}'.", thingHandler.getThing().getUID(),
                thingHandler);
        safeCaller.create(thingHandler, ThingHandler.class)
                .onTimeout(() -> logger.warn("Initializing handler for thing '{}' takes more than {}ms.",
                        thingHandler.getThing().getUID(), SafeCaller.DEFAULT_TIMEOUT))
                .onException(e -> {
                    setThingStatus(thingHandler.getThing(), buildStatusInfo(ThingStatus.UNINITIALIZED,
                            ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage()));
                    logger.error("Exception occurred while initializing handler of thing '{}': {}",
                            thingHandler.getThing().getUID(), e.getMessage(), e);
                }).build().initialize();
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isHandlerRegistered(Thing thing) {
        ThingHandler handler = thingHandlers.get(thing.getUID());
        return handler != null && handler == thing.getHandler();
    }

    private @Nullable Bridge getBridge(@Nullable ThingUID bridgeUID) {
        if (bridgeUID == null) {
            return null;
        }
        Thing bridge = thingRegistry.get(bridgeUID);
        return bridge instanceof Bridge ? (Bridge) bridge : null;
    }

    private void unregisterHandler(Thing thing, ThingHandlerFactory thingHandlerFactory) {
        Lock lock = getLockForThing(thing.getUID());
        lock.lock();
        try {
            if (isHandlerRegistered(thing)) {
                safeCaller.create(() -> doUnregisterHandler(thing, thingHandlerFactory), Runnable.class).build().run();
            }
        } finally {
            lock.unlock();
        }
    }

    private void doUnregisterHandler(final Thing thing, final ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Calling unregisterHandler handler for thing '{}' at '{}'.", thing.getUID(), thingHandlerFactory);

        ThingHandler thingHandler = thing.getHandler();
        thingHandlerFactory.unregisterHandler(thing);
        if (thingHandler != null) {
            thingHandler.setCallback(null);
        }
        thing.setHandler(null);

        ThingUID thingUID = thing.getUID();
        boolean enabled = !disabledStorage.containsKey(thingUID.getAsString());

        ThingStatusDetail detail = enabled ? ThingStatusDetail.HANDLER_MISSING_ERROR : ThingStatusDetail.DISABLED;

        setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, detail));
        thingHandlers.remove(thing.getUID());
        synchronized (thingHandlersByFactory) {
            final Set<ThingHandler> thingHandlers = thingHandlersByFactory.get(thingHandlerFactory);
            if (thingHandlers != null) {
                thingHandlers.remove(thingHandler);
            }
        }
    }

    private void disposeHandler(Thing thing, ThingHandler thingHandler) {
        Lock lock = getLockForThing(thing.getUID());
        lock.lock();
        try {
            doDisposeHandler(thingHandler);
            if (thing.getBridgeUID() != null) {
                notifyBridgeAboutChildHandlerDisposal(thing, thingHandler);
            }
        } finally {
            lock.unlock();
        }
    }

    private void doDisposeHandler(final ThingHandler thingHandler) {
        logger.debug("Calling dispose handler for thing '{}' at '{}'.", thingHandler.getThing().getUID(), thingHandler);
        setThingStatus(thingHandler.getThing(), buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE));
        safeCaller.create(thingHandler, ThingHandler.class) //
                .onTimeout(() -> logger.warn("Disposing handler for thing '{}' takes more than {}ms.",
                        thingHandler.getThing().getUID(), SafeCaller.DEFAULT_TIMEOUT)) //
                .onException(e -> logger.error("Exception occurred while disposing handler of thing '{}': {}",
                        thingHandler.getThing().getUID(), e.getMessage(), e)) //
                .build().dispose();
    }

    private void unregisterAndDisposeChildHandlers(Bridge bridge, ThingHandlerFactory thingHandlerFactory) {
        ThingUID bridgeUID = bridge.getUID();
        thingRegistry.stream().filter(thing -> bridgeUID.equals(thing.getBridgeUID())).forEach(child -> {
            ThingHandler handler = child.getHandler();
            if (handler != null) {
                logger.debug("Unregister and dispose child '{}' of bridge '{}'.", child.getUID(), bridge.getUID());
                unregisterAndDisposeHandler(thingHandlerFactory, child, handler);
            }
        });
    }

    private void unregisterAndDisposeHandler(ThingHandlerFactory thingHandlerFactory, Thing thing,
            ThingHandler handler) {
        if (thing instanceof Bridge bridge) {
            unregisterAndDisposeChildHandlers(bridge, thingHandlerFactory);
        }
        disposeHandler(thing, handler);
        unregisterHandler(thing, thingHandlerFactory);
    }

    protected void notifyThingsAboutBridgeStatusChange(final Bridge bridge, final ThingStatusInfo bridgeStatus) {
        if (ThingHandlerHelper.isHandlerInitialized(bridge)) {
            for (final Thing child : bridge.getThings()) {
                scheduler.execute(() -> {
                    try {
                        ThingHandler handler = child.getHandler();
                        if (handler != null && ThingHandlerHelper.isHandlerInitialized(child)) {
                            handler.bridgeStatusChanged(bridgeStatus);
                        }
                    } catch (Exception e) {
                        logger.error(
                                "Exception occurred during notification about bridge status change on thing '{}': {}",
                                child.getUID(), e.getMessage(), e);
                    }
                });
            }
        }
    }

    protected void notifyBridgeAboutChildHandlerInitialization(final Thing thing) {
        final Bridge bridge = getBridge(thing.getBridgeUID());
        if (bridge != null) {
            scheduler.execute(() -> {
                try {
                    BridgeHandler bridgeHandler = bridge.getHandler();
                    if (bridgeHandler != null) {
                        ThingHandler thingHandler = thing.getHandler();
                        if (thingHandler != null) {
                            bridgeHandler.childHandlerInitialized(thingHandler, thing);
                        }
                    }
                } catch (Exception e) {
                    logger.error(
                            "Exception occurred during bridge handler ('{}') notification about handler initialization of child '{}': {}",
                            bridge.getUID(), thing.getUID(), e.getMessage(), e);
                }
            });
        }
    }

    private void notifyBridgeAboutChildHandlerDisposal(final Thing thing, final ThingHandler thingHandler) {
        final Bridge bridge = getBridge(thing.getBridgeUID());
        if (bridge != null) {
            ThreadPoolManager.getPool(THING_MANAGER_THREAD_POOL_NAME).execute(() -> {
                try {
                    BridgeHandler bridgeHandler = bridge.getHandler();
                    if (bridgeHandler != null) {
                        bridgeHandler.childHandlerDisposed(thingHandler, thing);
                    }
                } catch (Exception ex) {
                    logger.error(
                            "Exception occurred during bridge handler ('{}') notification about handler disposal of child '{}': {}",
                            bridge.getUID(), thing.getUID(), ex.getMessage(), ex);
                }
            });
        }
    }

    protected void notifyThingHandlerAboutRemoval(final Thing thing) {
        logger.trace("Asking handler of thing '{}' to handle its removal.", thing.getUID());

        ThreadPoolManager.getPool(THING_MANAGER_THREAD_POOL_NAME).execute(() -> {
            try {
                ThingHandler handler = thing.getHandler();
                if (handler != null) {
                    handler.handleRemoval();
                    logger.trace("Handler of thing '{}' returned from handling its removal.", thing.getUID());
                } else {
                    logger.trace("No handler of thing '{}' available, so deferring the removal call.", thing.getUID());
                }
            } catch (Exception ex) {
                logger.error("The ThingHandler caused an exception while handling the removal of its thing", ex);
            }
        });
    }

    protected void notifyRegistryAboutForceRemove(final Thing thing) {
        logger.debug("Removal handling of thing '{}' completed. Going to remove it now.", thing.getUID());

        // call asynchronous to avoid deadlocks in thing handler
        ThreadPoolManager.getPool(FORCE_REMOVE_THREAD_POOL_NAME).execute(() -> {
            try {
                thingRegistry.forceRemove(thing.getUID());
            } catch (IllegalStateException ex) {
                logger.debug("Could not remove thing {}. Most likely because it is not managed.", thing.getUID(), ex);
            } catch (Exception ex) {
                logger.error(
                        "Could not remove thing {}, because an unknown Exception occurred. Most likely because it is not managed.",
                        thing.getUID(), ex);
            }
        });
    }

    private void registerAndInitializeHandler(final Thing thing,
            final @Nullable ThingHandlerFactory thingHandlerFactory) {
        ThingUID thingUID = thing.getUID();
        if (disabledStorage.containsKey(thingUID.getAsString())) {
            logger.debug("Not registering a handler at this point. Thing is disabled.");
            setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.DISABLED));
        } else {
            if (thingHandlerFactory != null) {
                if (!missingPrerequisites.containsKey(thing.getUID())) {
                    if (thingRegistry.getProvider(thing) instanceof ManagedProvider
                            && checkAndPerformUpdate(thing, thingHandlerFactory)) {
                        return;
                    }
                    try {
                        normalizeThingConfiguration(thing);
                    } catch (ConfigValidationException e) {
                        logger.warn("Failed to normalize configuration for thing '{}': {}", thing.getUID(),
                                e.getValidationMessages(null));
                    }
                    registerHandler(thing, thingHandlerFactory);
                    initializeHandler(thing);
                } else {
                    setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.NOT_YET_READY));
                    logger.debug(
                            "Not registering a handler at this point. The thing type '{}' is not fully loaded yet.",
                            thing.getThingTypeUID());
                }
            } else {
                setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED,
                        ThingStatusDetail.HANDLER_MISSING_ERROR, "Handler factory not found"));
                logger.debug("Not registering a handler at this point. No handler factory for thing '{}' found.",
                        thing.getUID());
            }
        }
    }

    private @Nullable ThingHandlerFactory getThingHandlerFactory(Thing thing) {
        ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
        if (thingHandlerFactory != null) {
            return thingHandlerFactory;
        }
        logger.debug("Not registering a handler at this point since no handler factory for thing '{}' found.",
                thing.getUID());
        return null;
    }

    private Lock getLockForThing(ThingUID thingUID) {
        return Objects.requireNonNull(thingLocks.computeIfAbsent(thingUID, k -> new ReentrantLock()));
    }

    private ThingStatusInfo buildStatusInfo(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail,
            @Nullable String description) {
        ThingStatusInfoBuilder statusInfoBuilder = ThingStatusInfoBuilder.create(thingStatus, thingStatusDetail);
        statusInfoBuilder.withDescription(description);
        return statusInfoBuilder.build();
    }

    private ThingStatusInfo buildStatusInfo(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail) {
        return buildStatusInfo(thingStatus, thingStatusDetail, null);
    }

    protected void setThingStatus(Thing thing, ThingStatusInfo thingStatusInfo) {
        ThingStatusInfo oldStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, null);
        thing.setStatusInfo(thingStatusInfo);
        ThingStatusInfo newStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, null);
        try {
            eventPublisher.post(ThingEventFactory.createStatusInfoEvent(thing.getUID(), newStatusInfo));
            if (!oldStatusInfo.equals(newStatusInfo)) {
                eventPublisher.post(
                        ThingEventFactory.createStatusInfoChangedEvent(thing.getUID(), newStatusInfo, oldStatusInfo));
            }
        } catch (Exception ex) {
            logger.error("Could not post 'ThingStatusInfoEvent' event: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void setEnabled(ThingUID thingUID, boolean enabled) {
        Thing thing = things.get(thingUID);

        persistThingEnableStatus(thingUID, enabled);

        if (thing == null) {
            logger.debug("Thing with the UID {} is unknown, cannot set its enabled status.", thingUID);
            return;
        }

        if (enabled) {
            // Enable a thing
            if (thing.isEnabled()) {
                logger.debug("Thing {} is already enabled.", thingUID);
                return;
            }

            logger.debug("Thing {} will be enabled.", thingUID);

            if (isHandlerRegistered(thing)) {
                // A handler is already registered for that thing. Try to initialize it.
                initializeHandler(thing);
            } else {
                // No handler registered. Try to register handler and initialize the thing.
                registerAndInitializeHandler(thing, findThingHandlerFactory(thing.getThingTypeUID()));
                // Check if registration was successful
                if (!isHandlerRegistered(thing)) {
                    setThingStatus(thing,
                            buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_MISSING_ERROR));
                }
            }
        } else {
            if (!thing.isEnabled()) {
                logger.debug("Thing {} is already disabled.", thingUID);
                return;
            }

            logger.debug("Thing {} will be disabled.", thingUID);

            boolean disposed = false;

            if (isHandlerRegistered(thing)) {
                // Dispose handler if registered.
                ThingHandler thingHandler = thing.getHandler();
                ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
                if (thingHandler != null && thingHandlerFactory != null) {
                    unregisterAndDisposeHandler(thingHandlerFactory, thing, thingHandler);
                    disposed = true;
                }
            }

            if (!disposed) {
                // Only set the correct status to the thing. There is no handler to be disposed
                setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.DISABLED));
            }

            if (thing instanceof Bridge bridge) {
                updateChildThingStatusForDisabledBridges(bridge);
            }
        }
    }

    private void updateChildThingStatusForDisabledBridges(Bridge bridge) {
        for (Thing childThing : bridge.getThings()) {
            ThingStatusDetail statusDetail = childThing.getStatusInfo().getStatusDetail();
            if (childThing.getStatus() == ThingStatus.UNINITIALIZED && statusDetail != ThingStatusDetail.DISABLED) {
                setThingStatus(childThing,
                        buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED));
            }
        }
    }

    private void persistThingEnableStatus(ThingUID thingUID, boolean enabled) {
        logger.debug("Thing with UID {} will be persisted as {}.", thingUID, enabled ? "enabled." : "disabled.");
        if (enabled) {
            // Clear the disabled thing storage. Otherwise, the handler will NOT be initialized later.
            disabledStorage.remove(thingUID.getAsString());
        } else {
            // Mark the thing as disabled in the storage.
            disabledStorage.put(thingUID.getAsString(), "");
        }
    }

    @Override
    public boolean isEnabled(ThingUID thingUID) {
        Thing thing = things.get(thingUID);
        if (thing != null) {
            return thing.isEnabled();
        }

        logger.debug("Thing with UID {} is unknown. Will try to get the enabled status from the persistent storage.",
                thingUID);
        return !disabledStorage.containsKey(thingUID.getAsString());
    }

    private boolean checkAndPerformUpdate(Thing thing, ThingHandlerFactory factory) {
        final int currentThingTypeVersion = Integer
                .parseInt(thing.getProperties().getOrDefault(PROPERTY_THING_TYPE_VERSION, "0"));

        UpdateInstructionKey thingKey = new UpdateInstructionKey(factory, thing.getThingTypeUID());
        List<ThingUpdateInstruction> instructions = updateInstructions.getOrDefault(thingKey, List.of()).stream()
                .filter(ThingUpdateInstruction.applies(currentThingTypeVersion)).toList();

        if (instructions.isEmpty()) {
            return false;
        }

        // create a thing builder and apply the update instructions
        ThingBuilder thingBuilder = ThingBuilder.create(thing);
        instructions.forEach(instruction -> instruction.perform(thing, thingBuilder));
        int newThingTypeVersion = instructions.get(instructions.size() - 1).getThingTypeVersion();
        thingBuilder.withProperty(PROPERTY_THING_TYPE_VERSION, String.valueOf(newThingTypeVersion));
        logger.info("Updating '{}' from version {} to {}", thing.getUID(), currentThingTypeVersion,
                newThingTypeVersion);

        Thing newThing = thingBuilder.build();
        thingUpdated(newThing);

        ThingPrerequisites thingPrerequisites = new ThingPrerequisites(newThing);
        if (!thingPrerequisites.isReady()) {
            missingPrerequisites.put(newThing.getUID(), thingPrerequisites);
        }

        registerAndInitializeHandler(newThing, getThingHandlerFactory(newThing));

        return true;
    }

    private void checkMissingPrerequisites() {
        Iterator<ThingPrerequisites> it = missingPrerequisites.values().iterator();
        while (it.hasNext()) {
            ThingPrerequisites prerequisites = it.next();
            try {
                if (prerequisites.isReady()) {
                    it.remove();
                    Thing thing = things.get(prerequisites.thingUID);
                    if (thing != null) {
                        if (!isHandlerRegistered(thing)) {
                            registerAndInitializeHandler(thing, getThingHandlerFactory(thing));
                        } else {
                            logger.warn(
                                    "Handler of thing '{}' already registered even though not all prerequisites were met.",
                                    thing.getUID());
                        }
                    } else {
                        logger.warn("Found missing thing while checking prerequisites of thing '{}'",
                                prerequisites.thingUID);
                    }
                }
            } catch (RuntimeException e) {
                logger.error("Checking/initializing thing '{}' failed unexpectedly.", prerequisites.thingUID, e);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Thing handler factory '{}' added", thingHandlerFactory.getClass().getSimpleName());
        updateInstructions.putAll(thingUpdateInstructionReader.readForFactory(thingHandlerFactory));
        thingHandlerFactories.add(thingHandlerFactory);
        things.values().stream().filter(thing -> thingHandlerFactory.supportsThingType(thing.getThingTypeUID()))
                .forEach(thing -> {
                    if (!isHandlerRegistered(thing)) {
                        ThingPrerequisites thingPrerequisites = new ThingPrerequisites(thing);
                        if (!thingPrerequisites.isReady()) {
                            missingPrerequisites.put(thing.getUID(), thingPrerequisites);
                        }
                        registerAndInitializeHandler(thing, thingHandlerFactory);
                    } else {
                        logger.debug("Thing handler for thing '{}' already registered", thing.getUID());
                    }
                });
    }

    protected synchronized void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Thing handler factory '{}' removed", thingHandlerFactory.getClass().getSimpleName());
        thingHandlerFactories.remove(thingHandlerFactory);
        final Set<ThingHandler> handlers = thingHandlersByFactory.remove(thingHandlerFactory);
        if (handlers != null) {
            for (ThingHandler thingHandler : handlers) {
                final Thing thing = thingHandler.getThing();
                if (isHandlerRegistered(thing)) {
                    unregisterAndDisposeHandler(thingHandlerFactory, thing, thingHandler);
                }
            }
        }
        updateInstructions.keySet().removeIf(key -> thingHandlerFactory.equals(key.factory()));
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        startLevelSetterJob = scheduler.scheduleWithFixedDelay(() -> {
            if (things.values().stream().anyMatch(t -> !ThingHandlerHelper.isHandlerInitialized(t) && t.isEnabled())) {
                return;
            }
            readyService.markReady(READY_MARKER_THINGS_LOADED);
            if (startLevelSetterJob != null) {
                startLevelSetterJob.cancel(false);
            }
            readyService.unregisterTracker(this);
        }, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
        prerequisiteCheckerJob = scheduler.scheduleWithFixedDelay(this::checkMissingPrerequisites, CHECK_INTERVAL,
                CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        // do nothing
    }

    /**
     * The {@link ThingPrerequisites} class is used to gather and check the pre-requisites of a given thing (i.e.
     * availability of the {@link ThingType} and all needed {@link ChannelType}s and {@link ConfigDescription}s).
     *
     */
    private class ThingPrerequisites {
        private final ThingUID thingUID;
        private @Nullable ThingTypeUID thingTypeUID;
        private final Set<ChannelTypeUID> channelTypeUIDs = new HashSet<>();
        private final Set<URI> configDescriptionUris = new HashSet<>();
        private int timesChecked = 0;

        public ThingPrerequisites(Thing thing) {
            thingUID = thing.getUID();
            thingTypeUID = thing.getThingTypeUID();
            thing.getChannels().stream().map(Channel::getChannelTypeUID).filter(Objects::nonNull)
                    .map(Objects::requireNonNull).distinct().forEach(channelTypeUIDs::add);
        }

        /**
         * Check if all necessary information is present in the registries.
         * <p />
         * If a {@link ThingHandlerFactory} reports that it supports {@link ThingTypeUID} but the {@link ThingType}
         * can't be found in the {@link ThingTypeRegistry} this method also returns <code>true</code> after
         * {@link #MAX_CHECK_PREREQUISITE_TIME} s.
         *
         * @return <code>true</code> if all pre-requisites are present, <code>false</code> otherwise
         */
        public synchronized boolean isReady() {
            ThingTypeUID thingTypeUID = this.thingTypeUID;
            // thing-type
            if (thingTypeUID != null) {
                ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
                if (thingType != null) {
                    this.thingTypeUID = null;
                    URI configDescriptionUri = thingType.getConfigDescriptionURI();
                    if (configDescriptionUri != null) {
                        configDescriptionUris.add(configDescriptionUri);
                    }
                } else if (thingHandlerFactories.stream().anyMatch(f -> f.supportsThingType(thingTypeUID))) {
                    timesChecked++;
                    if (timesChecked > MAX_CHECK_PREREQUISITE_TIME / CHECK_INTERVAL) {
                        logger.warn(
                                "A thing handler factory claims to support '{}' for thing '{}' for more than {}s, but the thing type can't be found in the registry. This should be fixed in the binding.",
                                thingTypeUID, thingUID, MAX_CHECK_PREREQUISITE_TIME);
                        this.thingTypeUID = null;
                    }
                }
            }

            // channel types
            Iterator<ChannelTypeUID> it = channelTypeUIDs.iterator();
            while (it.hasNext()) {
                ChannelType channelType = channelTypeRegistry.getChannelType(it.next());
                if (channelType != null) {
                    it.remove();
                    URI configDescriptionUri = channelType.getConfigDescriptionURI();
                    if (configDescriptionUri != null) {
                        configDescriptionUris.add(configDescriptionUri);
                    }
                }
            }

            // config descriptions
            configDescriptionUris.removeIf(uri -> configDescriptionRegistry.getConfigDescription(uri) != null);

            // timeout
            if (thingTypeUID == null && (!channelTypeUIDs.isEmpty() || !configDescriptionUris.isEmpty())) {
                // the thing type is present, so most likely the bundle is fully initialized
                // if channel types or config descriptions are missing increase the timeout counter
                timesChecked++;
                if (timesChecked > MAX_CHECK_PREREQUISITE_TIME / CHECK_INTERVAL) {
                    logger.warn(
                            "Channel types or config descriptions for thing '{}' are missing in the respective registry for more than {}s. This should be fixed in the binding.",
                            thingUID, MAX_CHECK_PREREQUISITE_TIME);
                    channelTypeUIDs.clear();
                    configDescriptionUris.clear();
                }
            }

            boolean isReady = this.thingTypeUID == null && channelTypeUIDs.isEmpty() && configDescriptionUris.isEmpty();
            if (!isReady) {
                logger.debug("Check result is 'not ready': {}", this);
            }
            return isReady;
        }

        @Override
        public String toString() {
            return "ThingPrerequisites{thingUID=" + thingUID + ", thingTypeUID=" + thingTypeUID + ", channelTypeUIDs="
                    + channelTypeUIDs + ", configDescriptionUris=" + configDescriptionUris + "}";
        }
    }
}
