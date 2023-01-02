/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.storage.json.internal.migration;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonElement;

/**
 * The {@link TypeMigrator} interface allows the implementation of JSON storage type migrations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface TypeMigrator {

    /**
     * Get the name of the old (stored) type
     *
     * @return Full class name
     */
    String getOldType();

    /**
     * Get the name of the new type
     *
     * @return Full class name
     */
    String getNewType();

    /**
     * Migrate the old type to the new type
     *
     * The default implementation can be used if type is renamed only.
     *
     * @param oldValue The {@link JsonElement} representation of the old type
     * @return The corresponding {@link JsonElement} representation of the new type
     * @throws TypeMigrationException if an error occurs
     */
    default JsonElement migrate(JsonElement oldValue) throws TypeMigrationException {
        return oldValue;
    }
}
