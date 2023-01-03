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
import com.google.gson.JsonObject;

/**
 * The {@link PersistedTransformationTypeMigrator} implements a {@link TypeMigrator} for stored things
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistedTransformationTypeMigrator implements TypeMigrator {

    @Override
    public String getOldType() {
        return "org.openhab.core.transform.ManagedTransformationConfigurationProvider$PersistedTransformationConfiguration";
    }

    @Override
    public String getNewType() {
        return "org.openhab.core.transform.ManagedTransformationProvider$PersistedTransformation";
    }

    @Override
    public JsonElement migrate(JsonElement oldValue) throws TypeMigrationException {
        JsonObject newValue = oldValue.deepCopy().getAsJsonObject();

        JsonObject configuration = new JsonObject();
        configuration.addProperty("function", newValue.remove("content").getAsString());
        newValue.remove("language");
        newValue.add("configuration", configuration);

        return newValue;
    }
}
