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
package org.openhab.core.thing.internal;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.DynamicCommandDescriptionProvider;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
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
 */
@Component
@NonNullByDefault
public class ChannelCommandDescriptionProvider implements CommandDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(ChannelCommandDescriptionProvider.class);

    private final List<DynamicCommandDescriptionProvider> dynamicCommandDescriptionProviders = new CopyOnWriteArrayList<>();
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ThingRegistry thingRegistry;

    @Activate
    public ChannelCommandDescriptionProvider(final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ThingTypeRegistry thingTypeRegistry, final @Reference ThingRegistry thingRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingTypeRegistry = thingTypeRegistry;
        this.thingRegistry = thingRegistry;
    }

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
                    if (dynamicCommandDescription.equals(originalCommandDescription)) {
                        logger.error(
                                "Dynamic command description matches original command description. DynamicCommandDescriptionProvider implementations must never return the original command description. {} has to be fixed.",
                                dynamicCommandDescription.getClass());
                    } else {
                        return dynamicCommandDescription;
                    }
                }
            } catch (Exception e) {
                logger.error("Error evaluating {}#getCommandDescription: {}",
                        dynamicCommandDescriptionProvider.getClass(), e.getLocalizedMessage(), e);
            }
        }
        return null;
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
