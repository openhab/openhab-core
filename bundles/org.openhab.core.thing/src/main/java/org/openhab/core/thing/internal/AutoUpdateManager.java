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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component which takes care of calculating and sending potential auto-update event.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Kai Kreuzer - fixed issues if a linked thing is OFFLINE
 */
@NonNullByDefault
@Component(immediate = true, service = {
        AutoUpdateManager.class }, configurationPid = "org.openhab.autoupdate", configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class AutoUpdateManager {

    private static final String AUTOUPDATE_KEY = "autoupdate";
    protected static final String EVENT_SOURCE = "org.openhab.core.autoupdate";
    protected static final String EVENT_SOURCE_OPTIMISTIC = "org.openhab.core.autoupdate.optimistic";

    protected static final String PROPERTY_ENABLED = "enabled";
    protected static final String PROPERTY_SEND_OPTIMISTIC_UPDATES = "sendOptimisticUpdates";

    private final Logger logger = LoggerFactory.getLogger(AutoUpdateManager.class);

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;

    private boolean enabled = true;
    private boolean sendOptimisticUpdates = false;

    private static enum Recommendation {
        /*
         * An automatic state update must be sent because no channels are linked to the item.
         */
        REQUIRED,

        /*
         * An automatic state update should be sent because none of the linked channels are capable to retrieve the
         * actual state for their device.
         */
        RECOMMENDED,

        /*
         * An automatic state update may be sent in the optimistic anticipation of what the handler is going to send
         * soon anyway.
         */
        OPTIMISTIC,

        /*
         * No automatic state update should be sent because at least one channel claims it can handle it better.
         */
        DONT,

        /*
         * There are channels linked to the item which in theory would do the state update, but none of them is
         * currently able to communicate with their devices, hence no automatic state update should be done and the
         * previous item state should be sent instead in order to revert any control.
         */
        REVERT
    }

    @Activate
    protected void activate(Map<String, @Nullable Object> configuration) {
        modified(configuration);
    }

    @Modified
    protected void modified(Map<String, @Nullable Object> configuration) {
        Object valueEnabled = configuration.get(PROPERTY_ENABLED);
        if (valueEnabled != null) {
            enabled = Boolean.parseBoolean(valueEnabled.toString());
        }

        Object valueSendOptimisticUpdates = configuration.get(PROPERTY_SEND_OPTIMISTIC_UPDATES);
        if (valueSendOptimisticUpdates != null) {
            sendOptimisticUpdates = Boolean.parseBoolean(valueSendOptimisticUpdates.toString());
        }
    }

    public void receiveCommand(ItemCommandEvent commandEvent, Item item) {
        if (!enabled) {
            return;
        }
        final String itemName = commandEvent.getItemName();
        final Command command = commandEvent.getItemCommand();
        if (command instanceof State) {
            final State state = (State) command;

            Recommendation autoUpdate = shouldAutoUpdate(itemName);

            // consider user-override via item meta-data
            MetadataKey key = new MetadataKey(AUTOUPDATE_KEY, itemName);
            Metadata metadata = metadataRegistry.get(key);
            if (metadata != null && !metadata.getValue().trim().isEmpty()) {
                boolean override = Boolean.parseBoolean(metadata.getValue());
                if (override) {
                    logger.trace("Auto update strategy {} overriden by item metadata to REQUIRED", autoUpdate);
                    autoUpdate = Recommendation.REQUIRED;
                } else {
                    logger.trace("Auto update strategy {} overriden by item metadata to DONT", autoUpdate);
                    autoUpdate = Recommendation.DONT;
                }
            }

            switch (autoUpdate) {
                case REQUIRED:
                    logger.trace("Automatically updating item '{}' because no channel is linked", itemName);
                    postUpdate(item, state, EVENT_SOURCE);
                    break;
                case RECOMMENDED:
                    logger.trace("Automatically updating item '{}' because no channel does it", itemName);
                    postUpdate(item, state, EVENT_SOURCE);
                    break;
                case OPTIMISTIC:
                    logger.trace("Optimistically updating item '{}'", itemName);
                    postPrediction(item, state, false);
                    if (sendOptimisticUpdates) {
                        postUpdate(item, state, EVENT_SOURCE_OPTIMISTIC);
                    }
                    break;
                case DONT:
                    logger.trace("Won't update item '{}' as it was vetoed.", itemName);
                    break;
                case REVERT:
                    logger.trace("Sending current item state to revert controls '{}'", itemName);
                    postPrediction(item, item.getState(), true);
                    break;
            }
        }
    }

    private Recommendation shouldAutoUpdate(String itemName) {
        Recommendation ret = Recommendation.REQUIRED;

        List<ChannelUID> linkedChannelUIDs = new ArrayList<>();
        for (ItemChannelLink link : itemChannelLinkRegistry.getAll()) {
            if (link.getItemName().equals(itemName)) {
                linkedChannelUIDs.add(link.getLinkedUID());
            }
        }

        // check if there is any channel ONLINE
        List<ChannelUID> onlineChannelUIDs = new ArrayList<>();
        for (ChannelUID channelUID : linkedChannelUIDs) {
            Thing thing = thingRegistry.get(channelUID.getThingUID());
            if (thing == null //
                    || thing.getChannel(channelUID.getId()) == null //
                    || thing.getHandler() == null //
                    || !ThingStatus.ONLINE.equals(thing.getStatus()) //
            ) {
                continue;
            }
            onlineChannelUIDs.add(channelUID);
        }
        if (!linkedChannelUIDs.isEmpty() && onlineChannelUIDs.isEmpty()) {
            // none of the linked channels is able to process the command
            return Recommendation.REVERT;
        }

        for (ChannelUID channelUID : onlineChannelUIDs) {
            Thing thing = thingRegistry.get(channelUID.getThingUID());
            if (thing == null) {
                continue;
            }
            AutoUpdatePolicy policy = AutoUpdatePolicy.DEFAULT;
            Channel channel = thing.getChannel(channelUID.getId());
            if (channel != null) {
                AutoUpdatePolicy channelpolicy = channel.getAutoUpdatePolicy();
                if (channelpolicy != null) {
                    policy = channelpolicy;
                } else {
                    ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
                    if (channelType != null && channelType.getAutoUpdatePolicy() != null) {
                        policy = channelType.getAutoUpdatePolicy();
                    }
                }
            }

            switch (policy) {
                case VETO:
                    ret = Recommendation.DONT;
                    break;
                case DEFAULT:
                    if (ret == Recommendation.REQUIRED || ret == Recommendation.RECOMMENDED) {
                        ret = Recommendation.OPTIMISTIC;
                    }
                    break;
                case RECOMMEND:
                    if (ret == Recommendation.REQUIRED) {
                        ret = Recommendation.RECOMMENDED;
                    }
                    break;
            }
        }

        return ret;
    }

    private void postUpdate(Item item, State newState, String origin) {
        boolean isAccepted = isAcceptedState(newState, item);
        if (isAccepted) {
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), newState, origin));
        } else {
            logger.debug("Received update of a not accepted type ({}) for item {}", newState.getClass().getSimpleName(),
                    item.getName());
        }
    }

    private void postPrediction(Item item, State predictedState, boolean isConfirmation) {
        boolean isAccepted = isAcceptedState(predictedState, item);
        if (isAccepted) {
            eventPublisher
                    .post(ItemEventFactory.createStatePredictedEvent(item.getName(), predictedState, isConfirmation));
        } else {
            logger.debug("Received prediction of a not accepted type ({}) for item {}",
                    predictedState.getClass().getSimpleName(), item.getName());
        }
    }

    private boolean isAcceptedState(State newState, Item item) {
        boolean isAccepted = false;
        if (item.getAcceptedDataTypes().contains(newState.getClass())) {
            isAccepted = true;
        } else {
            // Look for class hierarchy
            for (Class<?> state : item.getAcceptedDataTypes()) {
                try {
                    if (!state.isEnum() && state.newInstance().getClass().isAssignableFrom(newState.getClass())) {
                        isAccepted = true;
                        break;
                    }
                } catch (InstantiationException e) {
                    logger.warn("InstantiationException on {}", e.getMessage(), e); // Should never happen
                } catch (IllegalAccessException e) {
                    logger.warn("IllegalAccessException on {}", e.getMessage(), e); // Should never happen
                }
            }
        }
        return isAccepted;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference
    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    protected void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

}
