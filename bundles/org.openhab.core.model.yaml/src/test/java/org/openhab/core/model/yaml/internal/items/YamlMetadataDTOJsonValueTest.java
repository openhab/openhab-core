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
package org.openhab.core.model.yaml.internal.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link YamlMetadataDTO} YAML serialization through {@code @JsonValue}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class YamlMetadataDTOJsonValueTest {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testSerializeScalarWhenConfigEmpty() throws Exception {
        YamlMetadataDTO dto = new YamlMetadataDTO();
        dto.value = "scalarValue";
        dto.config = null;
        String yaml = mapper.writeValueAsString(dto).trim();
        assertEquals("--- \"scalarValue\"", yaml);
    }

    @Test
    void testSerializeScalarWhenConfigEmptyMap() throws Exception {
        YamlMetadataDTO dto = new YamlMetadataDTO();
        dto.value = "scalarValue";
        dto.config = new HashMap<>();
        String yaml = mapper.writeValueAsString(dto).trim();
        assertEquals("--- \"scalarValue\"", yaml);
    }

    @Test
    void testSerializeObjectWhenConfigPresent() throws Exception {
        YamlMetadataDTO dto = new YamlMetadataDTO();
        dto.value = "objectValue";
        Map<String, Object> config = new HashMap<>();
        config.put("foo", "bar");
        dto.config = config;
        String yaml = mapper.writeValueAsString(dto).trim();
        // Check for document start and all expected fields
        assertTrue(yaml.startsWith("---"));
        assertTrue(yaml.contains("value: \"objectValue\""));
        assertTrue(yaml.contains("config:"));
        assertTrue(yaml.contains("foo: \"bar\""));
    }
}
