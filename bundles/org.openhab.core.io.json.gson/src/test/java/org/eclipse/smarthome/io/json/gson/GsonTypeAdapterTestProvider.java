/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.json.gson;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.io.json.gson.GsonBindingServiceTestBean.Active;
import org.openhab.core.io.json.gson.GsonTypeAdapterProvider;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Custom serialization provider for {@link GsonBindingServiceTestBean}.
 *
 * @author Flavio Costa - Initial implementation
 */
public class GsonTypeAdapterTestProvider implements GsonTypeAdapterProvider, JsonSerializer<GsonBindingServiceTestBean>,
        JsonDeserializer<GsonBindingServiceTestBean> {

    @Override
    public Map<Class<?>, Object> getTypeAdapters() {
        Map<Class<?>, Object> map = new HashMap<>();
        map.put(GsonBindingServiceTestBean.class, this);
        return map;
    }

    @Override
    public JsonElement serialize(GsonBindingServiceTestBean src, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("text", src.getText());
        obj.addProperty("status", src.status == Active.YES ? "active" : "inactive");
        return obj;
    }

    @Override
    public GsonBindingServiceTestBean deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        GsonBindingServiceTestBean bean = new GsonBindingServiceTestBean();
        JsonObject obj = element.getAsJsonObject();
        Field textField;
        try {
            textField = bean.getClass().getField("text");
            textField.setAccessible(true);
            textField.set(bean, obj.get("text").getAsString());
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new JsonParseException(e);
        }
        bean.status = obj.get("status").getAsString().equals("active") ? Active.YES : Active.NO;
        return bean;
    }
}
