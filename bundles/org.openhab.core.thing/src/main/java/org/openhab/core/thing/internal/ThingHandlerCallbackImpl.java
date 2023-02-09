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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingHandlerCallbackImpl} implements the {@link ThingHandlerCallback} interface
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
class ThingHandlerCallbackImpl implements ThingHandlerCallback {
    private final Logger logger = LoggerFactory.getLogger(ThingHandlerCallbackImpl.class);

    private final ThingManagerImpl thingManager;

    public ThingHandlerCallbackImpl(ThingManagerImpl thingManager) {
        this.thingManager = thingManager;
    }

    @Override
    public void stateUpdated(ChannelUID channelUID, State state) {
        thingManager.communicationManager.stateUpdated(channelUID, state);
    }

    @Override
    public void postCommand(ChannelUID channelUID, Command command) {
        thingManager.communicationManager.postCommand(channelUID, command);
    }

    @Override
    public void channelTriggered(Thing thing, ChannelUID channelUID, String event) {
        thingManager.communicationManager.channelTriggered(thing, channelUID, event);
    }

    @Override
    public void statusUpdated(Thing thing, ThingStatusInfo statusInfo) {
        // note: all operations based on a status update should be executed asynchronously!
        ThingStatusInfo oldStatusInfo = thing.getStatusInfo();
        ensureValidStatus(oldStatusInfo.getStatus(), statusInfo.getStatus());

        if (ThingStatus.REMOVING.equals(oldStatusInfo.getStatus())
                && !ThingStatus.REMOVED.equals(statusInfo.getStatus())) {
            // if we go to ONLINE and are still in REMOVING, notify handler about required removal
            if (ThingStatus.ONLINE.equals(statusInfo.getStatus())) {
                logger.debug("Handler is initialized now and we try to remove it, because it is in REMOVING state.");
                thingManager.notifyThingHandlerAboutRemoval(thing);
            }
            // only allow REMOVING -> REMOVED transition, all others are ignored because they are illegal
            logger.debug(
                    "Ignoring illegal status transition for thing {} from REMOVING to {}, only REMOVED would have been allowed.",
                    thing.getUID(), statusInfo.getStatus());
            return;
        }

        // update thing status and send event about new status
        thingManager.setThingStatus(thing, statusInfo);

        // if thing is a bridge
        if (thing instanceof Bridge bridge) {
            handleBridgeStatusUpdate(bridge, statusInfo, oldStatusInfo);
        }
        // if thing has a bridge
        if (thing.getBridgeUID() != null) {
            handleBridgeChildStatusUpdate(thing, oldStatusInfo);
        }
        // notify thing registry about thing removal
        if (ThingStatus.REMOVED.equals(thing.getStatus())) {
            thingManager.notifyRegistryAboutForceRemove(thing);
        }
    }

