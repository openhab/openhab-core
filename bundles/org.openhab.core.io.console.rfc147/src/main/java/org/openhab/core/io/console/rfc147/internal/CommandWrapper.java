/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.console.rfc147.internal;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 * Wrapper class that bridges console command extensions to OSGi RFC 147 command interface.
 * This class wraps a ConsoleCommandExtension and exposes it as an OSGi command that can be
 * invoked through the OSGi command shell.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class CommandWrapper {

    private final ConsoleCommandExtension command;

    /**
     * Constructs a new command wrapper.
     *
     * @param command the console command extension to wrap
     */
    public CommandWrapper(final ConsoleCommandExtension command) {
        this.command = command;
    }

    /**
     * Main entry point for command execution via OSGi RFC 147 interface.
     * This method is called when no specific function is found for the command.
     * The first argument is the command name, and remaining arguments are passed to the command.
     *
     * @param args the command arguments, where args[0] is the command name
     * @throws Exception if command execution fails
     */
    public void _main(/* CommandSession session, */String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("missing command");
        } else {
            final String cmd = args[0];
            final String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

            if (cmd.equals(command.getCommand())) {
                ConsoleInterpreter.execute(ConsoleSupportRfc147.CONSOLE, command, cmdArgs);
            } else {
                System.out.println(
                        String.format("The command (%s) differs from expected one (%s).", cmd, command.getCommand()));
            }
        }
    }
}
