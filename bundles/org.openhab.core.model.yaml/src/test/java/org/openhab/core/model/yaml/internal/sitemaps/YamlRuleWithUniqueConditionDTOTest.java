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
 * The {@link YamlRuleWithUniqueConditionDTOTest} contains tests for the {@link YamlRuleWithUniqueConditionDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlRuleWithUniqueConditionDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlRuleWithUniqueConditionDTO rule = new YamlRuleWithUniqueConditionDTO();
        assertTrue(rule.isValid(err, warn));

        rule.value = "green";
        assertTrue(rule.isValid(err, warn));

        rule.item = "item";
        rule.operator = null;
        assertFalse(rule.isValid(err, warn));
        rule.item = null;
        rule.operator = "==";
        assertFalse(rule.isValid(err, warn));
        rule.item = "item";
        rule.operator = null;
        rule.argument = "50";
        assertTrue(rule.isValid(err, warn));
        rule.item = null;
        rule.operator = "==";
        rule.argument = "50";
        assertTrue(rule.isValid(err, warn));
        rule.item = "item";
        rule.operator = "==";
        rule.argument = "50";
        assertTrue(rule.isValid(err, warn));
        rule.operator = "EQ";
        assertFalse(rule.isValid(err, warn));
        rule.operator = "!=";
        assertTrue(rule.isValid(err, warn));
        rule.operator = "<";
        assertTrue(rule.isValid(err, warn));
        rule.operator = "<=";
        assertTrue(rule.isValid(err, warn));
        rule.operator = ">";
        assertTrue(rule.isValid(err, warn));
        rule.operator = ">=";
        assertTrue(rule.isValid(err, warn));
        rule.item = "my-item";
        assertFalse(rule.isValid(err, warn));
        rule.item = "0_item";
        assertFalse(rule.isValid(err, warn));
        rule.item = "_item";
        assertTrue(rule.isValid(err, warn));

        rule.value = null;
        assertTrue(rule.isValid(err, warn));
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        YamlRuleWithUniqueConditionDTO rule2 = new YamlRuleWithUniqueConditionDTO();

        assertTrue(rule1.equals(rule1));
        assertEquals(rule1.hashCode(), rule1.hashCode());

        rule1.value = "green";
        assertFalse(rule1.equals(rule2));
        rule2.value = "red";
        assertFalse(rule1.equals(rule2));
        rule2.value = "green";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.argument = "ON";
        assertFalse(rule1.equals(rule2));
        rule2.argument = "OFF";
        assertFalse(rule1.equals(rule2));
        rule2.argument = "ON";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.item = "item";
        assertFalse(rule1.equals(rule2));
        rule2.item = "item2";
        assertFalse(rule1.equals(rule2));
        rule2.item = "item";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.operator = "!=";
        assertFalse(rule1.equals(rule2));
        rule2.operator = "==";
        assertFalse(rule1.equals(rule2));
        rule2.operator = "!=";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.value = null;
        assertFalse(rule1.equals(rule2));
        rule2.value = null;
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }
}