    private void ensureValidStatus(ThingStatus oldStatus, ThingStatus newStatus) {
        if (!(ThingStatus.UNKNOWN.equals(newStatus) || ThingStatus.ONLINE.equals(newStatus)
                || ThingStatus.OFFLINE.equals(newStatus) || ThingStatus.REMOVED.equals(newStatus))) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Illegal status {0}. Bindings only may set {1}, {2}, {3} or {4}.", newStatus,
                            ThingStatus.UNKNOWN, ThingStatus.ONLINE, ThingStatus.OFFLINE, ThingStatus.REMOVED));
        }
        if (ThingStatus.REMOVED.equals(newStatus) && !ThingStatus.REMOVING.equals(oldStatus)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Illegal status {0}. The thing was in state {1} and not in {2}", newStatus,
                            oldStatus, ThingStatus.REMOVING));
        }
    }

    private void handleBridgeStatusUpdate(Bridge bridge, ThingStatusInfo statusInfo, ThingStatusInfo oldStatusInfo) {
        if (ThingHandlerHelper.isHandlerInitialized(bridge)
                && (ThingStatus.INITIALIZING.equals(oldStatusInfo.getStatus()))) {
            // bridge has just been initialized: initialize child things as well
            thingManager.registerChildHandlers(bridge);
        } else if (!statusInfo.equals(oldStatusInfo)) {
            // bridge status has been changed: notify child things about status change
            thingManager.notifyThingsAboutBridgeStatusChange(bridge, statusInfo);
        }
    }

    private void handleBridgeChildStatusUpdate(Thing thing, ThingStatusInfo oldStatusInfo) {
        if (ThingHandlerHelper.isHandlerInitialized(thing)
                && ThingStatus.INITIALIZING.equals(oldStatusInfo.getStatus())) {
            // child thing has just been initialized: notify bridge about it
            thingManager.notifyBridgeAboutChildHandlerInitialization(thing);
        }
    }

    @Override
    public void thingUpdated(final Thing thing) {
        thingManager.thingUpdated(thing);
    }

    @Override
    public void validateConfigurationParameters(Thing thing, Map<String, Object> configurationParameters) {
        ThingType thingType = thingManager.thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType != null) {
            URI configDescriptionURI = thingType.getConfigDescriptionURI();
            if (configDescriptionURI != null) {
                thingManager.configDescriptionValidator.validate(configurationParameters, configDescriptionURI);
            }
        }
    }

    @Override
    public void validateConfigurationParameters(Channel channel, Map<String, Object> configurationParameters) {
        ChannelType channelType = thingManager.channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
        if (channelType != null) {
            URI configDescriptionURI = channelType.getConfigDescriptionURI();
            if (configDescriptionURI != null) {
                thingManager.configDescriptionValidator.validate(configurationParameters, configDescriptionURI);
            }
        }
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(ChannelTypeUID channelTypeUID) {
        ChannelType channelType = thingManager.channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType != null) {
            URI configDescriptionUri = channelType.getConfigDescriptionURI();
            if (configDescriptionUri != null) {
                return thingManager.configDescriptionRegistry.getConfigDescription(configDescriptionUri);
            }
        }
        return null;
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(ThingTypeUID thingTypeUID) {
        ThingType thingType = thingManager.thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType != null) {
            URI configDescriptionUri = thingType.getConfigDescriptionURI();
            if (configDescriptionUri != null) {
                return thingManager.configDescriptionRegistry.getConfigDescription(configDescriptionUri);
            }
        }
        return null;
    }

    @Override
    public void configurationUpdated(Thing thing) {
        if (!ThingHandlerHelper.isHandlerInitialized(thing)) {
            thingManager.initializeHandler(thing);
        }
    }

    @Override
    public void migrateThingType(final Thing thing, final ThingTypeUID thingTypeUID,
            final Configuration configuration) {
        thingManager.migrateThingType(thing, thingTypeUID, configuration);
    }

    @Override
    public ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelTypeUID channelTypeUID) {
        ChannelType channelType = thingManager.channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            throw new IllegalArgumentException(String.format("Channel type '%s' is not known", channelTypeUID));
        }
        return ThingFactoryHelper.createChannelBuilder(channelUID, channelType, thingManager.configDescriptionRegistry);
    }

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
        ChannelGroupType channelGroupType = thingManager.channelGroupTypeRegistry
                .getChannelGroupType(channelGroupTypeUID);
        if (channelGroupType == null) {
            throw new IllegalArgumentException(
                    String.format("Channel group type '%s' is not known", channelGroupTypeUID));
        }
        List<ChannelBuilder> channelBuilders = new ArrayList<>();
        for (ChannelDefinition channelDefinition : channelGroupType.getChannelDefinitions()) {
            ChannelType channelType = thingManager.channelTypeRegistry
                    .getChannelType(channelDefinition.getChannelTypeUID());
            if (channelType != null) {
                ChannelUID channelUID = new ChannelUID(channelGroupUID, channelDefinition.getId());
                ChannelBuilder channelBuilder = ThingFactoryHelper.createChannelBuilder(channelUID, channelDefinition,
                        thingManager.configDescriptionRegistry);
                if (channelBuilder != null) {
                    channelBuilders.add(channelBuilder);
                }
            }
        }
        return channelBuilders;
    }

    @Override
    public boolean isChannelLinked(ChannelUID channelUID) {
        return thingManager.itemChannelLinkRegistry.isLinked(channelUID);
    }

    @Override
    public @Nullable Bridge getBridge(ThingUID bridgeUID) {
        Thing bridgeThing = thingManager.thingRegistry.get(bridgeUID);
        if (bridgeThing instanceof Bridge bridge) {
            return bridge;
        }
        return null;
    }
}
