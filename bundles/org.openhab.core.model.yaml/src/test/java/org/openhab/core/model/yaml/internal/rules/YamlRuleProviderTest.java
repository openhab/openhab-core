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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Rule.TemplateState;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.WatchService;

/**
 * Tests some general behavior and parsing of specific YAML rule files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlRuleProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/rules");
    private static final String RULES_NAME = "rules.yaml";
    private static final Path RULES_PATH = Path.of(RULES_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path rulesPath;

    @BeforeEach
    public void setup() {
        rulesPath = watchPath.resolve(RULES_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);
    }

    @Test
    public void yamlModelListenerTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("BasicRule.yaml"), rulesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleProvider ruleProvider = new YamlRuleProvider();
        TestRuleChangeListener ruleListener = new TestRuleChangeListener();
        ruleProvider.addProviderChangeListener(ruleListener);
        modelRepository.addYamlModelListener(ruleProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, rulesPath);

        assertThat(ruleListener.rules, is(aMapWithSize(1)));
        assertThat(ruleListener.rules, hasKey("basic:basicyamlrule"));

        Files.copy(SOURCE_PATH.resolve("MixedRules.yaml"), rulesPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, rulesPath);

        assertThat(ruleListener.rules, is(aMapWithSize(4)));
        assertThat(ruleListener.rules, hasKey("mode-tv-rule"));
        assertThat(ruleListener.rules, hasKey("stub:mode-tv-rule"));
        assertThat(ruleListener.rules, hasKey("rules_tools:tsm"));
        assertThat(ruleListener.rules, hasKey("ysc:washing_machine_alert_test"));

        modelRepository.processWatchEvent(WatchService.Kind.DELETE, rulesPath);

        assertThat(ruleListener.rules, is(anEmptyMap()));
    }

    @Test
    public void emptyRuleTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("EmptyRule.yaml"), rulesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleProvider ruleProvider = new YamlRuleProvider();
        TestRuleChangeListener ruleListener = new TestRuleChangeListener();
        ruleProvider.addProviderChangeListener(ruleListener);
        modelRepository.addYamlModelListener(ruleProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, rulesPath);

        assertThat(ruleListener.rules, is(anEmptyMap()));
    }

    @Test
    public void basicRuleTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("BasicRule.yaml"), rulesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleProvider ruleProvider = new YamlRuleProvider();
        TestRuleChangeListener ruleListener = new TestRuleChangeListener();
        ruleProvider.addProviderChangeListener(ruleListener);
        modelRepository.addYamlModelListener(ruleProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, rulesPath);

        Rule rule = Objects.requireNonNull(ruleListener.rules.get("basic:basicyamlrule"));
        assertThat(rule.getUID(), is("basic:basicyamlrule"));
        assertThat(rule.getName(), is("Basic YAML Rule"));
        assertThat(rule.getDescription(), is(emptyOrNullString()));
        assertThat(rule.getTemplateUID(), is(emptyOrNullString()));
        assertThat(rule.getTemplateState(), is(TemplateState.NO_TEMPLATE));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));
        assertThat(rule.getConfiguration().getProperties(), is(anEmptyMap()));
        assertThat(rule.getConfigurationDescriptions(), is(empty()));

        List<Trigger> triggers = rule.getTriggers();
        assertThat(triggers, hasSize(2));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("2"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", BigDecimal.valueOf(100L)));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("22"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("timer.TimeOfDayTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("time", "14:05"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        List<Condition> conditions = rule.getConditions();
        assertThat(conditions, hasSize(2));
        Condition condition = conditions.get(0);
        assertThat(condition.getId(), is("3"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(0L)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));
        condition = conditions.get(1);
        assertThat(condition.getId(), is("4"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(2L)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));

        List<Action> actions = rule.getActions();
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
    public void mixedRulesTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("MixedRules.yaml"), rulesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleProvider ruleProvider = new YamlRuleProvider();
        TestRuleChangeListener ruleListener = new TestRuleChangeListener();
        ruleProvider.addProviderChangeListener(ruleListener);
        modelRepository.addYamlModelListener(ruleProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, rulesPath);
        assertThat(ruleListener.rules, is(aMapWithSize(4)));

        Rule rule = Objects.requireNonNull(ruleListener.rules.get("mode-tv-rule"));
        assertThat(rule.getUID(), is("mode-tv-rule"));
        assertThat(rule.getName(), is("Mode TV"));
        assertThat(rule.getDescription(), is(emptyOrNullString()));
        assertThat(rule.getTemplateUID(), is(emptyOrNullString()));
        assertThat(rule.getTemplateState(), is(TemplateState.INSTANTIATED));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));
        Configuration config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(1)));
        assertThat(config.getProperties(), hasEntry("sourceItem", "None"));
        List<ConfigDescriptionParameter> configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(1));
        ConfigDescriptionParameter parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("sourceItem"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertThat(parameter.getLabel(), is("Source Item"));
        assertThat(parameter.getDescription(), is("The source Item whose state to monitor"));
        assertTrue(parameter.isRequired());

        List<Trigger> triggers = rule.getTriggers();
        assertThat(triggers, hasSize(1));
        Trigger trigger = triggers.get(0);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.ItemStateChangeTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("itemName", "TvPower"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("state", "ON"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("previousState", "OFF"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(3)));

        assertThat(rule.getConditions(), is(empty()));

        List<Action> actions = rule.getActions();
        assertThat(actions, hasSize(1));
        Action action = actions.get(0);
        assertThat(action.getId(), is("script"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/vnd.openhab.dsl.rule"));
        assertThat(action.getConfiguration().getProperties(), hasKey("script"));
        assertThat(action.getConfiguration().getProperties().get("script"), is(instanceOf(String.class)));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));

        rule = Objects.requireNonNull(ruleListener.rules.get("stub:mode-tv-rule"));
        assertThat(rule.getUID(), is("stub:mode-tv-rule"));
        assertThat(rule.getName(), is("Template based mode TV"));
        assertThat(rule.getDescription(), is(emptyOrNullString()));
        assertThat(rule.getTemplateUID(), is("mode-tv-template"));
        assertThat(rule.getTemplateState(), is(TemplateState.PENDING));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));
        config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(1)));
        assertThat(config.getProperties(), hasEntry("sourceItem", "TvPower"));
        assertThat(rule.getConfigurationDescriptions(), is(empty()));

        rule = Objects.requireNonNull(ruleListener.rules.get("rules_tools:tsm"));
        assertThat(rule.getUID(), is("rules_tools:tsm"));
        assertThat(rule.getName(), is("Time Based State Machine Test Rule"));
        assertThat(rule.getDescription(),
                is("Creates timers to transition a state Item to a new state at defined times of day."));
        assertThat(rule.getTemplateUID(), is("none"));
        assertThat(rule.getTemplateState(), is(TemplateState.TEMPLATE_MISSING));
        assertThat(rule.getVisibility(), is(Visibility.VISIBLE));
        config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(3)));
        assertThat(config.getProperties(), hasEntry("namespace", "None"));
        assertThat(config.getProperties(), hasEntry("timesOfDayGrp", "No"));
        assertThat(config.getProperties(), hasEntry("timeOfDay", "Yes"));
        configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(3));
        parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("timeOfDay"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertThat(parameter.getLabel(), is("Time of Day State Item"));
        assertThat(parameter.getDescription(), is("String Item that holds the current time of day's state."));
        assertTrue(parameter.isRequired());
        List<FilterCriteria> filterCriteria = parameter.getFilterCriteria();
        assertThat(filterCriteria, hasSize(1));
        assertThat(filterCriteria.get(0).getName(), is("type"));
        assertThat(filterCriteria.get(0).getValue(), is("String"));
        parameter = configDescriptions.get(1);
        assertThat(parameter.getName(), is("timesOfDayGrp"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertThat(parameter.getLabel(), is("Times of Day Group"));
        assertThat(parameter.getDescription(),
                is("Has as members all the DateTime Items that define time of day states."));
        assertTrue(parameter.isRequired());
        filterCriteria = parameter.getFilterCriteria();
        assertThat(filterCriteria, hasSize(1));
        assertThat(filterCriteria.get(0).getName(), is("type"));
        assertThat(filterCriteria.get(0).getValue(), is("Group"));
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("namespace"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertThat(parameter.getLabel(), is("Time of Day Namespace"));
        assertThat(parameter.getDescription(), is("The Item metadata namespace (e.g. \"tsm\")."));
        assertTrue(parameter.isRequired());
        assertThat(parameter.getFilterCriteria(), is(empty()));
        assertThat(parameter.getOptions(), is(empty()));

        triggers = rule.getTriggers();
        assertThat(triggers, hasSize(3));
        trigger = triggers.get(0);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.GroupStateChangeTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("groupName", "DemoSwitchGroup"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(1);
        assertThat(trigger.getId(), is("2"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.SystemStartlevelTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", BigDecimal.valueOf(100L)));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));
        trigger = triggers.get(2);
        assertThat(trigger.getId(), is("4"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("timer.TimeOfDayTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("time", "00:05"));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(1)));

        assertThat(rule.getConditions(), is(empty()));

        actions = rule.getActions();
        assertThat(actions, hasSize(1));
        action = actions.get(0);
        assertThat(action.getId(), is("3"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/javascript"));
        assertThat(action.getConfiguration().getProperties(), hasKey("script"));
        assertThat(action.getConfiguration().getProperties().get("script"), is(instanceOf(String.class)));
        assertThat(action.getConfiguration().getProperties().get("script"), is(
                "// Version 1.0\nvar {TimerMgr, helpers} = require('openhab_rules_tools');\nconsole.loggerName = 'org.openhab.automation.rules_tools.TimeStateMachine';\n"));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));

        rule = Objects.requireNonNull(ruleListener.rules.get("ysc:washing_machine_alert_test"));
        assertThat(rule.getUID(), is("ysc:washing_machine_alert_test"));
        assertThat(rule.getName(), is("Alert when Washing Machine Finished Test"));
        assertThat(rule.getDescription(), is(
                "This will monitor the power consumption of a washing machine and send an alert command when it gets below a threshold, meaning it has finished."));
        assertThat(rule.getTemplateUID(), is(emptyOrNullString()));
        assertThat(rule.getTemplateState(), is(TemplateState.TEMPLATE_MISSING));
        assertThat(rule.getVisibility(), is(Visibility.HIDDEN));
        assertThat(rule.getConfiguration().getProperties(), is(anEmptyMap()));
        configDescriptions = rule.getConfigurationDescriptions();
        assertThat(configDescriptions, hasSize(4));
        parameter = configDescriptions.get(0);
        assertThat(parameter.getName(), is("powerItem"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertThat(parameter.getLabel(), is("Power Item"));
        assertThat(parameter.getDescription(), is(
                "Item that holds the power (in watts) of the washing machine. Can be a quantity type (Number:Power)."));
        assertTrue(parameter.isRequired());
        parameter = configDescriptions.get(1);
        assertThat(parameter.getName(), is("threshold"));
        assertThat(parameter.getType(), is(Type.DECIMAL));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertThat(parameter.getLabel(), is("Threshold"));
        assertThat(parameter.getDescription(), is(
                "When the power measurement was at or above the threshold and crosses below it, trigger the alert."));
        assertThat(parameter.getDefault(), is("2"));
        assertTrue(parameter.isRequired());
        parameter = configDescriptions.get(2);
        assertThat(parameter.getName(), is("alertItem"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is("item"));
        assertThat(parameter.getLabel(), is("Alert Item"));
        assertThat(parameter.getDescription(), is(
                "Item to send a command to when the measured power gets below the threshold. For instance, a Hue light advanced Alert channel."));
        assertFalse(parameter.isRequired());
        parameter = configDescriptions.get(3);
        assertThat(parameter.getName(), is("alertCommand"));
        assertThat(parameter.getType(), is(Type.TEXT));
        assertThat(parameter.getContext(), is(emptyOrNullString()));
        assertThat(parameter.getLabel(), is("Alert Command"));
        assertThat(parameter.getDescription(), is(
                "Command to send to the alert item (for an item linked to a Hue light alert channel, LSELECT will flash the light for a few seconds)."));
        assertThat(parameter.getDefault(), is("LSELECT"));
        assertTrue(parameter.isRequired());

        triggers = rule.getTriggers();
        assertThat(triggers, hasSize(1));
        trigger = triggers.get(0);
        assertThat(trigger.getId(), is("1"));
        assertThat(trigger.getLabel(), is(emptyOrNullString()));
        assertThat(trigger.getDescription(), is(emptyOrNullString()));
        assertThat(trigger.getTypeUID(), is("core.ItemStateChangeTrigger"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("itemName", "CurrentPower"));
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("state", ""));
        assertThat(trigger.getConfiguration().getProperties(), is(aMapWithSize(2)));

        assertThat(rule.getConditions(), is(empty()));

        actions = rule.getActions();
        assertThat(actions, hasSize(1));
        action = actions.get(0);
        assertThat(action.getId(), is("2"));
        assertThat(action.getLabel(), is(emptyOrNullString()));
        assertThat(action.getDescription(), is(emptyOrNullString()));
        assertThat(action.getTypeUID(), is("script.ScriptAction"));
        assertThat(action.getConfiguration().getProperties(), hasEntry("type", "application/javascript"));
        assertThat(action.getConfiguration().getProperties(), hasKey("script"));
        assertThat(action.getConfiguration().getProperties().get("script"), is(instanceOf(String.class)));
        assertThat(action.getConfiguration().getProperties(), is(aMapWithSize(2)));
        assertThat(action.getInputs(), is(anEmptyMap()));
    }

    @Test
    public void fullRuleTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("FullRule.yaml"), rulesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleProvider ruleProvider = new YamlRuleProvider();
        TestRuleChangeListener ruleListener = new TestRuleChangeListener();
        ruleProvider.addProviderChangeListener(ruleListener);
        modelRepository.addYamlModelListener(ruleProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, rulesPath);
        assertThat(ruleListener.rules, is(aMapWithSize(1)));

        Rule rule = Objects.requireNonNull(ruleListener.rules.get("test:full-rule"));
        assertThat(rule.getUID(), is("test:full-rule"));
        assertThat(rule.getName(), is("Full Rule"));
        assertThat(rule.getDescription(), is("The description of the full rule"));
        assertThat(rule.getTemplateUID(), is("template:non-existing"));
        assertThat(rule.getTemplateState(), is(TemplateState.TEMPLATE_MISSING));
        assertThat(rule.getVisibility(), is(Visibility.EXPERT));
        Configuration config = rule.getConfiguration();
        assertThat(config.getProperties(), is(aMapWithSize(4)));
        assertThat(config.getProperties(), hasEntry("textParam", " text"));
        assertThat(config.getProperties(), hasEntry("integerParam", BigDecimal.valueOf(5L)));
        assertThat(config.getProperties(), hasEntry("decimalParam", BigDecimal.valueOf(6.75)));
        assertThat(config.getProperties(), hasEntry("booleanParam", true));

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
        assertThat(trigger.getConfiguration().getProperties(), hasEntry("startlevel", BigDecimal.valueOf(80L)));
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
        assertThat(action.getConfiguration().getProperties(), hasEntry("volume", BigDecimal.valueOf(100L)));
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

    public static class TestRuleChangeListener implements ProviderChangeListener<Rule> {

        public final Map<String, Rule> rules = new HashMap<>();

        @Override
        public void added(Provider<Rule> provider, Rule element) {
            rules.put(element.getUID(), element);
        }

        @Override
        public void removed(Provider<Rule> provider, Rule element) {
            rules.remove(element.getUID());
        }

        @Override
        public void updated(Provider<Rule> provider, Rule oldelement, Rule element) {
            rules.put(element.getUID(), element);
        }
    }
}
