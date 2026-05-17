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
package org.openhab.core.model.script.actions;

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
 * {@link Items} provides DSL access to things like OSGi instances, system registries and the ability to run other
 * rules.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Items {

    public static @Nullable Item getItem(String itemName) {
        return ScriptServiceUtil.getItemRegistry().get(itemName);
    }

    /**
     *
     * @param itemName
     * @param namespace
     * @param value
     * @throws IllegalArgumentException If either value is {@code null} or {@code namespace} or
     *             {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static void addMetadata(String itemName, String namespace, String value) {
        addMetadata(itemName, namespace, value, (String) null);
    }

    @NonNullByDefault({})
    public static void addMetadata(String itemName, String namespace, String value, Object... configuration) {
        addMetadata(itemName, namespace, value, parseObjectArray(configuration));
    }

    /**
     *
     * @param itemName
     * @param namespace
     * @param value
     * @param configuration
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null} or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
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
     *
     * @param namespace
     * @param itemName
     * @param value
     * @throws IllegalArgumentException If either value is {@code null} or {@code namespace} or
     *             {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(String itemName, String namespace, String value) {
        return updateMetadata(itemName, namespace, value, (Map<String, Object>) null);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(String itemName, String namespace, String value,
            Object... configuration) {
        return updateMetadata(itemName, namespace, value, parseObjectArray(configuration));
    }

    /**
     *
     * @param itemName
     * @param namespace
     * @param value
     * @param configuration
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null} or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
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
