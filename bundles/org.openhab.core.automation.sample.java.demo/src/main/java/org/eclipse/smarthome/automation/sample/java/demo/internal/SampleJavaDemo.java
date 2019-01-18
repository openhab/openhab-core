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
package org.eclipse.smarthome.automation.sample.java.demo.internal;

import java.util.ArrayList;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This class shows how to create a rule, using the Java API.It also shows how to add it to the rule engine via
 * RuleRegistry interface.
 *
 * @author Plamen Peev - Initial contribution
 */
@Component(immediate = true)
public class SampleJavaDemo {

    /**
     * Reference to a {@link RuleRegistry} service. The demo uses the service to add and remove a rule.
     */
    private static RuleRegistry ruleRegistry;

    /**
     * The UID of the added {@link Rule}.
     */
    private static final String RULE_UID = "JavaDemoRule";

    void addRule() {
        final Configuration triggerConfig = new Configuration();
        triggerConfig.put("itemName", "DemoSwitch");
        final Trigger ruleTrigger = ModuleBuilder.createTrigger().withId("RuleTrigger")
                .withTypeUID("ItemStateChangeTrigger").withConfiguration(triggerConfig).build();

        final Configuration actionConfig = new Configuration();
        actionConfig.put("itemName", "DemoDimmer");
        actionConfig.put("command", "ON");
        final Action ruleAction = ModuleBuilder.createAction().withId("RuleAction").withTypeUID("ItemPostCommandAction")
                .withConfiguration(actionConfig).build();

        final ArrayList<Trigger> triggers = new ArrayList<Trigger>();
        triggers.add(ruleTrigger);
        final ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(ruleAction);
        final Rule r = RuleBuilder.create(RULE_UID).withTriggers(triggers).withActions(actions)
                .withVisibility(Visibility.VISIBLE).withName("DemoRule").build();
        ruleRegistry.add(r);
    }

    /**
     * Called from DS when all of the required services are available.
     *
     * @param componentContext - the component's context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        addRule();
    }

    /**
     * Called from DS when one of the required services becomes unavailable.
     *
     * @param componentContext - the component's context.
     */
    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        SampleJavaDemo.ruleRegistry.remove(RULE_UID);
    }

    /**
     * Bind the {@link RuleRegistry} service - called from DS.
     *
     * @param ruleRegistry: RuleRegistry service.
     */
    @Reference
    protected void setRuleRegistry(RuleRegistry ruleRegistry) {
        SampleJavaDemo.ruleRegistry = ruleRegistry;
    }

    /**
     * Unbind the {@link RuleRegistry} service - called from DS.
     *
     * @param ruleRegistry: RuleRegistry service.
     */
    protected void unsetRuleRegistry(RuleRegistry ruleRegistry) {
        SampleJavaDemo.ruleRegistry = null;
    }
}
