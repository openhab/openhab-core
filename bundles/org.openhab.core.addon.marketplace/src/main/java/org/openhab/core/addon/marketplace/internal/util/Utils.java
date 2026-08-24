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
package org.openhab.core.addon.marketplace.internal.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A generic utility class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Utils {

    private Utils() {
        // Not to be instantiated
    }

    /**
     * Check whether the specified string contains a "new YAML" structure.
     *
     * @param yamlMapper the {@link ObjectMapper} used to parse the string.
     * @param yaml the {@link String} to parse.
     * @return {@code true} if the specified string is a "new YAML" structure, {@code false} otherwise.
     */
    public static boolean isNewYaml(ObjectMapper yamlMapper, String yaml) {
        if (yaml.isBlank()) {
            return false;
        }
        try {
            JsonNode rootNode = yamlMapper.readValue(yaml, JsonNode.class);
            Iterator<Entry<String, JsonNode>> iterator = rootNode.properties().iterator();
            if (iterator.hasNext()) {
                Entry<String, JsonNode> entry = iterator.next();
                return "version".equals(entry.getKey());
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
