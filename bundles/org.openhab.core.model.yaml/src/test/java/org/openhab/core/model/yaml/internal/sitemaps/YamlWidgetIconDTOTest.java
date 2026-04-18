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
 * The {@link YamlWidgetIconDTOTest} contains tests for the {@link YamlWidgetIconDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlWidgetIconDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetIconDTO icon = new YamlWidgetIconDTO();
        assertTrue(icon.isValid(err, warn));

        icon.name = Boolean.TRUE;
        assertFalse(icon.isValid(err, warn));
        icon.name = List.of(0);
        assertFalse(icon.isValid(err, warn));
        icon.name = "@switch";
        assertFalse(icon.isValid(err, warn));
        icon.name = "oh:classic:xxx:switch-on";
        assertFalse(icon.isValid(err, warn));
        icon.name = "switch";
        assertTrue(icon.isValid(err, warn));
        icon.name = "oh:classic:switch";
        assertTrue(icon.isValid(err, warn));

        icon.staticIcon = true;
        assertTrue(icon.isValid(err, warn));

        icon.color = Boolean.TRUE;
        assertFalse(icon.isValid(err, warn));
        icon.color = List.of(0);
        assertFalse(icon.isValid(err, warn));
        icon.color = "red";
        assertTrue(icon.isValid(err, warn));

        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        icon.color = rule1;
        assertFalse(icon.isValid(err, warn));
        rule1.value = "blue";
        assertTrue(icon.isValid(err, warn));
        rule1.operator = "<=";
        assertFalse(icon.isValid(err, warn));
        rule1.argument = "50";
        assertTrue(icon.isValid(err, warn));

        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();
        icon.color = rule2;
        assertFalse(icon.isValid(err, warn));
        rule2.value = "green";
        assertTrue(icon.isValid(err, warn));
        rule2.and = List.of();
        assertTrue(icon.isValid(err, warn));
        YamlConditionDTO condition = new YamlConditionDTO();
        rule2.and = List.of(condition);
        condition.operator = ">";
        assertFalse(icon.isValid(err, warn));
        condition.argument = "50";
        assertTrue(icon.isValid(err, warn));

        icon.color = List.of(rule1, rule2);
        assertTrue(icon.isValid(err, warn));
        rule1.value = null;
        assertFalse(icon.isValid(err, warn));
        rule1.value = "blue";
        assertTrue(icon.isValid(err, warn));
        rule1.argument = null;
        assertFalse(icon.isValid(err, warn));
        rule1.argument = "50";
        assertTrue(icon.isValid(err, warn));
        rule2.value = null;
        assertFalse(icon.isValid(err, warn));
        rule2.value = "green";
        assertTrue(icon.isValid(err, warn));
        condition.argument = null;
        assertFalse(icon.isValid(err, warn));
        condition.argument = "50";
        assertTrue(icon.isValid(err, warn));

        icon.staticIcon = null;
        assertTrue(icon.isValid(err, warn));
        icon.name = null;
        assertTrue(icon.isValid(err, warn));

        YamlRuleWithUniqueConditionDTO rule3 = new YamlRuleWithUniqueConditionDTO();
        icon.name = rule3;
        assertFalse(icon.isValid(err, warn));
        rule3.value = "@switch-on";
        assertFalse(icon.isValid(err, warn));
        rule3.value = "oh:classic:xxx:switch-on";
        assertFalse(icon.isValid(err, warn));
        rule3.value = "switch-on";
        assertTrue(icon.isValid(err, warn));
        rule3.value = "oh:classic:switch-on";
        assertTrue(icon.isValid(err, warn));
        rule3.operator = "<=";
        assertFalse(icon.isValid(err, warn));
        rule3.argument = "50";
        assertTrue(icon.isValid(err, warn));

        YamlRuleWithAndConditionsDTO rule4 = new YamlRuleWithAndConditionsDTO();
        icon.name = rule4;
        assertFalse(icon.isValid(err, warn));
        rule4.value = "@switch-off";
        assertFalse(icon.isValid(err, warn));
        rule4.value = "oh:classic:xxx:switch-off";
        assertFalse(icon.isValid(err, warn));
        rule4.value = "switch-off";
        assertTrue(icon.isValid(err, warn));
        rule4.value = "oh:classic:switch-off";
        assertTrue(icon.isValid(err, warn));
        rule4.and = List.of();
        assertTrue(icon.isValid(err, warn));
        YamlConditionDTO condition2 = new YamlConditionDTO();
        rule4.and = List.of(condition2);
        condition2.operator = ">";
        assertFalse(icon.isValid(err, warn));
        condition2.argument = "50";
        assertTrue(icon.isValid(err, warn));

        icon.name = List.of(rule3, rule4);
        assertTrue(icon.isValid(err, warn));
        rule3.value = null;
        assertFalse(icon.isValid(err, warn));
        rule3.value = "switch-on";
        assertTrue(icon.isValid(err, warn));
        rule3.argument = null;
        assertFalse(icon.isValid(err, warn));
        rule3.argument = "50";
        assertTrue(icon.isValid(err, warn));
        rule4.value = null;
        assertFalse(icon.isValid(err, warn));
        rule4.value = "switch-off";
        assertTrue(icon.isValid(err, warn));
        condition2.argument = null;
        assertFalse(icon.isValid(err, warn));
        condition2.argument = "50";
        assertTrue(icon.isValid(err, warn));
    }

    @Test
    public void testEquals() throws IOException {
        YamlWidgetIconDTO icon1 = new YamlWidgetIconDTO();
        YamlWidgetIconDTO icon2 = new YamlWidgetIconDTO();

        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());

        icon1.name = "switch-on";
        assertFalse(icon1.equals(icon2));
        icon2.name = "switch-off";
        assertFalse(icon1.equals(icon2));
        icon2.name = "switch-on";
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());

        icon1.staticIcon = true;
        assertFalse(icon1.equals(icon2));
        icon2.staticIcon = false;
        assertFalse(icon1.equals(icon2));
        icon2.staticIcon = true;
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());

        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        rule1.value = "blue";
        rule1.operator = "<";
        rule1.argument = "50";
        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();
        rule2.value = "green";
        YamlConditionDTO condition1 = new YamlConditionDTO();
        condition1.operator = ">";
        condition1.argument = "50";
        rule2.and = List.of(condition1);

        YamlRuleWithUniqueConditionDTO rule3 = new YamlRuleWithUniqueConditionDTO();
        rule3.value = "blue";
        rule3.operator = "<";
        rule3.argument = "50";
        YamlRuleWithAndConditionsDTO rule4 = new YamlRuleWithAndConditionsDTO();
        rule4.value = "green";
        YamlConditionDTO condition2 = new YamlConditionDTO();
        condition2.operator = ">";
        condition2.argument = "50";
        rule4.and = List.of(condition2);

        icon1.color = "red";
        assertFalse(icon1.equals(icon2));
        icon2.color = "green";
        assertFalse(icon1.equals(icon2));
        icon2.color = "red";
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
        icon1.color = rule1;
        icon2.color = null;
        assertFalse(icon1.equals(icon2));
        icon2.color = "red";
        assertFalse(icon1.equals(icon2));
        icon2.color = rule4;
        assertFalse(icon1.equals(icon2));
        icon2.color = rule3;
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
        icon1.color = rule2;
        icon2.color = null;
        assertFalse(icon1.equals(icon2));
        icon2.color = "red";
        assertFalse(icon1.equals(icon2));
        icon2.color = rule3;
        assertFalse(icon1.equals(icon2));
        icon2.color = rule4;
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
        icon1.color = List.of(rule1, rule2);
        assertFalse(icon1.equals(icon2));
        icon2.color = null;
        assertFalse(icon1.equals(icon2));
        icon2.color = "red";
        assertFalse(icon1.equals(icon2));
        icon2.color = List.of(rule3);
        assertFalse(icon1.equals(icon2));
        icon2.color = List.of(rule4, rule3);
        assertFalse(icon1.equals(icon2));
        icon2.color = List.of(rule3, rule4);
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());

        YamlRuleWithUniqueConditionDTO rule5 = new YamlRuleWithUniqueConditionDTO();
        rule5.value = "switch-on";
        rule5.operator = "<";
        rule5.argument = "50";
        YamlRuleWithAndConditionsDTO rule6 = new YamlRuleWithAndConditionsDTO();
        rule6.value = "switch-off";
        YamlConditionDTO condition3 = new YamlConditionDTO();
        condition3.operator = ">";
        condition3.argument = "50";
        rule6.and = List.of(condition3);

        YamlRuleWithUniqueConditionDTO rule7 = new YamlRuleWithUniqueConditionDTO();
        rule7.value = "switch-on";
        rule7.operator = "<";
        rule7.argument = "50";
        YamlRuleWithAndConditionsDTO rule8 = new YamlRuleWithAndConditionsDTO();
        rule8.value = "switch-off";
        YamlConditionDTO condition4 = new YamlConditionDTO();
        condition4.operator = ">";
        condition4.argument = "50";
        rule8.and = List.of(condition4);

        icon1.name = rule5;
        icon2.name = null;
        assertFalse(icon1.equals(icon2));
        icon2.name = "switch";
        assertFalse(icon1.equals(icon2));
        icon2.name = rule8;
        assertFalse(icon1.equals(icon2));
        icon2.name = rule7;
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
        icon1.name = rule6;
        icon2.name = null;
        assertFalse(icon1.equals(icon2));
        icon2.name = "switch";
        assertFalse(icon1.equals(icon2));
        icon2.name = rule7;
        assertFalse(icon1.equals(icon2));
        icon2.name = rule8;
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
        icon1.name = List.of(rule5, rule6);
        assertFalse(icon1.equals(icon2));
        icon2.name = null;
        assertFalse(icon1.equals(icon2));
        icon2.name = "switch";
        assertFalse(icon1.equals(icon2));
        icon2.name = List.of(rule7);
        assertFalse(icon1.equals(icon2));
        icon2.name = List.of(rule8, rule7);
        assertFalse(icon1.equals(icon2));
        icon2.name = List.of(rule7, rule8);
        assertTrue(icon1.equals(icon2));
        assertEquals(icon1.hashCode(), icon2.hashCode());
    }
}
