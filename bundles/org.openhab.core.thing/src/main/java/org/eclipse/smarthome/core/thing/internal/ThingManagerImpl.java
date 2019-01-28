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
package org.eclipse.smarthome.core.thing.internal;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.validation.ConfigDescriptionValidator;
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.eclipse.smarthome.core.common.registry.ManagedProvider;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.service.ReadyMarker;
import org.eclipse.smarthome.core.service.ReadyMarkerFilter;
import org.eclipse.smarthome.core.service.ReadyService;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelGroupUID;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingManager;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeMigrationService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.eclipse.smarthome.core.thing.events.ThingEventFactory;
import org.eclipse.smarthome.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.eclipse.smarthome.core.thing.util.ThingHandlerHelper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.util.BundleResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ThingManagerImpl} tracks all things in the {@link ThingRegistry} and
 * mediates the communication between the {@link Thing} and the {@link ThingHandler} from the binding. Therefore it
 * tracks {@link ThingHandlerFactory}s and calls {@link ThingHandlerFactory#registerHandler(Thing)} for each thing, that
 * was added to the {@link ThingRegistry}. In addition the {@link ThingManagerImpl} acts
 * as an {@link EventHandler} and subscribes to smarthome update and command
 * events. Finally the {@link ThingManagerImpl} implement the {@link ThingTypeMigrationService} to offer
 * a way to change the thing type of a {@link Thing}.
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
 */

@Component(immediate = true, service = { ThingTypeMigrationService.class, ThingManager.class })
public class ThingManagerImpl
        implements ThingManager, ThingTracker, ThingTypeMigrationService, ReadyService.ReadyTracker {

    static final String XML_THING_TYPE = "esh.xmlThingTypes";

    private static final String THING_STATUS_STORAGE_NAME = "thing_status_storage";
    private static final String FORCEREMOVE_THREADPOOL_NAME = "forceRemove";
    private static final String THING_MANAGER_THREADPOOL_NAME = "thingManager";

    private final Logger logger = LoggerFactory.getLogger(ThingManagerImpl.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(THING_MANAGER_THREADPOOL_NAME);

    private EventPublisher eventPublisher;

    private CommunicationManager communicationManager;

    private ReadyService readyService;

    private final List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<>();

    private final Map<ThingUID, ThingHandler> thingHandlers = new ConcurrentHashMap<>();

    private final Map<ThingHandlerFactory, Set<ThingHandler>> thingHandlersByFactory = new HashMap<>();

    private ThingTypeRegistry thingTypeRegistry;
    private ChannelTypeRegistry channelTypeRegistry;
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;

    private ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;

    private final Map<ThingUID, Lock> thingLocks = new HashMap<>();
    private final Set<String> loadedXmlThingTypes = new CopyOnWriteArraySet<>();
    private SafeCaller safeCaller;
    private volatile boolean active = false;
    private StorageService storageService;
    private Storage<String> storage;

    private final ThingHandlerCallback thingHandlerCallback = new ThingHandlerCallback() {

        @Override
        public void stateUpdated(ChannelUID channelUID, State state) {
            communicationManager.stateUpdated(channelUID, state);
        }

        @Override
        public void postCommand(ChannelUID channelUID, Command command) {
            communicationManager.postCommand(channelUID, command);
        }

        @Override
        public void channelTriggered(Thing thing, ChannelUID channelUID, String event) {
            communicationManager.channelTriggered(thing, channelUID, event);
        }

        @Override
        public void statusUpdated(Thing thing, ThingStatusInfo statusInfo) {
            // note: all provoked operations based on a status update should be executed asynchronously!
            ThingStatusInfo oldStatusInfo = thing.getStatusInfo();
            ensureValidStatus(oldStatusInfo.getStatus(), statusInfo.getStatus());

            if (ThingStatus.REMOVING.equals(oldStatusInfo.getStatus())
                    && !ThingStatus.REMOVED.equals(statusInfo.getStatus())) {
                // only allow REMOVING -> REMOVED transition, all others are illegal
                throw new IllegalArgumentException(MessageFormat.format(
                        "Illegal status transition from REMOVING to {0}, only REMOVED would have been allowed.",
                        statusInfo.getStatus()));
            }

            // update thing status and send event about new status
            setThingStatus(thing, statusInfo);

            // if thing is a bridge
            if (isBridge(thing)) {
                handleBridgeStatusUpdate((Bridge) thing, statusInfo, oldStatusInfo);
            }
            // if thing has a bridge
            if (hasBridge(thing)) {
                handleBridgeChildStatusUpdate(thing, oldStatusInfo);
            }
            // notify thing registry about thing removal
            if (ThingStatus.REMOVED.equals(thing.getStatus())) {
                notifyRegistryAboutForceRemove(thing);
            }
        }

        private void ensureValidStatus(ThingStatus oldStatus, ThingStatus newStatus) {
            if (!(ThingStatus.UNKNOWN.equals(newStatus) || ThingStatus.ONLINE.equals(newStatus)
                    || ThingStatus.OFFLINE.equals(newStatus) || ThingStatus.REMOVED.equals(newStatus))) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Illegal status {0}. Bindings only may set {1}, {2}, {3} or {4}.", newStatus,
                        ThingStatus.UNKNOWN, ThingStatus.ONLINE, ThingStatus.OFFLINE, ThingStatus.REMOVED));
            }
            if (ThingStatus.REMOVED.equals(newStatus) && !ThingStatus.REMOVING.equals(oldStatus)) {
                throw new IllegalArgumentException(
                        MessageFormat.format("Illegal status {0}. The thing was in state {1} and not in {2}", newStatus,
                                oldStatus, ThingStatus.REMOVING));
            }
        }

        private void handleBridgeStatusUpdate(Bridge bridge, ThingStatusInfo statusInfo,
                ThingStatusInfo oldStatusInfo) {
            if (ThingHandlerHelper.isHandlerInitialized(bridge)
                    && (ThingStatus.INITIALIZING.equals(oldStatusInfo.getStatus()))) {
                // bridge has just been initialized: initialize child things as well
                registerChildHandlers(bridge);
            } else if (!statusInfo.equals(oldStatusInfo)) {
                // bridge status has been changed: notify child things about status change
                notifyThingsAboutBridgeStatusChange(bridge, statusInfo);
            }
        }

        private void handleBridgeChildStatusUpdate(Thing thing, ThingStatusInfo oldStatusInfo) {
            if (ThingHandlerHelper.isHandlerInitialized(thing)
                    && ThingStatus.INITIALIZING.equals(oldStatusInfo.getStatus())) {
                // child thing has just been initialized: notify bridge about it
                notifyBridgeAboutChildHandlerInitialization(thing);
            }
        }

        @Override
        public void thingUpdated(final Thing thing) {
            thingUpdatedLock.add(thing.getUID());
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Provider<Thing> provider = thingRegistry.getProvider(thing);
                    if (provider == null) {
                        throw new IllegalArgumentException(MessageFormat.format(
                                "Provider for thing {0} cannot be determined because it is not known to the registry",
                                thing.getUID().getAsString()));
                    }
                    if (provider instanceof ManagedProvider) {
                        @SuppressWarnings("unchecked")
                        ManagedProvider<Thing, ThingUID> managedProvider = (ManagedProvider<Thing, ThingUID>) provider;
                        managedProvider.update(thing);
                    } else {
                        logger.debug("Only updating thing {} in the registry because provider {} is not managed.",
                                thing.getUID().getAsString(), provider);
                        thingRegistry.updated(provider, thingRegistry.get(thing.getUID()), thing);
                    }
                    return null;
                }
            });
            thingUpdatedLock.remove(thing.getUID());
        }

        @Override
        public void validateConfigurationParameters(Thing thing, Map<String, Object> configurationParameters) {
            ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
            if (thingType != null && thingType.getConfigDescriptionURI() != null) {
                configDescriptionValidator.validate(configurationParameters, thingType.getConfigDescriptionURI());
            }
        }

        @Override
        public void configurationUpdated(Thing thing) {
            if (!ThingHandlerHelper.isHandlerInitialized(thing)) {
                initializeHandler(thing);
            }
        }

        @Override
        public void migrateThingType(final Thing thing, final ThingTypeUID thingTypeUID,
                final Configuration configuration) {
            ThingManagerImpl.this.migrateThingType(thing, thingTypeUID, configuration);
        }

        @Override
        public ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelTypeUID channelTypeUID) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
            if (channelType == null) {
                throw new IllegalArgumentException(String.format("Channel type '%s' is not known", channelTypeUID));
            }
            return ThingFactoryHelper.createChannelBuilder(channelUID, channelType, configDescriptionRegistry);
        };

        @Override
        public ChannelBuilder editChannel(Thing thing, ChannelUID channelUID) {
            Channel channel = thing.getChannel(channelUID.getId());
            if (channel == null) {
                throw new IllegalArgumentException(
                        String.format("Channel '%s' does not exist for thing '%s'", channelUID, thing.getUID()));
            }
            return ChannelBuilder.create(channel);
        }

        @Override
        public List<ChannelBuilder> createChannelBuilders(ChannelGroupUID channelGroupUID,
                ChannelGroupTypeUID channelGroupTypeUID) {
            ChannelGroupType channelGroupType = channelGroupTypeRegistry.getChannelGroupType(channelGroupTypeUID);
            if (channelGroupType == null) {
                throw new IllegalArgumentException(
                        String.format("Channel group type '%s' is not known", channelGroupTypeUID));
            }
            List<ChannelBuilder> channelBuilders = new ArrayList<>();
            for (ChannelDefinition channelDefinition : channelGroupType.getChannelDefinitions()) {
                ChannelType channelType = channelTypeRegistry.getChannelType(channelDefinition.getChannelTypeUID());
                if (channelType != null) {
                    ChannelUID channelUID = new ChannelUID(channelGroupUID, channelDefinition.getId());
                    channelBuilders.add(ThingFactoryHelper.createChannelBuilder(channelUID, channelType,
                            configDescriptionRegistry));
                }
            }
            return channelBuilders;
        }

        @Override
        public boolean isChannelLinked(ChannelUID channelUID) {
            return itemChannelLinkRegistry.isLinked(channelUID);
        }
    };

    private ThingRegistryImpl thingRegistry;

    private BundleResolver bundleResolver;

    private ConfigDescriptionRegistry configDescriptionRegistry;
    private ConfigDescriptionValidator configDescriptionValidator;

    private final Set<Thing> things = new CopyOnWriteArraySet<>();

    private final Set<ThingUID> registerHandlerLock = new HashSet<>();

    private final Set<ThingUID> thingUpdatedLock = new HashSet<>();

    @Override
    public void migrateThingType(final Thing thing, final ThingTypeUID thingTypeUID,
            final Configuration configuration) {
        final ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            throw new IllegalStateException(
                    MessageFormat.format("No thing type {0} registered, cannot change thing type for thing {1}",
                            thingTypeUID.getAsString(), thing.getUID().getAsString()));
        }
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Lock lock = getLockForThing(thing.getUID());
                try {
                    lock.lock();
                    ThingUID thingUID = thing.getUID();
                    waitForRunningHandlerRegistrations(thingUID);

                    // Remove the ThingHandler, if any
                    final ThingHandlerFactory oldThingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
                    if (oldThingHandlerFactory != null) {
                        ThingHandler thingHandler = thing.getHandler();
                        unregisterAndDisposeHandler(oldThingHandlerFactory, thing, thingHandler);
                        waitUntilHandlerUnregistered(thing, 60 * 1000);
                    } else {
                        logger.debug("No ThingHandlerFactory available that can handle {}", thing.getThingTypeUID());
                    }

                    // Set the new channels
                    List<Channel> channels = ThingFactoryHelper.createChannels(thingType, thingUID,
                            configDescriptionRegistry);
                    ((ThingImpl) thing).setChannels(channels);

                    // Set the given configuration
                    ThingFactoryHelper.applyDefaultConfiguration(configuration, thingType, configDescriptionRegistry);
                    ((ThingImpl) thing).setConfiguration(configuration);

                    // Set the new properties (keeping old properties, unless they have the same name as a new property)
                    for (Entry<String, String> entry : thingType.getProperties().entrySet()) {
                        ((ThingImpl) thing).setProperty(entry.getKey(), entry.getValue());
                    }

                    // Change the ThingType
                    ((ThingImpl) thing).setThingTypeUID(thingTypeUID);

                    // Register the new Handler - ThingManager.updateThing() is going to take care of that
                    thingRegistry.update(thing);

                    ThingHandler handler = thing.getHandler();
                    String handlerString = "NO HANDLER";
                    if (handler != null) {
                        handlerString = handler.toString();
                    }
                    logger.debug("Changed ThingType of Thing {} to {}. New ThingHandler is {}.",
                            thing.getUID().toString(), thing.getThingTypeUID(), handlerString);
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

            private void waitForRunningHandlerRegistrations(ThingUID thingUID) {
                for (int i = 0; i < 10 * 10; i++) {
                    if (!registerHandlerLock.contains(thingUID)) {
                        return;
                    }
                    try {
                        // Wait a little to give running handler registrations a chance to complete...
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                String message = MessageFormat.format(
                        "Thing type migration failed for {0}. Could not obtain lock for hander registration.",
                        thingUID.getAsString());
                logger.error(message);
                throw new IllegalStateException(message);
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void thingAdded(Thing thing, ThingTrackerEvent thingTrackerEvent) {
        this.things.add(thing);
        logger.debug("Thing '{}' is tracked by ThingManager.", thing.getUID());
        if (!isHandlerRegistered(thing)) {
            registerAndInitializeHandler(thing, getThingHandlerFactory(thing));
        } else {
            logger.debug("Handler of tracked thing '{}' already registered.", thing.getUID());
        }
    }

    @Override
    public void thingRemoving(Thing thing, ThingTrackerEvent thingTrackerEvent) {
        setThingStatus(thing, ThingStatusInfoBuilder.create(ThingStatus.REMOVING).build());
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

        storage.remove(thing.getUID().getAsString());
        this.things.remove(thing);
    }

    @Override
    public void thingUpdated(final Thing thing, ThingTrackerEvent thingTrackerEvent) {
        ThingUID thingUID = thing.getUID();
        if (thingUpdatedLock.contains(thingUID)) {
            // called from the thing handler itself, therefore
            // it exists, is initializing/initialized and
            // must not be informed (in order to prevent infinite loops)
            replaceThing(getThing(thingUID), thing);
        } else {
            Lock lock1 = getLockForThing(thing.getUID());
            try {
                lock1.lock();
                Thing oldThing = getThing(thingUID);
                ThingHandler thingHandler = replaceThing(oldThing, thing);
                if (thingHandler != null) {
                    if (ThingHandlerHelper.isHandlerInitialized(thing) || isInitializing(thing)) {
                        if (oldThing != null) {
                            oldThing.setHandler(null);
                        }
                        thing.setHandler(thingHandler);
                        safeCaller.create(thingHandler, ThingHandler.class).build().thingUpdated(thing);
                    } else {
                        logger.debug(
                                "Cannot notify handler about updated thing '{}', because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE).",
                                thing.getThingTypeUID());
                        if (thingHandler.getThing() == thing) {
                            logger.debug("Initializing handler of thing '{}'", thing.getThingTypeUID());
                            if (oldThing != null) {
                                oldThing.setHandler(null);
                            }
                            thing.setHandler(thingHandler);
                            initializeHandler(thing);
                        } else {
                            logger.debug("Replacing uninitialized handler for updated thing '{}'",
                                    thing.getThingTypeUID());
                            ThingHandlerFactory thingHandlerFactory = getThingHandlerFactory(thing);
                            unregisterHandler(thingHandler.getThing(), thingHandlerFactory);
                            registerAndInitializeHandler(thing, thingHandlerFactory);
                        }
                    }
                } else {
                    registerAndInitializeHandler(thing, getThingHandlerFactory(thing));
                }
            } finally {
                lock1.unlock();
            }
        }
    }

    private ThingHandler replaceThing(Thing oldThing, Thing newThing) {
        final ThingHandler thingHandler = thingHandlers.get(newThing.getUID());
        if (oldThing != newThing) {
            this.things.remove(oldThing);
            this.things.add(newThing);
        }
        return thingHandler;
    }

    private Thing getThing(ThingUID id) {
        for (Thing thing : this.things) {
            if (thing.getUID().equals(id)) {
                return thing;
            }
        }
        return null;
    }

    private ThingType getThingType(Thing thing) {
        return thingTypeRegistry.getThingType(thing.getThingTypeUID());
    }

    private ThingHandlerFactory findThingHandlerFactory(ThingTypeUID thingTypeUID) {
        for (ThingHandlerFactory factory : thingHandlerFactories) {
            if (factory.supportsThingType(thingTypeUID)) {
                return factory;
            }
        }
        return null;
    }

    private void registerHandler(Thing thing, ThingHandlerFactory thingHandlerFactory) {
        Lock lock = getLockForThing(thing.getUID());
        try {
            lock.lock();
            if (!isHandlerRegistered(thing)) {
                if (!hasBridge(thing)) {
                    doRegisterHandler(thing, thingHandlerFactory);
                } else {
                    Bridge bridge = getBridge(thing.getBridgeUID());
                    if (bridge != null && ThingHandlerHelper.isHandlerInitialized(bridge)) {
                        doRegisterHandler(thing, thingHandlerFactory);
                    } else {
                        setThingStatus(thing,
                                buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED));
                    }
                }
            } else {
                logger.debug("Attempt to register a handler twice for thing {} at the same time will be ignored.",
                        thing.getUID());
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
            thingHandler.setCallback(ThingManagerImpl.this.thingHandlerCallback);
            thing.setHandler(thingHandler);
            thingHandlers.put(thing.getUID(), thingHandler);
            synchronized (thingHandlersByFactory) {
                thingHandlersByFactory.computeIfAbsent(thingHandlerFactory, unused -> new HashSet<>())
                        .add(thingHandler);
            }
        } catch (Exception ex) {
            ThingStatusInfo statusInfo = buildStatusInfo(ThingStatus.UNINITIALIZED,
                    ThingStatusDetail.HANDLER_REGISTERING_ERROR,
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            setThingStatus(thing, statusInfo);
            logger.error("Exception occurred while calling thing handler factory '{}': {}", thingHandlerFactory,
                    ex.getMessage(), ex);
        }
    }

    private void registerChildHandlers(final Bridge bridge) {
        for (final Thing child : bridge.getThings()) {
            logger.debug("Register and initialize child '{}' of bridge '{}'.", child.getUID(), bridge.getUID());
            ThreadPoolManager.getPool(THING_MANAGER_THREADPOOL_NAME).execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        registerAndInitializeHandler(child, getThingHandlerFactory(child));
                    } catch (Exception ex) {
                        logger.error(
                                "Registration resp. initialization of child '{}' of bridge '{}' has been failed: {}",
                                child.getUID(), bridge.getUID(), ex.getMessage(), ex);
                    }
                }
            });
        }
    }

    private void initializeHandler(Thing thing) {
        if (isDisabledByStorage(thing.getUID())) {
            setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.DISABLED));
            logger.debug("Thing '{}' will not be initialized. It is marked as disabled.", thing.getUID());
            return;
        }
        if (!isHandlerRegistered(thing)) {
            return;
        }
        Lock lock = getLockForThing(thing.getUID());
        try {
            lock.lock();
            if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                logger.debug("Attempt to initialize the already initialized thing '{}' will be ignored.",
                        thing.getUID());
                return;
            }
            if (isInitializing(thing)) {
                logger.debug("Attempt to initialize a handler twice for thing '{}' at the same time will be ignored.",
                        thing.getUID());
                return;
            }
            ThingHandler handler = thing.getHandler();
            if (handler == null) {
                throw new IllegalStateException("Handler should not be null here");
            } else {
                if (handler.getThing() != thing) {
                    logger.warn("The model of {} is inconsistent [thing.getHandler().getThing() != thing]",
                            thing.getUID());
                }
            }
            ThingType thingType = getThingType(thing);
            applyDefaultConfiguration(thing, thingType);

            if (isInitializable(thing, thingType)) {
                setThingStatus(thing, buildStatusInfo(ThingStatus.INITIALIZING, ThingStatusDetail.NONE));
                doInitializeHandler(thing.getHandler());
            } else {
                logger.debug("Thing '{}' not initializable, check required configuration parameters.", thing.getUID());
                setThingStatus(thing,
                        buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_CONFIGURATION_PENDING));
            }
        } finally {
            lock.unlock();
        }
    }

    private void applyDefaultConfiguration(Thing thing, ThingType thingType) {
        if (thingType != null) {
            ThingFactoryHelper.applyDefaultConfiguration(thing.getConfiguration(), thingType,
                    configDescriptionRegistry);
        }
    }

    private boolean isInitializable(Thing thing, ThingType thingType) {
        if (!isComplete(thingType, thing.getUID(), tt -> tt.getConfigDescriptionURI(), thing.getConfiguration())) {
            return false;
        }

        for (Channel channel : thing.getChannels()) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
            if (!isComplete(channelType, channel.getUID(), ct -> ct.getConfigDescriptionURI(),
                    channel.getConfiguration())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines if all 'required' configuration parameters are available in the configuration
     *
     * @param prototype the "prototype", i.e. thing type or channel type
     * @param targetUID the UID of the thing or channel entity
     * @param configDescriptionURIFunction a function to determine the the config description UID for the given
     *            prototype
     * @param configuration the current configuration
     * @return true if all required configuration parameters are given, false otherwise
     */
    private <T extends Identifiable<?>> boolean isComplete(T prototype, UID targetUID,
            Function<T, URI> configDescriptionURIFunction, Configuration configuration) {
        if (prototype == null) {
            logger.debug("Prototype for '{}' is not known, assuming it is initializable", targetUID);
            return true;
        }

        ConfigDescription description = resolve(configDescriptionURIFunction.apply(prototype), null);
        if (description == null) {
            logger.debug("Config description for '{}' is not resolvable, assuming '{}' is initializable",
                    prototype.getUID(), targetUID);
            return true;
        }

        List<String> requiredParameters = getRequiredParameters(description);
        Set<String> propertyKeys = configuration.getProperties().keySet();
        if (logger.isDebugEnabled()) {
            logger.debug("Configuration of '{}' needs {}, has {}.", targetUID, requiredParameters, propertyKeys);
        }
        return propertyKeys.containsAll(requiredParameters);
    }

    private ConfigDescription resolve(URI configDescriptionURI, Locale locale) {
        if (configDescriptionURI == null) {
            return null;
        }

        return configDescriptionRegistry != null
                ? configDescriptionRegistry.getConfigDescription(configDescriptionURI, locale)
                : null;
    }

    private List<String> getRequiredParameters(ConfigDescription description) {
        List<String> requiredParameters = new ArrayList<>();
        for (ConfigDescriptionParameter param : description.getParameters()) {
            if (param.isRequired()) {
                requiredParameters.add(param.getName());
            }
        }
        return requiredParameters;
    }

    private void doInitializeHandler(final ThingHandler thingHandler) {
        logger.debug("Calling initialize handler for thing '{}' at '{}'.", thingHandler.getThing().getUID(),
                thingHandler);
        safeCaller.create(thingHandler, ThingHandler.class).onTimeout(() -> {
            logger.warn("Initializing handler for thing '{}' takes more than {}ms.", thingHandler.getThing().getUID(),
                    SafeCaller.DEFAULT_TIMEOUT);
        }).onException(e -> {
            ThingStatusInfo statusInfo = buildStatusInfo(ThingStatus.UNINITIALIZED,
                    ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage());
            setThingStatus(thingHandler.getThing(), statusInfo);
            logger.error("Exception occurred while initializing handler of thing '{}': {}",
                    thingHandler.getThing().getUID(), e.getMessage(), e);
        }).build().initialize();
    }

    private boolean isInitializing(Thing thing) {
        return thing.getStatus() == ThingStatus.INITIALIZING;
    }

    private boolean isHandlerRegistered(Thing thing) {
        ThingHandler handler = thingHandlers.get(thing.getUID());
        return handler != null && handler == thing.getHandler();
    }

    private boolean isBridge(Thing thing) {
        return thing instanceof Bridge;
    }

    private boolean hasBridge(final Thing thing) {
        return thing.getBridgeUID() != null;
    }

    private Bridge getBridge(ThingUID bridgeUID) {
        Thing bridge = thingRegistry.get(bridgeUID);
        return isBridge(bridge) ? (Bridge) bridge : null;
    }

    private void unregisterHandler(Thing thing, ThingHandlerFactory thingHandlerFactory) {
        Lock lock = getLockForThing(thing.getUID());
        try {
            lock.lock();
            if (isHandlerRegistered(thing)) {
                doUnregisterHandler(thing, thingHandlerFactory);
            }
        } finally {
            lock.unlock();
        }
    }

    private void doUnregisterHandler(final Thing thing, final ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Calling unregisterHandler handler for thing '{}' at '{}'.", thing.getUID(), thingHandlerFactory);
        safeCaller.create(() -> {
            ThingHandler thingHandler = thing.getHandler();
            thingHandlerFactory.unregisterHandler(thing);
            if (thingHandler != null) {
                thingHandler.setCallback(null);
            }
            thing.setHandler(null);

            boolean enabled = !isDisabledByStorage(thing.getUID());

            ThingStatusDetail detail = enabled ? ThingStatusDetail.HANDLER_MISSING_ERROR : ThingStatusDetail.DISABLED;

            setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, detail));
            thingHandlers.remove(thing.getUID());
            synchronized (thingHandlersByFactory) {
                final Set<ThingHandler> thingHandlers = thingHandlersByFactory.get(thingHandlerFactory);
                if (thingHandlers != null) {
                    thingHandlers.remove(thingHandler);
                    if (thingHandlers.isEmpty()) {
                        thingHandlersByFactory.remove(thingHandlerFactory);
                    }
                }
            }
        }, Runnable.class).build().run();
    }

    private void disposeHandler(Thing thing, ThingHandler thingHandler) {
        Lock lock = getLockForThing(thing.getUID());
        try {
            lock.lock();
            doDisposeHandler(thingHandler);
            if (hasBridge(thing)) {
                notifyBridgeAboutChildHandlerDisposal(thing, thingHandler);
            }
        } finally {
            lock.unlock();
        }
    }

    private void doDisposeHandler(final ThingHandler thingHandler) {
        logger.debug("Calling dispose handler for thing '{}' at '{}'.", thingHandler.getThing().getUID(), thingHandler);
        setThingStatus(thingHandler.getThing(), buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE));
        safeCaller.create(thingHandler, ThingHandler.class).onTimeout(() -> {
            logger.warn("Disposing handler for thing '{}' takes more than {}ms.", thingHandler.getThing().getUID(),
                    SafeCaller.DEFAULT_TIMEOUT);
        }).onException(e -> {
            logger.error("Exception occurred while disposing handler of thing '{}': {}",
                    thingHandler.getThing().getUID(), e.getMessage(), e);
        }).build().dispose();
    }

    private void unregisterAndDisposeChildHandlers(Bridge bridge, ThingHandlerFactory thingHandlerFactory) {
        addThingsToBridge(bridge);
        for (Thing child : bridge.getThings()) {
            ThingHandler handler = child.getHandler();
            if (handler != null) {
                logger.debug("Unregister and dispose child '{}' of bridge '{}'.", child.getUID(), bridge.getUID());
                unregisterAndDisposeHandler(thingHandlerFactory, child, handler);
            }
        }
    }

    private void unregisterAndDisposeHandler(ThingHandlerFactory thingHandlerFactory, Thing thing,
            ThingHandler handler) {
        if (isBridge(thing)) {
            unregisterAndDisposeChildHandlers((Bridge) thing, thingHandlerFactory);
        }
        disposeHandler(thing, handler);
        unregisterHandler(thing, thingHandlerFactory);
    }

    private void addThingsToBridge(Bridge bridge) {
        Collection<Thing> things = thingRegistry.getAll();
        for (Thing thing : things) {
            ThingUID bridgeUID = thing.getBridgeUID();
            if (bridgeUID != null && bridgeUID.equals(bridge.getUID())) {
                if (bridge instanceof BridgeImpl && !bridge.getThings().contains(thing)) {
                    ((BridgeImpl) bridge).addThing(thing);
                }
            }
        }
    }

    private void notifyThingsAboutBridgeStatusChange(final Bridge bridge, final ThingStatusInfo bridgeStatus) {
        if (ThingHandlerHelper.isHandlerInitialized(bridge)) {
            for (final Thing child : bridge.getThings()) {
                ThreadPoolManager.getPool(THING_MANAGER_THREADPOOL_NAME).execute(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }
        }
    }

    private void notifyBridgeAboutChildHandlerInitialization(final Thing thing) {
        final Bridge bridge = getBridge(thing.getBridgeUID());
        if (bridge != null) {
            ThreadPoolManager.getPool(THING_MANAGER_THREADPOOL_NAME).execute(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
    }

    private void notifyBridgeAboutChildHandlerDisposal(final Thing thing, final ThingHandler thingHandler) {
        final Bridge bridge = getBridge(thing.getBridgeUID());
        if (bridge != null) {
            ThreadPoolManager.getPool(THING_MANAGER_THREADPOOL_NAME).execute(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
    }

    private void notifyThingHandlerAboutRemoval(final Thing thing) {
        logger.trace("Asking handler of thing '{}' to handle its removal.", thing.getUID());

        ThreadPoolManager.getPool(THING_MANAGER_THREADPOOL_NAME).execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ThingHandler handler = thing.getHandler();
                    if (handler != null) {
                        handler.handleRemoval();
                        logger.trace("Handler of thing '{}' returned from handling its removal.", thing.getUID());
                    } else {
                        logger.trace("No handler of thing '{}' available, so deferring the removal call.",
                                thing.getUID());
                    }
                } catch (Exception ex) {
                    logger.error("The ThingHandler caused an exception while handling the removal of its thing", ex);
                }
            }
        });
    }

    private void notifyRegistryAboutForceRemove(final Thing thing) {
        logger.debug("Removal handling of thing '{}' completed. Going to remove it now.", thing.getUID());

        // call asynchronous to avoid deadlocks in thing handler
        ThreadPoolManager.getPool(FORCEREMOVE_THREADPOOL_NAME).execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            thingRegistry.forceRemove(thing.getUID());
                            return null;
                        }
                    });
                } catch (IllegalStateException ex) {
                    logger.debug("Could not remove thing {}. Most likely because it is not managed.", thing.getUID(),
                            ex);
                } catch (Exception ex) {
                    logger.error(
                            "Could not remove thing {}, because an unknwon Exception occurred. Most likely because it is not managed.",
                            thing.getUID(), ex);
                }
            }
        });
    }

    @Activate
    protected synchronized void activate(ComponentContext componentContext) {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(XML_THING_TYPE));
        for (ThingHandlerFactory factory : thingHandlerFactories) {
            handleThingHandlerFactoryAddition(getBundleName(factory));
        }
        thingRegistry.addThingTracker(this);
        active = true;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Thing handler factory '{}' added", thingHandlerFactory.getClass().getSimpleName());
        thingHandlerFactories.add(thingHandlerFactory);
        if (active) {
            handleThingHandlerFactoryAddition(getBundleName(thingHandlerFactory));
        }
    }

    @Reference
    public void setReadyService(ReadyService readyService) {
        this.readyService = readyService;
    }

    public void unsetReadyService(ReadyService readyService) {
        this.readyService = null;
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        String bsn = readyMarker.getIdentifier();
        loadedXmlThingTypes.add(bsn);
        handleThingHandlerFactoryAddition(bsn);
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        String bsn = readyMarker.getIdentifier();
        loadedXmlThingTypes.remove(bsn);
    }

    private void handleThingHandlerFactoryAddition(String bsn) {
        thingHandlerFactories.stream().filter(it -> {
            return getBundleName(it).equals(bsn);
        }).forEach(thingHandlerFactory -> {
            things.forEach(thing -> {
                if (thingHandlerFactory.supportsThingType(thing.getThingTypeUID())) {
                    if (!isHandlerRegistered(thing)) {
                        registerAndInitializeHandler(thing, thingHandlerFactory);
                    } else {
                        logger.debug("Thing handler for thing '{}' already registered", thing.getUID());
                    }
                }
            });
        });
    }

    private String getBundleName(ThingHandlerFactory thingHandlerFactory) {
        return bundleResolver.resolveBundle(thingHandlerFactory.getClass()).getSymbolicName();
    }

    private void registerAndInitializeHandler(final Thing thing, final ThingHandlerFactory thingHandlerFactory) {
        if (thingHandlerFactory != null) {
            String bsn = getBundleName(thingHandlerFactory);
            if (loadedXmlThingTypes.contains(bsn)) {
                registerHandler(thing, thingHandlerFactory);
                initializeHandler(thing);
            } else {
                logger.debug(
                        "Not registering a handler at this point. The thing types of bundle {} are not fully loaded yet.",
                        bsn);
            }
        } else {
            logger.debug("Not registering a handler at this point. No handler factory for thing '{}' found.",
                    thing.getUID());
        }
    }

    private ThingHandlerFactory getThingHandlerFactory(Thing thing) {
        ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
        if (thingHandlerFactory != null) {
            return thingHandlerFactory;
        }
        logger.debug("Not registering a handler at this point since no handler factory for thing '{}' found.",
                thing.getUID());
        return null;
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext componentContext) {
        active = false;
        thingRegistry.removeThingTracker(this);
        for (ThingHandlerFactory factory : thingHandlerFactories) {
            removeThingHandlerFactory(factory);
        }
        readyService.unregisterTracker(this);
    }

    protected synchronized void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("Thing handler factory '{}' removed", thingHandlerFactory.getClass().getSimpleName());
        thingHandlerFactories.remove(thingHandlerFactory);
        if (active) {
            handleThingHandlerFactoryRemoval(thingHandlerFactory);
        }
    }

    private void handleThingHandlerFactoryRemoval(ThingHandlerFactory thingHandlerFactory) {
        final Set<ThingHandler> handlers;
        synchronized (thingHandlersByFactory) {
            handlers = thingHandlersByFactory.remove(thingHandlerFactory);
        }
        if (handlers != null) {
            for (ThingHandler thingHandler : handlers) {
                final Thing thing = thingHandler.getThing();
                if (isHandlerRegistered(thing)) {
                    unregisterAndDisposeHandler(thingHandlerFactory, thing, thingHandler);
                }
            }
        }
    }

    private synchronized Lock getLockForThing(ThingUID thingUID) {
        if (thingLocks.get(thingUID) == null) {
            Lock lock = new ReentrantLock();
            thingLocks.put(thingUID, lock);
        }
        return thingLocks.get(thingUID);
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = (ThingRegistryImpl) thingRegistry;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = null;
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

    private ThingStatusInfo buildStatusInfo(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail,
            String description) {
        ThingStatusInfoBuilder statusInfoBuilder = ThingStatusInfoBuilder.create(thingStatus, thingStatusDetail);
        statusInfoBuilder.withDescription(description);
        return statusInfoBuilder.build();
    }

    private ThingStatusInfo buildStatusInfo(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail) {
        return buildStatusInfo(thingStatus, thingStatusDetail, null);
    }

    private void setThingStatus(Thing thing, ThingStatusInfo thingStatusInfo) {
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
        Thing thing = getThing(thingUID);

        persistThingEnableStatus(thingUID, enabled);

        if (thing == null) {
            logger.debug("Thing with the UID {} is unknown, cannot set its enabled status.", thingUID);
            return;
        }

        if (enabled) {
            // Enable a thing
            if (thing.getStatus().equals(ThingStatus.ONLINE)) {
                logger.debug("Thing {} is already in the required state.", thingUID);
                return;
            }

            logger.debug("Thing {} will be enabled.", thingUID);

            if (isHandlerRegistered(thing)) {
                // A handler is already registered for that thing. Try to initialize it.
                initializeHandler(thing);
            } else {
                // No handler registered. Try to register handler and initialize the thing.
                registerAndInitializeHandler(thing, findThingHandlerFactory(thing.getThingTypeUID()));
            }
        } else {
            if (!thing.isEnabled()) {
                logger.debug("Thing {} is already in the required state.", thingUID);
                return;
            }

            logger.debug("Thing {} will be disabled.", thingUID);

            if (isHandlerRegistered(thing)) {
                // Dispose handler if registered.
                ThingHandlerFactory thingHandlerFactory = findThingHandlerFactory(thing.getThingTypeUID());
                unregisterAndDisposeHandler(thingHandlerFactory, thing, thing.getHandler());
            } else {
                // Only set the correct status to the thing. There is no handler to be disposed
                setThingStatus(thing, buildStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.DISABLED));
            }

            if (isBridge(thing)) {
                updateChildThingStatusForDisabledBridges((Bridge) thing);
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
        if (storage == null) {
            logger.debug("Cannot persist enable status of thing with UID {}. Persistent storage unavailable.", thingUID);
            return;
        }

        logger.debug("Thing with UID {} will be persisted as {}.", thingUID, enabled ? "enabled." : "disabled.");
        if (enabled) {
            // Clear the disabled thing storage. Otherwise the handler will NOT be initialized later.
            storage.remove(thingUID.getAsString());
        } else {
            // Mark the thing as disabled in the storage.
            storage.put(thingUID.getAsString(), "");
        }
    }

    @Override
    public boolean isEnabled(ThingUID thingUID) {
        Thing thing = getThing(thingUID);
        if (thing != null) {
            return thing.isEnabled();
        }

        logger.debug("Thing with UID {} is unknown. Will try to get the enabled status from the persistent storage.");
        return !isDisabledByStorage(thingUID);
    }

    private boolean isDisabledByStorage(ThingUID thingUID) {
        return storage != null && storage.containsKey(thingUID.getAsString());
    }

    @Reference
    protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Reference
    protected void setChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
    }

    protected void unsetChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = null;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    protected void setThingStatusInfoI18nLocalizationService(
            ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService) {
        this.thingStatusInfoI18nLocalizationService = thingStatusInfoI18nLocalizationService;
    }

    protected void unsetThingStatusInfoI18nLocalizationService(
            ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService) {
        this.thingStatusInfoI18nLocalizationService = null;
    }

    @Reference
    protected void setInboundCommunication(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    protected void unsetInboundCommunication(CommunicationManager communicationManager) {
        this.communicationManager = null;
    }

    @Reference
    protected void setSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = safeCaller;
    }

    protected void unsetSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setStorageService(StorageService storageService) {
        if (this.storageService != storageService) {
            this.storageService = storageService;
            storage = storageService.getStorage(THING_STATUS_STORAGE_NAME, this.getClass().getClassLoader());
        }
    }

    protected void unsetStorageService(StorageService storageService) {
        if (this.storageService == storageService) {
            this.storageService = null;
            this.storage = null;
        }
    }
}
