/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.thing.internal.link;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.profiles.StateProfileType;
import org.openhab.core.thing.profiles.TriggerProfileType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provider for framework config parameters on {@link ItemChannelLink}s.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component
@NonNullByDefault
public class ItemChannelLinkConfigDescriptionProvider implements ConfigDescriptionProvider {

    private static final String SCHEME = "link";
    public static final String PARAM_PROFILE = "profile";

    private final ProfileTypeRegistry profileTypeRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;

    @Activate
    public ItemChannelLinkConfigDescriptionProvider(final @Reference ProfileTypeRegistry profileTypeRegistry, //
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference ThingRegistry thingRegistry) {
        this.profileTypeRegistry = profileTypeRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        return Set.of();
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        if (SCHEME.equals(uri.getScheme())) {
            ItemChannelLink link = itemChannelLinkRegistry.get(uri.getSchemeSpecificPart());
            if (link == null) {
                return null;
            }
            Item item = itemRegistry.get(link.getItemName());
            if (item == null) {
                return null;
            }
            Thing thing = thingRegistry.get(link.getLinkedUID().getThingUID());
            if (thing == null) {
                return null;
            }
            Channel channel = thing.getChannel(link.getLinkedUID().getId());
            if (channel == null) {
                return null;
            }
            ConfigDescriptionParameter paramProfile = ConfigDescriptionParameterBuilder.create(PARAM_PROFILE, Type.TEXT)
                    .withLabel("Profile").withDescription("the profile to use").withRequired(false)
                    .withOptions(getOptions(link, item, channel, locale)).build();
            return ConfigDescriptionBuilder.create(uri).withParameter(paramProfile).build();
        }
        return null;
    }

    private List<ParameterOption> getOptions(ItemChannelLink link, Item item, Channel channel,
            @Nullable Locale locale) {
        Collection<ProfileType> profileTypes = profileTypeRegistry.getProfileTypes(locale);
        return profileTypes.stream().filter(profileType -> {
            switch (channel.getKind()) {
                case STATE:
                    return profileType instanceof StateProfileType && isSupportedItemType(profileType, item);
                case TRIGGER:
                    return profileType instanceof TriggerProfileType && isSupportedItemType(profileType, item)
                            && isSupportedChannelType((TriggerProfileType) profileType, channel);
                default:
                    throw new IllegalArgumentException("Unknown channel kind: " + channel.getKind());
            }
        }).map(profileType -> new ParameterOption(profileType.getUID().toString(), profileType.getLabel()))
                .collect(Collectors.toList());
    }

    private boolean isSupportedItemType(ProfileType profileType, Item item) {
        return profileType.getSupportedItemTypes().isEmpty()
                || profileType.getSupportedItemTypes().contains(item.getType());
    }

    private boolean isSupportedChannelType(TriggerProfileType profileType, Channel channel) {
        return profileType.getSupportedChannelTypeUIDs().isEmpty()
                || profileType.getSupportedChannelTypeUIDs().contains(channel.getChannelTypeUID());
    }
}
