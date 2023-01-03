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
package org.openhab.core.config.core;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serializes map data by ordering the keys
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class OrderingMapSerializer implements JsonSerializer<Map<@Nullable Object, @Nullable Object>> {

    @SuppressWarnings({ "rawtypes", "unchecked", "null" })
    @Override
    public JsonElement serialize(Map<@Nullable Object, @Nullable Object> src, Type typeOfSrc,
            JsonSerializationContext context) {
        JsonObject ordered = new JsonObject();
        final Stream<Entry<@Nullable Object, @Nullable Object>> possiblySortedStream;
        if (OrderingSetSerializer.allSameClassAndComparable(src.keySet())) {
            // Map keys are comparable as verified above so casting to plain Comparator is safe
            possiblySortedStream = src.entrySet().stream().sorted((Comparator) Map.Entry.comparingByKey());
        } else {
            possiblySortedStream = src.entrySet().stream();
        }
        possiblySortedStream.forEachOrdered(entry -> {
            Object key = entry.getKey();
            if (key instanceof String) {
                ordered.add((String) key, context.serialize(entry.getValue()));
            } else {
                JsonElement serialized = context.serialize(key);
                ordered.add(serialized.isJsonPrimitive() ? serialized.getAsString() : serialized.toString(),
                        context.serialize(entry.getValue()));
            }

        });
        return ordered;
    }
}
