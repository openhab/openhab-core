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
 * The {@link YamlRuleWithAndConditionsDTOTest} contains tests for the {@link YamlRuleWithAndConditionsDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlRuleWithAndConditionsDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlRuleWithAndConditionsDTO rule = new YamlRuleWithAndConditionsDTO();
        assertTrue(rule.isValid(err, warn));

        rule.value = "green";
        assertTrue(rule.isValid(err, warn));

        rule.and = List.of();
        assertTrue(rule.isValid(err, warn));

        YamlConditionDTO condition = new YamlConditionDTO();
        condition.item = "item";
        rule.and = List.of(condition);
        assertFalse(rule.isValid(err, warn));
        condition.argument = "50";
        rule.and = List.of(condition);
        assertTrue(rule.isValid(err, warn));
        condition.operator = "EQ";
        rule.and = List.of(condition);
        assertFalse(rule.isValid(err, warn));
        condition.operator = "!=";
        rule.and = List.of(condition);
        assertTrue(rule.isValid(err, warn));
        condition.item = "my-item";
        rule.and = List.of(condition);
        assertFalse(rule.isValid(err, warn));

        condition.item = "item";
        condition.operator = ">=";
        condition.argument = "25";
        YamlConditionDTO condition2 = new YamlConditionDTO();
        condition2.item = "item";
        condition2.operator = "<=";
        condition2.argument = "75";
        rule.and = List.of(condition, condition2);
        assertTrue(rule.isValid(err, warn));

        rule.value = null;
        assertTrue(rule.isValid(err, warn));
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlRuleWithAndConditionsDTO rule1 = new YamlRuleWithAndConditionsDTO();
        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();

        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.value = "green";
        assertFalse(rule1.equals(rule2));
        rule2.value = "red";
        assertFalse(rule1.equals(rule2));
        rule2.value = "green";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        YamlConditionDTO condition1 = new YamlConditionDTO();
        condition1.item = "item";
        condition1.operator = ">=";
        condition1.argument = "25";
        YamlConditionDTO condition2 = new YamlConditionDTO();
        condition2.item = "item2";
        condition2.operator = "<=";
        condition2.argument = "75";
        YamlConditionDTO condition3 = new YamlConditionDTO();
        condition3.item = "item";
        condition3.operator = ">=";
        condition3.argument = "25";
        YamlConditionDTO condition4 = new YamlConditionDTO();
        condition4.item = "item2";
        condition4.operator = "<=";
        condition4.argument = "75";

        rule1.and = List.of(condition1);
        assertFalse(rule1.equals(rule2));
        rule2.and = List.of(condition4);
        assertFalse(rule1.equals(rule2));
        rule2.and = List.of(condition3);
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());
        rule1.and = List.of(condition1, condition2);
        assertFalse(rule1.equals(rule2));
        rule2.and = List.of(condition4, condition3);
        assertFalse(rule1.equals(rule2));
        rule2.and = List.of(condition3, condition4);
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.value = null;
        assertFalse(rule1.equals(rule2));
        rule2.value = null;
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }
}
