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
package org.eclipse.smarthome.core.thing.internal.link;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeRegistry;
import org.eclipse.smarthome.core.thing.profiles.StateProfileType;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfileType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provider for framework config parameters on {@link ItemChannelLink}s.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@Component
public class ItemChannelLinkConfigDescriptionProvider implements ConfigDescriptionProvider {

    private static final String SCHEME = "link";

    public static final String PARAM_PROFILE = "profile";

    private ProfileTypeRegistry profileTypeRegistry;

    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ItemRegistry itemRegistry;
    private ThingRegistry thingRegistry;

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(Locale locale) {
        return Collections.emptySet();
    }

    @Override
    public ConfigDescription getConfigDescription(URI uri, Locale locale) {
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
                    .withLimitToOptions(true).withOptions(getOptions(link, item, channel, locale)).build();
            return new ConfigDescription(uri, Collections.singletonList(paramProfile));
        }
        return null;
    }

    private List<ParameterOption> getOptions(ItemChannelLink link, Item item, Channel channel, Locale locale) {
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

    @Reference
    public void setProfileTypeRegistry(ProfileTypeRegistry profileTypeRegistry) {
        this.profileTypeRegistry = profileTypeRegistry;
    }

    public void unsetProfileTypeRegistry(ProfileTypeRegistry profileTypeRegistry) {
        this.profileTypeRegistry = null;
    }

    @Reference
    public void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    public void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

}
