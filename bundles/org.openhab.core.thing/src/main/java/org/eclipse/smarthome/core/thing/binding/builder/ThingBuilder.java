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
package org.eclipse.smarthome.core.thing.binding.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.internal.ThingImpl;
import org.eclipse.smarthome.core.thing.util.ThingHelper;

/**
 * This class allows the easy construction of a {@link Thing} instance using the builder pattern.
 *
 * @author Dennis Nobel - Initial contribution and API
 * @author Kai Kreuzer - Refactoring to make BridgeBuilder a subclass
 *
 */
@NonNullByDefault
public class ThingBuilder {

    private final ThingImpl thing;

    protected ThingBuilder(ThingImpl thing) {
        this.thing = thing;
    }

    public static ThingBuilder create(ThingTypeUID thingTypeUID, String thingId) {
        ThingImpl thing = new ThingImpl(thingTypeUID, thingId);
        return new ThingBuilder(thing);
    }

    @Deprecated
    public static ThingBuilder create(ThingUID thingUID) {
        ThingImpl thing = new ThingImpl(thingUID);
        return new ThingBuilder(thing);
    }

    public static ThingBuilder create(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        ThingImpl thing = new ThingImpl(thingTypeUID, thingUID);
        return new ThingBuilder(thing);
    }

    public ThingBuilder withLabel(@Nullable String label) {
        this.thing.setLabel(label);
        return this;
    }

    public ThingBuilder withChannel(Channel channel) {
        final Collection<Channel> mutableThingChannels = this.thing.getChannelsMutable();
        validateChannelUIDs(Collections.singletonList(channel));
        ThingHelper.ensureUniqueChannels(mutableThingChannels, channel);
        mutableThingChannels.add(channel);
        return this;
    }

    public ThingBuilder withChannels(Channel... channels) {
        return withChannels(Arrays.asList(channels));
    }

    public ThingBuilder withChannels(List<Channel> channels) {
        validateChannelUIDs(channels);
        ThingHelper.ensureUniqueChannels(channels);
        this.thing.setChannels(new ArrayList<>(channels));
        return this;
    }

    public ThingBuilder withoutChannel(ChannelUID channelUID) {
        Iterator<Channel> iterator = this.thing.getChannelsMutable().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getUID().equals(channelUID)) {
                iterator.remove();
                break;
            }
        }
        return this;
    }

    public ThingBuilder withoutChannels(Channel... channels) {
        return withoutChannels(Arrays.asList(channels));
    }

    public ThingBuilder withoutChannels(List<Channel> channels) {
        for (Channel channel : channels) {
            withoutChannel(channel.getUID());
        }
        return this;
    }

    public ThingBuilder withConfiguration(Configuration thingConfiguration) {
        this.thing.setConfiguration(thingConfiguration);
        return this;
    }

    public ThingBuilder withBridge(@Nullable ThingUID bridgeUID) {
        this.thing.setBridgeUID(bridgeUID);
        return this;
    }

    public ThingBuilder withProperties(Map<String, String> properties) {
        for (String key : properties.keySet()) {
            this.thing.setProperty(key, properties.get(key));
        }
        return this;
    }

    public ThingBuilder withLocation(@Nullable String location) {
        this.thing.setLocation(location);
        return this;
    }

    public Thing build() {
        return this.thing;
    }

    private void validateChannelUIDs(List<Channel> channels) {
        for (Channel channel : channels) {
            if (!thing.getUID().equals(channel.getUID().getThingUID())) {
                throw new IllegalArgumentException(
                        "Channel UID '" + channel.getUID() + "' does not match thing UID '" + thing.getUID() + "'");
            }
        }
    }
}
