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
package org.openhab.core.storage.json.internal;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.internal.LinkedTreeMap;

/**
 * Serializes Configuration object with properties ordered
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class OrderingConfigurationSerializer implements JsonSerializer<Configuration> {

    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, Object> orderedProperties = new LinkedTreeMap<>();
        src.getProperties().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEachOrdered(entry -> {
                    orderedProperties.put(entry.getKey(), entry.getValue());
                });

        return context.serialize(new Configuration(orderedProperties)); // XXX: re-normalizes unnecessarily
    }
}
