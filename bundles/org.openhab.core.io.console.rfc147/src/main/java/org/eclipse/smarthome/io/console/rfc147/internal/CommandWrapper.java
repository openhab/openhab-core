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
package org.eclipse.smarthome.io.console.rfc147.internal;

import java.util.Arrays;

import org.eclipse.smarthome.io.console.ConsoleInterpreter;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;

/**
 *
 * @author Markus Rathgeb - Initial contribution and API
 *
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
                System.out.println(String.format("The command (%s) differs from expected one (%s).", cmd,
                        command.getCommand()));
            }
        }
    }

}
