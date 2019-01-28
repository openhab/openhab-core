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
package org.eclipse.smarthome.core.thing.internal.profiles;

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
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
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
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
@Component(service = { SystemProfileFactory.class, ProfileTypeProvider.class })
public class SystemProfileFactory implements ProfileFactory, ProfileAdvisor, ProfileTypeProvider {

    @NonNullByDefault({})
    private ChannelTypeRegistry channelTypeRegistry;

    private static final Set<ProfileType> SUPPORTED_PROFILE_TYPES = Stream.of(SystemProfiles.DEFAULT_TYPE,
            SystemProfiles.FOLLOW_TYPE, SystemProfiles.RAWBUTTON_TOGGLE_SWITCH_TYPE,
            SystemProfiles.RAWROCKER_ON_OFF_TYPE, SystemProfiles.RAWROCKER_DIMMER_TYPE, SystemProfiles.OFFSET_TYPE, SystemProfiles.RAWROCKER_PLAY_PAUSE_TYPE)
            .collect(Collectors.toSet());

    private static final Set<ProfileTypeUID> SUPPORTED_PROFILE_TYPE_UIDS = Stream
            .of(SystemProfiles.DEFAULT, SystemProfiles.FOLLOW, SystemProfiles.RAWBUTTON_TOGGLE_SWITCH,
                    SystemProfiles.RAWROCKER_ON_OFF, SystemProfiles.RAWROCKER_DIMMER, SystemProfiles.OFFSET, SystemProfiles.RAWROCKER_PLAY_PAUSE)
            .collect(Collectors.toSet());

    @Nullable
    @Override
    public Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext context) {
        if (SystemProfiles.DEFAULT.equals(profileTypeUID)) {
            return new SystemDefaultProfile(callback);
        } else if (SystemProfiles.FOLLOW.equals(profileTypeUID)) {
            return new SystemFollowProfile(callback);
        } else if (SystemProfiles.RAWBUTTON_TOGGLE_SWITCH.equals(profileTypeUID)) {
            return new RawButtonToggleSwitchProfile(callback);
        } else if (SystemProfiles.RAWROCKER_ON_OFF.equals(profileTypeUID)) {
            return new RawRockerOnOffProfile(callback);
        } else if (SystemProfiles.RAWROCKER_DIMMER.equals(profileTypeUID)) {
            return new RawRockerDimmerProfile(callback, context);
        } else if (SystemProfiles.RAWROCKER_PLAY_PAUSE.equals(profileTypeUID)) {
            return new RawRockerPlayPauseProfile(callback);
        } else if (SystemProfiles.OFFSET.equals(profileTypeUID)) {
            return new SystemOffsetProfile(callback, context);
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
                return SystemProfiles.DEFAULT;
            case TRIGGER:
                if (DefaultSystemChannelTypeProvider.SYSTEM_RAWBUTTON.getUID().equals(channelType.getUID())) {
                    if (CoreItemFactory.SWITCH.equalsIgnoreCase(itemType)) {
                        return SystemProfiles.RAWBUTTON_TOGGLE_SWITCH;
                    }
                } else if (DefaultSystemChannelTypeProvider.SYSTEM_RAWROCKER.getUID().equals(channelType.getUID())) {
                    if (CoreItemFactory.SWITCH.equalsIgnoreCase(itemType)) {
                        return SystemProfiles.RAWROCKER_ON_OFF;
                    } else if (CoreItemFactory.DIMMER.equalsIgnoreCase(itemType)) {
                        return SystemProfiles.RAWROCKER_DIMMER;
                    } else if (CoreItemFactory.PLAYER.equalsIgnoreCase(itemType)) {
                        return SystemProfiles.RAWROCKER_PLAY_PAUSE;
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
                    return SystemProfiles.DEFAULT;
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
