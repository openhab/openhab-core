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
package org.eclipse.smarthome.core.storage;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A Storage is the generic way to store key-value pairs in ESH. Each Storage
 * implementation can store its data differently, e.g in-memory or in-database.
 *
 * @author Thomas.Eichstaedt-Engelen - Initial Contribution and API
 * @author Kai Kreuzer - improved return values
 */
@NonNullByDefault
public interface Storage<T> {

    /**
     * Puts a key-value mapping into this Storage.
     *
     * @param key the key to add
     * @param value the value to add
     * @return previous value for the key or null if no value was replaced
     */
    @Nullable
    T put(String key, @Nullable T value);

    /**
     * Removes the specified mapping from this map.
     *
     * @param key the mapping to remove
     * @return the removed value or null if no entry existed
     */
    @Nullable
    T remove(String key);

    /**
     * Check if the storage contains a key.
     *
     * @param key the key
     * @return true if the storage contains the key, otherwise false
     */
    boolean containsKey(String key);

    /**
     * Gets the value mapped to the key specified.
     *
     * @param key the key
     * @return the mapped value, null if no match
     */
    @Nullable
    T get(String key);

    /**
     * Gets all keys of this Storage.
     *
     * @return the keys of this Storage
     */
    Collection<String> getKeys();

    /**
     * Gets all values of this Storage.
     *
     * @return the values of this Storage
     */
    Collection<@Nullable T> getValues();

    /**
     * Get all storage entries.
     *
     * @return a stream of all storage entries
     */
    default Stream<Map.Entry<String, @Nullable T>> stream() {
        return getKeys().stream().map(key -> new AbstractMap.SimpleImmutableEntry<>(key, get(key)));
    }

}
