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
package org.eclipse.smarthome.automation.sample.moduletype.demo.internal.handlers;

import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.sample.moduletype.demo.internal.factory.HandlerFactory;

/**
 * This class is handler for 'ConsolePrintAction' {@link Action}.
 *
 * <pre>
 * Example usage:
 *
 * "id":"RuleAction",
 * "type":"ConsolePrintAction",
 * "inputs":{
 *    "inputValue":"RuleTrigger.outputValue"
 * }
 * </pre>
 *
 * This handler prints to standard output {@link Action}'s typeUID + the UID of the rule for which is created.
 * 
 * @author Plamen Peev - Initial contribution
 */
public class ConsolePrintAction extends BaseModuleHandler<Action> implements ActionHandler {

    /**
     * This constant is used by {@link HandlerFactory} to create a correct handler instance. It must be the same as in
     * JSON definition of the module type.
     */
    public static final String UID = "ConsolePrintAction";

    /**
     * This constant contains the name of the input for this {@link Action} handler.
     */
    private static final String INPUT_NAME = "inputValue";

    /**
     * Contains the ID of the {@link Rule} for which this handler is created.
     */
    private final String ruleUID;

    /**
     * Constructs a {@link ConsolePrintAction} instance.
     *
     * @param module - the {@link Action} for which the instance is created.
     */
    public ConsolePrintAction(Action module, String ruleUID) {
        super(module);

        if (module == null) {
            throw new IllegalArgumentException("'module' can not be null.");
        }
        if (ruleUID == null) {
            throw new IllegalArgumentException("'ruleUID' can not be null.");
        }
        this.ruleUID = ruleUID;
    }

    /**
     * The Method is called by the RuleEngine to execute a {@link Rule} {@link Action}.
     *
     *
     * @param context contains action input values and snapshot of all module output values. The output ids are defined
     *            in form: ModuleId.outputId
     * @return values map of values which must be set to outputs of the {@link Action}.
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        final Integer inputValue = (Integer) context.get(INPUT_NAME);
        System.out
                .println("Type UID: " + module.getTypeUID() + ", rule ID " + ruleUID + ", Input value: " + inputValue);

        return null;
    }

}
