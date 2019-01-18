/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.storage;

/**
 * The {@link StorageService} provides instances of {@link Storage}s which are
 * meant as a means for generic storage of key-value pairs. You can think of
 * different {@link StorageService}s that store these key-value pairs
 * differently. One can think of e.g in-memory or in-database {@link Storage}s
 * and many more. This {@link StorageService} decides which kind of {@link Storage} is returned on request. It is meant
 * to be injected into service consumers with the need for storing generic key-value pairs like the
 * ManagedXXXProviders.
 *
 * @author Thomas.Eichstaedt-Engelen - Initial Contribution and API
 * @author Dennis Nobel - Added second method with ClassLoader
 */
public interface StorageService {

    /**
     * Returns the {@link Storage} with the given {@code name}. If no {@link Storage} with this name exists a new
     * initialised instance is returned.
     *
     * @param name the name of the {@link StorageService} to return
     * @return a ready to use {@link Storage}, never {@code null}
     */
    <T> Storage<T> getStorage(String name);

    /**
     * Returns the {@link Storage} with the given {@code name} and a given {@link ClassLoader}. If no {@link Storage}
     * with this name exists a new initialised instance is returned.
     *
     * @param name the name of the {@link StorageService} to return
     * @param classLoader the class loader which should be used by the {@link Storage}
     * @param <T> The type of the storage service
     * @return a ready to use {@link Storage}, never {@code null}
     */
    <T> Storage<T> getStorage(String name, ClassLoader classLoader);
}
