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
package org.openhab.core.auth.oauth2client.internal;

import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

/**
 * Specialized AccessTokenResponse deserializer
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class AccessTokenDeserializer implements JsonDeserializer<AccessTokenResponse> {

    @Override
    public AccessTokenResponse deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
            @Nullable JsonDeserializationContext context) throws JsonParseException {
        if (json == null) {
            throw new JsonParseException("Could not deserialized null JsonElement");
        }

        final JsonObject accessToken = json.getAsJsonObject();
        final JsonElement scope = accessToken.get(Keyword.SCOPE);

        if (scope instanceof JsonArray) {
            String result = "";
            for (JsonElement line : (JsonArray) scope) {
                result += line.getAsString() + " ";
            }
            accessToken.add(Keyword.SCOPE, new JsonPrimitive(result.trim()));
        }

        return new Gson().fromJson(json.getAsJsonObject(), AccessTokenResponse.class);
    }
}
