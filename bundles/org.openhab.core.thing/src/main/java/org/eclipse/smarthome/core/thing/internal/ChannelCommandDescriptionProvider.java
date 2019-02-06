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
package org.eclipse.smarthome.core.thing.internal;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.DynamicCommandDescriptionProvider;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.CommandDescriptionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the {@link ChannelType} specific {@link CommandDescription} for the given item name and locale.
 *
 * @author Henning Treu - Initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true)
public class ChannelCommandDescriptionProvider implements CommandDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(ChannelCommandDescriptionProvider.class);

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    private final List<DynamicCommandDescriptionProvider> dynamicCommandDescriptionProviders = new CopyOnWriteArrayList<>();

    @Override
    public @Nullable CommandDescription getCommandDescription(String itemName, @Nullable Locale locale) {
        Set<ChannelUID> boundChannels = itemChannelLinkRegistry.getBoundChannels(itemName);
        if (!boundChannels.isEmpty()) {
            ChannelUID channelUID = boundChannels.iterator().next();
            Channel channel = thingRegistry.getChannel(channelUID);
            if (channel != null) {
                CommandDescription commandDescription = null;
                ChannelType channelType = thingTypeRegistry.getChannelType(channel, locale);
                if (channelType != null) {
                    commandDescription = channelType.getCommandDescription();
                }
                CommandDescription dynamicCommandDescription = getDynamicCommandDescription(channel, commandDescription,
                        locale);
                if (dynamicCommandDescription != null) {
                    return dynamicCommandDescription;
                }
                return commandDescription;
            }
        }

        return null;
    }

    private @Nullable CommandDescription getDynamicCommandDescription(Channel channel,
            @Nullable CommandDescription originalCommandDescription, @Nullable Locale locale) {
        for (DynamicCommandDescriptionProvider dynamicCommandDescriptionProvider : dynamicCommandDescriptionProviders) {
            try {
                CommandDescription dynamicCommandDescription = dynamicCommandDescriptionProvider
                        .getCommandDescription(channel, originalCommandDescription, locale);
                if (dynamicCommandDescription != null) {
                    return dynamicCommandDescription;
                }
            } catch (Exception e) {
                logger.error("Error evaluating {}#getCommandDescription: {}",
                        dynamicCommandDescriptionProvider.getClass(), e.getLocalizedMessage(), e);
            }
        }

        return null;
    }

    @Reference
    protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addDynamicCommandDescriptionProvider(
            DynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        this.dynamicCommandDescriptionProviders.add(dynamicCommandDescriptionProvider);
    }

    protected void removeDynamicCommandDescriptionProvider(
            DynamicCommandDescriptionProvider dynamicCommandDescriptionProvider) {
        this.dynamicCommandDescriptionProviders.remove(dynamicCommandDescriptionProvider);
    }
}
