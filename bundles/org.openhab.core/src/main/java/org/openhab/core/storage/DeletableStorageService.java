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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link DeletableStorageService} extends the normal {@link StorageService} and provides instances of
 * {@link DeletableStorage}s.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface DeletableStorageService extends StorageService {

    @Override
    <T> DeletableStorage<T> getStorage(String name);

    @Override
    <T> DeletableStorage<T> getStorage(String name, @Nullable ClassLoader classLoader);
}
