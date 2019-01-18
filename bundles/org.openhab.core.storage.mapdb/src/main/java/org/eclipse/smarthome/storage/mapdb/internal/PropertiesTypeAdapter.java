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
package org.eclipse.smarthome.storage.mapdb.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Type adapter that makes sure that all Numeric values in Maps of type
 * Map<String, Object> are deserialized as BigDecimal instances instead of
 * doubles.
 *
 * @author Ivan Iliev
 *
 */
public class PropertiesTypeAdapter extends TypeAdapter<Map<String, Object>> {

    public static final TypeToken<Map<String, Object>> TOKEN = new TypeToken<Map<String, Object>>() {
    };

    private final TypeAdapter<Map<String, Object>> delegate;

    private final ConstructorConstructor constructor;

    private final TypeAdapter<String> keyAdapter;

    private final TypeAdapter<Object> valueAdapter;

    public PropertiesTypeAdapter(Gson gson) {
        // obtain the default type adapters for String and Object classes
        keyAdapter = gson.getAdapter(String.class);
        valueAdapter = gson.getAdapter(Object.class);

        // obtain default gson objects
        constructor = new ConstructorConstructor(Collections.<Type, InstanceCreator<?>> emptyMap());
        delegate = new MapTypeAdapterFactory(constructor, false).create(new Gson(), TOKEN);
    }

    @Override
    public void write(JsonWriter out, Map<String, Object> value) throws IOException {
        // write remains unchanged
        delegate.write(out, value);
    }

    @Override
    public Map<String, Object> read(JsonReader in) throws IOException {
        // gson implementation code is modified when deserializing numbers
        JsonToken peek = in.peek();
        if (peek == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        Map<String, Object> map = constructor.get(TOKEN).construct();

        if (peek == JsonToken.BEGIN_ARRAY) {
            in.beginArray();
            while (in.hasNext()) {
                in.beginArray(); // entry array
                String key = keyAdapter.read(in);

                // modification
                Object value = getValue(in);

                Object replaced = map.put(key, value);
                if (replaced != null) {
                    throw new JsonSyntaxException("duplicate key: " + key);
                }
                in.endArray();
            }
            in.endArray();
        } else {
            in.beginObject();
            while (in.hasNext()) {
                JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
                String key = keyAdapter.read(in);

                // modification
                Object value = getValue(in);

                Object replaced = map.put(key, value);
                if (replaced != null) {
                    throw new JsonSyntaxException("duplicate key: " + key);
                }
            }
            in.endObject();
        }
        return map;
    }

    private Object getValue(JsonReader in) throws IOException {
        Object value = null;

        // if the next json token is a number we read it as a BigDecimal,
        // otherwise use the default adapter to read it
        if (JsonToken.NUMBER.equals(in.peek())) {
            value = new BigDecimal(in.nextString());
        } else {
            value = valueAdapter.read(in);
        }

        return value;
    }

}
