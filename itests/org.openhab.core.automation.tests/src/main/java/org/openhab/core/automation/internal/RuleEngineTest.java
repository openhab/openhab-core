/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Test adding, retrieving and updating rules from the RuleEngineImpl
 *
 * @author Marin Mitev - Initial contribution
 * @author Thomas Höfer - Added config description parameter unit
 */
@NonNullByDefault
public class RuleEngineTest extends JavaOSGiTest {

    private @NonNullByDefault({}) RuleEngineImpl ruleEngine;
    private @NonNullByDefault({}) RuleRegistry ruleRegistry;

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();
        ruleEngine = (RuleEngineImpl) getService(RuleManager.class);
        ruleRegistry = getService(RuleRegistry.class);
        registerService(new TestModuleTypeProvider(), ModuleTypeProvider.class.getName());
    }

    /**
     * test auto map connections of the rule
     *
     */
    @Test
    @Disabled
    public void testAutoMapRuleConnections() {
        RuleImpl rule = createAutoMapRule();
        // check condition connections
        Map<String, String> conditionInputs = rule.getConditions().get(0).getInputs();
        assertEquals(1, conditionInputs.size(), "Number of user define condition inputs");
        assertEquals("triggerId.triggerOutput", conditionInputs.get("conditionInput"),
                "Check user define condition connection");

        // check action connections
        Map<String, String> actionInputs = rule.getActions().get(0).getInputs();
        assertEquals(2, actionInputs.size(), "Number of user define action inputs");
        assertEquals("triggerId.triggerOutput", actionInputs.get("actionInput"),
                "Check user define action connections for input actionInput");
        assertEquals("triggerId.triggerOutput", actionInputs.get("in6"),
                "Check user define action connections for input in6");

        // do connections auto mapping
        ruleRegistry.add(rule);
        Rule ruleGet = ruleEngine.getRule("AutoMapRule");
        assertEquals("AutoMapRule", ruleGet.getUID(), "Returned rule with wrong UID");

        // check condition connections
        conditionInputs = ruleGet.getConditions().get(0).getInputs();
        assertEquals(2, conditionInputs.size(), "Number of user define condition inputs");
        assertEquals("triggerId.triggerOutput", conditionInputs.get("conditionInput"),
                "Check user define condition connection");
        assertEquals("triggerId.out3", conditionInputs.get("in2"),
                "Auto map condition input in2[tagA, tagB] to trigger output out3[tagA, tagB, tagC]");

        // check action connections
        actionInputs = ruleGet.getActions().get(0).getInputs();
        assertEquals(4, actionInputs.size(), "Number of user define action inputs");
        assertEquals("triggerId.triggerOutput", actionInputs.get("actionInput"),
                "Check user define action connections for input actionInput");
        assertEquals("triggerId.triggerOutput", actionInputs.get("in6"),
                "Check user define action connections for input in6 is not changed by the auto mapping");
        assertEquals("triggerId.out3", actionInputs.get("in5"),
                "Auto map action input in5[tagA, tagB, tagC] to trigger output out3[tagA, tagB, tagC]");
        assertEquals("actionId.out5", actionInputs.get("in4"),
                "Auto map action input in5[tagD, tagE] to action output out5[tagD, tagE]");
    }

    /**
     * test editing rule tags
     *
     */
    @Test
    public void testRuleTags() {
        RuleImpl rule1 = new RuleImpl("ruleWithTag1");
        Set<String> ruleTags = new LinkedHashSet<>();
        ruleTags.add("tag1");
        rule1.setTags(ruleTags);
        ruleRegistry.add(rule1);

        RuleImpl rule2 = new RuleImpl("ruleWithTags12");
        ruleTags = new LinkedHashSet<>();
        ruleTags.add("tag1");
        ruleTags.add("tag2");
        rule2.setTags(ruleTags);
        ruleRegistry.add(rule2);

        Rule rule1Get = ruleEngine.getRule("ruleWithTag1");
        assertNotNull(rule1Get, "Cannot find rule by UID");
        assertNotNull(rule1Get.getTags(), "rule.getTags is null");
        assertEquals(1, rule1Get.getTags().size(), "rule.getTags is empty");

        Rule rule2Get = ruleEngine.getRule("ruleWithTags12");
        assertNotNull(rule2Get, "Cannot find rule by UID");
        assertNotNull(rule2Get.getTags(), "rule.getTags is null");
        assertEquals(2, rule2Get.getTags().size(), "rule.getTags is empty");
    }

    /**
     * test rule configurations with null
     *
     */
    @Test
    public void testRuleConfigNull() {
        Rule rule3 = RuleBuilder.create("rule3").withTriggers(createTriggers("typeUID"))
                .withConditions(createConditions("typeUID")).withActions(createActions("typeUID")).build();
        ruleRegistry.add(rule3);
        Rule rule3Get = ruleEngine.getRule("rule3");
        assertNotNull(rule3Get.getConfiguration(), "RuleImpl configuration is null");
    }

    /**
     * test rule configurations with real values
     *
     */
    @Test
    public void testRuleConfigValue() {
        List<ConfigDescriptionParameter> configDescriptions = createConfigDescriptions();
        Configuration configurations = new Configuration();
        configurations.put("config1", 5);

        Rule rule4 = RuleBuilder.create("rule4").withTriggers(createTriggers("typeUID"))
                .withConditions(createConditions("typeUID")).withActions(createActions("typeUID"))
                .withConfigurationDescriptions(configDescriptions).withConfiguration(configurations).build();
        ruleRegistry.add(rule4);
        Rule rule4Get = ruleEngine.getRule("rule4");
        Configuration rule4cfg = rule4Get.getConfiguration();
        List<ConfigDescriptionParameter> rule4cfgD = rule4Get.getConfigurationDescriptions();
        assertNotNull(rule4cfg, "RuleImpl configuration is null");
        assertTrue(rule4cfg.containsKey("config1"), "Missing config property in rule copy");
        assertEquals(new BigDecimal(5), rule4cfg.get("config1"), "Wrong config value");

        assertNotNull(rule4cfgD, "RuleImpl configuration description is null");
        assertEquals(1, rule4cfgD.size(), "Missing config description in rule copy");
        ConfigDescriptionParameter rule4cfgDP = rule4cfgD.iterator().next();
        assertEquals("3", rule4cfgDP.getDefault(), "Wrong default value in config description");
        assertEquals("context1", rule4cfgDP.getContext(), "Wrong context value in config description");
        assertNotNull(rule4cfgDP.getOptions(), "Null options in config description");
        assertEquals("1", rule4cfgDP.getOptions().get(0).getValue(), "Wrong option value in config description");
        assertEquals("one", rule4cfgDP.getOptions().get(0).getLabel(), "Wrong option label in config description");
    }

    /**
     * test rule actions
     *
     */
    @Test
    public void testRuleActions() {
        RuleImpl rule1 = createRule();
        List<Action> actions = new ArrayList<>(rule1.getActions());
        ruleRegistry.add(rule1);

        Rule rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet = rule1Get.getActions();
        assertNotNull(actionsGet, "Null actions list");
        assertEquals(1, actionsGet.size(), "Empty actions list");
        assertEquals(actionsGet, rule1Get.getActions(), "Returned actions list should not be a copy");

        actions.add(ModuleBuilder.createAction().withId("actionId2").withTypeUID("typeUID2").build());
        rule1.setActions(actions);
        ruleEngine.addRule(rule1);
        rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet2 = rule1Get.getActions();
        assertNotNull(actionsGet2, "Null actions list");
        assertEquals(2, actionsGet2.size(), "Action was not added to the rule's list of actions");
        assertNotNull(rule1Get.getModule("actionId2"), "RuleImpl action with wrong id is returned");

        actions.add(ModuleBuilder.createAction().withId("actionId3").withTypeUID("typeUID3").build());
        ruleEngine.addRule(rule1); // ruleEngine.update will update the RuleImpl2.moduleMap with the new module
        rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet3 = rule1Get.getActions();
        assertNotNull(actionsGet3, "Null actions list");
        assertEquals(3, actionsGet3.size(), "Action was not added to the rule's list of actions");
        assertNotNull(ruleEngine.getRule("rule1").getModule("actionId3"), "RuleImpl modules map was not updated");
    }

    /**
     * test rule triggers
     *
     */
    @Test
    public void testRuleTriggers() {
        RuleImpl rule1 = createRule();
        List<Trigger> triggers = new ArrayList<>(rule1.getTriggers());
        ruleRegistry.add(rule1);
        Rule rule1Get = ruleEngine.getRule("rule1");
        List<Trigger> triggersGet = rule1Get.getTriggers();
        assertNotNull(triggersGet, "Null triggers list");
        assertEquals(1, triggersGet.size(), "Empty triggers list");
        assertEquals(triggersGet, rule1Get.getTriggers(), "Returned triggers list should not be a copy");

        triggers.add(ModuleBuilder.createTrigger().withId("triggerId2").withTypeUID("typeUID2").build());
        rule1.setTriggers(triggers);
        ruleEngine.addRule(rule1); // ruleEngine.update will update the
                                   // RuleImpl2.moduleMap with the new
                                   // module
        Rule rule2Get = ruleEngine.getRule("rule1");
        List<Trigger> triggersGet2 = rule2Get.getTriggers();
        assertNotNull(triggersGet2, "Null triggers list");
        assertEquals(2, triggersGet2.size(), "Trigger was not added to the rule's list of triggers");
        assertEquals(triggersGet2, rule2Get.getTriggers(), "Returned triggers list should not be a copy");
        assertNotNull(rule2Get.getModule("triggerId2"), "RuleImpl trigger with wrong id is returned: " + triggersGet2);
    }

    /**
     * test rule condition
     */
    @Test
    public void testRuleConditions() {
        RuleImpl rule1 = createRule();
        List<Condition> conditions = new ArrayList<>(rule1.getConditions());
        ruleRegistry.add(rule1);
        Rule rule1Get = ruleEngine.getRule("rule1");
        List<Condition> conditionsGet = rule1Get.getConditions();
        assertNotNull(conditionsGet, "Null conditions list");
        assertEquals(1, conditionsGet.size(), "Empty conditions list");
        assertEquals(conditionsGet, rule1Get.getConditions(), "Returned conditions list should not be a copy");

        conditions.add(ModuleBuilder.createCondition().withId("conditionId2").withTypeUID("typeUID2").build());
        rule1.setConditions(conditions);
        ruleEngine.addRule(rule1); // ruleEngine.update will update the RuleImpl2.moduleMap with the new module
        Rule rule2Get = ruleEngine.getRule("rule1");
        List<Condition> conditionsGet2 = rule2Get.getConditions();
        assertNotNull(conditionsGet2, "Null conditions list");
        assertEquals(2, conditionsGet2.size(), "Condition was not added to the rule's list of conditions");
        assertEquals(conditionsGet2, rule2Get.getConditions(), "Returned conditions list should not be a copy");
        assertNotNull(rule2Get.getModule("conditionId2"),
                "RuleImpl condition with wrong id is returned: " + conditionsGet2);
    }

    private RuleImpl createRule() {
        return (RuleImpl) RuleBuilder.create("rule1").withTriggers(createTriggers("typeUID"))
                .withConditions(createConditions("typeUID")).withActions(createActions("typeUID")).build();
    }

    private RuleImpl createAutoMapRule() {
        return (RuleImpl) RuleBuilder.create("AutoMapRule")
                .withTriggers(createTriggers(TestModuleTypeProvider.TRIGGER_TYPE))
                .withConditions(createConditions(TestModuleTypeProvider.CONDITION_TYPE))
                .withActions(createActions(TestModuleTypeProvider.ACTION_TYPE)).build();
    }

    private List<Trigger> createTriggers(String type) {
        List<Trigger> triggers = new ArrayList<>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        triggers.add(ModuleBuilder.createTrigger().withId("triggerId").withTypeUID(type)
                .withConfiguration(configurations).build());
        return triggers;
    }

    private List<Condition> createConditions(String type) {
        List<Condition> conditions = new ArrayList<>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        Map<String, String> inputs = new HashMap<>(11);
        String ouputModuleId = "triggerId";
        String outputName = "triggerOutput";
        String inputName = "conditionInput";
        inputs.put(inputName, ouputModuleId + "." + outputName);
        conditions.add(ModuleBuilder.createCondition().withId("conditionId").withTypeUID(type)
                .withConfiguration(configurations).withInputs(inputs).build());
        return conditions;
    }

    private List<Action> createActions(String type) {
        List<Action> actions = new ArrayList<>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        Map<String, String> inputs = new HashMap<>(11);
        String ouputModuleId = "triggerId";
        String outputName = "triggerOutput";
        String inputName = "actionInput";
        inputs.put(inputName, ouputModuleId + "." + outputName);
        inputs.put("in6", ouputModuleId + "." + outputName);
        actions.add(ModuleBuilder.createAction().withId("actionId").withTypeUID(type).withConfiguration(configurations)
                .withInputs(inputs).build());
        return actions;
    }

    private List<ConfigDescriptionParameter> createConfigDescriptions() {
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<>();
        List<ParameterOption> options = new ArrayList<>();
        options.add(new ParameterOption("1", "one"));
        options.add(new ParameterOption("2", "two"));

        String groupName = null;
        Boolean advanced = false;
        Boolean limitToOptions = true;
        Integer multipleLimit = 0;

        String label = "label1";
        String pattern = null;
        String context = "context1";
        String description = "description1";
        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal step = null;
        Boolean required = true;
        Boolean multiple = false;
        Boolean readOnly = false;

        String typeStr = ConfigDescriptionParameter.Type.INTEGER.name();
        String defValue = "3";

        List<FilterCriteria> filter = new ArrayList<>();

        String configPropertyName = "config1";

        ConfigDescriptionParameter cfgDP = ConfigDescriptionParameterBuilder
                .create(configPropertyName, Type.valueOf(typeStr)).withMaximum(max).withMinimum(min).withStepSize(step)
                .withPattern(pattern).withRequired(required).withReadOnly(readOnly).withMultiple(multiple)
                .withContext(context).withDefault(defValue).withLabel(label).withDescription(description)
                .withOptions(options).withFilterCriteria(filter).withGroupName(groupName).withAdvanced(advanced)
                .withLimitToOptions(limitToOptions).withMultipleLimit(multipleLimit).build();
        configDescriptions.add(cfgDP);
        return configDescriptions;
    }
}
