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
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;

/**
 * Tests parsing of specific JSON rule files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RuleGSONParserTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/rules");

    @Test
    public void basicRuleTest() throws Exception {
        RuleGSONParser parser = new RuleGSONParser();
        Set<Rule> rules;
        try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(SOURCE_PATH.resolve("BasicRules.json"), StandardOpenOption.READ))) {
            rules = parser.parse(isr);
        }
        assertThat(rules, hasSize(2));

        Iterator<Rule> iterator = rules.iterator();
        Rule rule = iterator.next();
        assertThat(rule.getUID(), is("test:basic-json-rule-stub"));
        assertThat(rule.getName(), is("Basic JSON Rule Stub"));
        assertThat(rule.getDescription(), is(emptyOrNullString()));
        assertThat(rule.getTemplateUID(), is("basic:json-rule-template"));
        assertThat(rule.getVisibility(), is(Visibility.HIDDEN));

        assertThat(rule.getTags(), hasSize(1));
        assertThat(rule.getTags(), hasItem("Basic"));

        Configuration config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(1)));
        assertThat(config.getProperties(), hasEntry("startLevel", BigDecimal.valueOf(60.0)));

        List<ConfigDescriptionParameter> configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, is(empty()));

        List<Trigger> triggers = rule.getTriggers();
        assertThat(triggers, is(empty()));

        List<Condition> conditions = rule.getConditions();
        assertThat(conditions, is(empty()));

        List<Action> actions = rule.getActions();
        assertThat(actions, is(empty()));

        rule = iterator.next();
        assertThat(rule.getUID(), is("test:basic-json-rule"));
        assertThat(rule.getName(), is("Basic JSON Rule"));
        assertThat(rule.getDescription(), is(emptyOrNullString()));
        assertThat(rule.getTemplateUID(), is(emptyOrNullString()));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));

        assertThat(rule.getTags(), is(empty()));

        config = rule.getConfiguration();
        assertThat(config.getProperties(), is(anEmptyMap()));

        configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, is(empty()));

        triggers = rule.getTriggers();
        assertThat(triggers, hasSize(2));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("2"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", BigDecimal.valueOf(100.0)));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("22"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("timer.TimeOfDayTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("time", "14:05"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        conditions = rule.getConditions();
        assertThat(conditions, hasSize(2));
        Condition condition = conditions.get(0);
        assertThat(condition.getId(), is("3"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(0.0)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));
        condition = conditions.get(1);
        assertThat(condition.getId(), is("4"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(2.0)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));

        actions = rule.getActions();
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
        assertThat(action.getId(), is("11"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("media.SayAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("sink", "webaudio"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("text", "The sleep temperature has been set"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
    }

    @Test
    public void fullRuleTest() throws Exception {
        RuleGSONParser parser = new RuleGSONParser();
        Set<Rule> rules;
        try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(SOURCE_PATH.resolve("FullRule.json"), StandardOpenOption.READ))) {
            rules = parser.parse(isr);
        }
        assertThat(rules, hasSize(1));

        Rule rule = rules.iterator().next();
        assertThat(rule.getUID(), is("test:full-json-rule"));
        assertThat(rule.getName(), is("Full JSON Rule"));
        assertThat(rule.getDescription(), is("The description of the full JSON rule"));
        assertThat(rule.getTemplateUID(), is("template:non-existing"));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));

        Configuration config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(4)));
        assertThat(config.getProperties(), hasEntry("decimalParam", BigDecimal.valueOf(6.75)));
        assertThat(config.getProperties(), hasEntry("booleanParam", Boolean.TRUE));
        assertThat(config.getProperties(), hasEntry("integerParam", BigDecimal.valueOf(5.0)));
        assertThat(config.getProperties(), hasEntry("textParam", " text"));

        List<ConfigDescriptionParameter> configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(5));
        ConfigDescriptionParameter parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("textParam"));
        assertThat(parameter.getLabel(), is("Text parameter"));
        assertThat(parameter.getDescription(), is("This is a text parameter."));
        assertThat(parameter.getDefault(), is("A text"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is("\\s\\w+"));
        assertTrue(parameter.isReadOnly());
        assertTrue(parameter.isMultiple());
        assertThat(parameter.getMultipleLimit(), is(3));
        assertThat(parameter.getGroupName(), is("Group Name"));
        assertTrue(parameter.isAdvanced());
        assertTrue(parameter.isVerifyable());
        assertFalse(parameter.getLimitToOptions());
        List<ParameterOption> options = parameter.getOptions();
        assertThat(options, hasSize(2));
        assertThat(options.get(0).getLabel(), is("First Option"));
        assertThat(options.get(0).getValue(), is("1st"));
        assertThat(options.get(1).getLabel(), is("Second Option"));
        assertThat(options.get(1).getValue(), is("2nd"));
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
        assertThat(parameter.getDefault(), is("4"));
        assertThat(parameter.getType(), is(Type.INTEGER));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertThat(parameter.getMinimum(), is(BigDecimal.valueOf(2L)));
        assertThat(parameter.getMaximum(), is(BigDecimal.valueOf(8L)));
        assertThat(parameter.getStepSize(), is(BigDecimal.valueOf(1L)));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is("Group Name"));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());
        assertThat(parameter.getUnit(), is("rpm"));
        assertThat(parameter.getUnitLabel(), is("Rounds per Minute"));
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("itemParam"));
        assertThat(parameter.getLabel(), is("Item parameter"));
        assertThat(parameter.getDescription(), is("This is an item parameter"));
        assertThat(parameter.getDefault(), is(emptyOrNullString()));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertFalse(parameter.isRequired());
        assertThat(parameter.getPattern(), is(emptyOrNullString()));
        assertFalse(parameter.isReadOnly());
        assertFalse(parameter.isMultiple());
        assertThat(parameter.getGroupName(), is("Group Name"));
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
        assertThat(parameter.getGroupName(), is("Group Name"));
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
        assertThat(parameter.getGroupName(), is("Group Name"));
        assertFalse(parameter.isAdvanced());
        assertFalse(parameter.isVerifyable());

        assertThat(rule.getTags(), hasSize(2));
        assertThat(rule.getTags(), hasItem("First Tag"));
        assertThat(rule.getTags(), hasItem("Second Tag"));

        List<Trigger> triggers = rule.getTriggers();
        assertThat(triggers, hasSize(2));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("first"));
        assertThat(trigger.getLabel(), is("First Trigger"));
        assertThat(trigger.getDescription(), is("The first trigger."));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", BigDecimal.valueOf(80.0)));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is("Second Trigger"));
        assertThat(trigger.getDescription(), is("The second trigger."));
        assertThat(trigger.getTypeUID(), is("timer.GenericCronTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("cronExpression", "0 3/30 8 * * ? *"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        List<Condition> conditions = rule.getConditions();
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
        assertThat(condition.getConfiguration().getProperties(), hasEntry("itemName", "CurrentPower"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("operator", ">"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("state", "50"));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(3)));

        List<Action> actions = rule.getActions();
        assertThat(actions, hasSize(2));
        Action action = actions.get(0);
        assertThat(action.getId(), is("greet"));
        assertThat(action.getLabel(), is("Greet"));
        assertThat(action.getDescription(), is("Greets the person."));
        assertThat(action.getTypeUID(), is("media.SayAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("volume", BigDecimal.valueOf(100.0)));
        assertThat(action.getConfiguration().getProperties(), hasEntry("sink", "enhancedjavasound"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("text", "Welcome"));
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
}
