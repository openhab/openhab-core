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
package org.eclipse.smarthome.automation.internal.commands;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.automation.parser.Parser;

/**
 * This class provides common functionality of commands:
 * <ul>
 * <li>{@link AutomationCommands#EXPORT_MODULE_TYPES}
 * <li>{@link AutomationCommands#EXPORT_TEMPLATES}
 * <li>{@link AutomationCommands#EXPORT_RULES}
 * </ul>
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class AutomationCommandExport extends AutomationCommand {

    /**
     * This constant is used for detection of <tt>ParserType</tt> parameter. If some of the parameters of the command
     * is equal to this constant, then the <tt>ParserType</tt> parameter is present and its value is the next one.
     */
    private static final String OPTION_P = "-p";

    /**
     * This field keeps the value of the <tt>ParserType</tt> parameter and it is initialized as
     * {@link Parser#FORMAT_JSON} by default.
     */
    private String parserType = Parser.FORMAT_JSON;

    /**
     * This field keeps the path of the output file where the automation objects to be exported.
     */
    private File file;

    /**
     * This field stores the value of <b>locale</b> parameter of the command.
     */
    private Locale locale = Locale.getDefault(); // For now is initialized with the default locale, but when the
                                                 // localization is implemented, it will be initialized with a parameter
                                                 // of the command.

    /**
     * @see AutomationCommand#AutomationCommand(String, String[], int, AutomationCommandsPluggable)
     */
    public AutomationCommandExport(String command, String[] params, int providerType,
            AutomationCommandsPluggable autoCommands) {
        super(command, params, providerType, autoCommands);
    }

    /**
     * This method is responsible for execution of commands:
     * <ul>
     * <li>{@link AutomationCommands#EXPORT_MODULE_TYPES}
     * <li>{@link AutomationCommands#EXPORT_TEMPLATES}
     * <li>{@link AutomationCommands#EXPORT_RULES}
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Override
    public String execute() {
        if (parsingResult != SUCCESS) {
            return parsingResult;
        }
        @SuppressWarnings("rawtypes")
        Set set = new HashSet();
        switch (providerType) {
            case AutomationCommands.MODULE_TYPE_PROVIDER:
                @SuppressWarnings("rawtypes")
                Collection collection = autoCommands.getTriggers(locale);
                if (collection != null) {
                    set.addAll(collection);
                }
                collection = autoCommands.getConditions(locale);
                if (collection != null) {
                    set.addAll(collection);
                }
                collection = autoCommands.getActions(locale);
                if (collection != null) {
                    set.addAll(collection);
                }
                try {
                    return autoCommands.exportModuleTypes(parserType, set, file);
                } catch (Exception e) {
                    return getStackTrace(e);
                }
            case AutomationCommands.TEMPLATE_PROVIDER:
                collection = autoCommands.getTemplates(locale);
                if (collection != null) {
                    set.addAll(collection);
                }
                try {
                    return autoCommands.exportTemplates(parserType, set, file);
                } catch (Exception e) {
                    return getStackTrace(e);
                }
            case AutomationCommands.RULE_PROVIDER:
                collection = autoCommands.getRules();
                if (collection != null) {
                    set.addAll(collection);
                }
                try {
                    return autoCommands.exportRules(parserType, set, file);
                } catch (Exception e) {
                    return getStackTrace(e);
                }
        }
        return String.format("%s : Unsupported provider type!", FAIL);
    }

    /**
     * This method serves to create a {@link File} object from a string that is passed as a parameter of the command.
     *
     * @param parameterValue is a string that is passed as parameter of the command and it supposed to be a file
     *            representation.
     * @return a {@link File} object created from the string that is passed as a parameter of the command or <b>null</b>
     *         if the parent directory could not be found or created or the string could not be parsed.
     */
    private File initFile(String parameterValue) {
        File f = new File(parameterValue);
        File parent = f.getParentFile();
        return (parent == null || (!parent.isDirectory() && !parent.mkdirs())) ? null : f;
    }

    /**
     * This method is invoked from the constructor to parse all parameters and options of the command <b>EXPORT</b>.
     * If there are redundant parameters or options, or the required parameter is missing the result will be the failure
     * of the command. This command has:
     * <ul>
     * <b>Options:</b>
     * <ul>
     * <li><b>PrintStackTrace</b> which is common for all commands
     * </ul>
     * </ul>
     * <ul>
     * <b>Parameters:</b>
     * <ul>
     * <li><b>parserType</b> is optional and by default its value is {@link Parser#FORMAT_JSON}.
     * <li><b>file</b> is required and specifies the path to the file for export.
     * </ul>
     * </ul>
     */
    @Override
    protected String parseOptionsAndParameters(String[] parameterValues) {
        boolean getFile = true;
        for (int i = 0; i < parameterValues.length; i++) {
            if (null == parameterValues[i]) {
                continue;
            }
            if (parameterValues[i].equals(OPTION_ST)) {
                st = true;
            } else if (parameterValues[i].equalsIgnoreCase(OPTION_P)) {
                i++;
                if (i >= parameterValues.length) {
                    return String.format("The option [%s] should be followed by value for the parser type.", OPTION_P);
                }
                parserType = parameterValues[i];
            } else if (parameterValues[i].charAt(0) == '-') {
                return String.format("Unsupported option: %s", parameterValues[i]);
            } else if (getFile) {
                file = initFile(parameterValues[i]);
                if (file != null) {
                    getFile = false;
                }
            } else {
                return String.format("Unsupported parameter: %s", parameterValues[i]);
            }
        }
        if (getFile) {
            return "Missing destination file parameter!";
        }
        return SUCCESS;
    }

}
