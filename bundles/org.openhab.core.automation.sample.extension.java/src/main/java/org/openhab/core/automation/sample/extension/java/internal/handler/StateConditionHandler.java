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
package org.openhab.core.automation.sample.extension.java.internal.handler;

import java.util.Map;

import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.sample.extension.java.internal.type.StateConditionType;

/**
 * This class serves to handle the Condition types provided by this application. It is used to help the RuleManager
 * to decide to continue with the execution of the rule or to terminate it.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class StateConditionHandler extends BaseModuleHandler<Condition> implements ConditionHandler {

    public StateConditionHandler(Condition module) {
        super(module);
    }

    @Override
    public boolean isSatisfied(Map<String, Object> context) {
        String leftOperand = (String) context.get(StateConditionType.INPUT_CURRENT_STATE);
        String rightOperand = (String) module.getConfiguration().get(StateConditionType.CONFIG_STATE);
        if (rightOperand != null && !rightOperand.equalsIgnoreCase(leftOperand)) {
            return true;
        }
        return false;
    }

}
