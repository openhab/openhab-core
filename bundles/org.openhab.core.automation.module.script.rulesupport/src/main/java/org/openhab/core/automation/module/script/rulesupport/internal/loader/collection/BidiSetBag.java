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

package org.openhab.core.automation.module.script.rulesupport.internal.loader.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bidirectional bag of unique elements. A map allowing multiple, unique values to be stored against a single key.
 * Provides optimized lookup of values for a key, as well as keys referencing a value.
 *
 * @author Jonathan Gilbert - Initial contribution
 * @param <K> Type of Key
 * @param <V> Type of Value
 */
public class BidiSetBag<K, V> {
    private Map<K, Set<V>> keyToValues = new HashMap<>();
    private Map<V, Set<K>> valueToKeys = new HashMap<>();

    public void put(K key, V value) {
        addElement(keyToValues, key, value);
        addElement(valueToKeys, value, key);
    }

    public Set<V> getValues(K key) {
        Set<V> existing = keyToValues.get(key);
        return existing == null ? Collections.emptySet() : Collections.unmodifiableSet(existing);
    }

    public Set<K> getKeys(V value) {
        Set<K> existing = valueToKeys.get(value);
        return existing == null ? Collections.emptySet() : Collections.unmodifiableSet(existing);
    }

    public Set<V> removeKey(K key) {
        Set<V> values = keyToValues.remove(key);
        if (values != null) {
            for (V value : values) {
                valueToKeys.computeIfPresent(value, (k, v) -> {
                    v.remove(key);
                    return v;
                });
            }
            return values;
        } else {
            return Collections.emptySet();
        }
    }

    public Set<K> removeValue(V value) {
        Set<K> keys = valueToKeys.remove(value);
        if (keys != null) {
            for (K key : keys) {
                keyToValues.computeIfPresent(key, (k, v) -> {
                    v.remove(value);
                    return v;
                });
            }
            return keys;
        } else {
            return Collections.emptySet();
        }
    }

    private static <T, U> void addElement(Map<T, Set<U>> map, T key, U value) {
        Set<U> elements = map.compute(key, (k, l) -> l == null ? new HashSet<>() : l);
        elements.add(value);
    }
}
