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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openhab.core.automation.parser.Parser;

/**
 * This class provides common functionality of commands:
 * <ul>
 * <li>{@link AutomationCommands#IMPORT_MODULE_TYPES}
 * <li>{@link AutomationCommands#IMPORT_TEMPLATES}
 * <li>{@link AutomationCommands#IMPORT_RULES}
 * </ul>
 *
 * @author Ana Dimova - Initial contribution
 */
public class AutomationCommandImport extends AutomationCommand {

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
     * This field keeps URL of the source of automation objects that has to be imported.
     */
    private URL url;

    /**
     * @see AutomationCommand#AutomationCommand(String, String[], int, AutomationCommandsPluggable)
     */
    public AutomationCommandImport(String command, String[] params, int adminType,
            AutomationCommandsPluggable autoCommands) {
        super(command, params, adminType, autoCommands);
    }

    /**
     * This method is responsible for execution of commands:
     * <ul>
     * <li>{@link AutomationCommands#IMPORT_MODULE_TYPES}
     * <li>{@link AutomationCommands#IMPORT_TEMPLATES}
     * <li>{@link AutomationCommands#IMPORT_RULES}
     * </ul>
     */
    @Override
    public String execute() {
        if (parsingResult != SUCCESS) {
            return parsingResult;
        }
        try {
            switch (providerType) {
                case AutomationCommands.MODULE_TYPE_PROVIDER:
                    autoCommands.importModuleTypes(parserType, url);
                    break;
                case AutomationCommands.TEMPLATE_PROVIDER:
                    autoCommands.importTemplates(parserType, url);
                    break;
                case AutomationCommands.RULE_PROVIDER:
                    autoCommands.importRules(parserType, url);
                    break;
            }
        } catch (Exception e) {
            return getStackTrace(e);
        }
        return SUCCESS + "\n";
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
     * This method is invoked from the constructor to parse all parameters and options of the command <b>EXPORT</b>.
     * If there are redundant parameters or options or the required is missing the result will be the failure of the
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
     * <li><b>parserType</b> is optional and by default its value is {@link Parser#FORMAT_JSON}.
     * <li><b>url</b> is required and it points the resource of automation objects that has to be imported.
     * </ul>
     * </ul>
     */
    @Override
    protected String parseOptionsAndParameters(String[] parameterValues) {
        boolean getUrl = true;
        for (int i = 0; i < parameterValues.length; i++) {
            if (null == parameterValues[i]) {
                continue;
            }
            if (OPTION_ST.equals(parameterValues[i])) {
                st = true;
            } else if (OPTION_P.equalsIgnoreCase(parameterValues[i])) {
                i++;
                if (i >= parameterValues.length) {
                    return String.format("The option [%s] should be followed by value for the parser type.", OPTION_P);
                }
                parserType = parameterValues[i];
            } else if (parameterValues[i].charAt(0) == '-') {
                return String.format("Unsupported option: %s", parameterValues[i]);
            } else if (getUrl) {
                url = initURL(parameterValues[i]);
                if (url != null) {
                    getUrl = false;
                }
            } else {
                return String.format("Unsupported parameter: %s", parameterValues[i]);
            }
        }
        if (getUrl) {
            return "Missing source URL parameter or its value is incorrect!";
        }
        return SUCCESS;
    }
}
