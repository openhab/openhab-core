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
package org.openhab.core.config.discovery.internal;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.forThingUID;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.config.discovery.inbox.InboxListener;
import org.openhab.core.config.discovery.inbox.events.InboxEventFactory;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersistentInbox} class is a concrete implementation of the {@link Inbox}.
 * <p>
 * This implementation uses the {@link DiscoveryServiceRegistry} to register itself as {@link DiscoveryListener} to
 * receive {@link DiscoveryResult} objects automatically from {@link DiscoveryService}s.
 * <p>
 * This implementation does neither handle memory leaks (orphaned listener instances) nor blocked listeners. No
 * performance optimizations have been done (synchronization).
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Added automated removing of entries
 * @author Michael Grammling - Added dynamic configuration updates
 * @author Dennis Nobel - Added persistence support
 * @author Andre Fuechsel - Added removeOlderResults
 * @author Christoph Knauf - Added removeThingsForBridge and getPropsAndConfigParams
 * @author Laurent Garnier - Added parameter newThingId to method approve
 */
@Component(immediate = true, service = Inbox.class)
@NonNullByDefault
public final class PersistentInbox implements Inbox, DiscoveryListener, ThingRegistryChangeListener {

    // Internal enumeration to identify the correct type of the event to be fired.
    private enum EventType {
        ADDED,
        REMOVED,
        UPDATED
    }

    private class TimeToLiveCheckingThread implements Runnable {

        private final PersistentInbox inbox;

        public TimeToLiveCheckingThread(PersistentInbox inbox) {
            this.inbox = inbox;
        }

        @Override
        public void run() {
            long now = new Date().getTime();
            for (DiscoveryResult result : inbox.getAll()) {
                if (isResultExpired(result, now)) {
                    logger.debug("Inbox entry for thing '{}' is expired and will be removed.", result.getThingUID());
                    remove(result.getThingUID());
                }
            }
        }

