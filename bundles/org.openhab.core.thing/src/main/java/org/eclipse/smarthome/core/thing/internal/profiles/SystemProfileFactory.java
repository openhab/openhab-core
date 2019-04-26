/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.eclipse.smarthome.core.thing.profiles.SystemProfiles.*;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.DefaultSystemChannelTypeProvider;
import org.eclipse.smarthome.core.thing.profiles.Profile;
import org.eclipse.smarthome.core.thing.profiles.ProfileAdvisor;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileFactory;
import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeProvider;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A factory and advisor for default profiles.
 *
 * This {@link ProfileAdvisor} and {@link ProfileFactory} implementation handles all default {@link Profile}s.
 * It will be used as an advisor if the link is not configured and no other advisor returned a result (in that order).
 * The same applies to the creation of profile instances: This factory will be used of no other factory supported the
 * required profile type.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
@Component(service = { SystemProfileFactory.class, ProfileTypeProvider.class })
public class SystemProfileFactory implements ProfileFactory, ProfileAdvisor, ProfileTypeProvider {

    @NonNullByDefault({})
    private ChannelTypeRegistry channelTypeRegistry;

    private static final Set<ProfileType> SUPPORTED_PROFILE_TYPES = Stream
            .of(DEFAULT_TYPE, FOLLOW_TYPE, OFFSET_TYPE, RAWBUTTON_TOGGLE_PLAYER_TYPE, RAWBUTTON_TOGGLE_PLAYER_TYPE,
                    RAWBUTTON_TOGGLE_SWITCH_TYPE, RAWROCKER_DIMMER_TYPE, RAWROCKER_NEXT_PREVIOUS_TYPE,
                    RAWROCKER_ON_OFF_TYPE, RAWROCKER_PLAY_PAUSE_TYPE, RAWROCKER_REWIND_FASTFORWARD_TYPE,
                    RAWROCKER_STOP_MOVE_TYPE, RAWROCKER_UP_DOWN_TYPE, TIMESTAMP_CHANGE_TYPE, TIMESTAMP_UPDATE_TYPE)
            .collect(Collectors.toSet());

    private static final Set<ProfileTypeUID> SUPPORTED_PROFILE_TYPE_UIDS = Stream.of(DEFAULT, FOLLOW, OFFSET,
            RAWBUTTON_TOGGLE_PLAYER, RAWBUTTON_TOGGLE_PLAYER, RAWBUTTON_TOGGLE_SWITCH, RAWROCKER_DIMMER,
            RAWROCKER_NEXT_PREVIOUS, RAWROCKER_ON_OFF, RAWROCKER_PLAY_PAUSE, RAWROCKER_REWIND_FASTFORWARD,
            RAWROCKER_STOP_MOVE, RAWROCKER_UP_DOWN, TIMESTAMP_CHANGE, TIMESTAMP_UPDATE).collect(Collectors.toSet());

    @Nullable
    @Override
    public Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext context) {
        if (DEFAULT.equals(profileTypeUID)) {
            return new SystemDefaultProfile(callback);
        } else if (FOLLOW.equals(profileTypeUID)) {
            return new SystemFollowProfile(callback);
        } else if (OFFSET.equals(profileTypeUID)) {
            return new SystemOffsetProfile(callback, context);
        } else if (RAWBUTTON_TOGGLE_SWITCH.equals(profileTypeUID)) {
            return new RawButtonToggleSwitchProfile(callback);
        } else if (RAWBUTTON_TOGGLE_PLAYER.equals(profileTypeUID)) {
            return new RawButtonToggleRollershutterProfile(callback);
        } else if (RAWBUTTON_TOGGLE_PLAYER.equals(profileTypeUID)) {
            return new RawButtonTogglePlayerProfile(callback);
        } else if (RAWROCKER_DIMMER.equals(profileTypeUID)) {
            return new RawRockerDimmerProfile(callback, context);
        } else if (RAWROCKER_NEXT_PREVIOUS.equals(profileTypeUID)) {
            return new RawRockerNextPreviousProfile(callback);
        } else if (RAWROCKER_ON_OFF.equals(profileTypeUID)) {
            return new RawRockerOnOffProfile(callback);
        } else if (RAWROCKER_PLAY_PAUSE.equals(profileTypeUID)) {
            return new RawRockerPlayPauseProfile(callback);
        } else if (RAWROCKER_REWIND_FASTFORWARD.equals(profileTypeUID)) {
            return new RawRockerRewindFastforwardProfile(callback);
        } else if (RAWROCKER_STOP_MOVE.equals(profileTypeUID)) {
            return new RawRockerStopMoveProfile(callback);
        } else if (RAWROCKER_UP_DOWN.equals(profileTypeUID)) {
            return new RawRockerUpDownProfile(callback);
        } else if (TIMESTAMP_CHANGE.equals(profileTypeUID)) {
            return new TimestampChangeProfile(callback);
        } else if (TIMESTAMP_UPDATE.equals(profileTypeUID)) {
            return new TimestampUpdateProfile(callback);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public ProfileTypeUID getSuggestedProfileTypeUID(@Nullable ChannelType channelType, @Nullable String itemType) {
        if (channelType == null) {
            return null;
        }
        switch (channelType.getKind()) {
            case STATE:
                return DEFAULT;
            case TRIGGER:
                if (DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID().equals(channelType.getUID())) {
                    if (CoreItemFactory.PLAYER.equalsIgnoreCase(itemType)) {
                        return RAWBUTTON_TOGGLE_PLAYER;
                    } else if (CoreItemFactory.ROLLERSHUTTER.equalsIgnoreCase(itemType)) {
                        return RAWBUTTON_TOGGLE_ROLLERSHUTTER;
                    } else if (CoreItemFactory.SWITCH.equalsIgnoreCase(itemType)) {
                        return RAWBUTTON_TOGGLE_SWITCH;
                    }
                } else if (DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID().equals(channelType.getUID())) {
                    if (CoreItemFactory.DIMMER.equalsIgnoreCase(itemType)) {
                        return RAWROCKER_DIMMER;
                    } else if (CoreItemFactory.PLAYER.equalsIgnoreCase(itemType)) {
                        return RAWROCKER_PLAY_PAUSE;
                    } else if (CoreItemFactory.ROLLERSHUTTER.equalsIgnoreCase(itemType)) {
                        return RAWROCKER_UP_DOWN;
                    } else if (CoreItemFactory.SWITCH.equalsIgnoreCase(itemType)) {
                        return RAWROCKER_ON_OFF;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported channel kind: " + channelType.getKind());
        }
        return null;
    }

    @Nullable
    @Override
    public ProfileTypeUID getSuggestedProfileTypeUID(Channel channel, @Nullable String itemType) {
        ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
        if (channelType == null) {
            switch (channel.getKind()) {
                case STATE:
                    return DEFAULT;
                case TRIGGER:
                    return null;
                default:
                    throw new IllegalArgumentException("Unsupported channel kind: " + channel.getKind());
            }
        } else {
            return getSuggestedProfileTypeUID(channelType, itemType);
        }
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return SUPPORTED_PROFILE_TYPES;
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return SUPPORTED_PROFILE_TYPE_UIDS;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

}
