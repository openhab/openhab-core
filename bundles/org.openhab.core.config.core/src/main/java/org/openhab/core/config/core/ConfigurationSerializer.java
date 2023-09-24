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
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This class serializes elements of Configuration object into json as configuration object (not as
 * configuration.properties object).
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - provide serialization of multiple configuration values.
 * @author Sami Salonen - property names are sorted for serialization for minimal diffs
 */
public class ConfigurationSerializer implements JsonSerializer<Configuration> {

    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        src.keySet().stream().sorted().forEachOrdered((String propName) -> {
            Object value = src.get(propName);
            if (value instanceof List list) {
                JsonArray array = new JsonArray();
                for (Object element : list) {
                    array.add(serializePrimitive(element));
                }
                result.add(propName, array);
            } else {
                result.add(propName, serializePrimitive(value));
            }
        });
        return result;
    }

    private JsonPrimitive serializePrimitive(Object primitive) {
        if (primitive instanceof String string) {
            return new JsonPrimitive(string);
        } else if (primitive instanceof Number number) {
            return new JsonPrimitive(number);
        } else if (primitive instanceof Boolean boolean1) {
            return new JsonPrimitive(boolean1);
        }
        return null;
    }
}
