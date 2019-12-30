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
package org.openhab.core.io.console.rfc147.internal;

import java.util.Arrays;

import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class CommandWrapper {

    private final ConsoleCommandExtension command;

    public CommandWrapper(final ConsoleCommandExtension command) {
        this.command = command;
    }

    // Called for all commands there is no function found for.
    // The first argument is the command.
    // The CommandSession interface provides methods for executing commands and getting and setting session variables.
    // We could return an Object or void.
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
