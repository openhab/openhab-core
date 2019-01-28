/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.core.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleManager;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.FilterCriteria;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test adding, retrieving and updating rules from the RuleEngineImpl
 *
 * @author Marin Mitev - initial version
 * @author Thomas Höfer - Added config description parameter unit
 */
public class RuleEngineTest extends JavaOSGiTest {

    RuleEngineImpl ruleEngine;
    RuleRegistry ruleRegistry;

    @Before
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
    @Ignore
    public void testAutoMapRuleConnections() {
        RuleImpl rule = createAutoMapRule();
        // check condition connections
        Map<String, String> conditionInputs = rule.getConditions().get(0).getInputs();
        Assert.assertEquals("Number of user define condition inputs", 1, conditionInputs.size());
        Assert.assertTrue("Check user define condition connection",
                "triggerId.triggerOutput".equals(conditionInputs.get("conditionInput")));

        // check action connections
        Map<String, String> actionInputs = rule.getActions().get(0).getInputs();
        Assert.assertEquals("Number of user define action inputs", 2, actionInputs.size());
        Assert.assertTrue("Check user define action connections for input actionInput",
                "triggerId.triggerOutput".equals(actionInputs.get("actionInput")));
        Assert.assertTrue("Check user define action connections for input in6",
                "triggerId.triggerOutput".equals(actionInputs.get("in6")));

        // do connections auto mapping
        ruleRegistry.add(rule);
        Rule ruleGet = ruleEngine.getRule("AutoMapRule");
        Assert.assertEquals("Returned rule with wrong UID", "AutoMapRule", ruleGet.getUID());

        // check condition connections
        conditionInputs = ruleGet.getConditions().get(0).getInputs();
        Assert.assertEquals("Number of user define condition inputs", 2, conditionInputs.size());
        Assert.assertEquals("Check user define condition connection", "triggerId.triggerOutput",
                conditionInputs.get("conditionInput"));
        Assert.assertEquals("Auto map condition input in2[tagA, tagB] to trigger output out3[tagA, tagB, tagC]",
                "triggerId.out3", conditionInputs.get("in2"));

        // check action connections
        actionInputs = ruleGet.getActions().get(0).getInputs();
        Assert.assertEquals("Number of user define action inputs", 4, actionInputs.size());
        Assert.assertTrue("Check user define action connections for input actionInput",
                "triggerId.triggerOutput".equals(actionInputs.get("actionInput")));
        Assert.assertEquals("Check user define action connections for input in6 is not changed by the auto mapping",
                "triggerId.triggerOutput", actionInputs.get("in6"));
        Assert.assertEquals("Auto map action input in5[tagA, tagB, tagC] to trigger output out3[tagA, tagB, tagC]",
                "triggerId.out3", actionInputs.get("in5"));
        Assert.assertEquals("Auto map action input in5[tagD, tagE] to action output out5[tagD, tagE]", "actionId.out5",
                actionInputs.get("in4"));

    }

    /**
     * test editing rule tags
     *
     */
    @Test
    public void testRuleTags() {
        RuleImpl rule1 = new RuleImpl("ruleWithTag1");
        Set<String> ruleTags = new LinkedHashSet<String>();
        ruleTags.add("tag1");
        rule1.setTags(ruleTags);
        ruleRegistry.add(rule1);

        RuleImpl rule2 = new RuleImpl("ruleWithTags12");
        ruleTags = new LinkedHashSet<String>();
        ruleTags.add("tag1");
        ruleTags.add("tag2");
        rule2.setTags(ruleTags);
        ruleRegistry.add(rule2);

        Rule rule1Get = ruleEngine.getRule("ruleWithTag1");
        Assert.assertNotNull("Cannot find rule by UID", rule1Get);
        Assert.assertNotNull("rule.getTags is null", rule1Get.getTags());
        Assert.assertEquals("rule.getTags is empty", 1, rule1Get.getTags().size());

        Rule rule2Get = ruleEngine.getRule("ruleWithTags12");
        Assert.assertNotNull("Cannot find rule by UID", rule2Get);
        Assert.assertNotNull("rule.getTags is null", rule2Get.getTags());
        Assert.assertEquals("rule.getTags is empty", 2, rule2Get.getTags().size());
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
        Assert.assertNotNull("RuleImpl configuration is null", rule3Get.getConfiguration());
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
        Assert.assertNotNull("RuleImpl configuration is null", rule4cfg);
        Assert.assertTrue("Missing config property in rule copy", rule4cfg.containsKey("config1"));
        Assert.assertEquals("Wrong config value", new BigDecimal(5), rule4cfg.get("config1"));

        Assert.assertNotNull("RuleImpl configuration description is null", rule4cfgD);
        Assert.assertEquals("Missing config description in rule copy", 1, rule4cfgD.size());
        ConfigDescriptionParameter rule4cfgDP = rule4cfgD.iterator().next();
        Assert.assertEquals("Wrong default value in config description", "3", rule4cfgDP.getDefault());
        Assert.assertEquals("Wrong context value in config description", "context1", rule4cfgDP.getContext());
        Assert.assertNotNull("Null options in config description", rule4cfgDP.getOptions());
        Assert.assertEquals("Wrong option value in config description", "1", rule4cfgDP.getOptions().get(0).getValue());
        Assert.assertEquals("Wrong option label in config description", "one",
                rule4cfgDP.getOptions().get(0).getLabel());
    }

