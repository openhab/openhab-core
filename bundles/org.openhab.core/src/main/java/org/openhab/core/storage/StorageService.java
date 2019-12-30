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
 * The {@link StorageService} provides instances of {@link Storage}s which are
 * meant as a means for generic storage of key-value pairs. You can think of
 * different {@link StorageService}s that store these key-value pairs
 * differently. One can think of e.g in-memory or in-database {@link Storage}s
 * and many more. This {@link StorageService} decides which kind of {@link Storage} is returned on request. It is meant
 * to be injected into service consumers with the need for storing generic key-value pairs like the
 * ManagedXXXProviders.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Dennis Nobel - Added second method with ClassLoader
 */
@NonNullByDefault
public interface StorageService {

    /**
     * Returns the {@link Storage} with the given {@code name}. If no {@link Storage} with this name exists a new
     * initialized instance is returned.
     *
     * @param name the name of the {@link StorageService} to return
     * @return a ready to use {@link Storage}, never {@code null}
     */
    <T> Storage<T> getStorage(String name);

    /**
     * Returns the {@link Storage} with the given {@code name} and a given {@link ClassLoader}. If no {@link Storage}
     * with this name exists a new initialized instance is returned.
     *
     * @param name the name of the {@link StorageService} to return
     * @param classLoader the class loader which should be used by the {@link Storage}
     * @param <T> The type of the storage service
     * @return a ready to use {@link Storage}, never {@code null}
     */
    <T> Storage<T> getStorage(String name, @Nullable ClassLoader classLoader);
}
