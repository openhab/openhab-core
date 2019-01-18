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
package org.eclipse.smarthome.storage.json.internal;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Deserializes the internal data structure of the {@link JsonStorage})
 *
 * The contained entities remain json objects and won't me deserialized to their corresponding types at this point.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class StorageEntryMapDeserializer implements JsonDeserializer<Map<String, StorageEntry>> {

    /**
     *
     * Finds out whether the given object is the outer JSON storage map or not.
     *
     * It must be
     * <li>a Map of Maps
     * <li>with 2 entries each
     * <li>with {@link JsonStorage#CLASS} and {@link JsonStorage#VALUE} being their keys
     *
     * @param obj the object to be analyzed
     * @return {@code true} if it is the outer storage map
     */
    private boolean isOuterMap(JsonObject obj) {
        for (Map.Entry<String, JsonElement> me : obj.entrySet()) {
            JsonElement v = me.getValue();
            if (!v.isJsonObject()) {
                return false;
            }
            Set<Entry<String, JsonElement>> entrySet = ((JsonObject) v).entrySet();
            if (entrySet.size() != 2) {
                return false;
            }
            Set<String> keys = entrySet.stream().map(e -> e.getKey()).collect(Collectors.toSet());
            if (!keys.contains(JsonStorage.CLASS) || !keys.contains(JsonStorage.VALUE)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, StorageEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        if (!isOuterMap(obj)) {
            throw new IllegalArgumentException("Object {} is not an outer map: " + obj);
        }
        return readOuterMap(obj, context);
    }

    private Map<String, StorageEntry> readOuterMap(JsonObject obj, JsonDeserializationContext context) {
        Map<String, StorageEntry> map = new ConcurrentHashMap<>();
        for (Map.Entry<String, JsonElement> me : obj.entrySet()) {
            String key = me.getKey();
            JsonObject value = me.getValue().getAsJsonObject();
            StorageEntry innerMap = new StorageEntry(value.get(JsonStorage.CLASS).getAsString(),
                    value.get(JsonStorage.VALUE));
            map.put(key, innerMap);
        }
        return map;
    }

}
