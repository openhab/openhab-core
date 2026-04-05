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
     * Add a new key-value-pair to the cache or replace an existing value.
     * <p>
     * When returning,a lock is held on the object, which <b>must</b> be released with a subsequent call
     * to {@link #unlock(String)} with the same key. Failure to do so will result in a deadlock if this
     * entry is attempted accessed in the future.
     *
     * @param key the {@link String} used as a key.
     * @param object the object to store with the key
     * @return The old object associated with the key or {@code null}.
     */
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
     * Get an object from the cache.
     * <p>
     * When returning,a lock is held on the object, which <b>must</b> be released with a subsequent call
     * to {@link #unlock(String)} with the same key. Failure to do so will result in a deadlock if this
     * entry is attempted accessed in the future.
     *
     * @param key the {@link String} used as a key.
     * @return The object associated with the key or {@code null}.
     */
    @Nullable
    Object lockAndGet(String key);

    /**
     * Get an object from the cache or create a one for the key with the given supplier.
     * <p>
     * When returning,a lock is held on the object, which <b>must</b> be released with a subsequent call
     * to {@link #unlock(String)} with the same key. Failure to do so will result in a deadlock if this
     * entry is attempted accessed in the future.
     *
     * @param key the {@link String} used as a key.
     * @param supplier the supplier that returns a non-{@code null} object to be used if the key isn't present.
     * @return The object associated with the key.
     */
    Object lockAndGet(String key, Supplier<Object> supplier);

    /**
     * Unlock the value for the specified key and release the local reference to the cached instance.
     * <p>
     * To make sure that no reference is held to the cached instance, this method should always be used
     * like this:
     * 
     * <pre>
     * <code>
     * object = cache.lockAndGet("key");
     * try {
     *   ..
     *   (do something with the object)
     *   ..
     * } finally {
     *   object = cache.release("key");
     * }
     * </code>
     * </pre>
     *
     * @param key
     * @return
     */
    @Nullable
    Object unlock(String key);
}
