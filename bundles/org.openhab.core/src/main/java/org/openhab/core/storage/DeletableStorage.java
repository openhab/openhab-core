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

/**
 * A {@code Storage} that could be disposed.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
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
