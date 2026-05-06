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
package org.openhab.core.model.yaml.internal.sitemaps;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlMappingDTOTest} contains tests for the {@link YamlMappingDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlMappingDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlMappingDTO mapping = new YamlMappingDTO();
        assertFalse(mapping.isValid(err, warn));

        mapping.command = "ON";
        assertFalse(mapping.isValid(err, warn));

        mapping.label = "Command On";
        assertTrue(mapping.isValid(err, warn));

        mapping.releaseCommand = "OFF";
        assertTrue(mapping.isValid(err, warn));

        mapping.icon = "@icon";
        assertFalse(mapping.isValid(err, warn));
        mapping.icon = "oh:classic:switch:test";
        assertFalse(mapping.isValid(err, warn));
        mapping.icon = "oh:classic:switch";
        assertTrue(mapping.isValid(err, warn));
        mapping.icon = "switch";
        assertTrue(mapping.isValid(err, warn));
        mapping.icon = "material:favorite";
        assertTrue(mapping.isValid(err, warn));
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlMappingDTO mapping1 = new YamlMappingDTO();
        YamlMappingDTO mapping2 = new YamlMappingDTO();

        mapping1.command = "ON";
        mapping1.label = "Command On";
        mapping2.command = "OFF";
        mapping2.label = "Command On";
        assertFalse(mapping1.equals(mapping2));
        mapping2.command = "ON";
        mapping2.label = "Cmd On";
        assertFalse(mapping1.equals(mapping2));
        mapping2.command = "ON";
        mapping2.label = "Command On";
        assertTrue(mapping1.equals(mapping2));
        assertEquals(mapping1.hashCode(), mapping2.hashCode());

        mapping1.releaseCommand = "OFF";
        assertFalse(mapping1.equals(mapping2));
        mapping2.releaseCommand = "off";
        assertFalse(mapping1.equals(mapping2));
        mapping2.releaseCommand = "OFF";
        assertTrue(mapping1.equals(mapping2));
        assertEquals(mapping1.hashCode(), mapping2.hashCode());

        mapping1.icon = "switch-on";
        assertFalse(mapping1.equals(mapping2));
        mapping2.icon = "switch-off";
        assertFalse(mapping1.equals(mapping2));
        mapping2.icon = "switch-on";
        assertTrue(mapping1.equals(mapping2));
        assertEquals(mapping1.hashCode(), mapping2.hashCode());
    }
}
