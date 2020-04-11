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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateDescriptionFragmentProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ChannelStateDescriptionProvider} provides localized {@link StateDescription}s from the type of a
 * {@link Channel} bounded to an {@link Item}.
 *
 * @author Dennis Nobel - Initial contribution
 */
@Component(immediate = true, property = { "service.ranking:Integer=-1" })
@NonNullByDefault
public class ChannelStateDescriptionProvider implements StateDescriptionFragmentProvider {

    private final Logger logger = LoggerFactory.getLogger(ChannelStateDescriptionProvider.class);

    private final List<DynamicStateDescriptionProvider> dynamicStateDescriptionProviders = new CopyOnWriteArrayList<>();
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ThingRegistry thingRegistry;
    private Integer rank = 0;

    @Activate
    public ChannelStateDescriptionProvider(final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ThingTypeRegistry thingTypeRegistry, final @Reference ThingRegistry thingRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingTypeRegistry = thingTypeRegistry;
        this.thingRegistry = thingRegistry;
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer) {
            rank = (Integer) serviceRanking;
        }
    }

    @Override
    public Integer getRank() {
        return rank;
    }

    @Override
    public @Nullable StateDescriptionFragment getStateDescriptionFragment(String itemName, @Nullable Locale locale) {
        StateDescription stateDescription = getStateDescription(itemName, locale);
        if (stateDescription != null) {
            return StateDescriptionFragmentBuilder.create(stateDescription).build();
        }
        return null;
    }

    private @Nullable StateDescription getStateDescription(String itemName, @Nullable Locale locale) {
        Set<ChannelUID> boundChannels = itemChannelLinkRegistry.getBoundChannels(itemName);
        if (!boundChannels.isEmpty()) {
            ChannelUID channelUID = boundChannels.iterator().next();
            Channel channel = thingRegistry.getChannel(channelUID);
            if (channel != null) {
                StateDescription stateDescription = null;
                ChannelType channelType = thingTypeRegistry.getChannelType(channel, locale);
                if (channelType != null) {
                    stateDescription = channelType.getState();
                    if ((channelType.getItemType() != null)
                            && ((stateDescription == null) || (stateDescription.getPattern() == null))) {
                        String pattern = null;
                        if (CoreItemFactory.STRING.equalsIgnoreCase(channelType.getItemType())) {
                            pattern = "%s";
                        } else if (channelType.getItemType().startsWith(CoreItemFactory.NUMBER)) {
                            pattern = "%.0f";
                        }
                        if (pattern != null) {
                            logger.trace("Provide a default pattern {} for item {}", pattern, itemName);
                            StateDescriptionFragmentBuilder builder = (stateDescription == null)
                                    ? StateDescriptionFragmentBuilder.create()
                                    : StateDescriptionFragmentBuilder.create(stateDescription);
                            stateDescription = builder.withPattern(pattern).build().toStateDescription();
                        }
                    }
                }
                StateDescription dynamicStateDescription = getDynamicStateDescription(channel, stateDescription,
                        locale);
                if (dynamicStateDescription != null) {
                    return dynamicStateDescription;
                }
                return stateDescription;
            }
        }
        return null;
    }

    private @Nullable StateDescription getDynamicStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        for (DynamicStateDescriptionProvider provider : dynamicStateDescriptionProviders) {
            StateDescription stateDescription = provider.getStateDescription(channel, originalStateDescription, locale);
            if (stateDescription != null) {
                if (stateDescription.equals(originalStateDescription)) {
                    logger.error(
                            "Dynamic state description matches original state description. DynamicStateDescriptionProvider implementations must never return the original state description. {} has to be fixed.",
                            provider.getClass());
                } else {
                    return stateDescription;
                }
            }
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addDynamicStateDescriptionProvider(DynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.dynamicStateDescriptionProviders.add(dynamicStateDescriptionProvider);
    }

    protected void removeDynamicStateDescriptionProvider(
            DynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.dynamicStateDescriptionProviders.remove(dynamicStateDescriptionProvider);
    }
}
