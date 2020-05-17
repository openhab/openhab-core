/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.internal.ScriptedCustomModuleHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.internal.ScriptedCustomModuleTypeProvider;
import org.openhab.core.automation.module.script.rulesupport.internal.ScriptedPrivateModuleHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRuleActionHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRuleActionHandlerDelegate;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.automation.util.ActionBuilder;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.Configuration;

/**
 * This Registry is used for a single ScriptEngine instance. It allows the adding and removing of handlers.
 * It allows the removal of previously added modules on unload.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class ScriptedAutomationManager {

    private final RuleSupportRuleRegistryDelegate ruleRegistryDelegate;

    private final Set<String> modules = new HashSet<>();
    private final Set<String> moduleHandlers = new HashSet<>();
    private final Set<String> privateHandlers = new HashSet<>();

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
        Set<String> types = new HashSet<>(modules);
        for (String moduleType : types) {
            removeModuleType(moduleType);
        }

        Set<String> moduleHandlers = new HashSet<>(this.moduleHandlers);
        for (String uid : moduleHandlers) {
            removeHandler(uid);
        }

        Set<String> privateHandlers = new HashSet<>(this.privateHandlers);
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
            List<Condition> conditions = new ArrayList<>();
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
            List<Trigger> triggers = new ArrayList<>();
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

        List<Action> actions = new ArrayList<>();
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
