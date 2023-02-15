/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    /**
     * Create a new {@link ThingBuilder}
     *
     * @param thingTypeUID the {@link ThingTypeUID} of the new thing
     * @param thingId the id part of the {@link ThingUID} of the new thing
     * @return the created {@link ThingBuilder}
     */
    public static ThingBuilder create(ThingTypeUID thingTypeUID, String thingId) {
        return new ThingBuilder(thingTypeUID, new ThingUID(thingTypeUID, thingId));
    }

    /**
     * Create a new {@link ThingBuilder}
     *
     * @param thingTypeUID the {@link ThingTypeUID} of the new thing
     * @param thingUID the {@link ThingUID} of the new thing
     * @return the created {@link ThingBuilder}
     */
    public static ThingBuilder create(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        return new ThingBuilder(thingTypeUID, thingUID);
    }

    /**
     * Create a new thing {@link ThingBuilder} for a copy of the given thing
     *
     * @param thing the {@link Thing} to create this builder from
     * @return the created {@link ThingBuilder}
     *
     */
    public static ThingBuilder create(Thing thing) {
        return ThingBuilder.create(thing.getThingTypeUID(), thing.getUID()).withBridge(thing.getBridgeUID())
                .withChannels(thing.getChannels()).withConfiguration(thing.getConfiguration())
                .withLabel(thing.getLabel()).withLocation(thing.getLocation()).withProperties(thing.getProperties());
    }

    /**
     * Build the thing
     *
     * @return the {@link Thing}
     */
    public Thing build() {
        final ThingImpl thing = new ThingImpl(thingTypeUID, thingUID);
        return populate(thing);
    }

    /**
     * Sets the <code>label</code> for the thing
     *
     * @param label a string containing the label
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    /**
     * Adds the given channel to the thing
     *
     * @param channel the {@link Channel}
     * @return the {@link ThingBuilder} itself
     * @throws IllegalArgumentException if a channel with the same UID is already present or the {@link ChannelUID} is
     *             not valid
     */
    public ThingBuilder withChannel(Channel channel) {
        validateChannelUIDs(List.of(channel));
        ThingHelper.ensureUniqueChannels(channels, channel);
        channels.add(channel);
        return this;
    }

    /**
     * Replaces all channels of this thing with the given channels
     *
     * @param channels one or more {@link Channel}s
     * @return the {@link ThingBuilder} itself
     * @throws IllegalArgumentException if a channel with the same UID is already present or the {@link ChannelUID} is
     *             not valid
     */
    public ThingBuilder withChannels(Channel... channels) {
        return withChannels(Arrays.asList(channels));
    }

    /**
     * Replaces all channels of this thing with the given channels
     *
     * @param channels a {@link List} of {@link Channel}s
     * @return the {@link ThingBuilder} itself
     * @throws IllegalArgumentException if a channel with the same UID is already present or the {@link ChannelUID} is
     *             not valid
     */
    public ThingBuilder withChannels(List<Channel> channels) {
        validateChannelUIDs(channels);
        ThingHelper.ensureUniqueChannels(channels);
        this.channels.clear();
        this.channels.addAll(channels);
        return this;
    }

    /**
     * Removes the channel with the given UID from the thing
     *
     * @param channelUID the {@link ChannelUID} of the channel
     * @return the {@link ThingBuilder} itself
     */
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

    /**
     * Removes the given channels from the thing
     *
     * @param channels one or more {@link Channel}s
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withoutChannels(Channel... channels) {
        return withoutChannels(Arrays.asList(channels));
    }

    /**
     * Removes the given channels from the thing
     *
     * @param channels a {@link List} of {@link Channel}s
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withoutChannels(List<Channel> channels) {
        for (Channel channel : channels) {
            withoutChannel(channel.getUID());
        }
        return this;
    }

    /**
     * Set (or replace) the configuration of the thing
     *
     * @param configuration a {@link Configuration} for this thing
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Set the bridge for this thing
     *
     * @param bridgeUID the {@link ThingUID} of the bridge for the thing
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withBridge(@Nullable ThingUID bridgeUID) {
        this.bridgeUID = bridgeUID;
        return this;
    }

    /**
     * Set / replace a single property for this thing
     *
     * @param key the key / name of the property
     * @param value the value of the property
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withProperty(String key, String value) {
        Map<String, String> oldProperties = Objects.requireNonNullElse(this.properties, Map.of());
        Map<String, String> newProperties = new HashMap<>(oldProperties);
        newProperties.put(key, value);
        return withProperties(newProperties);
    }

    /**
     * Set/replace the properties for this thing
     *
     * @param properties a {@link Map<String, String>} containing the properties
     * @return the {@link ThingBuilder} itself
     */
    public ThingBuilder withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Set the location for this thing
     *
     * @param location a string wih the location of the thing
     * @return the {@link ThingBuilder} itself
     */
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
