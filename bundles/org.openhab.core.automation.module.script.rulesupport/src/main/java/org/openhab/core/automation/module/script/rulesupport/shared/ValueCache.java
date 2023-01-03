/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ValueCache} can be used by scripts to share information between subsequent runs of the same script or
 * between scripts (depending on implementation).
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ValueCache {

    /**
     * Add a new key-value-pair to the cache. If the key is already present, the old value is replaces by the new value.
     *
     * @param key a string used as key
     * @param value an {@code Object} to store with the key
     * @return the old value associated with this key or {@code null} if key didn't exist
     */
    @Nullable
    Object put(String key, Object value);

    /**
     * Remove a key (and its associated value) from the cache
     *
     * @param key the key to remove
     * @return the previously associated value to this key or {@code null} if key not present
     */
    @Nullable
    Object remove(String key);

    /**
     * Get a value from the cache
     *
     * @param key the key of the requested value
     * @return the value associated with the key or {@code null} if key not present
     */
    @Nullable
    Object get(String key);

    /**
     * Get a value from the cache or create a new key-value-pair from the given supplier
     *
     * @param key the key of the requested value
     * @param supplier a supplier that returns a non-null value to be used if the key was not present
     * @return the value associated with the key
     */
    Object get(String key, Supplier<Object> supplier);
}
