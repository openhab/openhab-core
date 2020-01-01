/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.internal.ThingTracker.ThingTrackerEvent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ThingRegistry}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Simon Kaufmann - Added forceRemove
 * @author Chris Jackson - ensure thing added event is sent before linked events
 * @auther Thomas Höfer - Added config description validation exception to updateConfiguration operation
 */
@NonNullByDefault
@Component(immediate = true)
public class ThingRegistryImpl extends AbstractRegistry<Thing, ThingUID, ThingProvider> implements ThingRegistry {

    private final Logger logger = LoggerFactory.getLogger(ThingRegistryImpl.class.getName());

    private final List<ThingTracker> thingTrackers = new CopyOnWriteArrayList<>();

    private final List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<>();

    public ThingRegistryImpl() {
        super(ThingProvider.class);
    }

    /**
     * Adds a thing tracker.
     *
     * @param thingTracker
     *            the thing tracker
     */
    public void addThingTracker(ThingTracker thingTracker) {
        notifyTrackerAboutAllThingsAdded(thingTracker);
        thingTrackers.add(thingTracker);
    }

    @Override
    public @Nullable Channel getChannel(ChannelUID channelUID) {
        ThingUID thingUID = channelUID.getThingUID();
        Thing thing = get(thingUID);
        if (thing != null) {
            return thing.getChannel(channelUID.getId());
        }
        return null;
    }

    @Override
    public void updateConfiguration(ThingUID thingUID, Map<String, Object> configurationParameters) {
        Thing thing = get(thingUID);
        if (thing != null) {
            ThingHandler thingHandler = thing.getHandler();
            if (thingHandler != null) {
                thingHandler.handleConfigurationUpdate(configurationParameters);
            } else {
                throw new IllegalStateException("Thing with UID " + thingUID + " has no handler attached.");
            }
        } else {
            throw new IllegalArgumentException("Thing with UID " + thingUID + " does not exists.");
        }
    }

    @Override
    public @Nullable Thing forceRemove(ThingUID thingUID) {
        return super.remove(thingUID);
    }

    @Override
    public @Nullable Thing remove(ThingUID thingUID) {
        Thing thing = get(thingUID);
        if (thing != null) {
            notifyTrackers(thing, ThingTrackerEvent.THING_REMOVING);
        }
        return thing;
    }

    /**
     * Removes a thing tracker.
     *
     * @param thingTracker
     *            the thing tracker
     */
    public void removeThingTracker(ThingTracker thingTracker) {
        notifyTrackerAboutAllThingsRemoved(thingTracker);
        thingTrackers.remove(thingTracker);
    }

    @Override
    protected void notifyListenersAboutAddedElement(Thing element) {
        super.notifyListenersAboutAddedElement(element);
        postEvent(ThingEventFactory.createAddedEvent(element));
        notifyTrackers(element, ThingTrackerEvent.THING_ADDED);
    }

