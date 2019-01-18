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
package org.eclipse.smarthome.core.thing.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link ThingTypeRegistry} tracks all {@link ThingType}s provided by registered {@link ThingTypeProvider}s.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Dennis Nobel - Added locale support
 */
@Component(immediate = true, service = ThingTypeRegistry.class)
public class ThingTypeRegistry {

    private final List<ThingTypeProvider> thingTypeProviders = new CopyOnWriteArrayList<>();
    private ChannelTypeRegistry channelTypeRegistry;

    /**
     * Returns all thing types.
     *
     * @param locale locale (can be null)
     * @return all thing types
     */
    public List<ThingType> getThingTypes(Locale locale) {
        List<ThingType> thingTypes = new ArrayList<>();
        for (ThingTypeProvider thingTypeProvider : thingTypeProviders) {
            thingTypes.addAll(thingTypeProvider.getThingTypes(locale));
        }
        return Collections.unmodifiableList(thingTypes);
    }

    /**
     * Returns all thing types.
     *
     * @return all thing types
     */
    public List<ThingType> getThingTypes() {
        return getThingTypes((Locale) null);
    }

    /**
     * Returns thing types for a given binding id.
     *
     * @param bindingId binding id
     * @param locale locale (can be null)
     * @return thing types for given binding id
     */
    public List<ThingType> getThingTypes(String bindingId, Locale locale) {
        List<ThingType> thingTypesForBinding = new ArrayList<>();

        for (ThingType thingType : getThingTypes()) {
            if (thingType.getBindingId().equals(bindingId)) {
                thingTypesForBinding.add(thingType);
            }
        }

        return Collections.unmodifiableList(thingTypesForBinding);
    }

    /**
     * Returns thing types for a given binding id.
     *
     * @param bindingId binding id
     * @return thing types for given binding id
     */
    public List<ThingType> getThingTypes(String bindingId) {
        return getThingTypes(bindingId, null);
    }

    /**
     * Returns a thing type for a given thing type UID.
     *
     * @param thingTypeUID thing type UID
     * @param locale locale (can be null)
     * @return thing type for given UID or null if no thing type with this UID
     *         was found
     */
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        for (ThingTypeProvider thingTypeProvider : thingTypeProviders) {
            ThingType thingType = thingTypeProvider.getThingType(thingTypeUID, locale);
            if (thingType != null) {
                return thingType;
            }
        }

        return null;
    }

    /**
     * Returns a thing type for a given thing type UID.
     *
     * @param thingTypeUID thing type UID
     * @return thing type for given UID or null if no thing type with this UID
     *         was found
     */
    public ThingType getThingType(ThingTypeUID thingTypeUID) {
        return getThingType(thingTypeUID, null);
    }

    /**
     * Returns the channel type for a given channel.
     *
     * <p>
     * <strong>Attention:</strong> If you iterate over multiple channels to find the according channel types, please
     * fetch the thing type first using
     * {@link ThingTypeRegistry#getThingType(ThingTypeUID)} and use
     * {@link ThingType#getChannelType(ChannelUID)} afterwards.
     *
     * @param channel channel
     * @return channel type or null if no channel type was found
     */
    public ChannelType getChannelType(Channel channel) {
        return getChannelType(channel, null);
    }

    /**
     * Returns the channel type for a given channel and locale.
     *
     * <p>
     * <strong>Attention:</strong> If you iterate over multiple channels to find the according channel types, please
     * fetch the thing type first using
     * {@link ThingTypeRegistry#getThingType(ThingTypeUID)} and use
     * {@link ThingType#getChannelType(ChannelUID)} afterwards.
     *
     * @param channel channel
     * @param locale locale (can be null)
     * @return channel type or null if no channel type was found
     */
    public ChannelType getChannelType(Channel channel, Locale locale) {
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeUID != null) {
            return channelTypeRegistry.getChannelType(channelTypeUID, locale);
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingTypeProvider(ThingTypeProvider thingTypeProvider) {
        if (thingTypeProvider != null) {
            this.thingTypeProviders.add(thingTypeProvider);
        }
    }

    protected void removeThingTypeProvider(ThingTypeProvider thingTypeProvider) {
        if (thingTypeProvider != null) {
            this.thingTypeProviders.remove(thingTypeProvider);
        }
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

}
