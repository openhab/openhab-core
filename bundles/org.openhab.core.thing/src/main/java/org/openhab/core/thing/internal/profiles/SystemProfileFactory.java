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
package org.openhab.core.thing.internal.profiles;

import static org.openhab.core.thing.profiles.SystemProfiles.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocalizedKey;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileAdvisor;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.i18n.ProfileTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
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
 * @author Christoph Weitkamp - Added translation for profile labels
 */
@Component(service = { SystemProfileFactory.class, ProfileTypeProvider.class })
@NonNullByDefault
public class SystemProfileFactory implements ProfileFactory, ProfileAdvisor, ProfileTypeProvider {

    private final ChannelTypeRegistry channelTypeRegistry;

    private static final Set<ProfileType> SUPPORTED_PROFILE_TYPES = Collections
            .unmodifiableSet(Stream
                    .of(DEFAULT_TYPE, FOLLOW_TYPE, OFFSET_TYPE, RAWBUTTON_ON_OFF_SWITCH_TYPE,
                            RAWBUTTON_TOGGLE_PLAYER_TYPE, RAWBUTTON_TOGGLE_PLAYER_TYPE, RAWBUTTON_TOGGLE_SWITCH_TYPE,
                            RAWROCKER_DIMMER_TYPE, RAWROCKER_NEXT_PREVIOUS_TYPE, RAWROCKER_ON_OFF_TYPE,
                            RAWROCKER_PLAY_PAUSE_TYPE, RAWROCKER_REWIND_FASTFORWARD_TYPE, RAWROCKER_STOP_MOVE_TYPE,
                            RAWROCKER_UP_DOWN_TYPE, TIMESTAMP_CHANGE_TYPE, TIMESTAMP_UPDATE_TYPE)
                    .collect(Collectors.toSet()));

    private static final Set<ProfileTypeUID> SUPPORTED_PROFILE_TYPE_UIDS = Collections
            .unmodifiableSet(Stream.of(DEFAULT, FOLLOW, OFFSET, RAWBUTTON_ON_OFF_SWITCH, RAWBUTTON_TOGGLE_PLAYER,
                    RAWBUTTON_TOGGLE_PLAYER, RAWBUTTON_TOGGLE_SWITCH, RAWROCKER_DIMMER, RAWROCKER_NEXT_PREVIOUS,
                    RAWROCKER_ON_OFF, RAWROCKER_PLAY_PAUSE, RAWROCKER_REWIND_FASTFORWARD, RAWROCKER_STOP_MOVE,
                    RAWROCKER_UP_DOWN, TIMESTAMP_CHANGE, TIMESTAMP_UPDATE).collect(Collectors.toSet()));

    private final Map<LocalizedKey, ProfileType> localizedProfileTypeCache = new ConcurrentHashMap<>();

    private final ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService;
    private final Bundle bundle;

    @Activate
    public SystemProfileFactory(final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService,
            final @Reference BundleResolver bundleResolver) {
        this.channelTypeRegistry = channelTypeRegistry;
        this.profileTypeI18nLocalizationService = profileTypeI18nLocalizationService;
        this.bundle = bundleResolver.resolveBundle(SystemProfileFactory.class);
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext context) {
        if (DEFAULT.equals(profileTypeUID)) {
            return new SystemDefaultProfile(callback);
        } else if (FOLLOW.equals(profileTypeUID)) {
            return new SystemFollowProfile(callback);
        } else if (OFFSET.equals(profileTypeUID)) {
            return new SystemOffsetProfile(callback, context);
        } else if (RAWBUTTON_ON_OFF_SWITCH.equals(profileTypeUID)) {
            return new RawButtonOnOffSwitchProfile(callback);
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

    @Override
    public @Nullable ProfileTypeUID getSuggestedProfileTypeUID(@Nullable ChannelType channelType,
            @Nullable String itemType) {
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

    @Override
    public @Nullable ProfileTypeUID getSuggestedProfileTypeUID(Channel channel, @Nullable String itemType) {
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
        return Collections.unmodifiableList(SUPPORTED_PROFILE_TYPES.stream()
                .map(p -> createLocalizedProfileType(p, locale)).collect(Collectors.toList()));
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return SUPPORTED_PROFILE_TYPE_UIDS;
    }

    private ProfileType createLocalizedProfileType(ProfileType profileType, @Nullable Locale locale) {
        LocalizedKey localizedKey = getLocalizedProfileTypeKey(profileType.getUID(), locale);

        ProfileType cachedEntry = localizedProfileTypeCache.get(localizedKey);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        ProfileType localizedProfileType = localize(profileType, locale);
        if (localizedProfileType != null) {
            localizedProfileTypeCache.put(localizedKey, localizedProfileType);
            return localizedProfileType;
        } else {
            return profileType;
        }
    }

    private @Nullable ProfileType localize(ProfileType profileType, @Nullable Locale locale) {
        return profileTypeI18nLocalizationService.createLocalizedProfileType(bundle, profileType, locale);
    }

    private LocalizedKey getLocalizedProfileTypeKey(UID uid, @Nullable Locale locale) {
        return new LocalizedKey(uid, locale != null ? locale.toLanguageTag() : null);
    }
}
