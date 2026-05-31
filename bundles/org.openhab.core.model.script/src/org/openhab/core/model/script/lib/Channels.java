/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.lib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * {@link Channels} provides DSL access to channel manipulation.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Channels {

    /**
     * Get the {@link ItemChannelLink}s that are linked to the specified item.
     *
     * @param itemName the name of the item.
     * @return The {@link Set} of {@link ItemChannelLink}s.
     */
    public static Set<ItemChannelLink> getLinks(@Nullable String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinks(itemName);
    }

    /**
     * Get the {@link ItemChannelLink}s that are linked to the specified item.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of {@link ItemChannelLink}s.
     */
    public static Set<ItemChannelLink> getLinks(@Nullable Item item) {
        if (item == null) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinks(item.getName());
    }

    /**
     * Get the {@link ItemChannelLink}s that are linked to the specified channel.
     *
     * @param channelUid the UID of the channel.
     * @return The {@link Set} of {@link ItemChannelLink}s.
     */
    public static Set<ItemChannelLink> getChannelLinks(@Nullable String channelUid) {
        if (channelUid == null || channelUid.isBlank()) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinks(new ChannelUID(channelUid));
    }

    /**
     * Get the {@link ItemChannelLink}s that are linked to the specified channel.
     *
     * @param channelUid the {@link ChannelUID} of the channel.
     * @return The {@link Set} of {@link ItemChannelLink}s.
     */
    public static Set<ItemChannelLink> getChannelLinks(@Nullable ChannelUID channelUid) {
        if (channelUid == null) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinks(channelUid);
    }

    /**
     * Get the {@link ChannelUID}s of the channels that are bound to the specified {@link Item}.
     *
     * @param itemName the name of the item.
     * @return The {@link Set} of {@link ChannelUID}s of the bound channels.
     */
    public static Set<ChannelUID> getBoundChannels(@Nullable String itemName) {
        if (itemName == null) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getBoundChannels(itemName);
    }

    /**
     * Get the {@link ChannelUID}s of the channels that are bound to the specified {@link Item}.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of {@link ChannelUID}s of the bound channels.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     */
    @NonNullByDefault({})
    public static @NonNull Set<@NonNull ChannelUID> getBoundChannels(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getBoundChannels(item.getName());
    }

    /**
     * Get the {@link Thing}s that are bound to the specified {@link Item}.
     *
     * @param itemName the name of the item.
     * @return The {@link Set} of bound {@link Thing}s.
     */
    public static Set<Thing> getBoundThings(@Nullable String itemName) {
        if (itemName == null) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getBoundThings(itemName);
    }

    /**
     * Get the {@link Thing}s that are bound to the specified {@link Item}.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of bound {@link Thing}s.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     */
    @NonNullByDefault({})
    public static @NonNull Set<@NonNull Thing> getBoundThings(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getBoundThings(item.getName());
    }

    /**
     * Get the names of all the items that are linked to a specific channel.
     *
     * @param channelUid the UID of the channel.
     * @return The {@link Set} of names of the linked items.
     */
    public static Set<String> getLinkedItemNames(@Nullable String channelUid) {
        if (channelUid == null || channelUid.isBlank()) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinkedItemNames(new ChannelUID(channelUid));
    }

    /**
     * Get the names of all the items that are linked to a specific channel.
     *
     * @param channelUid the {@link ChannelUID} of the channel.
     * @return The {@link Set} of names of the linked items.
     * @throws IllegalArgumentException If {@code channelUid} is {@code null}.
     */
    @NonNullByDefault({})
    public static @NonNull Set<@NonNull String> getLinkedItemNames(ChannelUID channelUid) {
        if (channelUid == null) {
            throw new IllegalArgumentException("channelUid cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinkedItemNames(channelUid);
    }

    /**
     * Get all {@link Item}s that are linked to a specific channel.
     *
     * @param channelUid the UID of the channel.
     * @return The {@link Set} of linked {@link Item}s.
     */
    public static Set<Item> getLinkedItems(@Nullable String channelUid) {
        if (channelUid == null || channelUid.isBlank()) {
            return Set.of();
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinkedItems(new ChannelUID(channelUid));
    }

    /**
     * Get all {@link Item}s that are linked to a specific channel.
     *
     * @param channelUid the {@link ChannelUID} of the channel.
     * @return The {@link Set} of linked {@link Item}s.
     * @throws IllegalArgumentException If {@code channelUid} is {@code null}.
     */
    @NonNullByDefault({})
    public static @NonNull Set<@NonNull Item> getLinkedItems(ChannelUID channelUid) {
        if (channelUid == null) {
            throw new IllegalArgumentException("channelUid cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().getLinkedItems(channelUid);
    }

    /**
     * Check if the specified item has at least one link.
     *
     * @param itemName the name of the item to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(itemName);
    }

    /**
     * Check if the specified item has at least one link.
     *
     * @param item the {@link Item} to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item) {
        if (item == null) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(item.getName());
    }

    /**
     * Check if the specified item and channel are linked.
     *
     * @param itemName the name of the item to check.
     * @param channelUid the UID of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable String itemName, @Nullable String channelUid) {
        if (itemName == null || itemName.isBlank() || channelUid == null || channelUid.isBlank()) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(itemName, new ChannelUID(channelUid));
    }

    /**
     * Check if the specified item and channel are linked.
     *
     * @param item the {@link Item} to check.
     * @param channelUid the UID of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item, @Nullable String channelUid) {
        if (item == null || channelUid == null || channelUid.isBlank()) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(item.getName(), new ChannelUID(channelUid));
    }

    /**
     * Check if the specified item and channel are linked.
     *
     * @param item the {@link Item} to check.
     * @param channelUid the {@link ChannelUID} of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item, @Nullable ChannelUID channelUid) {
        if (item == null || channelUid == null) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(item.getName(), channelUid);
    }

    /**
     * Check if the specified channel has at least one link.
     *
     * @param channelUid the UID of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isChannelLinked(@Nullable String channelUid) {
        if (channelUid == null || channelUid.isBlank()) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(new ChannelUID(channelUid));
    }

    /**
     * Check if the specified channel has at least one link.
     *
     * @param channelUid the {@link ChannelUID} of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isChannelLinked(@Nullable ChannelUID channelUid) {
        if (channelUid == null) {
            return false;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().isLinked(channelUid);
    }

    /**
     * Get an existing {@link ItemChannelLink} for the specified item and channel.
     *
     * @param item the {@link Item}.
     * @param channelUid the {@link ChannelUID} of the channel.
     * @return The {@link ItemChannelLink} or {@code null} if none were found.
     * @throws IllegalArgumentException If {@code item} or {@code channelUid} is {@code null}.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink getLink(Item item, ChannelUID channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        if (channelUid == null) {
            throw new IllegalArgumentException("channelUid cannot be null");
        }
        return getLink(item.getName(), channelUid.getAsString());
    }

    /**
     * Get an existing {@link ItemChannelLink} for the specified item and channel.
     *
     * @param item the {@link Item}.
     * @param channelUid the UID of the channel.
     * @return The {@link ItemChannelLink} or {@code null} if none were found.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink getLink(Item item, @Nullable String channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return getLink(item.getName(), channelUid);
    }

    /**
     * Get an existing {@link ItemChannelLink} for the specified item and channel.
     *
     * @param itemName the name of the item.
     * @param channelUid the UID of the channel.
     * @return The {@link ItemChannelLink} or {@code null} if none were found.
     */
    public static @Nullable ItemChannelLink getLink(@Nullable String itemName, @Nullable String channelUid) {
        if (itemName == null || channelUid == null) {
            return null;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().get(itemName + " -> " + channelUid);
    }

    /**
     * Add a new {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @return The newly created {@link ItemChannelLink}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, the {@link ItemChannelLink} already exists,
     *             or {@code channelUid} is invalid.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static ItemChannelLink addItemChannelLink(Item item, String channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry()
                .add(new ItemChannelLink(item.getName(), new ChannelUID(channelUid)));
    }

    /**
     * Add a new {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration.
     *            properties for the link. Must be in pairs, the first is the key, the second is the value.
     * @return The newly created {@link ItemChannelLink}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, {@code channelUid} is invalid, the
     *             {@link ItemChannelLink} already exists, or if there is an odd number of {@code configProperties}, or
     *             if any of the keys aren't {@link String}s.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    public static ItemChannelLink addItemChannelLink(Item item, String channelUid, Object... configProperties) {
        Map<@Nullable String, @Nullable Object> props = Map.copyOf(parseObjectArray(configProperties));
        return addItemChannelLink(item, channelUid, props);
    }

    /**
     * Add a new {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the map of configuration properties for the link.
     * @return The newly created {@link ItemChannelLink}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, the {@link ItemChannelLink} already exists,
     *             or {@code channelUid} is invalid.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static ItemChannelLink addItemChannelLink(Item item, String channelUid,
            @Nullable Map<@Nullable String, @Nullable Object> configProperties) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().add(
                new ItemChannelLink(item.getName(), new ChannelUID(channelUid), new Configuration(configProperties)));
    }

    /**
     * Add or replace a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @return The old {@link ItemChannelLink} if one existed, or {@code null}.
     * @throws IllegalArgumentException If {@code item} is {@code null} or {@code channelUid} is invalid.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink replaceItemChannelLink(Item item, String channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry()
                .update(new ItemChannelLink(item.getName(), new ChannelUID(channelUid)));
    }

    /**
     * Add or replace a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration.
     *            properties for the link. Must be in pairs, the first is the key, the second is the value.
     * @return The old {@link ItemChannelLink} if one existed, or {@code null}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, {@code channelUid} is invalid, or if there is
     *             an odd number of {@code configProperties}, or if any of the keys aren't {@link String}s.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    public static @Nullable ItemChannelLink replaceItemChannelLink(Item item, String channelUid,
            Object... configProperties) {
        Map<@Nullable String, @Nullable Object> props = Map.copyOf(parseObjectArray(configProperties));
        return replaceItemChannelLink(item, channelUid, props);
    }

    /**
     * Add or replace a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the map of configuration properties for the link.
     * @return The old {@link ItemChannelLink} if one existed, or {@code null}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, or {@code channelUid} is invalid.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink replaceItemChannelLink(Item item, String channelUid,
            @Nullable Map<@Nullable String, @Nullable Object> configProperties) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().update(
                new ItemChannelLink(item.getName(), new ChannelUID(channelUid), new Configuration(configProperties)));
    }

    /**
     * Remove a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} of the link to remove.
     * @param channelUid the UID of the channel of the link to remove.
     * @return The removed {@link ItemChannelLink} or {@code null} if none existed.
     * @throws IllegalArgumentException If {@code item} or {@code channelUid} is {@code null}.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink removeItemChannelLink(Item item, ChannelUID channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        if (channelUid == null) {
            throw new IllegalArgumentException("channelUid cannot be null");
        }
        return removeItemChannelLink(item.getName(), channelUid.getAsString());
    }

    /**
     * Remove a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} of the link to remove.
     * @param channelUid the UID of the channel of the link to remove.
     * @return The removed {@link ItemChannelLink} or {@code null} if none existed.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    @NonNullByDefault({})
    public static @Nullable ItemChannelLink removeItemChannelLink(Item item, @Nullable String channelUid) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return removeItemChannelLink(item.getName(), channelUid);
    }

    /**
     * Remove a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param itemName the name of the item of the link to remove.
     * @param channelUid the UID of the channel of the link to remove.
     * @return The removed {@link ItemChannelLink} or {@code null} if none existed.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    public static @Nullable ItemChannelLink removeItemChannelLink(@Nullable String itemName,
            @Nullable String channelUid) {
        if (itemName == null || channelUid == null) {
            return null;
        }
        return ScriptServiceUtil.getItemChannelLinkRegistry().remove(itemName + " -> " + channelUid);
    }

    /**
     * Remove all managed links related to the specified item.
     *
     * @param itemName the name of the item.
     * @return The number of removed links.
     */
    public static int removeLinksForItem(@Nullable String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return 0;
        }

        return ScriptServiceUtil.getItemChannelLinkRegistry().removeLinksForItem(itemName);
    }

    /**
     * Remove all managed links related to the specified item.
     *
     * @param item the {@link Item}.
     * @return The number of removed links.
     */
    public static int removeLinksForItem(@Nullable Item item) {
        if (item == null) {
            return 0;
        }

        return ScriptServiceUtil.getItemChannelLinkRegistry().removeLinksForItem(item.getName());
    }

    /**
     * Remove all orphaned (item or channel missing) managed links.
     * 
     * @return The number of removed links.
     */
    public static int removeOrphanedItemChannelLinks() {
        return ScriptServiceUtil.getItemChannelLinkRegistry().purge();
    }

    /**
     * Transforms pairs of {@link Object}s into a {@link Map}. The former of each pair (the key) must be a
     * {@link String}.
     *
     * @param objects the array of {@link Object}s to transform.
     * @return The resulting {@link Map}.
     * @throws IllegalArgumentException If there is an odd number of objects, or if any of the keys aren't
     *             {@link String}s.
     */
    private static Map<String, Object> parseObjectArray(Object @Nullable [] objects) throws IllegalArgumentException {
        if (objects == null || objects.length == 0) {
            return Map.of();
        }
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException("There must be an even number of objects (" + objects.length + ')');
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String key) {
                result.put(key, objects[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + objects[i]);
            }
        }
        return result;
    }
}