        private boolean isResultExpired(DiscoveryResult result, long now) {
            if (result.getTimeToLive() == DiscoveryResult.TTL_UNLIMITED) {
                return false;
            }
            return (result.getTimestamp() + result.getTimeToLive() * 1000 < now);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(PersistentInbox.class);

    private final Set<InboxListener> listeners = new CopyOnWriteArraySet<>();
    private final DiscoveryServiceRegistry discoveryServiceRegistry;
    private final ThingRegistry thingRegistry;
    private final ManagedThingProvider managedThingProvider;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final Storage<DiscoveryResult> discoveryResultStorage;
    private final Map<DiscoveryResult, Class<?>> resultDiscovererMap = new ConcurrentHashMap<>();
    private @NonNullByDefault({}) ScheduledFuture<?> timeToLiveChecker;
    private @NonNullByDefault({}) ScheduledFuture<?> delayedDiscoveryResultProcessor;

    private @Nullable EventPublisher eventPublisher;
    private final List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<>();

    @Activate
    public PersistentInbox(final @Reference StorageService storageService,
            final @Reference DiscoveryServiceRegistry discoveryServiceRegistry,
            final @Reference ThingRegistry thingRegistry, final @Reference ManagedThingProvider thingProvider,
            final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        // First set all member variables to ensure the object itself is initialized (as most as possible).
        this.discoveryResultStorage = storageService.getStorage(DiscoveryResult.class.getName(),
                this.getClass().getClassLoader());
        this.discoveryServiceRegistry = discoveryServiceRegistry;
        this.thingRegistry = thingRegistry;
        this.managedThingProvider = thingProvider;
        this.thingTypeRegistry = thingTypeRegistry;
        this.configDescRegistry = configDescriptionRegistry;
    }

    @Activate
    protected void activate() {
        discoveryServiceRegistry.addDiscoveryListener(this);
        thingRegistry.addRegistryChangeListener(this);
        ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("discovery");
        timeToLiveChecker = scheduler.scheduleWithFixedDelay(new TimeToLiveCheckingThread(this), 0, 30,
                TimeUnit.SECONDS);
        delayedDiscoveryResultProcessor = scheduler.scheduleWithFixedDelay(
                () -> Set.copyOf(delayedDiscoveryResults.values()).forEach(this::internalAdd), 0, 15, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        thingRegistry.removeRegistryChangeListener(this);
        discoveryServiceRegistry.removeDiscoveryListener(this);
        listeners.clear();
        timeToLiveChecker.cancel(true);
        delayedDiscoveryResultProcessor.cancel(true);
        delayedDiscoveryResults.values().forEach(dr -> dr.future.complete(false));
    }

    @Override
    public @Nullable Thing approve(ThingUID thingUID, @Nullable String label, @Nullable String newThingId) {
        if (thingUID == null) {
            throw new IllegalArgumentException("Thing UID must not be null");
        }
        List<DiscoveryResult> results = stream().filter(forThingUID(thingUID)).collect(Collectors.toList());
        if (results.isEmpty()) {
            throw new IllegalArgumentException("No Thing with UID " + thingUID.getAsString() + " in inbox");
        }
        if (newThingId != null && newThingId.contains(AbstractUID.SEPARATOR)) {
            throw new IllegalArgumentException("New Thing ID " + newThingId + " must not contain multiple segments");
        }
        DiscoveryResult result = results.get(0);
        final Map<String, String> properties = new HashMap<>();
        final Map<String, Object> configParams = new HashMap<>();
        getPropsAndConfigParams(result, properties, configParams);
        final Configuration config = new Configuration(configParams);
        ThingTypeUID thingTypeUID = result.getThingTypeUID();
        ThingUID newThingUID = thingUID;
        if (newThingId != null) {
            String newUID = thingUID.getAsString().substring(0,
                    thingUID.getAsString().lastIndexOf(AbstractUID.SEPARATOR) + 1) + newThingId;
            try {
                newThingUID = new ThingUID(newUID);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid thing UID " + newUID, e);
            }
        }
        Thing newThing = ThingFactory.createThing(newThingUID, config, properties, result.getBridgeUID(), thingTypeUID,
                thingHandlerFactories);
        if (newThing == null) {
            logger.warn("Cannot create thing. No binding found that supports creating a thing of type {}.",
                    thingTypeUID);
            return null;
        }
        if (label != null && !label.isEmpty()) {
            newThing.setLabel(label);
        } else {
            newThing.setLabel(result.getLabel());
        }
        addThingSafely(newThing);
        return newThing;
    }

    private Map<ThingUID, DiscoveryResultWrapper> delayedDiscoveryResults = new ConcurrentHashMap<>();

    @Override
    public synchronized CompletableFuture<Boolean> add(final @Nullable DiscoveryResult discoveryResult)
            throws IllegalStateException {
        if (discoveryResult == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        internalAdd(new DiscoveryResultWrapper(discoveryResult, future));
        return future;
    }

    private void internalAdd(DiscoveryResultWrapper discoveryResultWrapper) {
        DiscoveryResult discoveryResult = discoveryResultWrapper.discoveryResult;

        // if we already have a result for the same ThingUID that is not added yet, delete it from the delayed map
        delayedDiscoveryResults.remove(discoveryResult.getThingUID());

        ThingType thingType = thingTypeRegistry.getThingType(discoveryResult.getThingTypeUID());
        if (thingType == null) {
            discoveryResultWrapper.retryCount++;

            if (discoveryResultWrapper.retryCount >= 20) {
                logger.info(
                        "ThingTypeUID {} for discovery result with ThingUID {} not found, retried 20 times, aborting",
                        discoveryResult.getThingTypeUID(), discoveryResult.getThingUID());
                discoveryResultWrapper.future.complete(false);
            } else {
                logger.trace(
                        "ThingTypeUID {} for discovery result with ThingUID {} not found, delaying add, retry {}/20",
                        discoveryResult.getThingTypeUID(), discoveryResult.getThingUID(),
                        discoveryResultWrapper.retryCount);
                delayedDiscoveryResults.put(discoveryResult.getThingUID(), discoveryResultWrapper);
            }
            return;
        }

        List<String> configurationParameters = getConfigDescParams(thingType).stream()
                .map(ConfigDescriptionParameter::getName).collect(Collectors.toList());

        discoveryResult.normalizePropertiesOnConfigDescription(configurationParameters);

        ThingUID thingUID = discoveryResult.getThingUID();
        Thing thing = thingRegistry.get(thingUID);

        if (thing == null) {
            DiscoveryResult inboxResult = get(thingUID);

            if (inboxResult == null) {
                discoveryResultStorage.put(discoveryResult.getThingUID().toString(), discoveryResult);
                notifyListeners(discoveryResult, EventType.ADDED);
                logger.info("Added new thing '{}' to inbox.", thingUID);
                discoveryResultWrapper.future.complete(true);
            } else {
                if (inboxResult instanceof DiscoveryResultImpl) {
                    DiscoveryResultImpl resultImpl = (DiscoveryResultImpl) inboxResult;
                    resultImpl.synchronize(discoveryResult);
                    discoveryResultStorage.put(discoveryResult.getThingUID().toString(), resultImpl);
                    notifyListeners(resultImpl, EventType.UPDATED);
                    logger.debug("Updated discovery result for '{}'.", thingUID);
                    discoveryResultWrapper.future.complete(true);
                } else {
                    logger.warn("Cannot synchronize result with implementation class '{}'.",
                            inboxResult.getClass().getName());
                }
            }
        } else if (managedThingProvider.get(thingUID) != null) {
            // only try to update properties if thing is managed
            logger.debug(
                    "Discovery result with thing '{}' not added as inbox entry. It is already present as thing in the ThingRegistry.",
                    thingUID);

            boolean updated = synchronizeConfiguration(discoveryResult.getThingTypeUID(),
                    discoveryResult.getProperties(), thing.getConfiguration());

            if (updated) {
                logger.debug("The configuration for thing '{}' is updated...", thingUID);
                managedThingProvider.update(thing);
            }
        }

        discoveryResultWrapper.future.complete(false);
    }

    private boolean synchronizeConfiguration(ThingTypeUID thingTypeUID, Map<String, Object> properties,
            Configuration config) {
        boolean configUpdated = false;

        final Set<Map.Entry<String, Object>> propertySet = properties.entrySet();
        final ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        final List<ConfigDescriptionParameter> configDescParams = getConfigDescParams(thingType);

        for (Map.Entry<String, Object> propertyEntry : propertySet) {
            final String propertyKey = propertyEntry.getKey();
            final Object propertyValue = propertyEntry.getValue();

            // Check if the key is present in the configuration.
            if (!config.containsKey(propertyKey)) {
                continue;
            }

            // Normalize first
            ConfigDescriptionParameter configDescParam = getConfigDescriptionParam(configDescParams, propertyKey);
            Object normalizedValue = ConfigUtil.normalizeType(propertyValue, configDescParam);

            // If the value is equal to the one of the configuration, there is nothing to do.
            if (Objects.equals(normalizedValue, config.get(propertyKey))) {
                continue;
            }

            // - the given key is part of the configuration
            // - the values differ

            // update value
            config.put(propertyKey, normalizedValue);
            configUpdated = true;
        }

        return configUpdated;
    }

    private @Nullable ConfigDescriptionParameter getConfigDescriptionParam(
            List<ConfigDescriptionParameter> configDescParams, String paramName) {
        for (ConfigDescriptionParameter configDescriptionParameter : configDescParams) {
            if (configDescriptionParameter.getName().equals(paramName)) {
                return configDescriptionParameter;
            }
        }
        return null;
    }

    @Override
    public void addInboxListener(@Nullable InboxListener listener) throws IllegalStateException {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public List<DiscoveryResult> getAll() {
        return stream().collect(Collectors.toList());
    }

    @Override
    public Stream<DiscoveryResult> stream() {
        final Storage<DiscoveryResult> discoveryResultStorage = this.discoveryResultStorage;
        if (discoveryResultStorage == null) {
            final ScheduledFuture<?> timeToLiveChecker = this.timeToLiveChecker;
            logger.error("The OSGi lifecycle has been violated (storage: {}, ttl checker cancelled: {}).",
                    this.discoveryResultStorage == null ? "null" : this.discoveryResultStorage,
                    timeToLiveChecker == null ? "null" : timeToLiveChecker.isCancelled());
            return Stream.empty();
        }
        final Collection<@Nullable DiscoveryResult> values = discoveryResultStorage.getValues();
        if (values == null) {
            logger.warn(
                    "The storage service violates the nullness requirements (get values must not return null) (storage class: {}).",
                    discoveryResultStorage.getClass());
            return Stream.empty();
        }
        return (Stream<DiscoveryResult>) values.stream().filter(Objects::nonNull);
    }

    @Override
    public synchronized boolean remove(@Nullable ThingUID thingUID) throws IllegalStateException {
        if (thingUID != null) {
            DiscoveryResult discoveryResult = get(thingUID);
            if (discoveryResult != null) {
                if (!isInRegistry(thingUID)) {
                    removeResultsForBridge(thingUID);
                }
                resultDiscovererMap.remove(discoveryResult);
                discoveryResultStorage.remove(thingUID.toString());
                notifyListeners(discoveryResult, EventType.REMOVED);
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeInboxListener(@Nullable InboxListener listener) throws IllegalStateException {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        add(result).thenAccept(success -> {
            if (success) {
                resultDiscovererMap.put(result, source.getClass());
            }
        });
    }

    @Override
    public void thingRemoved(DiscoveryService source, ThingUID thingUID) {
        remove(thingUID);
    }

    @Override
    public @Nullable Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            @Nullable Collection<ThingTypeUID> thingTypeUIDs, @Nullable ThingUID bridgeUID) {
        Set<ThingUID> removedThings = new HashSet<>();
        for (DiscoveryResult discoveryResult : getAll()) {
            Class<?> discoverer = resultDiscovererMap.get(discoveryResult);
            if (thingTypeUIDs != null && thingTypeUIDs.contains(discoveryResult.getThingTypeUID())
                    && discoveryResult.getTimestamp() < timestamp
                    && (discoverer == null || source.getClass() == discoverer)) {
                ThingUID thingUID = discoveryResult.getThingUID();
                if (bridgeUID == null || bridgeUID.equals(discoveryResult.getBridgeUID())) {
                    removedThings.add(thingUID);
                    remove(thingUID);
                    logger.debug("Removed thing '{}' from inbox because it was older than {}.", thingUID,
                            new Date(timestamp));
                }
            }
        }
        return removedThings;
    }

    @Override
    public void added(Thing thing) {
        if (remove(thing.getUID())) {
            logger.debug(
                    "Discovery result for thing '{}' removed from inbox, because it was added as a Thing to the ThingRegistry.",
                    thing.getUID());
        }
    }

    @Override
    public void removed(Thing thing) {
        if (thing instanceof Bridge) {
            removeResultsForBridge(thing.getUID());
        }
    }

    @Override
    public void updated(Thing oldThing, Thing thing) {
        // Attention: Do NOT fire an event back to the ThingRegistry otherwise circular
        // events are fired! This event was triggered by the 'add(DiscoveryResult)'
        // method within this class. -> NOTHING TO DO HERE
    }

    @Override
    public void setFlag(ThingUID thingUID, @Nullable DiscoveryResultFlag flag) {
        DiscoveryResult result = get(thingUID);
        if (result instanceof DiscoveryResultImpl) {
            DiscoveryResultImpl resultImpl = (DiscoveryResultImpl) result;
            resultImpl.setFlag((flag == null) ? DiscoveryResultFlag.NEW : flag);
            discoveryResultStorage.put(resultImpl.getThingUID().toString(), resultImpl);
            notifyListeners(resultImpl, EventType.UPDATED);
        } else if (result == null) {
            logger.warn("Cannot set flag for result '{}' because it can't be found in storage", thingUID);
        } else {
            logger.warn("Cannot set flag for result of instance type '{}'", result.getClass().getName());
        }
    }

    /**
     * Returns the {@link DiscoveryResult} in this {@link Inbox} associated with
     * the specified {@code Thing} ID, or {@code null}, if no {@link DiscoveryResult} could be found.
     *
     * @param thingUID the Thing UID to which the discovery result should be returned
     *
     * @return the discovery result associated with the specified Thing ID, or
     *         null, if no discovery result could be found
     */
    private @Nullable DiscoveryResult get(ThingUID thingUID) {
        if (thingUID != null) {
            return discoveryResultStorage.get(thingUID.toString());
        }
        return null;
    }

    private void notifyListeners(DiscoveryResult result, EventType type) {
        for (InboxListener listener : listeners) {
            try {
                switch (type) {
                    case ADDED:
                        listener.thingAdded(this, result);
                        break;
                    case REMOVED:
                        listener.thingRemoved(this, result);
                        break;
                    case UPDATED:
                        listener.thingUpdated(this, result);
                        break;
                }
            } catch (Exception ex) {
                logger.error("Cannot notify the InboxListener '{}' about a Thing {} event!",
                        listener.getClass().getName(), type.name(), ex);
            }
        }

        // in case of EventType added/updated the listeners might have modified the result in the discoveryResultStorage
        final DiscoveryResult resultForEvent;
        if (type == EventType.REMOVED) {
            resultForEvent = result;
        } else {
            resultForEvent = get(result.getThingUID());
            if (resultForEvent == null) {
                return;
            }
        }
        postEvent(resultForEvent, type);
    }

    private void postEvent(DiscoveryResult result, EventType eventType) {
        if (eventPublisher != null) {
            try {
                switch (eventType) {
                    case ADDED:
                        eventPublisher.post(InboxEventFactory.createAddedEvent(result));
                        break;
                    case REMOVED:
                        eventPublisher.post(InboxEventFactory.createRemovedEvent(result));
                        break;
                    case UPDATED:
                        eventPublisher.post(InboxEventFactory.createUpdatedEvent(result));
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                logger.error("Could not post event of type '{}'.", eventType.name(), ex);
            }
        }
    }

    private boolean isInRegistry(ThingUID thingUID) {
        return thingRegistry.get(thingUID) != null;
    }

    private void removeResultsForBridge(ThingUID bridgeUID) {
        for (ThingUID thingUID : getResultsForBridge(bridgeUID)) {
            DiscoveryResult discoveryResult = get(thingUID);
            if (discoveryResult != null) {
                discoveryResultStorage.remove(thingUID.toString());
                notifyListeners(discoveryResult, EventType.REMOVED);
            }
        }
    }

    private List<ThingUID> getResultsForBridge(ThingUID bridgeUID) {
        List<ThingUID> thingsForBridge = new ArrayList<>();
        for (DiscoveryResult result : discoveryResultStorage.getValues()) {
            if (bridgeUID.equals(result.getBridgeUID())) {
                thingsForBridge.add(result.getThingUID());
            }
        }
        return thingsForBridge;
    }

    /**
     * Get the properties and configuration parameters for the thing with the given {@link DiscoveryResult}.
     *
     * @param discoveryResult the DiscoveryResult
     * @param props the location the properties should be stored to.
     * @param configParams the location the configuration parameters should be stored to.
     */
    private void getPropsAndConfigParams(final DiscoveryResult discoveryResult, final Map<String, String> props,
            final Map<String, Object> configParams) {
        final List<ConfigDescriptionParameter> configDescParams = getConfigDescParams(discoveryResult);
        final Set<String> paramNames = getConfigDescParamNames(configDescParams);
        final Map<String, Object> resultProps = discoveryResult.getProperties();
        for (Entry<String, Object> resultEntry : resultProps.entrySet()) {
            String resultKey = resultEntry.getKey();
            Object resultValue = resultEntry.getValue();
            if (paramNames.contains(resultKey)) {
                ConfigDescriptionParameter param = getConfigDescriptionParam(configDescParams, resultKey);
                Object normalizedValue = ConfigUtil.normalizeType(resultValue, param);
                configParams.put(resultKey, normalizedValue);
            } else {
                props.put(resultKey, String.valueOf(resultValue));
            }
        }
    }

    private Set<String> getConfigDescParamNames(List<ConfigDescriptionParameter> configDescParams) {
        Set<String> paramNames = new HashSet<>();
        if (configDescParams != null) {
            for (ConfigDescriptionParameter param : configDescParams) {
                paramNames.add(param.getName());
            }
        }
        return paramNames;
    }

    private List<ConfigDescriptionParameter> getConfigDescParams(DiscoveryResult discoveryResult) {
        ThingType thingType = thingTypeRegistry.getThingType(discoveryResult.getThingTypeUID());
        return getConfigDescParams(thingType);
    }

    private List<ConfigDescriptionParameter> getConfigDescParams(@Nullable ThingType thingType) {
        if (thingType != null && thingType.getConfigDescriptionURI() != null) {
            URI descURI = thingType.getConfigDescriptionURI();
            if (descURI != null) {
                ConfigDescription desc = configDescRegistry.getConfigDescription(descURI);
                if (desc != null) {
                    return desc.getParameters();
                }
            }
        }
        return Collections.emptyList();
    }

    private void addThingSafely(Thing thing) {
        ThingUID thingUID = thing.getUID();
        if (thingRegistry.get(thingUID) != null) {
            thingRegistry.remove(thingUID);
        }
        thingRegistry.add(thing);
    }

    void setTimeToLiveCheckingInterval(int interval) {
        timeToLiveChecker.cancel(true);
        timeToLiveChecker = ThreadPoolManager.getScheduledPool("discovery")
                .scheduleWithFixedDelay(new TimeToLiveCheckingThread(this), 0, interval, TimeUnit.SECONDS);
    }

    void setDiscoveryResultAddRetryInterval(int interval) {
        delayedDiscoveryResultProcessor.cancel(true);
        delayedDiscoveryResultProcessor = ThreadPoolManager.getScheduledPool("discovery").scheduleWithFixedDelay(
                () -> Set.copyOf(delayedDiscoveryResults.values()).forEach(this::internalAdd), 0, interval,
                TimeUnit.SECONDS);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        this.thingHandlerFactories.add(thingHandlerFactory);
    }

    protected void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        this.thingHandlerFactories.remove(thingHandlerFactory);
    }

    private static class DiscoveryResultWrapper {
        public final CompletableFuture<Boolean> future;
        public final DiscoveryResult discoveryResult;
        public int retryCount = 0;

        public DiscoveryResultWrapper(DiscoveryResult discoveryResult, CompletableFuture<Boolean> future) {
            this.discoveryResult = discoveryResult;
            this.future = future;
        }
    }
}