    /**
     * test rule actions
     *
     */
    @Test
    public void testRuleActions() {
        RuleImpl rule1 = createRule();
        List<Action> actions = new ArrayList<Action>(rule1.getActions());
        ruleRegistry.add(rule1);

        Rule rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet = rule1Get.getActions();
        Assert.assertNotNull("Null actions list", actionsGet);
        Assert.assertEquals("Empty actions list", 1, actionsGet.size());
        Assert.assertEquals("Returned actions list should not be a copy", actionsGet, rule1Get.getActions());

        actions.add(ModuleBuilder.createAction().withId("actionId2").withTypeUID("typeUID2").build());
        rule1.setActions(actions);
        ruleEngine.addRule(rule1);
        rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet2 = rule1Get.getActions();
        Assert.assertNotNull("Null actions list", actionsGet2);
        Assert.assertEquals("Action was not added to the rule's list of actions", 2, actionsGet2.size());
        Assert.assertNotNull("RuleImpl action with wrong id is returned", rule1Get.getModule("actionId2"));

        actions.add(ModuleBuilder.createAction().withId("actionId3").withTypeUID("typeUID3").build());
        ruleEngine.addRule(rule1); // ruleEngine.update will update the RuleImpl2.moduleMap with the new module
        rule1Get = ruleEngine.getRule("rule1");
        List<Action> actionsGet3 = rule1Get.getActions();
        Assert.assertNotNull("Null actions list", actionsGet3);
        Assert.assertEquals("Action was not added to the rule's list of actions", 3, actionsGet3.size());
        Assert.assertNotNull("RuleImpl modules map was not updated",
                ruleEngine.getRule("rule1").getModule("actionId3"));
    }

    /**
     * test rule triggers
     *
     */
    @Test
    public void testRuleTriggers() {
        RuleImpl rule1 = createRule();
        List<Trigger> triggers = new ArrayList<Trigger>(rule1.getTriggers());
        ruleRegistry.add(rule1);
        Rule rule1Get = ruleEngine.getRule("rule1");
        List<Trigger> triggersGet = rule1Get.getTriggers();
        Assert.assertNotNull("Null triggers list", triggersGet);
        Assert.assertEquals("Empty triggers list", 1, triggersGet.size());
        Assert.assertEquals("Returned triggers list should not be a copy", triggersGet, rule1Get.getTriggers());

        triggers.add(ModuleBuilder.createTrigger().withId("triggerId2").withTypeUID("typeUID2").build());
        rule1.setTriggers(triggers);
        ruleEngine.addRule(rule1); // ruleEngine.update will update the
                                   // RuleImpl2.moduleMap with the new
                                   // module
        Rule rule2Get = ruleEngine.getRule("rule1");
        List<Trigger> triggersGet2 = rule2Get.getTriggers();
        Assert.assertNotNull("Null triggers list", triggersGet2);
        Assert.assertEquals("Trigger was not added to the rule's list of triggers", 2, triggersGet2.size());
        Assert.assertEquals("Returned triggers list should not be a copy", triggersGet2, rule2Get.getTriggers());
        Assert.assertNotNull("RuleImpl trigger with wrong id is returned: " + triggersGet2,
                rule2Get.getModule("triggerId2"));
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
        Assert.assertNotNull("Null conditions list", conditionsGet);
        Assert.assertEquals("Empty conditions list", 1, conditionsGet.size());
        Assert.assertEquals("Returned conditions list should not be a copy", conditionsGet, rule1Get.getConditions());

        conditions.add(ModuleBuilder.createCondition().withId("conditionId2").withTypeUID("typeUID2").build());
        rule1.setConditions(conditions);
        ruleEngine.addRule(rule1); // ruleEngine.update will update the RuleImpl2.moduleMap with the new module
        Rule rule2Get = ruleEngine.getRule("rule1");
        List<Condition> conditionsGet2 = rule2Get.getConditions();
        Assert.assertNotNull("Null conditions list", conditionsGet2);
        Assert.assertEquals("Condition was not added to the rule's list of conditions", 2, conditionsGet2.size());
        Assert.assertEquals("Returned conditions list should not be a copy", conditionsGet2, rule2Get.getConditions());
        Assert.assertNotNull("RuleImpl condition with wrong id is returned: " + conditionsGet2,
                rule2Get.getModule("conditionId2"));
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
        List<Trigger> triggers = new ArrayList<Trigger>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        triggers.add(ModuleBuilder.createTrigger().withId("triggerId").withTypeUID(type)
                .withConfiguration(configurations).build());
        return triggers;
    }

    private List<Condition> createConditions(String type) {
        List<Condition> conditions = new ArrayList<Condition>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        Map<String, String> inputs = new HashMap<String, String>(11);
        String ouputModuleId = "triggerId";
        String outputName = "triggerOutput";
        String inputName = "conditionInput";
        inputs.put(inputName, ouputModuleId + "." + outputName);
        conditions.add(ModuleBuilder.createCondition().withId("conditionId").withTypeUID(type)
                .withConfiguration(configurations).withInputs(inputs).build());
        return conditions;
    }

    private List<Action> createActions(String type) {
        List<Action> actions = new ArrayList<Action>();
        Configuration configurations = new Configuration();
        configurations.put("a", "x");
        configurations.put("b", "y");
        configurations.put("c", "z");
        Map<String, String> inputs = new HashMap<String, String>(11);
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
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<ConfigDescriptionParameter>();
        List<ParameterOption> options = new ArrayList<ParameterOption>();
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

        List<FilterCriteria> filter = new ArrayList<FilterCriteria>();

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
