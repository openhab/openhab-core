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
 * A {@code Storage} that could be disposed.
 *
 * @author Markus Rathgeb - Initial Contribution and API
 */
public interface DeletableStorage<T> extends Storage<T> {

    /**
     * Delete the storage.
     *
     * <p>
     * This function could be called if the storage is not longer used (now and in future). The storage implementation
     * will clean up / remove the storage (e.g. file, database, etc.).
     * After this function has been called the storage must not be used anymore.
     */
    void delete();
}
