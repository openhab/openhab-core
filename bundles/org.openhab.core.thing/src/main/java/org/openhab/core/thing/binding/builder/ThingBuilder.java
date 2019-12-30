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
package org.openhab.core.thing.binding.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.internal.ThingImpl;
import org.openhab.core.thing.util.ThingHelper;

/**
 * This class allows the easy construction of a {@link Thing} instance using the builder pattern.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - Refactoring to make BridgeBuilder a subclass
 */
@NonNullByDefault
public class ThingBuilder {

    protected final ThingUID thingUID;
    protected final ThingTypeUID thingTypeUID;
    private @Nullable String label;
    private final List<Channel> channels = new ArrayList<>();
    private @Nullable Configuration configuration;
    private @Nullable ThingUID bridgeUID;
    private @Nullable Map<String, String> properties;
    private @Nullable String location;

    protected ThingBuilder(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        this.thingUID = thingUID;
        this.thingTypeUID = thingTypeUID;
    }

    public static ThingBuilder create(ThingTypeUID thingTypeUID, String thingId) {
        return new ThingBuilder(thingTypeUID, new ThingUID(thingTypeUID.getBindingId(), thingTypeUID.getId(), thingId));
    }

    public static ThingBuilder create(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        return new ThingBuilder(thingTypeUID, thingUID);
    }

    public Thing build() {
        final ThingImpl thing = new ThingImpl(thingTypeUID, thingUID);
        return populate(thing);
    }

    public ThingBuilder withLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    public ThingBuilder withChannel(Channel channel) {
        validateChannelUIDs(Collections.singletonList(channel));
        ThingHelper.ensureUniqueChannels(channels, channel);
        channels.add(channel);
        return this;
    }

    public ThingBuilder withChannels(Channel... channels) {
        return withChannels(Arrays.asList(channels));
    }

    public ThingBuilder withChannels(List<Channel> channels) {
        validateChannelUIDs(channels);
        ThingHelper.ensureUniqueChannels(channels);
        this.channels.clear();
        this.channels.addAll(channels);
        return this;
    }

    public ThingBuilder withoutChannel(ChannelUID channelUID) {
        Iterator<Channel> iterator = channels.iterator();
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

    public ThingBuilder withConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public ThingBuilder withBridge(@Nullable ThingUID bridgeUID) {
        this.bridgeUID = bridgeUID;
        return this;
    }

    public ThingBuilder withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ThingBuilder withLocation(@Nullable String location) {
        this.location = location;
        return this;
    }

    protected Thing populate(ThingImpl thing) {
        thing.setLabel(label);
        thing.setChannels(channels);
        thing.setConfiguration(configuration);
        thing.setBridgeUID(bridgeUID);
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                thing.setProperty(entry.getKey(), entry.getValue());
            }
        }
        thing.setLocation(location);
        return thing;
    }

    private void validateChannelUIDs(List<Channel> channels) {
        for (Channel channel : channels) {
            if (!thingUID.equals(channel.getUID().getThingUID())) {
                throw new IllegalArgumentException(
                        "Channel UID '" + channel.getUID() + "' does not match thing UID '" + thingUID + "'");
            }
        }
    }
}
