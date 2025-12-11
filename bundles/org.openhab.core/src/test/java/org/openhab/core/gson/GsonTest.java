/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.gson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Gson deserialization, DTOs.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
class GsonTest {

    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Load the test JSON payload string from a file, from hue binding
     */
    private String load(String fileName) {
        try (FileReader file = new FileReader(String.format("src/test/resources/%s.json", fileName));
                BufferedReader reader = new BufferedReader(file)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return "";
    }

    @Test
    void testLoadAndDeserializeJson() {
        // Load JSON from file
        String json = load("testdata");
        assertNotNull(json, "JSON content should not be null");
        assertFalse(json.isEmpty(), "JSON content should not be empty");

        // Deserialize into GsonTestClass_DTO
        GsonTestClass_DTO test = GSON.fromJson(json, GsonTestClass_DTO.class);
        assertNotNull(test, "Deserialized object should not be null");

        // Verify all fields are correctly deserialized
        assertEquals("John Doe", test.name, "Name field should match");
        assertEquals(30, test.age, "Age field should match");
        assertEquals("john.doe@example.com", test.email, "Email field should match");
        assertTrue(test.active, "Active field should match");
    }

    @Test
    void testLoadAndDeserializeAnnotatedJson() {
        // Load JSON from file
        String json = load("testdata");
        assertNotNull(json, "JSON content should not be null");
        assertFalse(json.isEmpty(), "JSON content should not be empty");

        // Deserialize into GsonTestClassAnnotated_DTO
        GsonTestClassAnnotated_DTO test = GSON.fromJson(json, GsonTestClassAnnotated_DTO.class);
        assertNotNull(test, "Deserialized object should not be null");

        // Verify all fields are correctly deserialized
        assertEquals("John Doe", test.n, "Name field should match");
        assertEquals(30, test.age, "Age field should match");
        assertTrue(test.active, "Active field should match");
        assertEquals("john.doe@example.com", test.e, "Email field should match");
    }

    @Test
    void testLoadAndDeserializeAnnotated2Json() {
        // Load JSON from file
        String json = load("testdata");
        assertNotNull(json, "JSON content should not be null");
        assertFalse(json.isEmpty(), "JSON content should not be empty");

        // Deserialize into GsonTestClassAnnotated2_DTO
        GsonTestClassAnnotated2_DTO test = GSON.fromJson(json, GsonTestClassAnnotated2_DTO.class);
        assertNotNull(test, "Deserialized object should not be null");

        // Verify all fields are correctly deserialized
        assertEquals("John Doe", test.n, "Name field should match");
        assertEquals(30, test.age, "Age field should match");
        assertTrue(test.active, "Active field should match");
        assertEquals("john.doe@example.com", test.e, "Email field should match");
    }
}
