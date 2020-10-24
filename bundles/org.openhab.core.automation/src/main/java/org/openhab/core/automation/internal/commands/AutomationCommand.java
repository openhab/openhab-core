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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class is base for all automation commands. It defines common functionality for an automation command. Each class
 * of commands is responsible for a group of commands, that are equivalent but each of them is related to a different
 * provider.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public abstract class AutomationCommand {

    /**
     * This constant is used as a part of the string representing understandable for the user message containing
     * information for the success of the command.
     */
    protected static final String SUCCESS = "SUCCESS";

    /**
     * This constant is used as a part of the string representing understandable for the user message containing
     * information for the failure of the command.
     */
    protected static final String FAIL = "FAIL";

    /**
     * This constant is used for detection of <tt>PrintStackTrace</tt> option. If some of the parameters of the command
     * is equal to this constant, then the option is present.
     */
    protected static final String OPTION_ST = "-st";

    /**
     * This field is an indicator of presence of <tt>PrintStackTrace</tt> option. Its value is <b>true</b> if the
     * option is present and <b>false</b> in the opposite case.
     */
    protected boolean st = false;

    /**
     * This field keeps the result of parsing the parameters and options of the command.
     */
    protected String parsingResult;

    /**
     * This field keeps the identifier of the command because each class of commands is responsible for a group
     * of commands.
     */
    protected String command;

    /**
     * This field keeps information about which provider is responsible for execution of the command.
     */
    protected int providerType;

    /**
     * This field keeps a reference to the particular implementation of the <tt>AutomationCommandsPluggable</tt>.
     */
    protected AutomationCommandsPluggable autoCommands;

    /**
     * This constructor is responsible for initializing the common properties for each automation command.
     *
     * @param command is the identifier of the command.
     * @param parameterValues is an array of strings which are basis for initializing the options and parameters of the
     *            command. The order for their description is a random.
     * @param providerType is which provider is responsible for execution of the command.
     * @param autoCommands a reference to the particular implementation of the <tt>AutomationCommandsPluggable</tt>.
     */
    public AutomationCommand(String command, String[] parameterValues, int providerType,
            AutomationCommandsPluggable autoCommands) {
        this.command = command;
        this.providerType = providerType;
        this.autoCommands = autoCommands;
        parsingResult = parseOptionsAndParameters(parameterValues);
    }

    /**
     * This method is common for all automation commands and it is responsible for execution of every particular
     * command.
     *
     * @return a string representing understandable for the user message containing information on the outcome of the
     *         command.
     */
    public abstract String execute();

    /**
     * This method is responsible for processing the <tt>st</tt> option of the command.
     *
     * @param e is the exception appeared during the command's execution.
     * @return a string representing the exception, corresponding to availability of the <tt>st</tt> option.
     */
    protected String getStackTrace(Exception e) {
        StringBuilder writer = new StringBuilder();
        if (st) {
            StackTraceElement[] ste = e.getStackTrace();
            for (int i = 0; i < ste.length; i++) {
                if (i == 0) {
                    writer.append(String.format("FAIL : %s", ste[i].toString() + "\n"));
                } else {
                    writer.append(String.format("%s", ste[i].toString() + "\n"));
                }
            }
        } else {
            writer.append(String.format("FAIL : %s", e.getMessage() + "\n"));
        }
        return writer.toString();
    }

    /**
     * This method is used to determine the options and parameters for every particular command. If there are redundant
     * options and parameters or the required are missing the execution of the command will be ended and the parsing
     * result will be returned as a result of the command.
     *
     * @param parameterValues is an array of strings which are basis for initializing the options and parameters of the
     *            command. The order for their description is a random.
     * @return a string representing understandable for the user message containing information on the outcome of the
     *         parsing the parameters and options.
     */
    protected abstract String parseOptionsAndParameters(String[] parameterValues);
}
