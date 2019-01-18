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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentProvider;
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
public class ChannelStateDescriptionProvider implements StateDescriptionFragmentProvider {

    private final Logger logger = LoggerFactory.getLogger(ChannelStateDescriptionProvider.class);

    private final List<DynamicStateDescriptionProvider> dynamicStateDescriptionProviders = new CopyOnWriteArrayList<>();
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ThingTypeRegistry thingTypeRegistry;
    private ThingRegistry thingRegistry;
    private Integer rank;

    @Activate
    protected void activate(Map<String, Object> properties) {
        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer) {
            rank = (Integer) serviceRanking;
        } else {
            rank = 0;
        }
    }

    @Override
    public Integer getRank() {
        return rank;
    }

    @Override
    public @Nullable StateDescriptionFragment getStateDescriptionFragment(@NonNull String itemName,
            @Nullable Locale locale) {
        StateDescription channelStateDescription = getStateDescription(itemName, locale);
        if (channelStateDescription != null) {
            return StateDescriptionFragmentBuilder.create(channelStateDescription).build();
        }

        return null;
    }

    private StateDescription getStateDescription(String itemName, Locale locale) {
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
                        if (channelType.getItemType().equalsIgnoreCase(CoreItemFactory.STRING)) {
                            pattern = "%s";
                        } else if (channelType.getItemType().startsWith(CoreItemFactory.NUMBER)) {
                            pattern = "%.0f";
                        }
                        if (pattern != null) {
                            logger.trace("Provide a default pattern {} for item {}", pattern, itemName);
                            if (stateDescription == null) {
                                stateDescription = new StateDescription(null, null, null, pattern, false, null);
                            } else {
                                stateDescription = new StateDescription(stateDescription.getMinimum(),
                                        stateDescription.getMaximum(), stateDescription.getStep(), pattern,
                                        stateDescription.isReadOnly(), stateDescription.getOptions());
                            }
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

    private StateDescription getDynamicStateDescription(Channel channel, StateDescription originalStateDescription,
            Locale locale) {
        for (DynamicStateDescriptionProvider provider : dynamicStateDescriptionProviders) {
            StateDescription stateDescription = provider.getStateDescription(channel, originalStateDescription, locale);
            if (stateDescription != null) {
                return stateDescription;
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
    protected void addDynamicStateDescriptionProvider(DynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.dynamicStateDescriptionProviders.add(dynamicStateDescriptionProvider);
    }

    protected void removeDynamicStateDescriptionProvider(
            DynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.dynamicStateDescriptionProviders.remove(dynamicStateDescriptionProvider);
    }

}
