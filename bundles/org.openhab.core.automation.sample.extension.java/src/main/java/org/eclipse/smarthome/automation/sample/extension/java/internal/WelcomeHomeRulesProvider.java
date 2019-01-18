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
package org.eclipse.smarthome.automation.sample.extension.java.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleProvider;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.sample.extension.java.internal.template.AirConditionerRuleTemplate;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.LightsTriggerType;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.StateConditionType;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.TemperatureConditionType;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.WelcomeHomeActionType;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This class presents simple implementation of {@link RuleProvider} interface. It provides four rules that give ability
 * to the user to switch on the air conditioner and lights, and to lower the blinds in its home remotely.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeRulesProvider implements RuleProvider {

    public static final String CONFIG_UNIT = "unit";
    public static final String CONFIG_EXPECTED_RESULT = "expectedResult";

    static final String AC_UID = "AirConditionerSwitchOnRule";
    static final String L_UID = "LightsSwitchOnRule";

    Map<String, Rule> rules;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration providerReg;
    private Collection<ProviderChangeListener<Rule>> listeners;

    /**
     * This method is used to initialize the provided rules by using templates and from scratch.
     * The configuration of the rule created by template should contain as keys all required parameter names of the
     * configuration of the template and their values.
     * In this example the UIDs of the rules is given by the provider, but can be <code>null</code>.
     * Then the RuleManager will generate the UID for each provided rule.
     */
    public WelcomeHomeRulesProvider() {
        // initialize the "AirConditionerSwitchOnRule" rule from template by using UID, templateUID and configuration.
        Rule acSwitchOn = createACRule();

        // initialize the "LightsSwitchOnRule" rule from scratch by using trigger, condition, action, configDescriptions
        // and configuration, tags.
        Rule lightsSwitchOn = createLightsRule();

        rules = new HashMap<String, Rule>();
        rules.put(AC_UID, acSwitchOn);
        rules.put(L_UID, lightsSwitchOn);
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
        if (listeners == null) {
            listeners = new ArrayList<ProviderChangeListener<Rule>>();
        }
        listeners.add(listener); // keep all listeners, interested about changing the rules to can inform them if
                                 // there is change on some rules
    }

    @Override
    public Collection<Rule> getAll() {
        return rules.values(); // adding the provided rules into RuleManager
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * This method is used to update the provided rules configuration.
     *
     * @param uid
     *            specifies the rule for updating by UID
     * @param template
     *            specifies the rule template by UID
     * @param config
     *            gives the new configuration of the rule
     */
    public void update(String uid, String template, Configuration config) {
        // specific for this application
        Rule oldelement = rules.get(uid);
        Rule element = RuleBuilder.create(uid).withTemplateUID(template).withConfiguration(config).build();
        rules.put(uid, element);

        // inform all listeners, interested about changing of the rules
        for (ProviderChangeListener<Rule> listener : listeners) {
            listener.updated(this, oldelement, element);
        }
    }

    /**
     * This method is used for registration of the WelcomeHomeRulesProvider as a {@link RuleProvider} service.
     *
     * @param bc
     *            is a bundle's execution context within the Framework.
     */
    public void register(BundleContext bc) {
        providerReg = bc.registerService(RuleProvider.class.getName(), this, null);
    }

    /**
     * This method is used to unregister the WelcomeHomeRulesProvider service.
     */
    public void unregister() {
        providerReg.unregister();
        providerReg = null;
        rules = null;
    }

    /**
     * This method creates a rule from template by using UID, templateUID and configuration.
     *
     * @return the created rule
     */
    private Rule createACRule() {
        Configuration config = new Configuration();
        config.put(CONFIG_UNIT, "Air Conditioner");
        config.put(CONFIG_EXPECTED_RESULT, "The air conditioner is switched on.");
        config.put(AirConditionerRuleTemplate.CONFIG_TARGET_TEMPERATURE, new Integer(18));
        config.put(AirConditionerRuleTemplate.CONFIG_OPERATION, TemperatureConditionType.OPERATOR_HEATING);
        Rule rule = RuleBuilder.create(AC_UID).withTemplateUID(AirConditionerRuleTemplate.UID).withConfiguration(config)
                .build();
        return rule;
    }

    /**
     * This method creates a rule from scratch by using trigger, condition, action, configDescriptions and
     * configuration, tags.
     *
     * @return the created rule
     */
    private Rule createLightsRule() {
        // initialize the trigger
        String triggerId = "LightsSwitchOnRuleTrigger";
        List<Trigger> triggers = new ArrayList<Trigger>();
        triggers.add(ModuleBuilder.createTrigger().withId(triggerId).withTypeUID(LightsTriggerType.UID).build());

        // initialize the condition - here the tricky part is the referring into the condition input - trigger output.
        // The syntax is a similar to the JUEL syntax.
        Configuration config = new Configuration();
        config.put(StateConditionType.CONFIG_STATE, "on");
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, String> inputs = new HashMap<String, String>();
        inputs.put(StateConditionType.INPUT_CURRENT_STATE, triggerId + "." + StateConditionType.INPUT_CURRENT_STATE);
        conditions.add(ModuleBuilder.createCondition().withId("LightsStateCondition")
                .withTypeUID(StateConditionType.UID).withConfiguration(config).withInputs(inputs).build());

        // initialize the action - here the tricky part is the referring into the action configuration parameter - the
        // template configuration parameter. The syntax is a similar to the JUEL syntax.
        config = new Configuration();
        config.put(WelcomeHomeActionType.CONFIG_DEVICE, "Lights");
        config.put(WelcomeHomeActionType.CONFIG_RESULT, "Lights are switched on");
        List<Action> actions = new ArrayList<Action>();
        actions.add(ModuleBuilder.createAction().withId("LightsSwitchOnAction").withTypeUID(WelcomeHomeActionType.UID)
                .withConfiguration(config).build());

        // initialize the configDescriptions
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<ConfigDescriptionParameter>();
        final ConfigDescriptionParameter device = ConfigDescriptionParameterBuilder
                .create(WelcomeHomeRulesProvider.CONFIG_UNIT, Type.TEXT).withRequired(true).withReadOnly(true)
                .withMultiple(false).withLabel("Device").withDescription("Device description").build();
        final ConfigDescriptionParameter result = ConfigDescriptionParameterBuilder
                .create(WelcomeHomeRulesProvider.CONFIG_EXPECTED_RESULT, Type.TEXT).withRequired(true)
                .withReadOnly(true).withMultiple(false).withLabel("Result").withDescription("Result description")
                .build();
        configDescriptions.add(device);
        configDescriptions.add(result);

        // initialize the configuration
        config = new Configuration();
        config.put(CONFIG_UNIT, "Lights");
        config.put(CONFIG_EXPECTED_RESULT, "The lights are switched on.");

        // create the rule
        Rule lightsSwitchOn = RuleBuilder.create(L_UID).withTriggers(triggers)
                .withConfigurationDescriptions(configDescriptions).withConditions(conditions).withActions(actions)
                .withTags("lights").build();

        return lightsSwitchOn;
    }

}
