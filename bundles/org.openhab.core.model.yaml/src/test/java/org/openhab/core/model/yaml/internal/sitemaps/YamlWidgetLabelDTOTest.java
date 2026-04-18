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
 * The {@link YamlWidgetLabelDTOTest} contains tests for the {@link YamlWidgetLabelDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlWidgetLabelDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetLabelDTO label = new YamlWidgetLabelDTO();
        assertTrue(label.isValid(err, warn));

        label.label = "Label";
        assertTrue(label.isValid(err, warn));

        label.format = "%d";
        assertTrue(label.isValid(err, warn));

        label.labelColor = Boolean.TRUE;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid type for \"labelColor\" field", err.getFirst());
        err.clear();
        label.labelColor = List.of(0);
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid type for rule in \"labelColor\" field", err.getFirst());
        err.clear();
        label.labelColor = "red";
        assertTrue(label.isValid(err, warn));

        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        label.labelColor = rule1;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule1.value = "blue";
        assertTrue(label.isValid(err, warn));
        rule1.operator = "<=";
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule1.argument = "50";
        assertTrue(label.isValid(err, warn));

        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();
        label.labelColor = rule2;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule2.value = "green";
        assertTrue(label.isValid(err, warn));
        rule2.and = List.of();
        assertTrue(label.isValid(err, warn));
        YamlConditionDTO condition = new YamlConditionDTO();
        rule2.and = List.of(condition);
        condition.operator = ">";
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition.argument = "50";
        assertTrue(label.isValid(err, warn));

        label.labelColor = List.of(rule1, rule2);
        assertTrue(label.isValid(err, warn));
        rule1.value = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule1.value = "blue";
        assertTrue(label.isValid(err, warn));
        rule1.argument = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule1.argument = "50";
        assertTrue(label.isValid(err, warn));
        rule2.value = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule2.value = "green";
        assertTrue(label.isValid(err, warn));
        condition.argument = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"labelColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition.argument = "50";
        assertTrue(label.isValid(err, warn));

        label.valueColor = Boolean.TRUE;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid type for \"valueColor\" field", err.getFirst());
        err.clear();
        label.valueColor = List.of(0);
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid type for rule in \"valueColor\" field", err.getFirst());
        err.clear();
        label.valueColor = "red";
        assertTrue(label.isValid(err, warn));

        YamlRuleWithUniqueConditionDTO rule3 = new YamlRuleWithUniqueConditionDTO();
        label.valueColor = rule3;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule3.value = "blue";
        assertTrue(label.isValid(err, warn));
        rule3.operator = "<=";
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule3.argument = "50";
        assertTrue(label.isValid(err, warn));

        YamlRuleWithAndConditionsDTO rule4 = new YamlRuleWithAndConditionsDTO();
        label.valueColor = rule4;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule4.value = "green";
        assertTrue(label.isValid(err, warn));
        rule4.and = List.of();
        assertTrue(label.isValid(err, warn));
        YamlConditionDTO condition2 = new YamlConditionDTO();
        rule4.and = List.of(condition2);
        condition2.operator = ">";
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition2.argument = "50";
        assertTrue(label.isValid(err, warn));

        label.valueColor = List.of(rule3, rule4);
        assertTrue(label.isValid(err, warn));
        rule3.value = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule3.value = "blue";
        assertTrue(label.isValid(err, warn));
        rule3.argument = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule3.argument = "50";
        assertTrue(label.isValid(err, warn));
        rule4.value = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"value\" field missing while mandatory", err.getFirst());
        err.clear();
        rule4.value = "green";
        assertTrue(label.isValid(err, warn));
        condition2.argument = null;
        assertFalse(label.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"valueColor\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition2.argument = "50";
        assertTrue(label.isValid(err, warn));

        label.label = null;
        assertTrue(label.isValid(err, warn));
        label.format = null;
        assertTrue(label.isValid(err, warn));

        assertEquals(0, err.size());
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlWidgetLabelDTO label1 = new YamlWidgetLabelDTO();
        YamlWidgetLabelDTO label2 = new YamlWidgetLabelDTO();

        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());

        label1.label = "Label";
        assertFalse(label1.equals(label2));
        label2.label = "Other llabel";
        assertFalse(label1.equals(label2));
        label2.label = "Label";
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());

        label1.format = "%d";
        assertFalse(label1.equals(label2));
        label2.format = "%f";
        assertFalse(label1.equals(label2));
        label2.format = "%d";
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());

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

        label1.labelColor = "red";
        assertFalse(label1.equals(label2));
        label2.labelColor = "green";
        assertFalse(label1.equals(label2));
        label2.labelColor = "red";
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.labelColor = rule1;
        label2.labelColor = null;
        assertFalse(label1.equals(label2));
        label2.labelColor = "red";
        assertFalse(label1.equals(label2));
        label2.labelColor = rule4;
        assertFalse(label1.equals(label2));
        label2.labelColor = rule3;
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.labelColor = rule2;
        label2.labelColor = null;
        assertFalse(label1.equals(label2));
        label2.labelColor = "red";
        assertFalse(label1.equals(label2));
        label2.labelColor = rule3;
        assertFalse(label1.equals(label2));
        label2.labelColor = rule4;
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.labelColor = List.of(rule1, rule2);
        assertFalse(label1.equals(label2));
        label2.labelColor = null;
        assertFalse(label1.equals(label2));
        label2.labelColor = "red";
        assertFalse(label1.equals(label2));
        label2.labelColor = List.of(rule3);
        assertFalse(label1.equals(label2));
        label2.labelColor = List.of(rule4, rule3);
        assertFalse(label1.equals(label2));
        label2.labelColor = List.of(rule3, rule4);
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());

        YamlRuleWithUniqueConditionDTO rule5 = new YamlRuleWithUniqueConditionDTO();
        rule5.value = "blue";
        rule5.operator = "<";
        rule5.argument = "50";
        YamlRuleWithAndConditionsDTO rule6 = new YamlRuleWithAndConditionsDTO();
        rule6.value = "green";
        YamlConditionDTO condition3 = new YamlConditionDTO();
        condition3.operator = ">";
        condition3.argument = "50";
        rule6.and = List.of(condition3);

        YamlRuleWithUniqueConditionDTO rule7 = new YamlRuleWithUniqueConditionDTO();
        rule7.value = "blue";
        rule7.operator = "<";
        rule7.argument = "50";
        YamlRuleWithAndConditionsDTO rule8 = new YamlRuleWithAndConditionsDTO();
        rule8.value = "green";
        YamlConditionDTO condition4 = new YamlConditionDTO();
        condition4.operator = ">";
        condition4.argument = "50";
        rule8.and = List.of(condition4);

        label1.valueColor = "red";
        assertFalse(label1.equals(label2));
        label2.valueColor = "green";
        assertFalse(label1.equals(label2));
        label2.valueColor = "red";
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.valueColor = rule5;
        label2.valueColor = null;
        assertFalse(label1.equals(label2));
        label2.valueColor = "red";
        assertFalse(label1.equals(label2));
        label2.valueColor = rule8;
        assertFalse(label1.equals(label2));
        label2.valueColor = rule7;
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.valueColor = rule6;
        label2.valueColor = null;
        assertFalse(label1.equals(label2));
        label2.valueColor = "red";
        assertFalse(label1.equals(label2));
        label2.valueColor = rule7;
        assertFalse(label1.equals(label2));
        label2.valueColor = rule8;
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
        label1.valueColor = List.of(rule5, rule6);
        assertFalse(label1.equals(label2));
        label2.valueColor = null;
        assertFalse(label1.equals(label2));
        label2.valueColor = "red";
        assertFalse(label1.equals(label2));
        label2.valueColor = List.of(rule7);
        assertFalse(label1.equals(label2));
        label2.valueColor = List.of(rule8, rule7);
        assertFalse(label1.equals(label2));
        label2.valueColor = List.of(rule7, rule8);
        assertTrue(label1.equals(label2));
        assertEquals(label1.hashCode(), label2.hashCode());
    }
}
