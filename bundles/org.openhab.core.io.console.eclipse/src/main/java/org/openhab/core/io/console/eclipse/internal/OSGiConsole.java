/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.console.eclipse.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.openhab.core.io.console.Console;

/**
 * Implementation of the Console interface for Eclipse OSGi console.
 * This class wraps the Eclipse OSGi CommandInterpreter to provide a unified console interface.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Split to separate file
 */
@NonNullByDefault
public class OSGiConsole implements Console {

    private final String baseCommand;
    private final CommandInterpreter interpreter;

    /**
     * Constructs a new OSGi console wrapper.
     *
     * @param baseCommand the base command name (e.g., "openhab")
     * @param interpreter the Eclipse OSGi command interpreter
     */
    public OSGiConsole(final String baseCommand, final CommandInterpreter interpreter) {
        this.baseCommand = baseCommand;
        this.interpreter = interpreter;
    }

    /**
     * Prints a string to the console without a newline.
     *
     * @param s the string to print
     */
    @Override
    public void print(final String s) {
        interpreter.print(s);
    }

    /**
     * Prints a string to the console with a newline.
     *
     * @param s the string to print
     */
    @Override
    public void println(final String s) {
        interpreter.println(s);
    }

    /**
     * Prints the usage information for a command.
     * The output format is: "Usage: {baseCommand} {usageString}".
     *
     * @param s the usage string describing command syntax
     */
    @Override
    public void printUsage(final String s) {
        interpreter.println(String.format("Usage: %s %s", baseCommand, s));
    }
}
