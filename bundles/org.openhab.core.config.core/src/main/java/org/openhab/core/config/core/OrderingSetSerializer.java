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
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

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
public class OrderingSetSerializer implements JsonSerializer<Set<@Nullable Object>> {

    public static boolean allSameClassAndComparable(Set<@Nullable Object> src) {
        Class<?> expectedClass = null;
        for (Object object : src) {
            if (!(object instanceof Comparable<?>)) {
                // not comparable or simply null
                return false;
            } else if (expectedClass == null) {
                // first item
                expectedClass = object.getClass();
            } else if (!object.getClass().equals(expectedClass)) {
                // various classes in the Set, let's not try to sort
                return false;
            }
        }
        return true;
    }

    @Override
    public JsonElement serialize(Set<@Nullable Object> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray ordered = new JsonArray();
        final Stream<@Nullable Object> possiblySortedStream;
        if (allSameClassAndComparable(src)) {
            possiblySortedStream = src.stream().sorted();
        } else {
            possiblySortedStream = src.stream();
        }
        possiblySortedStream.map(context::serialize).forEachOrdered(ordered::add);
        return ordered;
    }
}
