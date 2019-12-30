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
package org.openhab.core.test.storage;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;

/**
 * A {@link Storage} implementation which stores it's data in-memory.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - improved return values
 */
@NonNullByDefault
public class VolatileStorage<T> implements Storage<T> {

    Map<String, T> storage = new ConcurrentHashMap<>();

    @Override
    public @Nullable T put(String key, @Nullable T value) {
        return storage.put(key, value);
    }

    @Override
    public @Nullable T remove(String key) {
        return storage.remove(key);
    }

    @Override
    public boolean containsKey(final String key) {
        return storage.containsKey(key);
    }

    @Override
    public @Nullable T get(String key) {
        return storage.get(key);
    }

    @Override
    public Collection<String> getKeys() {
        return storage.keySet();
    }

    @Override
    public Collection<@Nullable T> getValues() {
        return storage.values();
    }

}
