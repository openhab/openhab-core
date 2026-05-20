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
package org.openhab.core.model.script.helper;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.link.ItemChannelLink;

/**
 * {@link ItemExtensions} provides DSL {@link Item} extensions.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ItemExtensions {

    /**
     * Add metadata to an item.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, if {@code namespace} is
     *             invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static void addMetadata(Item item, String namespace, String value) {
        Items.addMetadata(item.getName(), namespace, value, (String) null);
    }

    /**
     * Add metadata to an item.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, if {@code namespace} is
     *             invalid, or if there is an odd number of {@code configProperties}, or if any of the keys aren't
     *             {@link String}s.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static void addMetadata(Item item, String namespace, String value, Object... configProperties) {
        Items.addMetadata(item.getName(), namespace, value, configProperties);
    }

    /**
     * Add metadata to an item.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @param configuration the {@link Map} of configuration properties that make up the configuration.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, or if {@code namespace}
     *             is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static void addMetadata(Item item, String namespace, String value,
            @Nullable Map<String, Object> configuration) {
        Items.addMetadata(item.getName(), namespace, value, configuration);
    }

    /**
     * Get item metadata for the specified namespace.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @return The matching {@link Metadata} or {@code null}.
     */
    public static @Nullable Metadata getMetadata(Item item, String namespace) {
        return Items.getMetadata(item.getName(), namespace);
    }

    /**
     * Remove metadata from an item for the specified namespace.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @return The removed {@link Metadata} or {@code null} if no such metadata existed.
     */
    public static @Nullable Metadata removeMetadata(Item item, String namespace) {
        return Items.removeMetadata(item.getName(), namespace);
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, or if {@code namespace}
     *             is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value) {
        return Items.updateMetadata(item.getName(), namespace, value);
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, or is {@code namespace}
     *             is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            Object... configuration) {
        return Items.updateMetadata(item.getName(), namespace, value, configuration);
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param item the {@link Item}.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @param configuration the {@link Map} of configuration properties that make up the configuration.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace} or {@code value} is {@code null}, or if {@code namespace}
     *             is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            @Nullable Map<String, Object> configuration) {
        return Items.updateMetadata(item.getName(), namespace, value, configuration);
    }

    /**
     * Get the {@link ItemChannelLink}s that are linked to the specified item.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of {@link ItemChannelLink}s.
     */
    public static Set<ItemChannelLink> getChannelLinks(@Nullable Item item) {
        return Channels.getLinks(item);
    }

    /**
     * Get the {@link ChannelUID}s of the channels that are bound to the specified {@link Item}.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of {@link ChannelUID}s of the bound channels.
     */
    public static Set<ChannelUID> getBoundChannels(Item item) {
        return Channels.getBoundChannels(item);
    }

    /**
     * Get the {@link Thing}s that are bound to the specified {@link Item}.
     *
     * @param item the {@link Item}.
     * @return The {@link Set} of bound {@link Thing}s.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     */
    public static Set<Thing> getBoundThings(Item item) {
        return Channels.getBoundThings(item);
    }

    /**
     * Check if the specified item has at least one link.
     *
     * @param item the {@link Item} to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item) {
        return Channels.isLinked(item);
    }

    /**
     * Check if the specified item and channel are linked.
     *
     * @param item the {@link Item} to check.
     * @param channelUid the UID of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item, @Nullable String channelUid) {
        return Channels.isLinked(item, channelUid);
    }

    /**
     * Check if the specified item and channel are linked.
     *
     * @param item the {@link Item} to check.
     * @param channelUid the {@link ChannelUID} of the channel to check.
     * @return {@code true} if a link exists, {@code false} otherwise.
     */
    public static boolean isLinked(@Nullable Item item, @Nullable ChannelUID channelUid) {
        return Channels.isLinked(item, channelUid);
    }

    /**
     * Get an existing {@link ItemChannelLink} for the specified item and channel.
     *
     * @param item the {@link Item}.
     * @param channelUid the {@link ChannelUID} of the channel.
     * @return The {@link ItemChannelLink} or {@code null} if none were found.
     * @throws IllegalArgumentException If {@code item} or {@code channelUid} is {@code null}.
     */
    public static @Nullable ItemChannelLink getChannelLink(Item item, ChannelUID channelUid) {
        return Channels.getLink(item, channelUid);
    }

    /**
     * Get an existing {@link ItemChannelLink} for the specified item and channel.
     *
     * @param item the {@link Item}.
     * @param channelUid the UID of the channel.
     * @return The {@link ItemChannelLink} or {@code null} if none were found.
     * @throws IllegalArgumentException If {@code item} is {@code null}.
     */
    public static @Nullable ItemChannelLink getChannelLink(Item item, @Nullable String channelUid) {
        return Channels.getLink(item, channelUid);
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
    public static ItemChannelLink addChannelLink(Item item, String channelUid) {
        return Channels.addItemChannelLink(item, channelUid);
    }

    /**
     * Add a new {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration
     *            properties for the link. Must be in pairs, the first is the key, the second is the value.
     * @return The newly created {@link ItemChannelLink}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, {@code channelUid} is invalid, the
     *             {@link ItemChannelLink} already exists, or if there is an odd number of {@code configProperties}, or
     *             if any of the keys aren't {@link String}s.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    public static ItemChannelLink addChannelLink(Item item, String channelUid, Object... configProperties) {
        return Channels.addItemChannelLink(item, channelUid, configProperties);
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
    public static ItemChannelLink addChannelLink(Item item, String channelUid,
            @Nullable Map<@Nullable String, @Nullable Object> configProperties) {
        return Channels.addItemChannelLink(item, channelUid, configProperties);
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
    public static @Nullable ItemChannelLink replaceChannelLink(Item item, String channelUid) {
        return Channels.replaceItemChannelLink(item, channelUid);
    }

    /**
     * Add or replace a {@link ItemChannelLink} between an existing {@link Item} and a {@link Channel}.
     *
     * @param item the {@link Item} to link.
     * @param channelUid the UID of the channel to link.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration
     *            properties for the link. Must be in pairs, the first is the key, the second is the value.
     * @return The old {@link ItemChannelLink} if one existed, or {@code null}.
     * @throws IllegalArgumentException If {@code item} is {@code null}, {@code channelUid} is invalid, or if there is
     *             an odd number of {@code configProperties}, or if any of the keys aren't {@link String}s.
     * @throws IllegalStateException If a {@link ManagedProvider} isn't available.
     */
    public static @Nullable ItemChannelLink replaceChannelLink(Item item, String channelUid,
            Object... configProperties) {
        return Channels.replaceItemChannelLink(item, channelUid, configProperties);
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
    public static @Nullable ItemChannelLink replaceChannelLink(Item item, String channelUid,
            @Nullable Map<@Nullable String, @Nullable Object> configProperties) {
        return Channels.replaceItemChannelLink(item, channelUid, configProperties);
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
    public static @Nullable ItemChannelLink removeChannelLink(Item item, ChannelUID channelUid) {
        return Channels.removeItemChannelLink(item, channelUid);
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
    public static @Nullable ItemChannelLink removeChannelLink(Item item, @Nullable String channelUid) {
        return Channels.removeItemChannelLink(item, channelUid);
    }

    /**
     * Remove all managed links related to the specified item.
     *
     * @param item the {@link Item}.
     * @return The number of removed links.
     */
    public static int removeChannelLinks(@Nullable Item item) {
        return Channels.removeLinksForItem(item);
    }
}
