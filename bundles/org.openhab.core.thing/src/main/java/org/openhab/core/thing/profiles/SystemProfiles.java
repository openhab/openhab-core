/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

    public static final ProfileTypeUID DEFAULT = new ProfileTypeUID(SYSTEM_SCOPE, "default");
    public static final ProfileTypeUID FOLLOW = new ProfileTypeUID(SYSTEM_SCOPE, "follow");
    public static final ProfileTypeUID OFFSET = new ProfileTypeUID(SYSTEM_SCOPE, "offset");
    public static final ProfileTypeUID HYSTERESIS = new ProfileTypeUID(SYSTEM_SCOPE, "hysteresis");
    public static final ProfileTypeUID RANGE = new ProfileTypeUID(SYSTEM_SCOPE, "range");
    public static final ProfileTypeUID RAWBUTTON_ON_OFF_SWITCH = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawbutton-on-off-switch");
    public static final ProfileTypeUID RAWBUTTON_TOGGLE_PLAYER = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawbutton-toggle-player");
    public static final ProfileTypeUID RAWBUTTON_TOGGLE_ROLLERSHUTTER = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawbutton-toggle-rollershutter");
    public static final ProfileTypeUID RAWBUTTON_TOGGLE_SWITCH = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawbutton-toggle-switch");
    public static final ProfileTypeUID RAWROCKER_DIMMER = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-dimmer");
    public static final ProfileTypeUID RAWROCKER_NEXT_PREVIOUS = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawrocker-to-next-previous");
    public static final ProfileTypeUID RAWROCKER_ON_OFF = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-on-off");
    public static final ProfileTypeUID RAWROCKER_PLAY_PAUSE = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawrocker-to-play-pause");
    public static final ProfileTypeUID RAWROCKER_REWIND_FASTFORWARD = new ProfileTypeUID(SYSTEM_SCOPE,
            "rawrocker-to-rewind-fastforward");
    public static final ProfileTypeUID RAWROCKER_STOP_MOVE = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-stop-move");
    public static final ProfileTypeUID RAWROCKER_UP_DOWN = new ProfileTypeUID(SYSTEM_SCOPE, "rawrocker-to-up-down");
    public static final ProfileTypeUID TIMESTAMP_CHANGE = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-change");
    public static final ProfileTypeUID TIMESTAMP_OFFSET = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-offset");
    public static final ProfileTypeUID TIMESTAMP_TRIGGER = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-trigger");
    public static final ProfileTypeUID TIMESTAMP_UPDATE = new ProfileTypeUID(SYSTEM_SCOPE, "timestamp-update");

    static final ProfileType DEFAULT_TYPE = ProfileTypeBuilder.newState(DEFAULT, "Default").build();

    static final ProfileType FOLLOW_TYPE = ProfileTypeBuilder.newState(FOLLOW, "Follow").build();

    static final ProfileType OFFSET_TYPE = ProfileTypeBuilder.newState(OFFSET, "Offset")
            .withSupportedItemTypes(CoreItemFactory.NUMBER).withSupportedItemTypesOfChannel(CoreItemFactory.NUMBER)
            .build();

    static final ProfileType HYSTERESIS_TYPE = ProfileTypeBuilder.newState(HYSTERESIS, "Hysteresis") //
            .withSupportedItemTypesOfChannel(CoreItemFactory.DIMMER, CoreItemFactory.NUMBER) //
            .withSupportedItemTypes(CoreItemFactory.SWITCH) //
            .build();

    static final ProfileType RANGE_TYPE = ProfileTypeBuilder.newState(RANGE, "Range") //
            .withSupportedItemTypesOfChannel(CoreItemFactory.DIMMER, CoreItemFactory.NUMBER) //
            .withSupportedItemTypes(CoreItemFactory.SWITCH) //
            .build();

    static final ProfileType RAWBUTTON_ON_OFF_SWITCH_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_ON_OFF_SWITCH, "Raw Button To On Off")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON).build();

    static final ProfileType RAWBUTTON_TOGGLE_PLAYER_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_PLAYER, "Raw Button Toggle Player")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON).build();

    static final ProfileType RAWBUTTON_TOGGLE_ROLLERSHUTTER_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_ROLLERSHUTTER, "Raw Button Toggle Rollershutter")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON).build();

    static final ProfileType RAWBUTTON_TOGGLE_SWITCH_TYPE = ProfileTypeBuilder
            .newTrigger(RAWBUTTON_TOGGLE_SWITCH, "Raw Button Toggle Switch")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWBUTTON).build();

    static final ProfileType RAWROCKER_ON_OFF_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_ON_OFF, "Raw Rocker To On Off")
            .withSupportedItemTypes(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER, CoreItemFactory.COLOR)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_DIMMER_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_DIMMER, "Raw Rocker To Dimmer").withSupportedItemTypes(CoreItemFactory.DIMMER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_NEXT_PREVIOUS_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_NEXT_PREVIOUS, "Raw Rocker To Next/Previous")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_PLAY_PAUSE_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_PLAY_PAUSE, "Raw Rocker To Play/Pause").withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_REWIND_FASTFORWARD_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_REWIND_FASTFORWARD, "Raw Rocker To Rewind/Fastforward")
            .withSupportedItemTypes(CoreItemFactory.PLAYER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_STOP_MOVE_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_STOP_MOVE, "Raw Rocker To Stop/Move")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType RAWROCKER_UP_DOWN_TYPE = ProfileTypeBuilder
            .newTrigger(RAWROCKER_UP_DOWN, "Raw Rocker To Up/Down")
            .withSupportedItemTypes(CoreItemFactory.ROLLERSHUTTER)
            .withSupportedChannelTypeUIDs(DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_RAWROCKER).build();

    static final ProfileType TIMESTAMP_CHANGE_TYPE = ProfileTypeBuilder
            .newState(TIMESTAMP_CHANGE, "Timestamp on change").withSupportedItemTypes(CoreItemFactory.DATETIME).build();

    static final ProfileType TIMESTAMP_OFFSET_TYPE = ProfileTypeBuilder.newState(TIMESTAMP_OFFSET, "Timestamp Offset")
            .withSupportedItemTypes(CoreItemFactory.DATETIME).withSupportedItemTypesOfChannel(CoreItemFactory.DATETIME)
            .build();

    static final ProfileType TIMESTAMP_TRIGGER_TYPE = ProfileTypeBuilder
            .newTrigger(TIMESTAMP_TRIGGER, "Timestamp on Trigger").withSupportedItemTypes(CoreItemFactory.DATETIME)
            .build();

    static final ProfileType TIMESTAMP_UPDATE_TYPE = ProfileTypeBuilder
            .newState(TIMESTAMP_UPDATE, "Timestamp on update").withSupportedItemTypes(CoreItemFactory.DATETIME).build();
}
