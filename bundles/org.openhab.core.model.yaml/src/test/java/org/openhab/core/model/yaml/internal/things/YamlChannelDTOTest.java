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
package org.openhab.core.model.yaml.internal.things;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlChannelDTOTest} contains tests for the {@link YamlChannelDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlChannelDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlChannelDTO ch = new YamlChannelDTO();
        assertFalse(ch.isValid(err, warn));

        ch.type = "channel-type";
        assertTrue(ch.isValid(err, warn));

        ch.type = "$channel-type";
        assertFalse(ch.isValid(err, warn));

        ch.type = null;
        ch.itemType = "String";
        assertTrue(ch.isValid(err, warn));

        ch.itemType = "string";
        assertTrue(ch.isValid(err, warn));

        ch.itemType = "Other";
        assertFalse(ch.isValid(err, warn));

        ch.itemType = "Number";
        assertTrue(ch.isValid(err, warn));

        ch.itemDimension = "Dimensionless";
        assertTrue(ch.isValid(err, warn));

        ch.itemType = "number";
        ch.itemDimension = "dimensionless";
        assertTrue(ch.isValid(err, warn));

        ch.itemDimension = "Other";
        assertFalse(ch.isValid(err, warn));

        ch.itemType = "Color";
        ch.itemDimension = null;
        ch.kind = "wrong";
        assertTrue(ch.isValid(err, warn));
    }

    @Test
    public void testEquals() throws IOException {
        YamlChannelDTO ch1 = new YamlChannelDTO();
        YamlChannelDTO ch2 = new YamlChannelDTO();

        ch1.type = "channel-type";
        ch2.type = "channel-type";
        assertTrue(ch1.equals(ch2));

        ch1.type = null;
        ch1.itemType = "String";
        ch2.type = null;
        ch2.itemType = "String";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "trigger";
        ch2.kind = "TRIGGER";
        assertTrue(ch1.equals(ch2));

        ch1.label = "A label";
        ch2.label = "A label";
        assertTrue(ch1.equals(ch2));

        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertTrue(ch1.equals(ch2));
    }

    @Test
    public void testEqualsWithTypeOrItemType() throws IOException {
        YamlChannelDTO ch1 = new YamlChannelDTO();
        YamlChannelDTO ch2 = new YamlChannelDTO();

        ch1.type = "channel-type";
        ch2.type = "channel-type";
        assertTrue(ch1.equals(ch2));
        ch2.type = "channel-type2";
        assertFalse(ch1.equals(ch2));

        ch1.type = null;
        ch1.itemType = "String";
        ch2.type = null;
        ch2.itemType = "String";
        assertTrue(ch1.equals(ch2));
        ch1.itemType = "String";
        ch2.itemType = "Number";
        assertFalse(ch1.equals(ch2));
        ch1.itemType = "Number";
        ch2.itemType = "Number";
        assertTrue(ch1.equals(ch2));
        ch2.itemType = "number";
        assertTrue(ch1.equals(ch2));
        ch1.itemDimension = "Temperature";
        assertFalse(ch1.equals(ch2));
        ch2.itemDimension = "Humidity";
        assertFalse(ch1.equals(ch2));
        ch2.itemDimension = "Temperature";
        assertTrue(ch1.equals(ch2));
        ch2.itemDimension = "temperature";
        assertTrue(ch1.equals(ch2));

        ch1.type = "channel-type";
        ch1.itemType = null;
        ch1.itemDimension = null;
        ch2.type = null;
        ch2.itemType = "String";
        ch2.itemDimension = null;
        assertFalse(ch1.equals(ch2));

        ch1.type = null;
        ch1.itemType = "String";
        ch2.type = "channel-type";
        ch2.itemType = null;
        assertFalse(ch1.equals(ch2));

        ch1.type = null;
        ch1.itemType = "Switch";
        ch1.kind = "state";
        ch2.type = null;
        ch2.itemType = "Switch";
        ch2.kind = "state";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "trigger";
        ch2.kind = "trigger";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "trigger";
        ch2.kind = "Trigger";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "Trigger";
        ch2.kind = "TRIGGER";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "state";
        ch2.kind = "trigger";
        assertFalse(ch1.equals(ch2));

        ch1.kind = "trigger";
        ch2.kind = "state";
        assertFalse(ch1.equals(ch2));

        ch1.kind = null;
        ch2.kind = "trigger";
        assertFalse(ch1.equals(ch2));

        ch1.kind = "trigger";
        ch2.kind = null;
        assertFalse(ch1.equals(ch2));

        ch1.kind = null;
        ch2.kind = "state";
        assertTrue(ch1.equals(ch2));

        ch1.kind = "state";
        ch2.kind = null;
        assertTrue(ch1.equals(ch2));
    }

    @Test
    public void testEqualsWithLabel() throws IOException {
        YamlChannelDTO ch1 = new YamlChannelDTO();
        YamlChannelDTO ch2 = new YamlChannelDTO();

        ch1.itemType = "String";
        ch2.itemType = "String";

        ch1.label = null;
        ch2.label = null;
        assertTrue(ch1.equals(ch2));
        ch1.label = "A label";
        ch2.label = null;
        assertFalse(ch1.equals(ch2));
        ch1.label = null;
        ch2.label = "A label";
        assertFalse(ch1.equals(ch2));
        ch1.label = "A label";
        ch2.label = "A different label";
        assertFalse(ch1.equals(ch2));
        ch1.label = "A label";
        ch2.label = "A label";
        assertTrue(ch1.equals(ch2));
    }

    @Test
    public void testEqualsWithConfigurations() throws IOException {
        YamlChannelDTO ch1 = new YamlChannelDTO();
        YamlChannelDTO ch2 = new YamlChannelDTO();

        ch1.type = "channel-type";
        ch2.type = "channel-type";

        ch1.config = null;
        ch2.config = null;
        assertTrue(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = null;
        assertFalse(ch1.equals(ch2));
        ch1.config = null;
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "other value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 25, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", false, "param4", List.of("val 1", "val 2"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "value 2"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1"));
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", 75);
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true);
        assertFalse(ch1.equals(ch2));
        ch1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        ch2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertTrue(ch1.equals(ch2));
    }
}
