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
package org.openhab.core.model.script.internal.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A general utility class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Utils {

    private Utils() {
        // Not to be instantiated
    }

    /**
     * Transforms pairs of {@link Object}s into a {@link Map}. The former of each pair (the key) must be a
     * {@link String}. The resulting map can have {@code null} values.
     *
     * @param objects the array of {@link Object}s to transform.
     * @return The resulting {@link Map}.
     * @throws IllegalArgumentException If there is an odd number of objects, or if any of the keys aren't
     *             {@link String}s.
     */
    public static Map<String, @Nullable Object> parseObjectArrayNullableValues(Object @Nullable [] objects)
            throws IllegalArgumentException {
        if (objects == null || objects.length == 0) {
            return Map.of();
        }
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException("There must be an even number of objects (" + objects.length + ')');
        }
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String key) {
                result.put(key, objects[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + objects[i]);
            }
        }
        return result;
    }

    /**
     * Transforms pairs of {@link Object}s into a {@link Map}. The former of each pair (the key) must be a
     * {@link String}. The resulting map cannot have {@code null} values.
     *
     * @param objects the array of {@link Object}s to transform.
     * @return The resulting {@link Map}.
     * @throws IllegalArgumentException If there is an odd number of objects, if any of the keys aren't {@link String}s,
     *             or if any of the values are {@code null}.
     */
    public static Map<String, Object> parseObjectArray(Object @Nullable [] objects) throws IllegalArgumentException {
        if (objects == null || objects.length == 0) {
            return Map.of();
        }
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException("There must be an even number of objects (" + objects.length + ')');
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String key) {
                if (objects[i + 1] == null) {
                    throw new IllegalArgumentException("Values cannot be null: " + objects[i]);
                }
                result.put(key, objects[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + objects[i]);
            }
        }
        return result;
    }
}
