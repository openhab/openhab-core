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
package org.openhab.core.auth.client.oauth2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class AccessTokenResponseExtraFieldsAdapterFactory implements TypeAdapterFactory {

    private static final Set<String> KNOWN_FIELDS = Set.of("access_token", "token_type", "expires_in", "refresh_token",
            "scope", "state");

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!AccessTokenResponse.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<>() {

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement tree = elementAdapter.read(in);
                if (tree != null) {
                    JsonObject obj = tree.getAsJsonObject();

                    T parsed = delegate.fromJsonTree(tree);
                    AccessTokenResponse response = (AccessTokenResponse) parsed;

                    Map<String, String> extras = new HashMap<>();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String key = entry.getKey();
                        if (KNOWN_FIELDS.contains(key)) {
                            continue;
                        }
                        extras.put(key, toStringValue(gson, entry.getValue()));
                    }

                    if (response != null) {
                        response.setExtraFields(extras);
                    }
                    return parsed;
                }
                return null;
            }
        };
    }

    private static String toStringValue(Gson gson, JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            return p.getAsString();
        }

        return gson.toJson(el);
    }
}
