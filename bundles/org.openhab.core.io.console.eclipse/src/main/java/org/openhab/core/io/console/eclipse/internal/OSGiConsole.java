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
package org.openhab.core.io.console.eclipse.internal;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.openhab.core.io.console.Console;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Split to separate file
 */
public class OSGiConsole implements Console {

    private final String baseCommand;
    private final CommandInterpreter interpreter;

    public OSGiConsole(final String baseCommand, final CommandInterpreter interpreter) {
        this.baseCommand = baseCommand;
        this.interpreter = interpreter;
    }

    @Override
    public void print(final String s) {
        interpreter.print(s);
    }

    @Override
    public void println(final String s) {
        interpreter.println(s);
    }

    @Override
    public void printUsage(final String s) {
        interpreter.println(String.format("Usage: %s %s", baseCommand, s));
    }
}
