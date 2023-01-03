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
 * The {@link BridgeImplTypeMigrator} implements a {@link TypeMigrator} for stored bridges
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class BridgeImplTypeMigrator extends ThingImplTypeMigrator {

    @Override
    public String getOldType() {
        return "org.openhab.core.thing.internal.BridgeImpl";
    }

    @Override
    public String getNewType() {
        return "org.openhab.core.thing.internal.ThingStorageEntity";
    }

    @Override
    public JsonElement migrate(JsonElement oldValue) throws TypeMigrationException {
        JsonElement newValue = super.migrate(oldValue);
        newValue.getAsJsonObject().addProperty("isBridge", true);
        return newValue;
    }
}
