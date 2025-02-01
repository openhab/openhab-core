/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.core.items.ItemUtil;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for framework config parameters on {@link ItemChannelLink}s.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component
@NonNullByDefault
public class ItemChannelLinkConfigDescriptionProvider implements ConfigDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(ItemChannelLinkConfigDescriptionProvider.class);

    private static final String SCHEME = "link";
    public static final String PARAM_PROFILE = "profile";

    private final ProfileTypeRegistry profileTypeRegistry;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;

    @Activate
    public ItemChannelLinkConfigDescriptionProvider(final @Reference ProfileTypeRegistry profileTypeRegistry, //
            final @Reference ChannelTypeRegistry channelTypeRegistry, //
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference ThingRegistry thingRegistry) {
        this.profileTypeRegistry = profileTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
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
            Channel channel = thing.getChannel(link.getLinkedUID());
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
            return isSupportedItemType(profileType, item) && isSupportedChannelType(profileType, channel, locale);
        }).map(profileType -> new ParameterOption(profileType.getUID().toString(), profileType.getLabel())).toList();
    }

    private boolean isSupportedItemType(ProfileType profileType, Item item) {
        return profileType.getSupportedItemTypes().isEmpty()
                || profileType.getSupportedItemTypes().contains(item.getType());
    }

    private boolean isSupportedChannelType(ProfileType profileType, Channel channel, @Nullable Locale locale) {
        ChannelKind supportedChannelKind = profileType.getSupportedChannelKind();
        if (supportedChannelKind != null && supportedChannelKind != channel.getKind())
            return false;

        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();

        Collection<ChannelTypeUID> supportedChannelTypeUIDsOnProfileType = profileType.getSupportedChannelTypeUIDs();
        if (!supportedChannelTypeUIDsOnProfileType.isEmpty()
                && !supportedChannelTypeUIDsOnProfileType.contains(channelTypeUID)) {
            return false;
        }

        Collection<String> supportedItemTypesOfChannelOnProfileType = profileType.getSupportedItemTypesOfChannel();
        if (supportedItemTypesOfChannelOnProfileType.isEmpty()) {
            return true;
        } else {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID, locale);
            if (channelType == null) {
                logger.error("Requested to filter against an unknown channel type: {} is not known to the registry",
                        channelTypeUID.toString());
                return false;
            }

            String itemType = channelType.getItemType();
            return itemType != null
                    && supportedItemTypesOfChannelOnProfileType.contains(ItemUtil.getMainItemType(itemType));
        }
    }
}
