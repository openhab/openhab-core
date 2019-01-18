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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.automation.sample.extension.java.internal.handler.WelcomeHomeHandlerFactory;
import org.eclipse.smarthome.automation.sample.extension.java.internal.handler.WelcomeHomeTriggerHandler;
import org.eclipse.smarthome.automation.sample.extension.java.internal.template.AirConditionerRuleTemplate;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.StateConditionType;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.TemperatureConditionType;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This class serves as a simple user interface. User can use it to configure the provided rules,
 * to trigger their execution or to discard the requested execution if it is still pending.
 * It is written like this, to allow the user to test the behavior without need to write additional code.
 * Can be replaced by GUI or another service, which can deliver the necessary information to allow the rules to work.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeCommands extends AbstractConsoleCommandExtension {

    private static final String CMD = "welcomehome";
    private static final String DESC = "Welcome Home Application Commands Group";

    private static final String COMMAND_SETTINGS = "settings";
    private static final String COMMAND_SETTINGS_SHORT = "set";
    private static final String COMMAND_ACTIVATE_AC = "activateAC";
    private static final String COMMAND_ACTIVATE_AC_SHORT = "aAC";
    private static final String COMMAND_ACTIVATE_L = "activateLights";
    private static final String COMMAND_ACTIVATE_L_SHORT = "aL";

    private WelcomeHomeRulesProvider rulesProvider;
    private WelcomeHomeHandlerFactory handlerFactory;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration commandsServiceReg;
    private Integer currentTemperature;
    private String currentState;

    /**
     * This method is used to initialize the commands service, provider of the rules and factory for the handlers of the
     * modules that compose the rules.
     *
     * @param context
     *            is a bundle's execution context within the Framework.
     */
    public WelcomeHomeCommands(BundleContext bc, WelcomeHomeRulesProvider rulesProvider,
            WelcomeHomeHandlerFactory handlerFactory) {
        super(CMD, DESC);
        this.rulesProvider = rulesProvider;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length == 0) {
            console.println(StringUtils.join(getUsages(), "\n"));
            return;
        }

        String command = args[0];// the first argument is the subcommand name

        String[] params = new String[args.length - 1];// extract the remaining arguments except the first one
        if (params.length > 0) {
            System.arraycopy(args, 1, params, 0, params.length);
        }
        if (COMMAND_SETTINGS.equalsIgnoreCase(command) || COMMAND_SETTINGS_SHORT.equalsIgnoreCase(command)) {
            settings(params, console);
        } else if (COMMAND_ACTIVATE_AC.equalsIgnoreCase(command)
                || COMMAND_ACTIVATE_AC_SHORT.equalsIgnoreCase(command)) {
            activate(params, console);
        } else if (COMMAND_ACTIVATE_L.equalsIgnoreCase(command) || COMMAND_ACTIVATE_L_SHORT.equalsIgnoreCase(command)) {
            activateLights(params, console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(COMMAND_SETTINGS + "(" + COMMAND_SETTINGS_SHORT + ") <mode> <temperature>",
                        "Configures air conditioner's mode(cooling/heating) and target temperature."),
                buildCommandUsage(
                        COMMAND_ACTIVATE_AC + "(" + COMMAND_ACTIVATE_AC_SHORT + ") <currentState> <currentTemperature>",
                        "Activates Switch On the Air Conditioner by providing its current state(on/off),"
                                + " and current room temperature."),
                buildCommandUsage(COMMAND_ACTIVATE_L + "(" + COMMAND_ACTIVATE_L_SHORT + ") <currentState>",
                        "Activates Switch On the Lights by providing their current state(on/off).") });
    }

    /**
     * This method is used to register the commands service, provider of the rules and handlers of the modules that
     * compose the rules.
     *
     * @param context
     *            is a bundle's execution context within the Framework.
     */
    public void register(BundleContext context) {
        commandsServiceReg = context.registerService(ConsoleCommandExtension.class.getName(), this, null);
    }

    /**
     * This method is used to unregister the commands service, provider of the rules and handlers of the modules that
     * compose the rules.
     */
    public void unregister() {
        if (commandsServiceReg != null) {
            commandsServiceReg.unregister();
        }
        commandsServiceReg = null;
        rulesProvider = null;
        handlerFactory = null;
    }

    /**
     * This method is used to schedule the execution of the provided rules. It gives ability to provide external data,
     * that can affect the execution of the rules.
     *
     * @param params
     *            provides external data, that can affect the execution of the rules.
     * @param console
     *            provides the output of the command.
     */
    private void activate(String[] params, Console console) {
        // parsing command parameter values
        if (params.length < 2) {
            console.println("Missing required parameters");
            return;
        }
        if (params[0] != null && (params[0].equalsIgnoreCase("on") || params[0].equalsIgnoreCase("off"))) {
            currentState = params[0];
        } else {
            console.println("Invalid parameter value of the parameter \"currentState\". Should be \"on\" or \"off\"");
            return;
        }

        try {
            currentTemperature = new Integer(params[1]);
        } catch (NumberFormatException e) {
            console.println("Invalid parameter value of the parameter \"currentTemperature\". Should be number.");
            return;
        }

        // initialize the output of the trigger of the rule WelcomeHomeRulesProvider.AC_UID
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(TemperatureConditionType.INPUT_CURRENT_TEMPERATURE, currentTemperature);
        context.put(StateConditionType.INPUT_CURRENT_STATE, currentState);

        // causes the execution of the rule WelcomeHomeRulesProvider.AC_UID
        WelcomeHomeTriggerHandler handler = handlerFactory.getTriggerHandler(WelcomeHomeRulesProvider.AC_UID);
        if (handler != null) {
            handler.trigger(context);
        }

        console.println("SUCCESS");
    }

    private void activateLights(String[] params, Console console) {
        // parsing command parameter values
        if (params.length < 1) {
            console.println("Missing required parameters");
            return;
        }
        if (params[0] != null && (params[0].equalsIgnoreCase("on") || params[0].equalsIgnoreCase("off"))) {
            currentState = params[0];
        } else {
            console.println("Invalid parameter value of the parameter \"currentState\". Should be \"on\" or \"off\"");
            return;
        }

        // initialize the output of the trigger of the rule WelcomeHomeRulesProvider.L_UID
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(StateConditionType.INPUT_CURRENT_STATE, currentState);

        // causes the execution of the rule WelcomeHomeRulesProvider.LRL_UID
        WelcomeHomeTriggerHandler handler = handlerFactory.getTriggerHandler(WelcomeHomeRulesProvider.L_UID);
        if (handler != null) {
            handler.trigger(context);
        }

        console.println("SUCCESS");
    }

    /**
     * This method is used to configure the provided rules.
     *
     * @param params
     *            provides external data, that is used for configuring the rules.
     * @param console
     *            provides the output of the command.
     */
    private void settings(String[] params, Console console) {
        if (params.length < 2) {
            console.println("Missing required parameters");
            return;
        }
        Configuration config = rulesProvider.rules.get(WelcomeHomeRulesProvider.AC_UID).getConfiguration();
        if (params[0] != null && (params[0].equalsIgnoreCase(TemperatureConditionType.OPERATOR_HEATING)
                || params[0].equalsIgnoreCase(TemperatureConditionType.OPERATOR_COOLING))) {
            config.put(AirConditionerRuleTemplate.CONFIG_OPERATION, params[0]);
        } else {
            console.println("Invalid parameter value of the parameter \"mode\". Should be \""
                    + TemperatureConditionType.OPERATOR_HEATING + "\" or \"" + TemperatureConditionType.OPERATOR_COOLING
                    + "\"");
            return;
        }
        if (params[1] != null) {
            Integer temperature;
            try {
                temperature = new Integer(params[1]);
                config.put(AirConditionerRuleTemplate.CONFIG_TARGET_TEMPERATURE, temperature);
            } catch (NumberFormatException e) {
                console.println("Invalid parameter value of the parameter \"temperature\". Should be number.");
                return;
            }
        }
        rulesProvider.update(WelcomeHomeRulesProvider.AC_UID, AirConditionerRuleTemplate.UID, config);
        console.println("SUCCESS");
    }

}
