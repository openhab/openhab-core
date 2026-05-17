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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.model.script.ScriptServiceUtil;

/**
 * {@link Items} provides DSL access to item and metadata manipulation.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Items {

    /**
     * Check whether a named item exists.
     *
     * @param itemName the item name.
     * @return {@code true} if the item exists, {@code false} if it doesn't.
     */
    public static boolean exists(String itemName) {
        return ScriptServiceUtil.getItemRegistry().get(itemName) != null;
    }

    /**
     * Get an item by name.
     *
     * @param itemName the item name.
     * @return The {@link Item} or {@code null} if it doesn't exist.
     */
    public static @Nullable Item get(String itemName) {
        return ScriptServiceUtil.getItemRegistry().get(itemName);
    }

    /**
     * Get all items.
     *
     * @return The {@link Collection} of {@link Item}s.
     */
    public static Collection<Item> getAll() {
        return ScriptServiceUtil.getItemRegistry().getAll();
    }

    /**
     * Get all items that match a pattern using {@code ?} and {@code *}.
     *
     * @param pattern the pattern.
     * @return The {@link Collection} of matching {@link Item}s.
     */
    public static Collection<Item> getByPattern(String pattern) {
        return ScriptServiceUtil.getItemRegistry().getItems(pattern);
    }

    /**
     * Get all items that have all the specified tags.
     *
     * @param tags the tags.
     * @return The {@link Collection} of matching {@link Item}s.
     */
    public static Collection<Item> getByTag(String... tags) {
        return ScriptServiceUtil.getItemRegistry().getItemsByTag(tags);
    }

    /**
     * Get all items of the specified type.
     * <p>
     * Types includes: {@code Call}, {@code Color}, {@code Contact}, {@code DateTime}, {@code Dimmer}, {@code Image},
     * {@code Location}, {@code Number}, {@code Player}, {@code Rollershutter}, {@code String} and {@code Switch}.
     *
     * @param type the type.
     * @return The {@link Collection} of matching {@link Item}s.
     */
    public static Collection<Item> getOfType(String type) {
        return ScriptServiceUtil.getItemRegistry().getItemsOfType(type);
    }

    /**
     * Get all items of the specified type that also have all the specified tags.
     * <p>
     * Types includes: {@code Call}, {@code Color}, {@code Contact}, {@code DateTime}, {@code Dimmer}, {@code Image},
     * {@code Location}, {@code Number}, {@code Player}, {@code Rollershutter}, {@code String} and {@code Switch}.
     *
     * @param type the type.
     * @param tags the tags.
     * @return The {@link Collection} of matching {@link Item}s.
     */
    public static Collection<Item> getByTagAndType(String type, String... tags) {
        return ScriptServiceUtil.getItemRegistry().getItemsByTagAndType(type, tags);
    }

    /**
     * Get item metadata for the specified namespace.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @return The matching {@link Metadata} or {@code null}.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata getMetadata(String itemName, String namespace) {
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        return ScriptServiceUtil.getMetadataRegistry().get(new MetadataKey(namespace, itemName));
    }

    /**
     * Add metadata to an item.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @throws IllegalArgumentException If {@code value} is {@code null}, {@code namespace} is {@code null}, or
     *             {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static void addMetadata(String itemName, String namespace, String value) {
        addMetadata(itemName, namespace, value, (String) null);
    }

    /**
     * Add metadata to an item.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration.
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null}, if
     *             {@code namespace} or {@code itemName} is invalid, or if there is an odd number of
     *             {@code configProperties}, or if any of the keys aren't {@link String}s.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static void addMetadata(String itemName, String namespace, String value, Object... configProperties) {
        addMetadata(itemName, namespace, value, parseObjectArray(configProperties));
    }

    /**
     * Add metadata to an item.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the metadata value.
     * @param configuration the {@link Map} of configuration properties that make up the configuration.
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null}, or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    @NonNullByDefault({})
    public static void addMetadata(String itemName, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        ScriptServiceUtil.getMetadataRegistry()
                .add(new Metadata(new MetadataKey(namespace, itemName), value, configuration));
    }

    /**
     * Remove metadata from an item for the specified namespace.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @return The removed {@link Metadata} or {@code null} if no such metadata existed.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata removeMetadata(String itemName, String namespace) {
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        return ScriptServiceUtil.getMetadataRegistry().remove(new MetadataKey(namespace, itemName));
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null}, or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static @Nullable Metadata updateMetadata(String itemName, String namespace, String value) {
        return updateMetadata(itemName, namespace, value, (Map<String, Object>) null);
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @param configProperties the pairs of {@link String}s and {@link Object}s that constitutes the configuration.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null}, if
     *             {@code namespace} or {@code itemName} is invalid, or if there is an odd number of
     *             {@code configProperties}, or if any of the keys aren't {@link String}s.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    public static @Nullable Metadata updateMetadata(String itemName, String namespace, String value,
            Object... configProperties) {
        return updateMetadata(itemName, namespace, value, parseObjectArray(configProperties));
    }

    /**
     * Update item metadata for the specified namespace.
     *
     * @param itemName the item name.
     * @param namespace the metadata namespace.
     * @param value the new metadata value.
     * @param configuration the {@link Map} of configuration properties that make up the configuration.
     * @return The old {@link Metadata} or {@code null} if no previous metadata existed.
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null}, or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no {@code ManagedProvider} is available.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(String itemName, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        return ScriptServiceUtil.getMetadataRegistry()
                .update(new Metadata(new MetadataKey(namespace, itemName), value, configuration));
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
