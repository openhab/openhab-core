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
package org.eclipse.smarthome.config.core;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

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
 * @author Yordan Mihaylov - initial content
 * @author Ana Dimova - provide serialization of multiple configuration values.
 */
public class ConfigurationSerializer implements JsonSerializer<Configuration> {

    @SuppressWarnings("unchecked")
    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = null;
        if (src != null) {
            Set<String> kyes = src.keySet();
            if (kyes.size() > 0) {
                result = new JsonObject();
                for (String propName : kyes) {
                    Object value = src.get(propName);
                    if (value instanceof List) {
                        JsonArray array = new JsonArray();
                        for (Object element : (List<Object>) value) {
                            array.add(serializePrimitive(element));
                        }
                        result.add(propName, array);
                    } else {
                        result.add(propName, serializePrimitive(value));
                    }
                }
            }
        }
        return result;
    }

    private JsonPrimitive serializePrimitive(Object primitive) {
        if (primitive instanceof String) {
            return new JsonPrimitive((String) primitive);
        } else if (primitive instanceof Number) {
            return new JsonPrimitive((Number) primitive);
        } else if (primitive instanceof Boolean) {
            return new JsonPrimitive((Boolean) primitive);
        }
        return null;
    }

}