    @Override
    protected void notifyListenersAboutRemovedElement(Thing element) {
        super.notifyListenersAboutRemovedElement(element);
        notifyTrackers(element, ThingTrackerEvent.THING_REMOVED);
        postEvent(ThingEventFactory.createRemovedEvent(element));
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(Thing oldElement, Thing element) {
        super.notifyListenersAboutUpdatedElement(oldElement, element);
        notifyTrackers(element, ThingTrackerEvent.THING_UPDATED);
        postEvent(ThingEventFactory.createUpdateEvent(element, oldElement));
    }

    @Override
    protected void onAddElement(Thing thing) throws IllegalArgumentException {
        addThingToBridge(thing);
        if (thing instanceof Bridge) {
            addThingsToBridge((Bridge) thing);
        }
    }

    @Override
    protected void onRemoveElement(Thing thing) {
        // needed because the removed element was taken from the storage and lost its dynamic state
        preserveDynamicState(thing);
        ThingUID bridgeUID = thing.getBridgeUID();
        if (bridgeUID != null) {
            Thing bridge = this.get(bridgeUID);
            if (bridge instanceof BridgeImpl) {
                ((BridgeImpl) bridge).removeThing(thing);
            }
        }
    }

    @Override
    protected void onUpdateElement(Thing oldThing, Thing thing) {
        // better call it explicitly here, even if it is called in onRemoveElement
        preserveDynamicState(thing);
        onRemoveElement(thing);
        onAddElement(thing);
    }

    private void preserveDynamicState(Thing thing) {
        final Thing existingThing = get(thing.getUID());
        if (existingThing != null) {
            thing.setHandler(existingThing.getHandler());
            thing.setStatusInfo(existingThing.getStatusInfo());
        }
    }

    private void addThingsToBridge(Bridge bridge) {
        forEach(thing -> {
            ThingUID bridgeUID = thing.getBridgeUID();
            if (bridgeUID != null && bridgeUID.equals(bridge.getUID())) {
                if (bridge instanceof BridgeImpl && !bridge.getThings().contains(thing)) {
                    ((BridgeImpl) bridge).addThing(thing);
                }
            }
        });
    }

    private void addThingToBridge(Thing thing) {
        ThingUID bridgeUID = thing.getBridgeUID();
        if (bridgeUID != null) {
            Thing bridge = this.get(bridgeUID);
            if (bridge instanceof BridgeImpl && !((Bridge) bridge).getThings().contains(thing)) {
                ((BridgeImpl) bridge).addThing(thing);
            }
        }
    }

    private void notifyTrackers(Thing thing, ThingTrackerEvent event) {
        for (ThingTracker thingTracker : thingTrackers) {
            try {
                switch (event) {
                    case THING_ADDED:
                        thingTracker.thingAdded(thing, ThingTrackerEvent.THING_ADDED);
                        break;
                    case THING_REMOVING:
                        thingTracker.thingRemoving(thing, ThingTrackerEvent.THING_REMOVING);
                        break;
                    case THING_REMOVED:
                        thingTracker.thingRemoved(thing, ThingTrackerEvent.THING_REMOVED);
                        break;
                    case THING_UPDATED:
                        thingTracker.thingUpdated(thing, ThingTrackerEvent.THING_UPDATED);
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                logger.error("Could not inform the ThingTracker '{}' about the '{}' event!", thingTracker, event.name(),
                        ex);
            }
        }
    }

    private void notifyTrackerAboutAllThingsAdded(ThingTracker thingTracker) {
        for (Thing thing : getAll()) {
            thingTracker.thingAdded(thing, ThingTrackerEvent.TRACKER_ADDED);
        }
    }

    private void notifyTrackerAboutAllThingsRemoved(ThingTracker thingTracker) {
        for (Thing thing : getAll()) {
            thingTracker.thingRemoved(thing, ThingTrackerEvent.TRACKER_REMOVED);
        }
    }

    @Override
    public @Nullable Thing createThingOfType(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            @Nullable ThingUID bridgeUID, @Nullable String label, Configuration configuration) {
        logger.debug("Creating thing for type '{}'.", thingTypeUID);
        for (ThingHandlerFactory thingHandlerFactory : thingHandlerFactories) {
            if (thingHandlerFactory.supportsThingType(thingTypeUID)) {
                Thing thing = thingHandlerFactory.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
                if (thing == null) {
                    logger.warn(
                            "Cannot create thing of type '{}'. Binding '{}' says it supports it, but it could not be created.",
                            thingTypeUID, thingHandlerFactory.getClass().getName());
                } else {
                    thing.setLabel(label);
                    return thing;
                }
            }
        }
        logger.warn("Cannot create thing. No binding found that supports creating a thing of type '{}'.", thingTypeUID);
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        this.thingHandlerFactories.add(thingHandlerFactory);
    }

    protected void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        this.thingHandlerFactories.remove(thingHandlerFactory);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "ManagedThingProvider")
    protected void setManagedProvider(ManagedThingProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedThingProvider managedProvider) {
        super.unsetManagedProvider(managedProvider);
    }

}
