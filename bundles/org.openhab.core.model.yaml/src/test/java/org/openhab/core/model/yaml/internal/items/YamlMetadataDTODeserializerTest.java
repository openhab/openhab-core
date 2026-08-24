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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link YamlMetadataDTODeserializer}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class YamlMetadataDTODeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @ParameterizedTest
    @ValueSource(strings = { "string value", "123", "45.67", "true", "false" })
    void shouldDeserializeScalarAsValueWithEmptyConfig(String scalar) throws IOException {
        String yaml = "ns: " + scalar;
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals(scalar, dto.getValue());
        assertNull(dto.config);
    }

    @Test
    void shouldDeserializeObjectWithValueAndConfig() throws IOException {
        String yaml = "ns: { value: bar, config: { a: 1, b: two } }";
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals("bar", dto.getValue());
        assertNotNull(dto.config);
        assertEquals(1, dto.config.get("a"));
        assertEquals("two", dto.config.get("b"));
    }

    @Test
    void shouldDeserializeObjectWithValueAndNoConfig() throws IOException {
        String yaml = "ns: { value: bar }";
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals("bar", dto.getValue());
        assertNull(dto.config);
    }

    @Test
    void shouldDeserializeObjectWithNoValueAndConfig() throws IOException {
        String yaml = "ns: { config: { a: 1, b: two } }";
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals("", dto.getValue());
        assertNotNull(dto.config);
        assertEquals(1, dto.config.get("a"));
        assertEquals("two", dto.config.get("b"));
    }

    @Test
    void shouldDeserializeEmptyObjectAsEmptyDto() throws IOException {
        String yaml = "ns: { }";
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals("", dto.getValue());
        assertNull(dto.config);
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "", "''" })
    void shouldDeserializeNullAndEmptyStringAsEmptyDto(String scalar) throws IOException {
        String yaml = "ns: " + scalar;
        YamlMetadataDTO dto = mapper.treeToValue(mapper.readTree(yaml).get("ns"), YamlMetadataDTO.class);
        assertNotNull(dto);
        assertEquals("", dto.getValue());
        assertNull(dto.config);
    }
}
