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
package org.eclipse.smarthome.model.script.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to log to the SLF4J-Log.
 *
 * @author Thomas.Eichstaedt-Engelen
 */
public class LogAction {

    private final static String LOGGER_NAME_PREFIX = "org.eclipse.smarthome.model.script.";

    /**
     * Creates the Log-Entry <code>format</code> with level <code>DEBUG</code> and logs under the loggers name
     * <code>org.eclipse.smarthome.model.script.&lt;loggerName&gt;</code>
     * 
     * @param loggerName the name of the Logger which is prefixed with <code>org.eclipse.smarthome.model.script.</code>
     * @param format the Log-Statement which can contain placeholders '<code>{}</code>'
     * @param args the arguments to replace the placeholders contained in <code>format</code>
     * 
     * @see Logger
     */
    static public void logDebug(String loggerName, String format, Object... args) {
        LoggerFactory.getLogger(LOGGER_NAME_PREFIX.concat(loggerName)).debug(format, args);
    }

    /**
     * Creates the Log-Entry <code>format</code> with level <code>INFO</code> and logs under the loggers name
     * <code>org.eclipse.smarthome.model.script.&lt;loggerName&gt;</code>
     * 
     * @param loggerName the name of the Logger which is prefixed with <code>org.eclipse.smarthome.model.script.</code>
     * @param format the Log-Statement which can contain placeholders '<code>{}</code>'
     * @param args the arguments to replace the placeholders contained in <code>format</code>
     * 
     * @see Logger
     */
    static public void logInfo(String loggerName, String format, Object... args) {
        LoggerFactory.getLogger(LOGGER_NAME_PREFIX.concat(loggerName)).info(format, args);
    }

    /**
     * Creates the Log-Entry <code>format</code> with level <code>WARN</code> and logs under the loggers name
     * <code>org.eclipse.smarthome.model.script.&lt;loggerName&gt;</code>
     * 
     * @param loggerName the name of the Logger which is prefixed with <code>org.eclipse.smarthome.model.script.</code>
     * @param format the Log-Statement which can contain placeholders '<code>{}</code>'
     * @param args the arguments to replace the placeholders contained in <code>format</code>
     * 
     * @see Logger
     */
    static public void logWarn(String loggerName, String format, Object... args) {
        LoggerFactory.getLogger(LOGGER_NAME_PREFIX.concat(loggerName)).warn(format, args);
    }

    /**
     * Creates the Log-Entry <code>format</code> with level <code>ERROR</code> and logs under the loggers name
     * <code>org.eclipse.smarthome.model.script.&lt;loggerName&gt;</code>
     * 
     * @param loggerName the name of the Logger which is prefixed with <code>org.eclipse.smarthome.model.script.</code>
     * @param format the Log-Statement which can contain placeholders '<code>{}</code>'
     * @param args the arguments to replace the placeholders contained in <code>format</code>
     * 
     * @see Logger
     */
    static public void logError(String loggerName, String format, Object... args) {
        LoggerFactory.getLogger(LOGGER_NAME_PREFIX.concat(loggerName)).error(format, args);
    }

}
