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
package org.openhab.core.model.yaml.internal.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Visibility;

/**
 * The {@link YamlRuleDTOTest} contains tests for the {@link YamlRuleDTO} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class YamlRuleDTOTest {

    @Test
    public void testIsValid() throws IOException {
        YamlRuleDTO rule = new YamlRuleDTO();
        assertFalse(rule.isValid(null, null));

        rule.uid = " ";
        assertFalse(rule.isValid(null, null));

        rule.label = "Test";
        rule.config = new HashMap<>();
        rule.config.put("foo", "bar");

        rule.uid = "id";
        assertTrue(rule.isValid(null, null));

        rule.uid = "rule:id";
        assertTrue(rule.isValid(null, null));

        rule.uid = "rule:type:@id";
        assertFalse(rule.isValid(null, null));

        rule.uid = "rule:type:id";
        assertTrue(rule.isValid(null, null));

        rule.uid = "rule:type:$subType:id";
        assertFalse(rule.isValid(null, null));

        rule.uid = "rule:type:subType:id";
        assertTrue(rule.isValid(null, null));

        rule.label = null;
        assertFalse(rule.isValid(null, null));

        rule.label = "\t";
        assertFalse(rule.isValid(null, null));

        rule.label = "Test";
        rule.config.clear();
        assertFalse(rule.isValid(null, null));

        rule.triggers = new ArrayList<>();
        assertFalse(rule.isValid(null, null));

        rule.triggers.add(new YamlModuleDTO());
        assertTrue(rule.isValid(null, null));

        rule.triggers.clear();
        rule.conditions = new ArrayList<>();
        assertFalse(rule.isValid(null, null));

        rule.conditions.add(new YamlConditionDTO());
        assertTrue(rule.isValid(null, null));

        rule.conditions.clear();
        rule.actions = new ArrayList<>();
        assertFalse(rule.isValid(null, null));

        rule.actions.add(new YamlActionDTO());
        assertTrue(rule.isValid(null, null));

        YamlModuleDTO trigger = new YamlModuleDTO();
        trigger.id = "1";
        rule.triggers.add(trigger);
        YamlModuleDTO trigger2 = new YamlModuleDTO();
        trigger2.id = "1";
        rule.triggers.add(trigger2);
        assertFalse(rule.isValid(null, null));

        trigger2.id = "3";
        assertTrue(rule.isValid(null, null));

        YamlConditionDTO condition = new YamlConditionDTO();
        condition.id = "3";
        rule.conditions.add(condition);
        assertFalse(rule.isValid(null, null));

        condition.id = "2";
        assertTrue(rule.isValid(null, null));

        YamlActionDTO action = new YamlActionDTO();
        action.id = "script";
        rule.actions.add(action);
        assertTrue(rule.isValid(null, null));

        action = new YamlActionDTO();
        action.id = "script";
        rule.actions.add(action);
        assertFalse(rule.isValid(null, null));

        action.id = "1";
        assertFalse(rule.isValid(null, null));

        action.id = "2";
        assertFalse(rule.isValid(null, null));

        action.id = "3";
        assertFalse(rule.isValid(null, null));

        action.id = "4";
        assertTrue(rule.isValid(null, null));
    }

    @Test
    public void testEquals() throws IOException {
        YamlRuleDTO rule1 = new YamlRuleDTO();
        YamlRuleDTO rule2 = new YamlRuleDTO();

        assertNotNull(rule1);
        assertTrue(rule1.equals(rule1));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.uid = "rule:id";
        rule2.uid = "rule:id2";
        assertFalse(rule1.equals(rule2));
        assertNotEquals(rule1.hashCode(), rule2.hashCode());

        rule2.uid = "rule:id";
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());

        rule1.label = "A label";
        rule2.label = "Another label";
        assertFalse(rule1.equals(rule2));
        assertNotEquals(rule1.hashCode(), rule2.hashCode());

        rule2.label = "A label";
        assertTrue(rule1.equals(rule2));

        rule1.description = "A description";
        assertFalse(rule1.equals(rule2));

        rule2.description = "Another description";
        assertFalse(rule1.equals(rule2));

        rule2.description = "A description";
        assertTrue(rule1.equals(rule2));

        rule1.visibility = Visibility.VISIBLE;
        assertFalse(rule1.equals(rule2));

        rule2.visibility = Visibility.EXPERT;
        assertFalse(rule1.equals(rule2));

        rule1.visibility = Visibility.EXPERT;
        assertTrue(rule1.equals(rule2));

        rule1.template = "template:1";
        assertFalse(rule1.equals(rule2));

        rule2.template = "template:2";
        assertFalse(rule1.equals(rule2));

        rule2.template = "template:1";
        assertTrue(rule1.equals(rule2));

        rule1.tags = new HashSet<>();
        assertFalse(rule1.equals(rule2));

        rule1.tags.add("Tag1");
        assertFalse(rule1.equals(rule2));

        rule2.tags = new HashSet<>();
        assertFalse(rule1.equals(rule2));

        rule2.tags.add("Tag2");
        assertFalse(rule1.equals(rule2));

        rule2.tags.add("Tag1");
        assertFalse(rule1.equals(rule2));

        rule2.tags.remove("Tag2");
        assertTrue(rule1.equals(rule2));

        rule1.actions = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlActionDTO action1 = new YamlActionDTO();
        rule1.actions.add(action1);
        assertFalse(rule1.equals(rule2));

        rule2.actions = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlActionDTO action2 = new YamlActionDTO();
        rule2.actions.add(action2);
        assertTrue(rule1.equals(rule2));

        action1.id = "action1";
        assertFalse(rule1.equals(rule2));

        action2.id = "action2";
        assertFalse(rule1.equals(rule2));

        action2.id = "action1";
        assertTrue(rule1.equals(rule2));

        rule1.conditions = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlConditionDTO condition1 = new YamlConditionDTO();
        rule1.conditions.add(condition1);
        assertFalse(rule1.equals(rule2));

        rule2.conditions = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlConditionDTO condition2 = new YamlConditionDTO();
        rule2.conditions.add(condition2);
        assertTrue(rule1.equals(rule2));

        condition1.id = "condition1";
        assertFalse(rule1.equals(rule2));

        condition2.id = "condition2";
        assertFalse(rule1.equals(rule2));

        condition2.id = "condition1";
        assertTrue(rule1.equals(rule2));

        rule1.triggers = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlModuleDTO trigger1 = new YamlModuleDTO();
        rule1.triggers.add(trigger1);
        assertFalse(rule1.equals(rule2));

        rule2.triggers = new ArrayList<>();
        assertFalse(rule1.equals(rule2));

        YamlModuleDTO trigger2 = new YamlModuleDTO();
        rule2.triggers.add(trigger2);
        assertTrue(rule1.equals(rule2));

        trigger1.id = "trigger1";
        assertFalse(rule1.equals(rule2));

        trigger2.id = "trigger2";
        assertFalse(rule1.equals(rule2));

        trigger2.id = "trigger1";
        assertTrue(rule1.equals(rule2));

        rule1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        rule2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertTrue(rule1.equals(rule2));
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }
}
