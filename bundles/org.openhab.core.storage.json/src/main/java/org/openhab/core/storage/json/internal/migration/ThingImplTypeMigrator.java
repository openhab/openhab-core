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

import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link ThingImplTypeMigrator} implements a {@link TypeMigrator} for stored things
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ThingImplTypeMigrator implements TypeMigrator {

    @Override
    public String getOldType() {
        return "org.openhab.core.thing.internal.ThingImpl";
    }

    @Override
    public String getNewType() {
        return "org.openhab.core.thing.internal.ThingStorageEntity";
    }

    @Override
    public JsonElement migrate(JsonElement oldValue) throws TypeMigrationException {
        JsonObject newValue = oldValue.deepCopy().getAsJsonObject();
        segmentUidToStringUid(newValue, "uid", "UID");
        segmentUidToStringUid(newValue, "bridgeUID");
        segmentUidToStringUid(newValue, "thingTypeUID");

        for (JsonElement jsonElement : newValue.get("channels").getAsJsonArray()) {
            JsonObject channel = jsonElement.getAsJsonObject();
            channel.add("itemType", channel.remove("acceptedItemType"));
            channel.add("configuration", channel.remove("configuration").getAsJsonObject().get("properties"));
            segmentUidToStringUid(channel, "uid");
            segmentUidToStringUid(channel, "channelTypeUID");
        }

        newValue.add("configuration", newValue.remove("configuration").getAsJsonObject().get("properties"));
        newValue.addProperty("isBridge", false);

        return newValue;
    }

    private void segmentUidToStringUid(JsonObject object, String name) {
        segmentUidToStringUid(object, name, name);
    }

    private void segmentUidToStringUid(JsonObject object, String oldName, String newName) {
        JsonElement element = object.remove(oldName);
        if (element != null) {
            Spliterator<JsonElement> segments = element.getAsJsonObject().get("segments").getAsJsonArray()
                    .spliterator();
            String uid = StreamSupport.stream(segments, false).map(JsonElement::getAsString)
                    .collect(Collectors.joining(":"));
            object.addProperty(newName, uid);
        }
    }
}
