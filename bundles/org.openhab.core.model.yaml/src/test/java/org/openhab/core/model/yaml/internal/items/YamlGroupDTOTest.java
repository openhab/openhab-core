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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlGroupDTOTest} contains tests for the {@link YamlGroupDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlGroupDTOTest {

    @Test
    public void testGetBaseType() throws IOException {
        YamlGroupDTO gr = new YamlGroupDTO();
        assertEquals(null, gr.getBaseType());
        gr.type = "Number";
        assertEquals("Number", gr.getBaseType());
        gr.type = "number";
        assertEquals("Number", gr.getBaseType());
        gr.dimension = "Dimensionless";
        assertEquals("Number:Dimensionless", gr.getBaseType());
        gr.dimension = "dimensionless";
        assertEquals("Number:Dimensionless", gr.getBaseType());
    }

    @Test
    public void testGetFunction() throws IOException {
        YamlGroupDTO gr = new YamlGroupDTO();
        assertEquals("EQUALITY", gr.getFunction());
        gr.function = "AND";
        assertEquals("AND", gr.getFunction());
        gr.function = "or";
        assertEquals("OR", gr.getFunction());
        gr.function = "Min";
        assertEquals("MIN", gr.getFunction());
    }

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlGroupDTO gr = new YamlGroupDTO();
        assertTrue(gr.isValid(err, warn));

        gr.type = "String";
        assertTrue(gr.isValid(err, warn));
        gr.type = "string";
        assertTrue(gr.isValid(err, warn));
        gr.type = "Other";
        assertFalse(gr.isValid(err, warn));
        gr.type = "Number";
        assertTrue(gr.isValid(err, warn));
        gr.dimension = "Dimensionless";
        assertTrue(gr.isValid(err, warn));
        gr.type = "number";
        gr.dimension = "dimensionless";
        assertTrue(gr.isValid(err, warn));
        gr.dimension = "Other";
        assertFalse(gr.isValid(err, warn));
        gr.type = "Color";
        gr.dimension = null;
        assertEquals("Color", gr.getBaseType());

        gr.function = "AND";
        assertTrue(gr.isValid(err, warn));
        gr.function = "or";
        assertTrue(gr.isValid(err, warn));
        gr.function = "Min";
        assertTrue(gr.isValid(err, warn));
        gr.function = "invalid";
        assertFalse(gr.isValid(err, warn));
    }

    @Test
    public void testEquals() throws IOException {
        YamlGroupDTO gr1 = new YamlGroupDTO();
        YamlGroupDTO gr2 = new YamlGroupDTO();

        gr1.type = "String";
        gr2.type = "String";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr1.type = "String";
        gr2.type = "Number";
        assertFalse(gr1.equals(gr2));
        gr1.type = "Number";
        gr2.type = "Number";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr2.type = "number";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr1.dimension = "Temperature";
        assertFalse(gr1.equals(gr2));
        gr2.dimension = "Humidity";
        assertFalse(gr1.equals(gr2));
        gr2.dimension = "Temperature";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr2.dimension = "temperature";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());

        gr1.function = "or";
        gr2.function = null;
        assertFalse(gr1.equals(gr2));
        gr1.function = null;
        gr2.function = "or";
        assertFalse(gr1.equals(gr2));
        gr1.function = "or";
        gr2.function = "or";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr2.function = "Or";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
        gr2.function = "OR";
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());

        gr1.parameters = List.of("ON", "OFF");
        gr2.parameters = null;
        assertFalse(gr1.equals(gr2));
        gr2.parameters = List.of("ON");
        assertFalse(gr1.equals(gr2));
        gr1.parameters = null;
        gr2.parameters = List.of("ON", "OFF");
        assertFalse(gr1.equals(gr2));
        gr1.parameters = List.of("ON");
        assertFalse(gr1.equals(gr2));
        gr1.parameters = List.of("ON", "OFF");
        assertTrue(gr1.equals(gr2));
        assertEquals(gr1.hashCode(), gr2.hashCode());
    }
}
