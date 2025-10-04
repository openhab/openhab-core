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

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link LockingCache} can be used by scripts to share information between subsequent runs of the same script or
 * between scripts (depending on implementation).
 *
 * @author Ravi Nadahar - Initial contribution
 *
 * @apiNote Implementations must be thread-safe.
 */
@NonNullByDefault
public interface LockingCache {// TODO: (Nad) Redo JavaDocs

    /**
     * Add a new key-value-pair to the cache. If the key is already present, the old value is replaces by the new value.
     *
     * @param key a {@link String} used as key
     * @param object the {@code Object} to store with the key
     * @return the old object associated with this key or {@code null} if the key didn't exist
     */
    @Nullable
    Object put(String key, Object object);

    @Nullable
    Object lockAndPut(String key, Object object);

    /**
     * Remove a key (and its associated object) from the cache
     *
     * @param key the key to remove.
     * @return the object previously associated to this key or {@code null}.
     */
    @Nullable
    Object remove(String key);

    /**
     * Get a object from the cache
     *
     * @param key the key of the requested object
     * @return the object associated with the key or {@code null}.
     */
    @Nullable
    Object lockAndGet(String key);

    /**
     * Get a serialized object from the cache or create a new key-value-pair from the given supplier
     *
     * @param key the key of the requested serialized object
     * @param supplier a supplier that returns a non-null serialized object to be used if the key isn't present
     * @return the serialized object associated with the key
     */
    Object lockAndGet(String key, Supplier<Object> supplier);

    void unlock(String key);
}
