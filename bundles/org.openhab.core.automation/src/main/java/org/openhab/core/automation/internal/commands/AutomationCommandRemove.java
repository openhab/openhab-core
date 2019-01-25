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
package org.openhab.core.automation.internal.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openhab.core.automation.Rule;

/**
 * This class provides common functionality of commands:
 * <ul>
 * <li>{@link AutomationCommands#REMOVE_MODULE_TYPES}
 * <li>{@link AutomationCommands#REMOVE_TEMPLATES}
 * <li>{@link AutomationCommands#REMOVE_RULES}
 * <li>{@link AutomationCommands#REMOVE_RULE}
 * </ul>
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - fixed feedback when deleting non-existent rule
 * @author Marin Mitev - removed prefixes in the output
 *
 */
public class AutomationCommandRemove extends AutomationCommand {

    /**
     * This field keeps the UID of the {@link Rule} if command is {@link AutomationCommands#REMOVE_RULE}
     */
    private String id;

    /**
     * This field keeps URL of the source of automation objects that has to be removed.
     */
    private URL url;

    /**
     * @see AutomationCommand#AutomationCommand(String, String[], int, AutomationCommandsPluggable)
     */
    public AutomationCommandRemove(String command, String[] params, int providerType,
            AutomationCommandsPluggable autoCommands) {
        super(command, params, providerType, autoCommands);
    }

    /**
     * This method is responsible for execution of commands:
     * <ul>
     * <li>{@link AutomationCommands#REMOVE_MODULE_TYPES}
     * <li>{@link AutomationCommands#REMOVE_TEMPLATES}
     * <li>{@link AutomationCommands#REMOVE_RULES}
     * <li>{@link AutomationCommands#REMOVE_RULE}
     * </ul>
     */
    @Override
    public String execute() {
        if (parsingResult != SUCCESS) {
            return parsingResult;
        }
        switch (providerType) {
            case AutomationCommands.MODULE_TYPE_PROVIDER:
                return autoCommands.remove(AutomationCommands.MODULE_TYPE_PROVIDER, url);
            case AutomationCommands.TEMPLATE_PROVIDER:
                return autoCommands.remove(AutomationCommands.TEMPLATE_PROVIDER, url);
            case AutomationCommands.RULE_PROVIDER:
                if (command == AutomationCommands.REMOVE_RULE) {
                    return autoCommands.removeRule(id);
                } else if (command == AutomationCommands.REMOVE_RULES) {
                    return autoCommands.removeRules(id);
                }
        }
        return FAIL;
    }

    /**
     * This method serves to create an {@link URL} object or {@link File} object from a string that is passed as
     * a parameter of the command. From the {@link File} object the URL is constructed.
     *
     * @param parameterValue is a string that is passed as parameter of the command and it supposed to be an URL
     *            representation.
     * @return an {@link URL} object created from the string that is passed as parameter of the command or <b>null</b>
     *         if either no legal protocol could be found in the specified string or the string could not be parsed.
     */
    private URL initURL(String parameterValue) {
        try {
            return new URL(parameterValue);
        } catch (MalformedURLException mue) {
            File f = new File(parameterValue);
            if (f.isFile()) {
                try {
                    return f.toURI().toURL();
                } catch (MalformedURLException e) {
                }
            }
        }
        return null;
    }

    /**
     * This method is invoked from the constructor to parse all parameters and options of the command <b>REMOVE</b>.
     * If there are redundant parameters or options or the required are missing the result will be the failure of the
     * command. This command has:
     * <ul>
     * <b>Options:</b>
     * <ul>
     * <li><b>PrintStackTrace</b> is common for all commands and its presence triggers printing of stack trace in case
     * of exception.
     * </ul>
     * </ul>
     * <ul>
     * <b>Parameters:</b>
     * <ul>
     * <li><b>id</b> is required for {@link AutomationCommands#REMOVE_RULE} command. If it is present for all
     * <b>REMOVE</b> commands, except {@link AutomationCommands#REMOVE_RULE}, it will be treated as redundant.
     * <li><b>url</b> is required for all <b>REMOVE</b> commands, except {@link AutomationCommands#REMOVE_RULE}.
     * If it is present for {@link AutomationCommands#REMOVE_RULE}, it will be treated as redundant.
     * </ul>
     * </ul>
     */
    @Override
    protected String parseOptionsAndParameters(String[] parameterValues) {
        boolean getUrl = true;
        boolean getId = true;
        if (providerType == AutomationCommands.RULE_PROVIDER) {
            getUrl = false;
        } else {
            getId = false;
        }
        for (int i = 0; i < parameterValues.length; i++) {
            if (null == parameterValues[i]) {
                continue;
            }
            if (parameterValues[i].equals(OPTION_ST)) {
                st = true;
            } else if (parameterValues[i].charAt(0) == '-') {
                return String.format("Unsupported option: %s", parameterValues[i]);
            } else if (getUrl) {
                url = initURL(parameterValues[i]);
                if (url != null) {
                    getUrl = false;
                }
            } else if (getId) {
                id = parameterValues[i];
                if (id != null) {
                    getId = false;
                }
            } else {
                return String.format("Unsupported parameter: %s", parameterValues[i]);
            }
        }
        if (getUrl) {
            return "Missing source URL parameter!";
        }
        if (getId) {
            return "Missing UID parameter!";
        }
        return SUCCESS;
    }

}
