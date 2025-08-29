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
import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Visibility;

/**
 * The {@link YamlRuleTemplateDTOTest} contains tests for the {@link YamlRuleTemplateDTO} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class YamlRuleTemplateDTOTest {

    @Test
    public void testIsValid() throws IOException {
        YamlRuleTemplateDTO template = new YamlRuleTemplateDTO();
        assertFalse(template.isValid(null, null));

        template.uid = " ";
        assertFalse(template.isValid(null, null));

        template.label = "Test";

        template.uid = "id";
        template.triggers = new ArrayList<>();
        template.triggers.add(new YamlModuleDTO());
        assertTrue(template.isValid(null, null));

        template.uid = "template:id";
        assertTrue(template.isValid(null, null));

        template.uid = "template:type:@id";
        assertFalse(template.isValid(null, null));

        template.uid = "template:type:id";
        assertTrue(template.isValid(null, null));

        template.uid = "template:type:$subType:id";
        assertFalse(template.isValid(null, null));

        template.uid = "template:type:subType:id";
        assertTrue(template.isValid(null, null));

        template.label = null;
        assertFalse(template.isValid(null, null));

        template.label = "\t";
        assertFalse(template.isValid(null, null));

        template.label = "Test";
        assertTrue(template.isValid(null, null));

        template.triggers = new ArrayList<>();
        assertFalse(template.isValid(null, null));

        template.triggers.add(new YamlModuleDTO());
        assertTrue(template.isValid(null, null));

        template.triggers.clear();
        template.conditions = new ArrayList<>();
        assertFalse(template.isValid(null, null));

        template.conditions.add(new YamlConditionDTO());
        assertTrue(template.isValid(null, null));

        template.conditions.clear();
        template.actions = new ArrayList<>();
        assertFalse(template.isValid(null, null));

        template.actions.add(new YamlActionDTO());
        assertTrue(template.isValid(null, null));

        YamlModuleDTO trigger = new YamlModuleDTO();
        trigger.id = "1";
        template.triggers.add(trigger);
        YamlModuleDTO trigger2 = new YamlModuleDTO();
        trigger2.id = "1";
        template.triggers.add(trigger2);
        assertFalse(template.isValid(null, null));

        trigger2.id = "3";
        assertTrue(template.isValid(null, null));

        YamlConditionDTO condition = new YamlConditionDTO();
        condition.id = "3";
        template.conditions.add(condition);
        assertFalse(template.isValid(null, null));

        condition.id = "2";
        assertTrue(template.isValid(null, null));

        YamlActionDTO action = new YamlActionDTO();
        action.id = "script";
        template.actions.add(action);
        assertTrue(template.isValid(null, null));

        action = new YamlActionDTO();
        action.id = "script";
        template.actions.add(action);
        assertFalse(template.isValid(null, null));

        action.id = "1";
        assertFalse(template.isValid(null, null));

        action.id = "2";
        assertFalse(template.isValid(null, null));

        action.id = "3";
        assertFalse(template.isValid(null, null));

        action.id = "4";
        assertTrue(template.isValid(null, null));
    }

    @Test
    public void testEquals() throws IOException {
        YamlRuleTemplateDTO template1 = new YamlRuleTemplateDTO();
        YamlRuleTemplateDTO template2 = new YamlRuleTemplateDTO();

        assertNotNull(template1);
        assertTrue(template1.equals(template1));
        assertEquals(template1.hashCode(), template2.hashCode());

        template1.uid = "template:id";
        template2.uid = "template:id2";
        assertFalse(template1.equals(template2));
        assertNotEquals(template1.hashCode(), template2.hashCode());

        template2.uid = "template:id";
        assertTrue(template1.equals(template2));
        assertEquals(template1.hashCode(), template2.hashCode());

        template1.label = "A label";
        template2.label = "Another label";
        assertFalse(template1.equals(template2));
        assertNotEquals(template1.hashCode(), template2.hashCode());

        template2.label = "A label";
        assertTrue(template1.equals(template2));

        template1.description = "A description";
        assertFalse(template1.equals(template2));

        template2.description = "Another description";
        assertFalse(template1.equals(template2));

        template2.description = "A description";
        assertTrue(template1.equals(template2));

        template1.visibility = Visibility.VISIBLE;
        assertFalse(template1.equals(template2));

        template2.visibility = Visibility.EXPERT;
        assertFalse(template1.equals(template2));

        template1.visibility = Visibility.EXPERT;
        assertTrue(template1.equals(template2));

        template1.tags = new HashSet<>();
        assertFalse(template1.equals(template2));

        template1.tags.add("Tag1");
        assertFalse(template1.equals(template2));

        template2.tags = new HashSet<>();
        assertFalse(template1.equals(template2));

        template2.tags.add("Tag2");
        assertFalse(template1.equals(template2));

        template2.tags.add("Tag1");
        assertFalse(template1.equals(template2));

        template2.tags.remove("Tag2");
        assertTrue(template1.equals(template2));

        template1.actions = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlActionDTO action1 = new YamlActionDTO();
        template1.actions.add(action1);
        assertFalse(template1.equals(template2));

        template2.actions = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlActionDTO action2 = new YamlActionDTO();
        template2.actions.add(action2);
        assertTrue(template1.equals(template2));

        action1.id = "action1";
        assertFalse(template1.equals(template2));

        action2.id = "action2";
        assertFalse(template1.equals(template2));

        action2.id = "action1";
        assertTrue(template1.equals(template2));

        template1.conditions = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlConditionDTO condition1 = new YamlConditionDTO();
        template1.conditions.add(condition1);
        assertFalse(template1.equals(template2));

        template2.conditions = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlConditionDTO condition2 = new YamlConditionDTO();
        template2.conditions.add(condition2);
        assertTrue(template1.equals(template2));

        condition1.id = "condition1";
        assertFalse(template1.equals(template2));

        condition2.id = "condition2";
        assertFalse(template1.equals(template2));

        condition2.id = "condition1";
        assertTrue(template1.equals(template2));

        template1.triggers = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlModuleDTO trigger1 = new YamlModuleDTO();
        template1.triggers.add(trigger1);
        assertFalse(template1.equals(template2));

        template2.triggers = new ArrayList<>();
        assertFalse(template1.equals(template2));

        YamlModuleDTO trigger2 = new YamlModuleDTO();
        template2.triggers.add(trigger2);
        assertTrue(template1.equals(template2));

        trigger1.id = "trigger1";
        assertFalse(template1.equals(template2));

        trigger2.id = "trigger2";
        assertFalse(template1.equals(template2));

        trigger2.id = "trigger1";
        assertTrue(template1.equals(template2));
        assertEquals(template1.hashCode(), template2.hashCode());
    }
}
