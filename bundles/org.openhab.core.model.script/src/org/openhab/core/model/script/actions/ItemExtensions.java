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
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;

/**
 * {@link ItemExtensions} provides DSL access to things like OSGi instances, system registries and the ability to run
 * other
 * rules.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ItemExtensions {

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, (String) null);
    }

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value, Object... configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, parseObjectArray(configuration));
    }

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, configuration);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata getMetadata(Item item, String namespace) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.getMetadata(item.getName(), namespace);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata removeMetadata(Item item, String namespace) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.removeMetadata(item.getName(), namespace);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.updateMetadata(item.getName(), namespace, value);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            Object... configuration) {
        return Items.updateMetadata(item.getName(), namespace, value, parseObjectArray(configuration));
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.updateMetadata(item.getName(), namespace, value);
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
