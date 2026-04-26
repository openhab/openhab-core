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
 * The {@link YamlConditionDTOTest} contains tests for the {@link YamlConditionDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlConditionDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlConditionDTO condition = new YamlConditionDTO();
        assertTrue(condition.isValid(err, warn));

        condition.item = "item";
        condition.operator = null;
        assertFalse(condition.isValid(err, warn));
        condition.item = null;
        condition.operator = "==";
        assertFalse(condition.isValid(err, warn));
        condition.item = "item";
        condition.operator = null;
        condition.argument = "50";
        assertTrue(condition.isValid(err, warn));
        condition.item = null;
        condition.operator = "==";
        condition.argument = "50";
        assertTrue(condition.isValid(err, warn));
        condition.item = "item";
        condition.operator = "==";
        condition.argument = "50";
        assertTrue(condition.isValid(err, warn));
        condition.operator = "EQ";
        assertFalse(condition.isValid(err, warn));
        condition.operator = "!=";
        assertTrue(condition.isValid(err, warn));
        condition.operator = "<";
        assertTrue(condition.isValid(err, warn));
        condition.operator = "<=";
        assertTrue(condition.isValid(err, warn));
        condition.operator = ">";
        assertTrue(condition.isValid(err, warn));
        condition.operator = ">=";
        assertTrue(condition.isValid(err, warn));
        condition.item = "my-item";
        assertFalse(condition.isValid(err, warn));
        condition.item = "0_item";
        assertFalse(condition.isValid(err, warn));
        condition.item = "_item";
        assertTrue(condition.isValid(err, warn));
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlConditionDTO condition1 = new YamlConditionDTO();
        YamlConditionDTO condition2 = new YamlConditionDTO();

        assertTrue(condition1.equals(condition1));
        assertEquals(condition1.hashCode(), condition1.hashCode());

        condition1.argument = "ON";
        assertFalse(condition1.equals(condition2));
        condition2.argument = "OFF";
        assertFalse(condition1.equals(condition2));
        condition2.argument = "ON";
        assertTrue(condition1.equals(condition2));
        assertEquals(condition1.hashCode(), condition2.hashCode());

        condition1.item = "item";
        assertFalse(condition1.equals(condition2));
        condition2.item = "item2";
        assertFalse(condition1.equals(condition2));
        condition2.item = "item";
        assertTrue(condition1.equals(condition2));
        assertEquals(condition1.hashCode(), condition2.hashCode());

        condition1.operator = "!=";
        assertFalse(condition1.equals(condition2));
        condition2.operator = "==";
        assertFalse(condition1.equals(condition2));
        condition2.operator = "!=";
        assertTrue(condition1.equals(condition2));
        assertEquals(condition1.hashCode(), condition2.hashCode());
    }
}
