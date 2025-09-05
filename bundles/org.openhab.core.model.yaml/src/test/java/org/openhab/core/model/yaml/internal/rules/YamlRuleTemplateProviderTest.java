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
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.WatchService;

/**
 * Tests some general behavior and parsing of specific YAML rule template files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlRuleTemplateProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/rule-templates");
    private static final String TEMPLATES_NAME = "rule-templates.yaml";
    private static final Path TEMPLATES_PATH = Path.of(TEMPLATES_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path templatesPath;

    @BeforeEach
    public void setup() {
        templatesPath = watchPath.resolve(TEMPLATES_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);
    }

    @Test
    public void yamlModelListenerTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("BasicRuleTemplate.yaml"), templatesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleTemplateProvider templateProvider = new YamlRuleTemplateProvider();
        TestRuleTemplateChangeListener templateListener = new TestRuleTemplateChangeListener();
        templateProvider.addProviderChangeListener(templateListener);
        modelRepository.addYamlModelListener(templateProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, templatesPath);

        assertThat(templateListener.templates, is(aMapWithSize(1)));
        assertThat(templateListener.templates, hasKey("basic:yaml-rule-template"));

        Files.copy(SOURCE_PATH.resolve("FullRuleTemplate.yaml"), templatesPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, templatesPath);

        assertThat(templateListener.templates, is(aMapWithSize(1)));
        assertThat(templateListener.templates, hasKey("test:full-rule-template"));

        modelRepository.processWatchEvent(WatchService.Kind.DELETE, templatesPath);

        assertThat(templateListener.templates, is(anEmptyMap()));
    }

    @Test
    public void basicRuleTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("BasicRuleTemplate.yaml"), templatesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleTemplateProvider ruleTemplateProvider = new YamlRuleTemplateProvider();
        TestRuleTemplateChangeListener templateListener = new TestRuleTemplateChangeListener();
        ruleTemplateProvider.addProviderChangeListener(templateListener);
        modelRepository.addYamlModelListener(ruleTemplateProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, templatesPath);

        RuleTemplate template = Objects.requireNonNull(templateListener.templates.get("basic:yaml-rule-template"));
        assertThat(template.getUID(), is("basic:yaml-rule-template"));
        assertThat(template.getLabel(), is("Basic YAML Rule Template"));
        assertThat(template.getDescription(), is("A YAML rule made from a template."));
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
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(0L)));
        assertThat(condition.getConfiguration().getProperties(), is(aMapWithSize(1)));
        assertThat(condition.getInputs(), is(anEmptyMap()));
        condition = conditions.get(1);
        assertThat(condition.getId(), is("5"));
        assertThat(condition.getLabel(), is(emptyOrNullString()));
        assertThat(condition.getDescription(), is(emptyOrNullString()));
        assertThat(condition.getTypeUID(), is("ephemeris.WeekdayCondition"));
        assertThat(condition.getConfiguration().getProperties(), hasEntry("offset", BigDecimal.valueOf(2L)));
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
    public void fullRuleTest() throws IOException {
        Files.copy(SOURCE_PATH.resolve("FullRuleTemplate.yaml"), templatesPath);
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        YamlRuleTemplateProvider ruleTemplateProvider = new YamlRuleTemplateProvider();
        TestRuleTemplateChangeListener templateListener = new TestRuleTemplateChangeListener();
        ruleTemplateProvider.addProviderChangeListener(templateListener);
        modelRepository.addYamlModelListener(ruleTemplateProvider);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, templatesPath);
        assertThat(templateListener.templates, is(aMapWithSize(1)));

        RuleTemplate template = Objects.requireNonNull(templateListener.templates.get("test:full-rule-template"));
        assertThat(template.getUID(), is("test:full-rule-template"));
        assertThat(template.getLabel(), is("Full Rule Template"));
        assertThat(template.getDescription(), is("The description of the template-based full rule"));
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
        assertThat(action.getConfiguration().getProperties(), hasEntry("volume", BigDecimal.valueOf(100L)));
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

    public static class TestRuleTemplateChangeListener implements ProviderChangeListener<RuleTemplate> {

        public final Map<String, RuleTemplate> templates = new HashMap<>();

        @Override
        public void added(Provider<RuleTemplate> provider, RuleTemplate element) {
            templates.put(element.getUID(), element);
        }

        @Override
        public void removed(Provider<RuleTemplate> provider, RuleTemplate element) {
            templates.remove(element.getUID());
        }

        @Override
        public void updated(Provider<RuleTemplate> provider, RuleTemplate oldelement, RuleTemplate element) {
            templates.put(element.getUID(), element);
        }
    }
}
