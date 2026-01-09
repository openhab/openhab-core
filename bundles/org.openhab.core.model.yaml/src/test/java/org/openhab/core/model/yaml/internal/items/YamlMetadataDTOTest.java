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
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlMetadataDTOTest} contains tests for the {@link YamlMetadataDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlMetadataDTOTest {

    @Test
    public void testGetValue() throws IOException {
        YamlMetadataDTO md = new YamlMetadataDTO();
        md.value = null;
        assertEquals("", md.getValue());
        md.value = "value";
        assertEquals("value", md.getValue());
    }

    @Test
    public void testEquals() throws IOException {
        YamlMetadataDTO md1 = new YamlMetadataDTO();
        YamlMetadataDTO md2 = new YamlMetadataDTO();

        md1.value = null;
        md2.value = null;
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
        md1.value = null;
        md2.value = "";
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
        md1.value = "";
        md2.value = null;
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
        md1.value = "value";
        md2.value = null;
        assertFalse(md1.equals(md2));
        md2.value = "other value";
        assertFalse(md1.equals(md2));
        md2.value = "value";
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());

        md1.config = Map.of("param", 50);
        md2.config = Map.of("param", 50);
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    public void testEqualsWithConfigurations() throws IOException {
        YamlMetadataDTO md1 = new YamlMetadataDTO();
        YamlMetadataDTO md2 = new YamlMetadataDTO();

        md1.value = "value";
        md2.value = "value";

        md1.config = null;
        md2.config = null;
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
        md1.config = Map.of("param1", "value", "param2", 50, "param3", true);
        md2.config = null;
        assertFalse(md1.equals(md2));
        md1.config = null;
        md2.config = Map.of("param1", "value", "param2", 50, "param3", true);
        assertFalse(md1.equals(md2));
        md1.config = Map.of("param1", "value", "param2", 50, "param3", true);
        md2.config = Map.of("param1", "other value", "param2", 50, "param3", true);
        assertFalse(md1.equals(md2));
        md2.config = Map.of("param1", "value", "param2", 25, "param3", true);
        assertFalse(md1.equals(md2));
        md2.config = Map.of("param1", "value", "param2", 50, "param3", false);
        assertFalse(md1.equals(md2));
        md2.config = Map.of("param1", "value", "param2", 50);
        assertFalse(md1.equals(md2));
        md2.config = Map.of("param1", "value", "param2", 50, "param3", true);
        assertTrue(md1.equals(md2));
        assertEquals(md1.hashCode(), md2.hashCode());
    }
}
