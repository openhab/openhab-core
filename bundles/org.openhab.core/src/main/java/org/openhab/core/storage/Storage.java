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
package org.openhab.core.storage;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link Storage} is the generic way to store key-value pairs in OHC. Each storage implementation can store its data
 * differently, e.g in-memory or in-database.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - improved return values
 */
@NonNullByDefault
public interface Storage<T> {

    /**
     * Puts a key-value mapping into this storage.
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
     * Checks if the storage contains a key.
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
     * Gets all keys of this storage.
     *
     * @return the keys of this storage
     */
    Collection<String> getKeys();

    /**
     * Gets all values of this storage.
     *
     * @return the values of this storage
     */
    Collection<@Nullable T> getValues();

    /**
     * Gets all storage entries as {@link Stream}.
     *
     * @return a stream of all storage entries
     */
    default Stream<Map.Entry<String, @Nullable T>> stream() {
        return getKeys().stream().map(key -> new AbstractMap.SimpleImmutableEntry<>(key, get(key)));
    }

}
