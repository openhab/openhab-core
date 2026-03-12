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
package org.openhab.core.library.types;

import java.io.Serial;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * The HSBType is a complex type with constituents for hue, saturation and
 * brightness and can be used for color items.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class JSonType implements ComplexType, State, Command {

    protected Gson gson = new Gson();

    @Serial
    private static final long serialVersionUID = 322902950356613226L;

    // constants for the constituents
    public static final String KEY_JSON = "json";

    private String json = "";
    private @Nullable JsonElement jsonRoot;

    public JSonType() {
    }

    public JSonType(String json) {
        this.json = json;
        jsonRoot = gson.fromJson(json, JsonElement.class);
        this.json = json;

    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        TreeMap<String, PrimitiveType> map = new TreeMap<>();
        map.put(KEY_JSON, getJSon());
        return map;
    }

    public StringType getJSon() {
        return new StringType(json);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return json;
    }

    public static JSonType valueOf(String value) {
        return new JSonType(value);
    }

    @Override
    public String format(String pattern) {
        String formatPattern = pattern;
        String val = json;
        return String.format(formatPattern, val);
    }

    @Override
    public int hashCode() {
        int tmp = json.hashCode();
        return tmp;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JSonType)) {
            return false;
        }
        JSonType other = (JSonType) obj;
        return false;
    }

}
