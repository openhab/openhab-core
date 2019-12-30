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
package org.openhab.core.thing.profiles;

import static org.openhab.core.thing.profiles.ProfileTypeUID.SYSTEM_SCOPE;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;

/**
 * System profile constants.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface SystemProfiles {

    ProfileTypeUID DEFAULT = new ProfileTypeUID(SYSTEM_SCOPE, "default");
    ProfileTypeUID FOLLOW = new ProfileTypeUID(SYSTEM_SCOPE, "follow");
    ProfileTypeUID OFFSET = new ProfileTypeUID(SYSTEM_SCOPE, "offset");
    ProfileTypeUID RAWBUTTON_ON_OFF_SWITCH = new ProfileTypeUID(SYSTEM_SCOPE, "rawbutton-on-off-switch");
    ProfileTypeUID RAWBUTTON_TOGGLE_PLAYER = new ProfileTypeUID(SYSTEM_SCOPE, "rawbutton-toggle-player");
    ProfileTypeUID RAWBUTTON_TOGGLE_ROLLERSHUTTER = new ProfileTypeUID(SYSTEM_SCOPE, "rawbutton-toggle-rollershutter");
    ProfileTypeUID RAWBUTTON_TOGGLE_SWITCH = new ProfileTypeUID(SYSTEM_SCOPE, "rawbutton-toggle-switch");
    ProfileTypeUID RAWROCKER_DIMMER = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-dimmer");
    ProfileTypeUID RAWROCKER_NEXT_PREVIOUS = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-next-previous");
    ProfileTypeUID RAWROCKER_ON_OFF = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-on-off");
    ProfileTypeUID RAWROCKER_PLAY_PAUSE = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-play-pause");
    ProfileTypeUID RAWROCKER_REWIND_FASTFORWARD = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-rewind-fastforward");
    ProfileTypeUID RAWROCKER_STOP_MOVE = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-stop-move");
    ProfileTypeUID RAWROCKER_UP_DOWN = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-up-down");
    ProfileTypeUID TIMESTAMP_CHANGE = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-change");
    ProfileTypeUID TIMESTAMP_UPDATE = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-update");

    StateProfileType DEFAULT_TYPE = ProfileTypeBuilder.newState(DEFAULT, "Default").build();

    StateProfileType FOLLOW_TYPE = ProfileTypeBuilder.newState(FOLLOW, "Follow").build();

    StateProfileType OFFSET_TYPE = ProfileTypeBuilder.newState(OFFSET, "Offset")
            .withSupportedItemTypes(CoreItemFactory.NUMBER).withSupportedItemTypesOfChannel(CoreItemFactory.NUMBER)
            .build();

    TriggerProfileType RAWBUTTON_ON_OFF_SWITCH_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_ON_OFF_SWITCH, "Raw Button To On Off")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID()).build();

    TriggerProfileType RAWBUTTON_TOGGLE_PLAYER_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_PLAYER, "Raw Button Toggle Player")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID()).build();

    TriggerProfileType RAWBUTTON_TOGGLE_ROLLERSHUTTER_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_ROLLERSHUTTER, "Raw Button Toggle Rollershutter")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID()).build();

    TriggerProfileType RAWBUTTON_TOGGLE_SWITCH_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_SWITCH, "Raw Button Toggle Switch")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID()).build();

    TriggerProfileType RAWROCKER_ON_OFF_TYPE = ProfileTypeBuilder.newTrigger(RAWROCKER_ON_OFF, "Raw Rocker To On Off")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_DIMMER_TYPE = ProfileTypeBuilder.newTrigger(RAWROCKER_DIMMER, "Raw Rocker To Dimmer")
            .withSupportedItemTypes(CoreItemFactory.DIMMER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_NEXT_PREVIOUS_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_NEXT_PREVIOUS, "Raw Rocker To Next/Previous")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_PLAY_PAUSE_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_PLAY_PAUSE, "Raw Rocker To Play/Pause").withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_REWIND_FASTFORWARD_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_REWIND_FASTFORWARD, "Raw Rocker To Rewind/Fastforward")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_STOP_MOVE_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_STOP_MOVE, "Raw Rocker To Stop/Move")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    TriggerProfileType RAWROCKER_UP_DOWN_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_UP_DOWN, "Raw Rocker To Up/Down")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID()).build();

    StateProfileType TIMESTAMP_CHANGE_TYPE = ProfileTypeBuilder.newState(TIMESTAMP_CHANGE, "Timestamp on change")
            .withSupportedItemTypes(CoreItemFactory.DATETIME).build();

    StateProfileType TIMESTAMP_UPDATE_TYPE = ProfileTypeBuilder.newState(TIMESTAMP_UPDATE, "Timestamp on update")
            .withSupportedItemTypes(CoreItemFactory.DATETIME).build();
}
