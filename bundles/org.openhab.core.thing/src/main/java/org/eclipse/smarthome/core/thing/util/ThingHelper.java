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
package org.eclipse.smarthome.core.thing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTOMapper;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.eclipse.smarthome.core.thing.internal.ThingImpl;

/**
 * {@link ThingHelper} provides a utility method to create and bind items.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Andre Fuechsel - graceful creation of items and links
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingTypeDescription
 * @author Dennis Nobel - Removed createAndBindItems method
 * @author Kai Kreuzer - Added merge method
 */
public class ThingHelper {

    /**
     * Indicates whether two {@link Thing}s are technical equal.
     *
     * @param a Thing object
     * @param b another Thing object
     * @return true whether a and b are equal, otherwise false
     */
    public static boolean equals(Thing a, Thing b) {
        if (!a.getUID().equals(b.getUID())) {
            return false;
        }
        // bridge
        if (!Objects.equals(a.getBridgeUID(), b.getBridgeUID())) {
            return false;
        }
        // configuration
        if (!Objects.equals(a.getConfiguration(), b.getConfiguration())) {
            return false;
        }
        // label
        if (!Objects.equals(a.getLabel(), b.getLabel())) {
            return false;
        }
        // location
        if (!Objects.equals(a.getLocation(), b.getLocation())) {
            return false;
        }
        // channels
        List<Channel> channelsOfA = a.getChannels();
        List<Channel> channelsOfB = b.getChannels();
        if (channelsOfA.size() != channelsOfB.size()) {
            return false;
        }
        if (!toString(channelsOfA).equals(toString(channelsOfB))) {
            return false;
        }
        return true;
    }

    private static String toString(List<Channel> channels) {
        List<String> strings = new ArrayList<>(channels.size());
        for (Channel channel : channels) {
            strings.add(channel.getUID().toString() + '#' + channel.getAcceptedItemType() + '#' + channel.getKind());
        }
        Collections.sort(strings);
        return strings.stream().collect(Collectors.joining(","));
    }

    public static void addChannelsToThing(Thing thing, Collection<Channel> channels) {
        List<Channel> mutableChannels = ((ThingImpl) thing).getChannelsMutable();
        ensureUniqueChannels(mutableChannels, channels);
        mutableChannels.addAll(channels);
    }

    public static void ensureUnique(Collection<Channel> channels) {
        HashSet<UID> ids = new HashSet<>();
        for (Channel channel : channels) {
            if (!ids.add(channel.getUID())) {
                throw new IllegalArgumentException("Duplicate channels " + channel.getUID().getAsString());
            }
        }
    }

    /**
     * Ensures that there are no duplicate channels in the array (i.e. not using the same ChannelUID)
     *
     * @param channels the channels to check
     * @throws IllegalArgumentException in case there are duplicate channels found
     */
    public static void ensureUniqueChannels(final Channel[] channels) {
        @SuppressWarnings("unchecked")
        final Iterator<Channel> it = new ArrayIterator(channels);

        ensureUniqueChannels(it, new HashSet<UID>(channels.length));
    }

    /**
     * Ensures that there are no duplicate channels in the collection (i.e. not using the same ChannelUID)
     *
     * @param channels the channels to check
     * @throws IllegalArgumentException in case there are duplicate channels found
     */
    public static void ensureUniqueChannels(final Collection<Channel> channels) {
        ensureUniqueChannels(channels.iterator(), new HashSet<UID>(channels.size()));
    }

    /**
     * Ensures that there are no duplicate channels in the collection plus the additional one (i.e. not using the same
     * ChannelUID)
     *
     * @param channels the {@link List} of channels to check
     * @param channel an additional channel
     * @throws IllegalArgumentException in case there are duplicate channels found
     */
    public static void ensureUniqueChannels(final Collection<Channel> channels, final Channel channel) {
        ensureUniqueChannels(channels, Collections.singleton(channel));
    }

    private static void ensureUniqueChannels(final Collection<Channel> channels1, final Collection<Channel> channels2) {
        ensureUniqueChannels(channels1.iterator(),
                ensureUniqueChannels(channels2.iterator(), new HashSet<UID>(channels1.size() + channels2.size())));
    }

    private static HashSet<UID> ensureUniqueChannels(final Iterator<Channel> channels, final HashSet<UID> ids) {
        while (channels.hasNext()) {
            final Channel channel = channels.next();
            if (!ids.add(channel.getUID())) {
                throw new IllegalArgumentException("Duplicate channels " + channel.getUID().getAsString());
            }
        }
        return ids;
    }

    /**
     * Merges the content of a ThingDTO with an existing Thing.
     * Where ever the DTO has null values, the content of the original Thing is kept.
     * Where ever the DTO has non-null values, these are used.
     * In consequence, care must be taken when the content of a list (like configuration, properties or channels) is to
     * be updated - the DTO must contain the full list, otherwise entries will be deleted.
     *
     * @param thing the Thing instance to merge the new content into
     * @param updatedContents a DTO which carries the updated content
     * @return A Thing instance, which is the result of the merge
     */
    public static Thing merge(Thing thing, ThingDTO updatedContents) {
        ThingBuilder builder;

        if (thing instanceof Bridge) {
            builder = BridgeBuilder.create(thing.getThingTypeUID(), thing.getUID());
        } else {
            builder = ThingBuilder.create(thing.getThingTypeUID(), thing.getUID());
        }

        // Update the label
        if (updatedContents.label != null) {
            builder.withLabel(updatedContents.label);
        } else {
            builder.withLabel(thing.getLabel());
        }

        // Update the location
        if (updatedContents.location != null) {
            builder.withLocation(updatedContents.location);
        } else {
            builder.withLocation(thing.getLocation());
        }

        // update bridge UID
        if (updatedContents.bridgeUID != null) {
            builder.withBridge(new ThingUID(updatedContents.bridgeUID));
        } else {
            builder.withBridge(thing.getBridgeUID());
        }

        // update thing configuration
        if (updatedContents.configuration != null && !updatedContents.configuration.keySet().isEmpty()) {
            builder.withConfiguration(new Configuration(updatedContents.configuration));
        } else {
            builder.withConfiguration(thing.getConfiguration());
        }

        // update thing properties
        if (updatedContents.properties != null) {
            builder.withProperties(updatedContents.properties);
        } else {
            builder.withProperties(thing.getProperties());
        }

        // Update the channels
        if (updatedContents.channels != null) {
            for (ChannelDTO channelDTO : updatedContents.channels) {
                builder.withChannel(ChannelDTOMapper.map(channelDTO));
            }
        } else {
            builder.withChannels(thing.getChannels());
        }

        if (updatedContents.location != null) {
            builder.withLocation(updatedContents.location);
        } else {
            builder.withLocation(thing.getLocation());
        }

        Thing mergedThing = builder.build();

        // keep all child things in place on a merged bridge
        if (mergedThing instanceof BridgeImpl && thing instanceof Bridge) {
            Bridge bridge = (Bridge) thing;
            BridgeImpl mergedBridge = (BridgeImpl) mergedThing;
            for (Thing child : bridge.getThings()) {
                mergedBridge.addThing(child);
            }
        }

        return mergedThing;
    }
}
