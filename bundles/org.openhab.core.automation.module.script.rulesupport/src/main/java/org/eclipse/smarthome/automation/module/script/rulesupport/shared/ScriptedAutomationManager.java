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
package org.eclipse.smarthome.automation.module.script.rulesupport.shared;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ActionBuilder;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedCustomModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedCustomModuleTypeProvider;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedPrivateModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleRuleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleRuleActionHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Registry is used for a single ScriptEngine instance. It allows the adding and removing of handlers.
 * It allows the removal of previously added modules on unload.
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptedAutomationManager {
    private final Logger logger = LoggerFactory.getLogger(ScriptedAutomationManager.class);

    private final RuleSupportRuleRegistryDelegate ruleRegistryDelegate;

    private final HashSet<String> modules = new HashSet<>();
    private final HashSet<String> moduleHandlers = new HashSet<>();
    private final HashSet<String> privateHandlers = new HashSet<>();

    private final ScriptedCustomModuleHandlerFactory scriptedCustomModuleHandlerFactory;
    private final ScriptedCustomModuleTypeProvider scriptedCustomModuleTypeProvider;
    private final ScriptedPrivateModuleHandlerFactory scriptedPrivateModuleHandlerFactory;

    public ScriptedAutomationManager(RuleSupportRuleRegistryDelegate ruleRegistryDelegate,
            ScriptedCustomModuleHandlerFactory scriptedCustomModuleHandlerFactory,
            ScriptedCustomModuleTypeProvider scriptedCustomModuleTypeProvider,
            ScriptedPrivateModuleHandlerFactory scriptedPrivateModuleHandlerFactory) {
        this.ruleRegistryDelegate = ruleRegistryDelegate;
        this.scriptedCustomModuleHandlerFactory = scriptedCustomModuleHandlerFactory;
        this.scriptedCustomModuleTypeProvider = scriptedCustomModuleTypeProvider;
        this.scriptedPrivateModuleHandlerFactory = scriptedPrivateModuleHandlerFactory;
    }

    public void removeModuleType(String UID) {
        if (modules.remove(UID)) {
            scriptedCustomModuleTypeProvider.removeModuleType(UID);
            removeHandler(UID);
        }
    }

    public void removeHandler(String typeUID) {
        if (moduleHandlers.remove(typeUID)) {
            scriptedCustomModuleHandlerFactory.removeModuleHandler(typeUID);
        }
    }

    public void removePrivateHandler(String privId) {
        if (privateHandlers.remove(privId)) {
            scriptedPrivateModuleHandlerFactory.removeHandler(privId);
        }
    }

    public void removeAll() {
        logger.info("removeAll added handlers");

        HashSet<String> types = new HashSet<>(modules);
        for (String moduleType : types) {
            removeModuleType(moduleType);
        }

        HashSet<String> moduleHandlers = new HashSet<>(this.moduleHandlers);
        for (String uid : moduleHandlers) {
            removeHandler(uid);
        }

        HashSet<String> privateHandlers = new HashSet<>(this.privateHandlers);
        for (String privId : privateHandlers) {
            removePrivateHandler(privId);
        }

        ruleRegistryDelegate.removeAllAddedByScript();
    }

    public Rule addRule(Rule element) {
        RuleBuilder builder = RuleBuilder.create(element.getUID());

        String name = element.getName();
        if (name == null || name.isEmpty()) {
            name = element.getClass().getSimpleName();
            if (name.contains("$")) {
                name = name.substring(0, name.indexOf('$'));
            }
        }

        builder.withName(name).withDescription(element.getDescription()).withTags(element.getTags());

        // used for numbering the modules of the rule
        int moduleIndex = 1;

        try {
            ArrayList<Condition> conditions = new ArrayList<>();
            for (Condition cond : element.getConditions()) {
                Condition toAdd = cond;
                if (cond.getId().isEmpty()) {
                    toAdd = ModuleBuilder.createCondition().withId(Integer.toString(moduleIndex++))
                            .withTypeUID(cond.getTypeUID()).withConfiguration(cond.getConfiguration())
                            .withInputs(cond.getInputs()).build();
                }

                conditions.add(toAdd);
            }

            builder.withConditions(conditions);
        } catch (Exception ex) {
            // conditions are optional
        }

        try {
            ArrayList<Trigger> triggers = new ArrayList<>();
            for (Trigger trigger : element.getTriggers()) {
                Trigger toAdd = trigger;
                if (trigger.getId().isEmpty()) {
                    toAdd = ModuleBuilder.createTrigger().withId(Integer.toString(moduleIndex++))
                            .withTypeUID(trigger.getTypeUID()).withConfiguration(trigger.getConfiguration()).build();
                }

                triggers.add(toAdd);
            }

            builder.withTriggers(triggers);
        } catch (Exception ex) {
            // triggers are optional
        }

        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(element.getActions());

        if (element instanceof SimpleRuleActionHandler) {
            String privId = addPrivateActionHandler(
                    new SimpleRuleActionHandlerDelegate((SimpleRuleActionHandler) element));

            Action scriptedAction = ActionBuilder.create().withId(Integer.toString(moduleIndex++))
                    .withTypeUID("jsr223.ScriptedAction").withConfiguration(new Configuration()).build();
            scriptedAction.getConfiguration().put("privId", privId);
            actions.add(scriptedAction);
        }

        builder.withActions(actions);

        Rule rule = builder.build();

        ruleRegistryDelegate.add(rule);
        return rule;
    }

    public void addConditionType(ConditionType condititonType) {
        modules.add(condititonType.getUID());
        scriptedCustomModuleTypeProvider.addModuleType(condititonType);
    }

    public void addConditionHandler(String uid, ScriptedHandler conditionHandler) {
        moduleHandlers.add(uid);
        scriptedCustomModuleHandlerFactory.addModuleHandler(uid, conditionHandler);
        scriptedCustomModuleTypeProvider.updateModuleHandler(uid);
    }

    public String addPrivateConditionHandler(SimpleConditionHandler conditionHandler) {
        String uid = scriptedPrivateModuleHandlerFactory.addHandler(conditionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    public void addActionType(ActionType actionType) {
        modules.add(actionType.getUID());
        scriptedCustomModuleTypeProvider.addModuleType(actionType);
    }

    public void addActionHandler(String uid, ScriptedHandler actionHandler) {
        moduleHandlers.add(uid);
        scriptedCustomModuleHandlerFactory.addModuleHandler(uid, actionHandler);
        scriptedCustomModuleTypeProvider.updateModuleHandler(uid);
    }

    public String addPrivateActionHandler(SimpleActionHandler actionHandler) {
        String uid = scriptedPrivateModuleHandlerFactory.addHandler(actionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    public void addTriggerType(TriggerType triggerType) {
        modules.add(triggerType.getUID());
        scriptedCustomModuleTypeProvider.addModuleType(triggerType);
    }

    public void addTriggerHandler(String uid, ScriptedHandler triggerHandler) {
        moduleHandlers.add(uid);
        scriptedCustomModuleHandlerFactory.addModuleHandler(uid, triggerHandler);
        scriptedCustomModuleTypeProvider.updateModuleHandler(uid);
    }

    public String addPrivateTriggerHandler(SimpleTriggerHandler triggerHandler) {
        String uid = scriptedPrivateModuleHandlerFactory.addHandler(triggerHandler);
        privateHandlers.add(uid);
        return uid;
    }
}
