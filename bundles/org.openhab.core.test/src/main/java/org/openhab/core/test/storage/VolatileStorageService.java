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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;

/**
 * The {@link VolatileStorageService} returns {@link VolatileStorage}s
 * which stores their data in-memory.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 */
public class VolatileStorageService implements StorageService {

    @SuppressWarnings("rawtypes")
    Map<String, Storage> storages = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> Storage<T> getStorage(String name) {
        if (!storages.containsKey(name)) {
            storages.put(name, new VolatileStorage<>());
        }
        return storages.get(name);
    }

    @Override
    public <T> Storage<T> getStorage(String name, ClassLoader classLoader) {
        return getStorage(name);
    }
}
