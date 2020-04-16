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
package org.openhab.core.automation.internal.commands;

import org.openhab.core.automation.RuleStatus;

/**
 * This class provides functionality of command {@link AutomationCommands#ENABLE_RULE}.
 *
 * @author Ana Dimova - Initial contribution
 */
public class AutomationCommandEnableRule extends AutomationCommand {

    /**
     * This field keeps the value of "enable" parameter of the command.
     */
    private boolean enable;

    /**
     * This field indicates the presence of the "enable" parameter of the command.
     */
    private boolean hasEnable;

    /**
     * This field keeps the specified rule UID.
     */
    private String uid;

    public AutomationCommandEnableRule(String command, String[] parameterValues, int providerType,
            AutomationCommandsPluggable autoCommands) {
        super(command, parameterValues, providerType, autoCommands);
    }

    @Override
    public String execute() {
        if (parsingResult != SUCCESS) {
            return parsingResult;
        }
        if (hasEnable) {
            autoCommands.setEnabled(uid, enable);
            return SUCCESS;
        } else {
            RuleStatus status = autoCommands.getRuleStatus(uid);
            if (status != null) {
                return Printer.printRuleStatus(uid, status);
            }
        }
        return FAIL;
    }

    @Override
    protected String parseOptionsAndParameters(String[] parameterValues) {
        for (int i = 0; i < parameterValues.length; i++) {
            if (null == parameterValues[i]) {
                continue;
            }
            if (parameterValues[i].charAt(0) == '-') {
                if (OPTION_ST.equals(parameterValues[i])) {
                    st = true;
                    continue;
                }
                return String.format("Unsupported option: %s", parameterValues[i]);
            }
            if (uid == null) {
                uid = parameterValues[i];
                continue;
            }
            getEnable(parameterValues[i]);
            if (hasEnable) {
                continue;
            }
            if (uid == null) {
                return "Missing required parameter: Rule UID";
            }
            return String.format("Unsupported parameter: %s", parameterValues[i]);
        }
        return SUCCESS;
    }

    /**
     * Utility method for parsing the command parameter - "enable".
     *
     * @param parameterValue is the value entered from command line.
     */
    private void getEnable(String parameterValue) {
        if ("true".equals(parameterValue)) {
            enable = true;
            hasEnable = true;
        } else if ("false".equals(parameterValue)) {
            enable = false;
            hasEnable = true;
        } else {
            hasEnable = false;
        }
    }

}
