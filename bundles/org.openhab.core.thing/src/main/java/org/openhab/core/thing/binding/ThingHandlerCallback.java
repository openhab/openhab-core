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
package org.openhab.core.thing.binding;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * {@link ThingHandlerCallback} is callback interface for {@link ThingHandler}s. The implementation of a
 * {@link ThingHandler} must use the callback to inform the framework about changes like state updates, status updated
 * or an update of the whole thing.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Bußweiler - Added new thing status info, added new configuration update info
 * @author Christoph Weitkamp - Moved OSGI ServiceTracker from BaseThingHandler to ThingHandlerCallback
 * @author Christoph Weitkamp - Added preconfigured ChannelGroupBuilder
 */
@NonNullByDefault
public interface ThingHandlerCallback {

    /**
     * Informs about an updated state for a channel.
     *
     * @param channelUID channel UID (must not be null)
     * @param state state (must not be null)
     */
    void stateUpdated(ChannelUID channelUID, State state);

    /**
     * Informs about a command, which is sent from the channel.
     *
     * @param channelUID channel UID
     * @param command command
     */
    void postCommand(ChannelUID channelUID, Command command);

    /**
     * Informs about an updated status of a thing.
     *
     * @param thing thing (must not be null)
     * @param thingStatus thing status (must not be null)
     */
    void statusUpdated(Thing thing, ThingStatusInfo thingStatus);

    /**
     * Informs about an update of the whole thing.
     *
     * @param thing thing that was updated (must not be null)
     * @throws IllegalStateException if the {@link Thing} is read-only.
     */
    void thingUpdated(Thing thing);

    /**
     * Validates the given configuration parameters against the configuration description.
     *
     * @param thing thing with the updated configuration (must no be null)
     * @param configurationParameters the configuration parameters to be validated
     * @throws ConfigValidationException if one or more of the given configuration parameters do not match
     *             their declarations in the configuration description
     */
    void validateConfigurationParameters(Thing thing, Map<String, Object> configurationParameters);

    /**
     * Informs about an updated configuration of a thing.
     *
     * @param thing thing with the updated configuration (must no be null)
     */
    void configurationUpdated(Thing thing);

    /**
     * Informs the framework that the ThingType of the given {@link Thing} should be changed.
     *
     * @param thing thing that should be migrated to another ThingType (must not be null)
     * @param thingTypeUID the new type of the thing (must not be null)
     * @param configuration a configuration that should be applied to the given {@link Thing}
     */
    void migrateThingType(Thing thing, ThingTypeUID thingTypeUID, Configuration configuration);

    /**
     * Informs the framework that a channel has been triggered.
     *
     * @param thing thing (must not be null)
     * @param channelUID UID of the channel over which has been triggered.
     * @param event Event.
     */
    void channelTriggered(Thing thing, ChannelUID channelUID, String event);

    /**
     * Creates a {@link ChannelBuilder} which is preconfigured with values from the given {@link ChannelType}.
     *
     * @param channelUID the UID of the {@link Channel} to be created
     * @param channelTypeUID the {@link ChannelTypeUID} for which the {@link Channel} should be created
     * @return a preconfigured {@link ChannelBuilder}
     * @throws IllegalArgumentException if the referenced {@link ChannelType} is not known
     */
    ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelTypeUID channelTypeUID);

    /**
     * Creates a {@link ChannelBuilder} which is preconfigured with values from the given {@link Channel} and allows to
     * modify it. The methods {@link BaseThingHandler#editThing(Thing)} and {@link BaseThingHandler#updateThing(Thing)}
     * must be called to persist the changes.
     *
     * @param thing {@link Thing} (must not be null)
     * @param channelUID the UID of the {@link Channel} to be edited
     * @return a preconfigured {@link ChannelBuilder}
     * @throws IllegalArgumentException if no {@link Channel} with the given UID exists for the given {@link Thing}
     */
    ChannelBuilder editChannel(Thing thing, ChannelUID channelUID);

    /**
     * Creates a list of {@link ChannelBuilder}s which are preconfigured with values from the given
     * {@link ChannelGroupType}.
     *
     * @param channelGroupUID the UID of the channel group to be created
     * @param channelGroupTypeUID the {@link ChannelGroupUID} for which the {@link Channel}s should be created
     * @return a list of preconfigured {@link ChannelBuilder}s
     * @throws IllegalArgumentException if the referenced {@link ChannelGroupType} is not known
     */
    List<ChannelBuilder> createChannelBuilders(ChannelGroupUID channelGroupUID,
            ChannelGroupTypeUID channelGroupTypeUID);

    /**
     * Returns whether at least one item is linked for the given UID of the channel.
     *
     * @param channelUID UID of the channel (must not be null)
     * @return true if at least one item is linked, false otherwise
     */
    boolean isChannelLinked(ChannelUID channelUID);

    /**
     * Returns the bridge of the thing.
     *
     * @param bridgeUID {@link ThingUID} UID of the bridge (must not be null)
     * @return returns the bridge of the thing or null if the thing has no bridge
     */
    @Nullable
    Bridge getBridge(ThingUID bridgeUID);
}
