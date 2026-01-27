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
package org.openhab.core.model.yaml.internal.semantics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test class for {@link YamlSemanticTagDTO} to verify correct serialization and deserialization
 * of semantic tags in both short-form and detailed object-form.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class YamlSemanticTagDTOTest {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verifies short-form syntax: { "Tag_uid": "label value" }
     */
    @Test
    void parsesShortFormTag() throws Exception {
        String json = """
                {"Tag_uid":"label value"}
                """;
        Map<String, YamlSemanticTagDTO> tags = mapper.readValue(json,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, YamlSemanticTagDTO.class));
        YamlSemanticTagDTO tag = tags.get("Tag_uid");
        assertThat(tag, notNullValue());
        assertThat(tag.label, is("label value"));
        assertThat(tag.description, nullValue());
        assertThat(tag.synonyms, anyOf(nullValue(), empty()));
    }

    /**
     * Verifies object-form syntax using default mapping:
     * { "Tag_uid": { "label": "Label", "description": "Desc", "synonyms": ["a", "b"] } }
     */
    @Test
    void parsesDetailedFormTag() throws Exception {
        String json = """
                {"Tag_uid":{"label":"Label","description":"Desc","synonyms":["a","b"]}}
                """;
        Map<String, YamlSemanticTagDTO> tags = mapper.readValue(json,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, YamlSemanticTagDTO.class));
        YamlSemanticTagDTO tag = tags.get("Tag_uid");
        assertThat(tag, notNullValue());
        assertThat(tag.label, is("Label"));
        assertThat(tag.description, is("Desc"));
        assertThat(tag.synonyms, contains("a", "b"));
    }

    /**
     * Verifies that a simple DTO serializes to a plain String (Short-form)
     */
    @Test
    void serializesToShortForm() throws Exception {
        YamlSemanticTagDTO dto = new YamlSemanticTagDTO();
        dto.label = "label value";

        String json = mapper.writeValueAsString(dto);

        // Should be "label value", not {"label":"label value"...}
        assertThat(json, is("\"label value\""));
    }

    /**
     * Verifies that a detailed DTO serializes to a full JSON Object (Detailed-form)
     */
    @Test
    void serializesToDetailedForm() throws Exception {
        YamlSemanticTagDTO dto = new YamlSemanticTagDTO();
        dto.label = "Label";
        dto.description = "Desc";
        dto.synonyms = List.of("a", "b");

        String json = mapper.writeValueAsString(dto);

        // Verify the structure contains the keys
        assertThat(json, containsString("\"label\":\"Label\""));
        assertThat(json, containsString("\"description\":\"Desc\""));
        assertThat(json, containsString("\"synonyms\":[\"a\",\"b\"]"));
        // Ensure it is wrapped in braces
        assertThat(json, startsWith("{"));
        assertThat(json, endsWith("}"));
    }

    /**
     * Verifies round-trip consistency: Map -> String -> Map
     */
    @Test
    void verifiesMapRoundTrip() throws Exception {
        Map<String, YamlSemanticTagDTO> originalTags = new HashMap<>();

        YamlSemanticTagDTO simple = new YamlSemanticTagDTO();
        simple.label = "Simple";

        YamlSemanticTagDTO complex = new YamlSemanticTagDTO();
        complex.label = "Complex";
        complex.description = "With Desc";
        complex.synonyms = List.of("a", "b");

        originalTags.put("tag1", simple);
        originalTags.put("tag2", complex);

        String json = mapper.writeValueAsString(originalTags);

        // Assert the JSON string looks like the mixed format we want
        assertThat(json, containsString("\"tag1\":\"Simple\""));
        assertThat(json, containsString("\"tag2\":{"));

        // Deserialize back and check equality
        Map<String, YamlSemanticTagDTO> result = mapper.readValue(json,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, YamlSemanticTagDTO.class));

        assertThat(result.get("tag1").label, is("Simple"));
        assertThat(result.get("tag2").description, is("With Desc"));
    }
}
