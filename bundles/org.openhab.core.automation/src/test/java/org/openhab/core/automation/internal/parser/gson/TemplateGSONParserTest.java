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
package org.openhab.core.automation.internal.parser.gson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;

/**
 * Tests parsing of specific JSON rule template files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class TemplateGSONParserTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/rule-templates");

    @Test
    public void basicTemplateTest() throws Exception {
        TemplateGSONParser parser = new TemplateGSONParser();
        Set<Template> templates;
        try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(SOURCE_PATH.resolve("BasicRuleTemplate.json"), StandardOpenOption.READ))) {
            templates = parser.parse(isr);
        }
        assertThat(templates, hasSize(1));

        RuleTemplate template = (RuleTemplate) templates.iterator().next();
        assertThat(template.getUID(), is("basic:json-rule-template"));
        assertThat(template.getLabel(), is("Basic JSON Rule Template"));
        assertThat(template.getDescription(), is("A basic JSON rule template."));
        assertThat(template.getVisibility(), is(Visibility.VISIBLE));

        List<ConfigDescriptionParameter> configDescriptions = template.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(1));
        ConfigDescriptionParameter parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("startLevel"));
        assertThat(parameter.getLabel(), is("Start Level"));
        assertThat(parameter.getDescription(), is("The start level which will trigger the rule."));
        assertThat(parameter.getDefault(), is("80"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getMultipleLimit(), is(nullValue()));
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertTrue(parameter.getLimitToOptions());
        assertThat(parameter.getOptions(), is(empty()));
        assertThat(parameter.getFilterCriteria(), is(empty()));

        List<Trigger> triggers = template.getTriggers();
        assertThat(triggers, hasSize(2));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("3"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", "{{startLevel}}"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("timeofday"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("timer.TimeOfDayTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("time", "14:05"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        List<Condition> conditions = template.getConditions();
        assertThat(conditions, hasSize(2));
        Condition condition = conditions.get(0);
        assertThat(condition.getId(), is("4"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(0.0)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));
        condition = conditions.get(1);
        assertThat(condition.getId(), is("5"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(2.0)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));

        List<Action> actions = template.getActions();
        assertThat(actions, hasSize(2));
        Action action = actions.get(0);
        assertThat(action.getId(), is("1"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("core.ItemCommandAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("itemName", "SleepSetTemperature"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("command", "21.0"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
        action = actions.get(1);
        assertThat(action.getId(), is("2"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("media.SayAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("sink", "webaudio"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("text", "The sleep temperature has been set"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
    }

    @Test
    public void fullTemplateTest() throws Exception {
        TemplateGSONParser parser = new TemplateGSONParser();
        Set<Template> templates;
        try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(SOURCE_PATH.resolve("FullRuleTemplate.json"), StandardOpenOption.READ))) {
            templates = parser.parse(isr);
        }
        assertThat(templates, hasSize(1));

        RuleTemplate template = (RuleTemplate) templates.iterator().next();
        assertThat(template.getUID(), is("test:json-full-rule-template"));
        assertThat(template.getLabel(), is("JSON Full Rule Template"));
        assertThat(template.getDescription(), is("The description of the JSON template-based full rule"));
        assertThat(template.getVisibility(), is(Visibility.VISIBLE));

        List<ConfigDescriptionParameter> configDescriptions = template.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(5));
        ConfigDescriptionParameter parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("textParam"));
        assertThat(parameter.getLabel(), is("Text parameter"));
        assertThat(parameter.getDescription(), is("This is a text parameter."));
        assertThat(parameter.getDefault(), is("A text"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(nullValue()));
        assertFalse(parameter.isReadOnly());
        assertTrue(parameter.isMultiple());
        assertThat(parameter.getMultipleLimit(), is(3));
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertTrue(parameter.isAdvanced());
        assertTrue(parameter.isVerifyable());
        assertFalse(parameter.getLimitToOptions());
        List<ParameterOption> options = parameter.getOptions();
        assertThat(options, hasSize(2));
        assertThat(options.get(0).getLabel(), is("First Option"));
        assertThat(options.get(0).getValue(), is("Welcome"));
        assertThat(options.get(1).getLabel(), is("Second Option"));
        assertThat(options.get(1).getValue(), is("Willkommen"));
        List<FilterCriteria> filterCriterias = parameter.getFilterCriteria();
        assertThat(filterCriterias, hasSize(2));
        assertThat(filterCriterias.get(0).getName(), is("filter1"));
        assertThat(filterCriterias.get(0).getValue(), is(".*"));
        assertThat(filterCriterias.get(1).getName(), is("filter2"));
        assertThat(filterCriterias.get(1).getValue(), is("a.*"));
        parameter = configDescriptions.get(1);
        assertThat(parameter.getName(), is("integerParam"));
        assertThat(parameter.getLabel(), is("Integer parameter"));
        assertThat(parameter.getDescription(), is("This is an integer parameter."));
        assertThat(parameter.getDefault(), is("70"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertThat(parameter.getMinimum(), is(BigDecimal.valueOf(60L)));
        assertThat(parameter.getMaximum(), is(BigDecimal.valueOf(100L)));
        assertThat(parameter.getStepSize(), is(BigDecimal.valueOf(1L)));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertThat(parameter.getUnit(), is("%"));
        assertThat(parameter.getUnitLabel(), is("Start Level"));
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("itemParam"));
        assertThat(parameter.getLabel(), is("Item parameter"));
        assertThat(parameter.getDescription(), is("This is an item parameter"));
        assertThat(parameter.getDefault(), is("CurrentPower"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        parameter = configDescriptions.get(3);
        assertThat(parameter.getName(), is("decimalParam"));
        assertThat(parameter.getLabel(), is("Decimal parameter"));
        assertThat(parameter.getDescription(), is("This is a decimal parameter."));
        assertThat(parameter.getDefault(), is("3.25"));
        assertThat(parameter.getType(), is(Type.DECIMAL));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertThat(parameter.getMinimum(), is(BigDecimal.valueOf(0.5)));
        assertThat(parameter.getMaximum(), is(BigDecimal.valueOf(11L)));
        assertThat(parameter.getStepSize(), is(BigDecimal.valueOf(0.25)));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertTrue(parameter.isAdvanced());
        assertTrue(parameter.isVerifyable());
        parameter = configDescriptions.get(4);
        assertThat(parameter.getName(), is("booleanParam"));
        assertThat(parameter.getLabel(), is("Boolean parameter"));
        assertThat(parameter.getDescription(), is("This is a boolean parameter."));
        assertThat(parameter.getDefault(), is("true"));
        assertThat(parameter.getType(), is(Type.BOOLEAN));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());

        assertThat(template.getTags(), hasSize(2));
        assertThat(template.getTags(), hasItem("First Tag"));
        assertThat(template.getTags(), hasItem("Second Tag"));

        List<Trigger> triggers = template.getTriggers();
        assertThat(triggers, hasSize(2));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("first"));
        assertThat(trigger.getLabel(), is("First Trigger"));
        assertThat(trigger.getDescription(), is("The first trigger."));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", "{{integerParam}}"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is("Second Trigger"));
        assertThat(trigger.getDescription(), is("The second trigger."));
        assertThat(trigger.getTypeUID(), is("timer.GenericCronTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("cronExpression", "0 3/30 8 * * ? *"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        List<Condition> conditions = template.getConditions();
        assertThat(conditions, hasSize(4));
        Condition condition = conditions.get(0);
        assertThat(condition.getId(), is("holiday"));
        assertThat(condition.getLabel(), is("Is Holiday"));
        assertThat(condition.getDescription(), is("It must be a holiday."));
        assertThat(condition.getTypeUID(), is("ephemeris.HolidayCondition"));
        condition = conditions.get(1);
        assertThat(condition.getId(), is("weekday"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        condition = conditions.get(2);
        assertThat(condition.getId(), is("2"));
        assertThat(condition.getLabel(), is("Work Hours"));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("core.TimeOfDayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("startTime", "08:00"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("endTime", "16:00"));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(2)));
        condition = conditions.get(3);
        assertThat(condition.getId(), is("3"));
        assertThat(condition.getLabel(), is("Heating Power Sufficient"));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("core.ItemStateCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("itemName", "{{itemParam}}"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("operator", ">"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("state", "50"));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(3)));

        List<Action> actions = template.getActions();
        assertThat(actions, hasSize(2));
        Action action = actions.get(0);
        assertThat(action.getId(), is("greet"));
        assertThat(action.getLabel(), is("Greet"));
        assertThat(action.getDescription(), is("Greets the person."));
        assertThat(action.getTypeUID(), is("media.SayAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("volume", BigDecimal.valueOf(100.0)));
        assertThat(action.getConfiguration().getProperties(), hasEntry("sink", "enhancedjavasound"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("text", "{{textParam}}"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(3)));
        assertThat(action.getInputs(), is(anEmptyMap()));

        action = actions.get(1);
        assertThat(action.getId(), is("4"));
        assertThat(action.getLabel(), is("Print"));
        assertThat(action.getDescription(), is("Gives a warm welcome."));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/x-ruby"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("script", "puts \"Hello and welcome\"\n"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
    }

    @Test
    public void multipleTemplatesTest() throws Exception {
        TemplateGSONParser parser = new TemplateGSONParser();
        Set<Template> templates;
        try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(SOURCE_PATH.resolve("MultipleRuleTemplates.json"), StandardOpenOption.READ))) {
            templates = parser.parse(isr);
        }
        assertThat(templates, hasSize(2));

        Iterator<Template> iterator = templates.iterator();
        RuleTemplate template = (RuleTemplate) iterator.next();
        assertThat(template.getUID(), is("kaikreuzer:energymeter-json"));
        assertThat(template.getLabel(), is("Energy Meter JSON"));
        assertThat(template.getDescription(), is("Visualizes the current energy consumption."));
        assertThat(template.getVisibility(), is(Visibility.VISIBLE));

        List<ConfigDescriptionParameter> configDescriptions = template.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(3));
        ConfigDescriptionParameter parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("consumption"));
        assertThat(parameter.getLabel(), is("Consumption Item"));
        assertThat(parameter.getDescription(), is("Data source for current consumption"));
        assertThat(parameter.getDefault(), is(emptyOrNullString()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(nullValue()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getMultipleLimit(), is(nullValue()));
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertTrue(parameter.getLimitToOptions());
        List<ParameterOption> options = parameter.getOptions();
        assertThat(options, is(empty()));
        List<FilterCriteria> filterCriterias = parameter.getFilterCriteria();
        assertThat(filterCriterias, hasSize(1));
        assertThat(filterCriterias.get(0).getName(), is("type"));
        assertThat(filterCriterias.get(0).getValue(), is("Number"));
        parameter = configDescriptions.get(1);
        assertThat(parameter.getName(), is("light"));
        assertThat(parameter.getLabel(), is("Color Item"));
        assertThat(parameter.getDescription(), is("Color light to use for visualisation"));
        assertThat(parameter.getDefault(), is(nullValue()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertThat(parameter.getMinimum(), is(nullValue()));
        assertThat(parameter.getMaximum(), is(nullValue()));
        assertThat(parameter.getStepSize(), is(nullValue()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertThat(parameter.getUnit(), is(nullValue()));
        assertThat(parameter.getUnitLabel(), is(nullValue()));
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("max"));
        assertThat(parameter.getLabel(), is("Max. consumption"));
        assertThat(parameter.getDescription(), is("Maximum value for red light"));
        assertThat(parameter.getDefault(), is("1500"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(nullValue()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());

        assertThat(template.getTags(), is(empty()));

        List<Trigger> triggers = template.getTriggers();
        assertThat(triggers, hasSize(1));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("trigger"));
        assertThat(trigger.getLabel(), is("Current consumption changes"));
        assertThat(trigger.getDescription(), is("Triggers whenever the current consumption changes its value"));
        assertThat(trigger.getTypeUID(), is("core.ItemStateChangeTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("itemName", "{{consumption}}"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        List<Condition> conditions = template.getConditions();
        assertThat(conditions, is(empty()));

        List<Action> actions = template.getActions();
        assertThat(actions, hasSize(1));
        Action action = actions.get(0);
        assertThat(action.getId(), is("setcolor"));
        assertThat(action.getLabel(), is("Change the light color"));
        assertThat(action.getDescription(),
                is("Sets the color to a value in the range from green (low consumption) to red (high consumption)"));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/vnd.openhab.dsl.rule"));
        assertThat(action.getConfiguration().getProperties(),
                hasEntry("script",
                        "var power = (newState as Number).intValue\n" + "var percent = power / (30.0 / 100.0)\n"
                                + "if(percent < 0) percent = 0\n" + "var hue = 120 - percent * 1.2\n"
                                + "sendCommand({{light}}, hue +',100,100')"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));

        template = (RuleTemplate) iterator.next();
        assertThat(template.getUID(), is("ysc:simulate_sunrise_json"));
        assertThat(template.getLabel(), is("Simulate Sunrise JSON"));
        assertThat(template.getDescription(), is(
                "This rule will gradually increase a Dimmer or Color item to the target brightness and time over a configurable period."));
        assertThat(template.getVisibility(), is(Visibility.EXPERT));

        configDescriptions = template.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(6));
        parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("itemTargetTime"));
        assertThat(parameter.getLabel(), is("Target Time (DateTime Item)"));
        assertThat(parameter.getDescription(), is(
                "DateTime Item that holds the target time (for instance, linked to the Sunrise End Time channel of an Astro Sun Thing). Set either this or a fixed target time below."));
        assertThat(parameter.getDefault(), is(emptyOrNullString()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(nullValue()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getMultipleLimit(), is(nullValue()));
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertTrue(parameter.getLimitToOptions());
        assertThat(parameter.getOptions(), is(empty()));
        assertThat(parameter.getFilterCriteria(), is(empty()));
        parameter = configDescriptions.get(1);
        assertThat(parameter.getName(), is("fixedTargetTime"));
        assertThat(parameter.getLabel(), is("Fixed Target Time"));
        assertThat(parameter.getDescription(),
                is("Set a fixed target time - ignored if Target Time (DateTime Item) is set above."));
        assertThat(parameter.getDefault(), is(nullValue()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("time"));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("targetBrightness"));
        assertThat(parameter.getLabel(), is("Target Brightness"));
        assertThat(parameter.getDescription(), is("Brightness to reach at the target time."));
        assertThat(parameter.getDefault(), is("100"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        parameter = configDescriptions.get(3);
        assertThat(parameter.getName(), is("sunriseDuration"));
        assertThat(parameter.getLabel(), is("Sunrise Duration"));
        assertThat(parameter.getDescription(), is(
                "Duration of the sunrise in minutes (The brightness will be set to 0 at the start of the period and gradually every minute to the target brightness until the end)."));
        assertThat(parameter.getDefault(), is("60"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertThat(parameter.getMinimum(), is(nullValue()));
        assertThat(parameter.getMaximum(), is(nullValue()));
        assertThat(parameter.getStepSize(), is(nullValue()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        parameter = configDescriptions.get(4);
        assertThat(parameter.getName(), is("brightnessItem"));
        assertThat(parameter.getLabel(), is("Brightness Item"));
        assertThat(parameter.getDescription(), is("Dimmer or Color Item to use to control the brightness."));
        assertThat(parameter.getDefault(), is(nullValue()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        parameter = configDescriptions.get(5);
        assertThat(parameter.getName(), is("colorPrefix"));
        assertThat(parameter.getLabel(), is("Color Prefix"));
        assertThat(parameter.getDescription(), is(
                "In case of a Color Item set above, prefix the command with the comma-separated Hue,Saturation components to send to the item (a separator comma and the brightness will be appended)."));
        assertThat(parameter.getDefault(), is(emptyOrNullString()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is(emptyOrNullString()));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());

        assertThat(template.getTags(), hasSize(1));
        assertThat(template.getTags(), hasItem("Astro"));

        triggers = template.getTriggers();
        assertThat(triggers, hasSize(1));
        trigger = triggers.get(0);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("timer.GenericCronTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("cronExpression", "0 * * * * ? *"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        conditions = template.getConditions();
        assertThat(conditions, is(empty()));

        actions = template.getActions();
        assertThat(actions, hasSize(1));
        action = actions.get(0);
        assertThat(action.getId(), is("2"));
        assertThat(action.getLabel(), is("Calculate & set the target brightness"));
        assertThat(action.getDescription(),
                is("Sets the brightness appropriately or do nothing if outside the sunrise time"));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/javascript"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("script",
                "// set by the rule template\nvar itemTargetTime = \"{{itemTargetTime}}\";\nvar fixedTargetTime = \"{{fixedTargetTime}}\";\nvar sunriseDuration = {{sunriseDuration}};\nvar targetBrightness = {{targetBrightness}};\nvar brightnessItem = \"{{brightnessItem}}\";\nvar colorPrefix = \"{{colorPrefix}}\";\n\nvar openhab = (typeof(require) === \"function\") ? require(\"@runtime\") : {\n  ir: ir, events: events\n};\n\nvar logger = Java.type(\"org.slf4j.LoggerFactory\").getLogger(\"org.openhab.rule.\" + this.ctx.ruleUID);\n\n// returns the number of minutes past midnight for a Date object\nfunction getMinutesPastMidnight(date) {\n  return date.getHours() * 60 + date.getMinutes();\n}\n\n\n// returns the brightness to set at the current time (Date), given the target time (Date),\n// target brightness (int) & desired sunrise duration (int)\n\nfunction getBrightnessAtTime(currentTime, targetTime, targetBrightness, sunriseDuration) {\n  var currentMinutes = getMinutesPastMidnight(now);\n  var targetMinutes = getMinutesPastMidnight(targetTime);\n  if (currentMinutes > targetMinutes) return null;\n  if (currentMinutes < targetMinutes - sunriseDuration) return null;\n  var minutesToGo = targetMinutes - currentMinutes;\n  return parseInt(parseInt(targetBrightness) * ((sunriseDuration - minutesToGo) / sunriseDuration));\n}\n\nvar now = new Date();\nvar targetTime = null;\n\nif (itemTargetTime) {\n  targetTime = new Date(openhab.ir.getItem(itemTargetTime).getState());\n} else if (fixedTargetTime.match(/\\d\\d:\\d\\d/)) {\n  targetTime = new Date();\n  targetTime.setHours(parseInt(fixedTargetTime.split(\":\")[0]));\n  targetTime.setMinutes(parseInt(fixedTargetTime.split(\":\")[1]));\n  targetTime.setSeconds(0);\n} else {\n  logger.warn(\"Invalid target time\");\n}\n\nif (targetTime != null) {\n  var brightness = getBrightnessAtTime(now, targetTime, targetBrightness, sunriseDuration);\n  if (brightness != null) {\n    openhab.events.sendCommand(brightnessItem, (colorPrefix ? colorPrefix + \",\" : \"\") + brightness.toString());\n  }\n}\n"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
    }
}
