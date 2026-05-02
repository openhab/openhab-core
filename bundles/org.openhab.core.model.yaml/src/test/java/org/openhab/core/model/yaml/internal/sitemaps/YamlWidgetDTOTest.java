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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlWidgetDTOTest} contains tests for the {@link YamlWidgetDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlWidgetDTOTest {

    private @NonNullByDefault({}) YamlWidgetDTO subWidgetText;
    private @NonNullByDefault({}) YamlWidgetDTO subWidgetSwitch;
    private @NonNullByDefault({}) YamlWidgetDTO subWidgetFrame;
    private @NonNullByDefault({}) YamlWidgetDTO subWidgetButton;
    private @NonNullByDefault({}) YamlMappingDTO mappingOn;
    private @NonNullByDefault({}) YamlMappingDTO mappingOff;

    @BeforeEach
    public void setup() {
        subWidgetText = new YamlWidgetDTO();
        subWidgetText.type = "Text";

        subWidgetSwitch = new YamlWidgetDTO();
        subWidgetSwitch.type = "Switch";
        subWidgetSwitch.item = "switchItem";

        subWidgetFrame = new YamlWidgetDTO();
        subWidgetFrame.type = "Frame";

        subWidgetButton = new YamlWidgetDTO();
        subWidgetButton.type = "Button";
        subWidgetButton.item = "switchItem";
        subWidgetButton.row = 1;
        subWidgetButton.column = 1;
        subWidgetButton.label = "On";
        subWidgetButton.command = "ON";
        subWidgetButton.stateless = true;

        mappingOn = new YamlMappingDTO();
        mappingOn.command = "ON";
        mappingOn.label = "On";

        mappingOff = new YamlMappingDTO();
        mappingOff.command = "OFF";
        mappingOff.label = "Off";
    }

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"type\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.type = "Unknown";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value \"%s\" for \"type\" field".formatted(widget.type), err.getFirst());
        err.clear();
        widget.type = "Text";
        assertTrue(widget.isValid(err, warn));
        widget.item = "@item";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals(
                "invalid value \"%s\" for \"item\" field; it must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols"
                        .formatted(widget.item),
                err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));

        widget.label = "Test";
        assertTrue(widget.isValid(err, warn));
        YamlWidgetLabelDTO label = new YamlWidgetLabelDTO();
        label.label = "Test";
        widget.label = label;
        assertTrue(widget.isValid(err, warn));
        label.format = "%s";
        assertTrue(widget.isValid(err, warn));
        label.labelColor = "green";
        assertTrue(widget.isValid(err, warn));
        label.valueColor = "red";
        assertTrue(widget.isValid(err, warn));

        widget.icon = "material:home";
        assertTrue(widget.isValid(err, warn));
        YamlWidgetIconDTO icon = new YamlWidgetIconDTO();
        icon.name = "material:home";
        widget.icon = icon;
        assertTrue(widget.isValid(err, warn));
        icon.staticIcon = true;
        assertTrue(widget.isValid(err, warn));
        icon.color = "blue";
        assertTrue(widget.isValid(err, warn));

        assertEquals(0, warn.size());

        widget.visibility = List.of();
        assertTrue(widget.isValid(err, warn));

        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        widget.visibility = rule1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule1.operator = "<=";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule1.argument = "50";
        assertTrue(widget.isValid(err, warn));
        rule1.operator = "EQ";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: invalid value \"EQ\" for \"operator\" field in condition",
                err.getFirst());
        err.clear();
        rule1.operator = null;
        assertTrue(widget.isValid(err, warn));
        rule1.item = "@item";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals(
                "invalid rule in \"visibility\" field: invalid value \"@item\" for \"item\" field in condition; it must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols",
                err.getFirst());
        err.clear();
        rule1.item = "item";
        assertTrue(widget.isValid(err, warn));
        rule1.value = "xxx";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("rule in \"visibility\" field: unexpected \"value\" field is ignored", warn.getFirst());
        warn.clear();
        rule1.value = null;

        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();
        widget.visibility = rule2;
        assertTrue(widget.isValid(err, warn));
        rule2.and = List.of();
        assertTrue(widget.isValid(err, warn));
        YamlConditionDTO condition = new YamlConditionDTO();
        rule2.and = List.of(condition);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition.operator = ">";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition.argument = "50";
        assertTrue(widget.isValid(err, warn));
        condition.operator = "EQ";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: invalid value \"EQ\" for \"operator\" field in condition",
                err.getFirst());
        err.clear();
        condition.operator = null;
        assertTrue(widget.isValid(err, warn));
        condition.item = "@item";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals(
                "invalid rule in \"visibility\" field: invalid value \"@item\" for \"item\" field in condition; it must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols",
                err.getFirst());
        err.clear();
        condition.item = "item";
        assertTrue(widget.isValid(err, warn));
        rule2.value = "yyy";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("rule in \"visibility\" field: unexpected \"value\" field is ignored", warn.getFirst());
        warn.clear();
        rule2.value = null;

        widget.visibility = List.of(rule1, rule2);
        assertTrue(widget.isValid(err, warn));
        rule1.argument = null;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        rule1.argument = "50";
        assertTrue(widget.isValid(err, warn));
        condition.argument = null;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition",
                err.getFirst());
        err.clear();
        condition.argument = "50";
        assertTrue(widget.isValid(err, warn));
        rule1.value = "xxx";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("rule in \"visibility\" field: unexpected \"value\" field is ignored", warn.getFirst());
        warn.clear();
        rule1.value = null;
        rule2.value = "yyy";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("rule in \"visibility\" field: unexpected \"value\" field is ignored", warn.getFirst());
        warn.clear();
    }

    @Test
    public void testIsValidFrame() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Frame";
        assertTrue(widget.isValid(err, warn));
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.widgets = List.of(subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 1/1 of type Frame: Frame widget must not contain other Frames", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetButton);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 1/1 of type Button: Buttons are only allowed in Buttongrid", warn.getFirst());
        warn.clear();
        widget.widgets = List.of();
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetText, subWidgetSwitch);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
    }

    @Test
    public void testIsValidButtongrid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO subWidgetButton2 = new YamlWidgetDTO();
        subWidgetButton2.type = "Button";
        subWidgetButton2.item = "switchItem";
        subWidgetButton2.row = 1;
        subWidgetButton2.column = 2;
        subWidgetButton2.label = "Off";
        subWidgetButton2.command = "OFF";
        subWidgetButton2.stateless = true;

        YamlRuleWithUniqueConditionDTO visibility1 = new YamlRuleWithUniqueConditionDTO();
        visibility1.argument = "ON";

        YamlWidgetDTO subWidgetButton3 = new YamlWidgetDTO();
        subWidgetButton3.type = "Button";
        subWidgetButton3.item = "switchItem";
        subWidgetButton3.row = 1;
        subWidgetButton3.column = 3;
        subWidgetButton3.label = "On";
        subWidgetButton3.command = "OFF";
        subWidgetButton3.visibility = visibility1;

        YamlRuleWithUniqueConditionDTO visibility2 = new YamlRuleWithUniqueConditionDTO();
        visibility2.argument = "OFF";

        YamlWidgetDTO subWidgetButton4 = new YamlWidgetDTO();
        subWidgetButton4.type = "Button";
        subWidgetButton4.item = "switchItem";
        subWidgetButton4.row = 1;
        subWidgetButton4.column = 3;
        subWidgetButton4.label = "Off";
        subWidgetButton4.command = "ON";
        subWidgetButton4.visibility = visibility2;

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Buttongrid";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.widgets = List.of(subWidgetText, subWidgetSwitch, subWidgetButton, subWidgetButton2);
        assertTrue(widget.isValid(err, warn));
        assertEquals(2, warn.size());
        assertEquals("widget 1/4 of type Text: Buttongrid must contain only Buttons", warn.getFirst());
        assertEquals("widget 2/4 of type Switch: Buttongrid must contain only Buttons", warn.get(1));
        warn.clear();
        widget.widgets = List.of();
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetButton, subWidgetButton2, subWidgetButton3, subWidgetButton4);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        subWidgetButton2.column = 1;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 2/4 of type Button: Button widget already exists for position (1,1)", warn.getFirst());
        warn.clear();
        subWidgetButton2.column = 2;
        subWidgetButton3.column = 2;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals(
                "widget 3/4 of type Button: Button widget without and with visibility rule for same position (1,2)",
                warn.getFirst());
        warn.clear();
        subWidgetButton3.column = 3;
        subWidgetButton2.column = 3;
        widget.widgets = List.of(subWidgetButton3, subWidgetButton4, subWidgetButton, subWidgetButton2);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals(
                "widget 4/4 of type Button: Button widget with and without visibility rule for same position (1,3)",
                warn.getFirst());
        warn.clear();
        subWidgetButton2.column = 2;
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"item\" field is ignored", warn.getFirst());
    }

    @Test
    public void testIsValidButton() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Button";
        assertFalse(widget.isValid(err, warn));
        assertEquals(4, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        assertEquals("\"row\" field missing while mandatory", err.get(1));
        assertEquals("\"column\" field missing while mandatory", err.get(2));
        assertEquals("\"command\" field missing while mandatory", err.get(3));
        err.clear();
        widget.item = "item";
        widget.row = 1;
        widget.column = 2;
        widget.command = "ON";
        assertTrue(widget.isValid(err, warn));
        widget.row = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %d for \"row\" field; value must be greater than 0".formatted(widget.row),
                err.getFirst());
        err.clear();
        widget.row = 0;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %d for \"row\" field; value must be greater than 0".formatted(widget.row),
                err.getFirst());
        err.clear();
        widget.row = 1;
        assertTrue(widget.isValid(err, warn));
        widget.column = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %d for \"column\" field; value must be greater than 0".formatted(widget.column),
                err.getFirst());
        err.clear();
        widget.column = 0;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %d for \"column\" field; value must be greater than 0".formatted(widget.column),
                err.getFirst());
        err.clear();
        widget.column = 2;
        assertTrue(widget.isValid(err, warn));
        widget.releaseCommand = "OFF";
        assertTrue(widget.isValid(err, warn));
        widget.stateless = false;
        assertTrue(widget.isValid(err, warn));
        widget.stateless = true;
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Button widget", err.getFirst());
    }

    @Test
    public void testIsValidGroup() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Group";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of();
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetText);
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.widgets = List.of(subWidgetText, subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("Group widget should contain either only frames or none at all", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetButton);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 1/1 of type Button: Buttons are only allowed in Buttongrid", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetText, subWidgetSwitch);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
    }

    @Test
    public void testIsValidText() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Text";
        assertTrue(widget.isValid(err, warn));
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of();
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetText);
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.widgets = List.of(subWidgetText, subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("Text widget should contain either only frames or none at all", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetButton);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 1/1 of type Button: Buttons are only allowed in Buttongrid", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetText, subWidgetSwitch);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
    }

    @Test
    public void testIsValidColorpicker() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Colorpicker";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Colorpicker widget", err.getFirst());
    }

    @Test
    public void testIsValidColortemperaturepicker() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Colortemperaturepicker";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.min = BigDecimal.valueOf(5000);
        widget.max = BigDecimal.valueOf(3000);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("larger value %f for \"min\" field than value %f for \"max\" field"
                .formatted(widget.min.doubleValue(), widget.max.doubleValue()), warn.getFirst());
        warn.clear();
        widget.min = BigDecimal.valueOf(3000);
        widget.max = BigDecimal.valueOf(5000);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Colortemperaturepicker widget", err.getFirst());
    }

    @Test
    public void testIsValidInput() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Input";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.hint = "any";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value \"%s\" for \"hint\" field".formatted(widget.hint), warn.getFirst());
        warn.clear();
        widget.hint = "text";
        assertTrue(widget.isValid(err, warn));
        widget.hint = "number";
        assertTrue(widget.isValid(err, warn));
        widget.hint = "date";
        assertTrue(widget.isValid(err, warn));
        widget.hint = "time";
        assertTrue(widget.isValid(err, warn));
        widget.hint = "datetime";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Input widget", err.getFirst());
    }

    @Test
    public void testIsValidSwitch() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Switch";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.mappings = List.of(mappingOn, mappingOff);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Switch widget", err.getFirst());
    }

    @Test
    public void testIsValidSelection() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Selection";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.mappings = List.of(mappingOn, mappingOff);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Selection widget", err.getFirst());
    }

    @Test
    public void testIsValidSetpoint() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Setpoint";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.min = BigDecimal.valueOf(25);
        widget.max = BigDecimal.valueOf(10);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("larger value %f for \"min\" field than value %f for \"max\" field"
                .formatted(widget.min.doubleValue(), widget.max.doubleValue()), warn.getFirst());
        warn.clear();
        widget.min = BigDecimal.valueOf(10);
        widget.max = BigDecimal.valueOf(25);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.step = BigDecimal.valueOf(-1);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %f for \"step\" field; value must be greater than 0"
                .formatted(widget.step.doubleValue()), warn.getFirst());
        warn.clear();
        widget.step = BigDecimal.valueOf(0);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %f for \"step\" field; value must be greater than 0"
                .formatted(widget.step.doubleValue()), warn.getFirst());
        warn.clear();
        widget.step = BigDecimal.valueOf(0.5);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Setpoint widget", err.getFirst());
    }

    @Test
    public void testIsValidSlider() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Slider";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.min = BigDecimal.valueOf(75);
        widget.max = BigDecimal.valueOf(25);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("larger value %f for \"min\" field than value %f for \"max\" field"
                .formatted(widget.min.doubleValue(), widget.max.doubleValue()), warn.getFirst());
        warn.clear();
        widget.min = BigDecimal.valueOf(25);
        widget.max = BigDecimal.valueOf(75);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.step = BigDecimal.valueOf(-5);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %f for \"step\" field; value must be greater than 0"
                .formatted(widget.step.doubleValue()), warn.getFirst());
        warn.clear();
        widget.step = BigDecimal.valueOf(0);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %f for \"step\" field; value must be greater than 0"
                .formatted(widget.step.doubleValue()), warn.getFirst());
        warn.clear();
        widget.step = BigDecimal.valueOf(5);
        assertTrue(widget.isValid(err, warn));
        widget.switchSupport = true;
        assertTrue(widget.isValid(err, warn));
        widget.releaseOnly = true;
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Slider widget", err.getFirst());
    }

    @Test
    public void testIsValidImage() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Image";
        assertTrue(widget.isValid(err, warn));
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.url = "https://www.test.org/image.png";
        assertTrue(widget.isValid(err, warn));
        widget.refresh = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %d for \"refresh\" field; value must be greater than 0".formatted(widget.refresh),
                err.getFirst());
        err.clear();
        assertEquals(0, warn.size());
        widget.refresh = 0;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %d for \"refresh\" field; value must be greater than 0".formatted(widget.refresh),
                warn.getFirst());
        warn.clear();
        widget.refresh = 10000;
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of();
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetText);
        assertTrue(widget.isValid(err, warn));
        widget.widgets = List.of(subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.widgets = List.of(subWidgetText, subWidgetFrame);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("Image widget should contain either only frames or none at all", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetButton);
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("widget 1/1 of type Button: Buttons are only allowed in Buttongrid", warn.getFirst());
        warn.clear();
        widget.widgets = List.of(subWidgetText, subWidgetSwitch);
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
    }

    @Test
    public void testIsValidChart() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Chart";
        assertFalse(widget.isValid(err, warn));
        assertEquals(2, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        assertEquals("\"period\" field missing while mandatory", err.get(1));
        err.clear();
        widget.item = "item";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"period\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = null;
        widget.period = "D";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        widget.period = "D";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.refresh = 0;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %d for \"refresh\" field; value must be greater than 0".formatted(widget.refresh),
                warn.getFirst());
        warn.clear();
        widget.refresh = 15000;
        assertTrue(widget.isValid(err, warn));
        widget.service = "rrd4j";
        assertTrue(widget.isValid(err, warn));
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        widget.legend = true;
        assertTrue(widget.isValid(err, warn));
        widget.forceAsItem = false;
        assertTrue(widget.isValid(err, warn));
        widget.forceAsItem = true;
        assertTrue(widget.isValid(err, warn));
        widget.yAxisDecimalPattern = "#.##E0";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.interpolation = "any";
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value \"%s\" for \"interpolation\" field".formatted(widget.interpolation),
                warn.getFirst());
        warn.clear();
        widget.interpolation = "linear";
        assertTrue(widget.isValid(err, warn));
        widget.interpolation = "step";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.stateless = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"stateless\" field is ignored", warn.getFirst());
        warn.clear();
        widget.stateless = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Chart widget", err.getFirst());
    }

    @Test
    public void testIsValidVideo() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Video";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"url\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.url = "https://www.test.org/video.mp4";
        assertTrue(widget.isValid(err, warn));
        widget.encoding = "HLS";
        assertTrue(widget.isValid(err, warn));
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Video widget", err.getFirst());
    }

    @Test
    public void testIsValidMapview() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Mapview";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.height = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                err.getFirst());
        err.clear();
        assertEquals(0, warn.size());
        widget.height = 0;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                warn.getFirst());
        warn.clear();
        widget.height = 6;
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Mapview widget", err.getFirst());
    }

    @Test
    public void testIsValidWebview() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Webview";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"url\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.url = "https://www.test.org/test.html";
        assertTrue(widget.isValid(err, warn));
        widget.height = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                err.getFirst());
        err.clear();
        assertEquals(0, warn.size());
        widget.height = 0;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                warn.getFirst());
        warn.clear();
        widget.height = 6;
        assertTrue(widget.isValid(err, warn));
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Webview widget", err.getFirst());
    }

    @Test
    public void testIsValidDefault() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widget = new YamlWidgetDTO();
        widget.type = "Default";
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("\"item\" field missing while mandatory", err.getFirst());
        err.clear();
        widget.item = "item";
        assertTrue(widget.isValid(err, warn));
        widget.height = -1;
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                err.getFirst());
        err.clear();
        assertEquals(0, warn.size());
        widget.height = 0;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("invalid value %s for \"height\" field; value must be greater than 0".formatted(widget.height),
                warn.getFirst());
        warn.clear();
        widget.height = 6;
        assertTrue(widget.isValid(err, warn));
        assertEquals(0, warn.size());
        widget.legend = false;
        assertTrue(widget.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("unexpected \"legend\" field is ignored", warn.getFirst());
        warn.clear();
        widget.legend = null;
        widget.widgets = List.of(subWidgetText);
        assertFalse(widget.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("unexpected sub-widgets in Default widget", err.getFirst());
    }

    @Test
    public void testEquals() throws IOException {
        YamlWidgetDTO widget1 = new YamlWidgetDTO();
        YamlWidgetDTO widget2 = new YamlWidgetDTO();

        widget1.type = "Text";
        widget2.type = "Frame";
        assertFalse(widget1.equals(widget2));
        widget2.type = "Text";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        widget1.item = "item";
        assertFalse(widget1.equals(widget2));
        widget2.item = "item2";
        assertFalse(widget1.equals(widget2));
        widget2.item = "item";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        widget1.label = "Label";
        assertFalse(widget1.equals(widget2));
        widget2.label = "Other label";
        assertFalse(widget1.equals(widget2));
        widget2.label = "Label";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        YamlWidgetLabelDTO label = new YamlWidgetLabelDTO();
        label.label = "Label";
        label.format = "%s";
        label.labelColor = "green";
        label.valueColor = "red";
        widget1.label = label;
        assertFalse(widget1.equals(widget2));
        YamlWidgetLabelDTO label2 = new YamlWidgetLabelDTO();
        label2.label = "Label";
        label2.format = "%s";
        label2.labelColor = "green";
        label2.valueColor = "red";
        widget2.label = label2;
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        label2.label = "Other label";
        assertFalse(widget1.equals(widget2));
        label2.label = "Label";
        label2.format = null;
        assertFalse(widget1.equals(widget2));
        label2.format = "%s";
        label2.labelColor = "blue";
        assertFalse(widget1.equals(widget2));
        label2.labelColor = "green";
        label2.valueColor = "orange";
        assertFalse(widget1.equals(widget2));
        label2.valueColor = "red";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        widget1.icon = "material:home";
        assertFalse(widget1.equals(widget2));
        widget2.icon = "material:favorite";
        assertFalse(widget1.equals(widget2));
        widget2.icon = "material:home";
        assertTrue(widget1.equals(widget2));
        YamlWidgetIconDTO icon1 = new YamlWidgetIconDTO();
        icon1.name = "material:home";
        icon1.staticIcon = true;
        icon1.color = "green";
        widget1.icon = icon1;
        assertFalse(widget1.equals(widget2));
        YamlWidgetIconDTO icon2 = new YamlWidgetIconDTO();
        icon2.name = "material:home";
        icon2.staticIcon = true;
        icon2.color = "green";
        widget2.icon = icon2;
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        icon2.name = "material:favorite";
        assertFalse(widget1.equals(widget2));
        icon2.name = "material:home";
        icon2.staticIcon = false;
        assertFalse(widget1.equals(widget2));
        icon2.staticIcon = true;
        icon2.color = "blue";
        assertFalse(widget1.equals(widget2));
        icon2.color = "green";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        YamlWidgetDTO subWidgetText2 = new YamlWidgetDTO();
        subWidgetText2.type = "Text";
        YamlWidgetDTO subWidgetSwitch2 = new YamlWidgetDTO();
        subWidgetSwitch2.type = "Switch";
        subWidgetSwitch2.item = "switchItem";

        widget1.widgets = List.of();
        widget2.widgets = List.of();
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        widget1.widgets = List.of(subWidgetText, subWidgetSwitch);
        widget2.widgets = null;
        assertFalse(widget1.equals(widget2));
        widget2.widgets = List.of();
        assertFalse(widget1.equals(widget2));
        widget2.widgets = List.of(subWidgetText2);
        assertFalse(widget1.equals(widget2));
        widget2.widgets = List.of(subWidgetSwitch2);
        assertFalse(widget1.equals(widget2));
        widget2.widgets = List.of(subWidgetSwitch2, subWidgetText2);
        assertFalse(widget1.equals(widget2));
        widget2.widgets = List.of(subWidgetText2, subWidgetSwitch2);
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        YamlRuleWithUniqueConditionDTO rule1 = new YamlRuleWithUniqueConditionDTO();
        rule1.operator = "<";
        rule1.argument = "50";
        YamlRuleWithAndConditionsDTO rule2 = new YamlRuleWithAndConditionsDTO();
        YamlConditionDTO condition1 = new YamlConditionDTO();
        condition1.operator = ">";
        condition1.argument = "50";
        rule2.and = List.of(condition1);

        YamlRuleWithUniqueConditionDTO rule3 = new YamlRuleWithUniqueConditionDTO();
        rule3.operator = "<";
        rule3.argument = "50";
        YamlRuleWithAndConditionsDTO rule4 = new YamlRuleWithAndConditionsDTO();
        YamlConditionDTO condition2 = new YamlConditionDTO();
        condition2.operator = ">";
        condition2.argument = "50";
        rule4.and = List.of(condition2);

        widget1.visibility = rule1;
        widget2.visibility = null;
        assertFalse(widget1.equals(widget2));
        widget2.visibility = rule4;
        assertFalse(widget1.equals(widget2));
        widget2.visibility = rule3;
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        widget1.visibility = rule2;
        widget2.visibility = null;
        assertFalse(widget1.equals(widget2));
        widget2.visibility = rule3;
        assertFalse(widget1.equals(widget2));
        widget2.visibility = rule4;
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        widget1.visibility = List.of(rule1, rule2);
        assertFalse(widget1.equals(widget2));
        widget2.visibility = null;
        assertFalse(widget1.equals(widget2));
        widget2.visibility = List.of(rule3);
        assertFalse(widget1.equals(widget2));
        widget2.visibility = List.of(rule4, rule3);
        assertFalse(widget1.equals(widget2));
        widget2.visibility = List.of(rule3, rule4);
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());

        widget1.widgets = null;
        widget1.type = "Input";
        widget1.item = "item";
        widget2.widgets = null;
        widget2.type = "Input";
        widget2.item = "item";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
        widget1.hint = "number";
        assertFalse(widget1.equals(widget2));
        widget2.hint = "date";
        assertFalse(widget1.equals(widget2));
        widget2.hint = "number";
        assertTrue(widget1.equals(widget2));
        assertEquals(widget1.hashCode(), widget2.hashCode());
    }
}
