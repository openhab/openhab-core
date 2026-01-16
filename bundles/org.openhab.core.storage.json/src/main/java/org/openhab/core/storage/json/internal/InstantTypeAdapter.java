/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.storage.json.internal;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * The {@link InstantTypeAdapter} implements serialization and deserialization of {@link Instant}.
 * as formatted UTC strings.
 *
 * Deserialization supports milliseconds since epoch as well.
 *
 * @author Jacob Laursen - Initial contribution
 */
@NonNullByDefault
public class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
    /**
     * Converts an {@link Instant} to a formatted UTC string.
     */
    @Override
    public JsonElement serialize(Instant instant, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(instant.toString());
    }

    /**
     * Converts a formatted UTC string to {@link Instant}.
     * As fallback, milliseconds since epoch is supported as well.
     */
    @Override
    public @Nullable Instant deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        try {
            return Instant.parse(element.getAsString());
        } catch (DateTimeParseException e) {
            // Fallback to milliseconds since epoch for backwards compatibility.
            return Instant.ofEpochMilli(element.getAsLong());
        }
    }
}
