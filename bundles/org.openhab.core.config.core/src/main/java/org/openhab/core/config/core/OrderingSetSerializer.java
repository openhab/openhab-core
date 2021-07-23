/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serializes set by ordering the elements
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class OrderingSetSerializer implements JsonSerializer<Set<Object>> {

    @Override
    public JsonElement serialize(Set<Object> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray ordered = new JsonArray();
        src.stream().map(context::serialize).sorted().forEachOrdered(ordered::add);
        return ordered;
    }
}
