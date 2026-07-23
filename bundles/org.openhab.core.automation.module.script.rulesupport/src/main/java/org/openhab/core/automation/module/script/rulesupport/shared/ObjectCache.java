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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ObjectCache} can be used by scripts to share information between subsequent runs of the same script or
 * between scripts (depending on implementation).
 *
 * @author Ravi Nadahar - Initial contribution
 *
 * @apiNote Implementations must be thread-safe.
 */
@NonNullByDefault
public interface ObjectCache {// TODO: (Nad) Redo JavaDocs

    /**
     * Add a new key-value-pair to the cache. If the key is already present, the old value is replaces by the new value.
     *
     * @param key a {@link String} used as key
     * @param serializedObject the {@code String} to store with the key
     * @return the old serialized object associated with this key or {@code null} if key didn't exist
     */
    @Nullable
    String put(String key, String serializedObject);

    /**
     * Remove a key (and its associated serialized object) from the cache
     *
     * @param key the key to remove.
     * @return the serialized object previously associated to this key or {@code null} if key not present.
     */
    @Nullable
    String remove(String key);

    /**
     * Get a serialized object from the cache
     *
     * @param key the key of the requested serialized object
     * @return the serialized object associated with the key or {@code null}.
     */
    @Nullable
    String get(String key);

    /**
     * Get a serialized object from the cache or create a new key-value-pair from the given supplier
     *
     * @param key the key of the requested serialized object
     * @param supplier a supplier that returns a non-null serialized object to be used if the key isn't present
     * @return the serialized object associated with the key
     */
    String get(String key, Supplier<String> supplier);

    /**
     * Attempts to compute a mapping for the specified key and its current mapped serialized object
     * (or {@code null} if there is no current mapping).
     *
     * See {@code java.util.Map.compute()} for details.
     *
     * @param key the key of the requested serialized object.
     * @param remappingFunction the remapping function to compute a serialized object.
     * @return the new serialized object associated with the specified key or {@code null}.
     */
    @Nullable
    String compute(String key, BiFunction<String, @Nullable String, @Nullable String> remappingFunction);
}
