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
package org.openhab.core.io.console.rfc147.internal.extension;

import java.util.Collections;
import java.util.List;

import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.rfc147.internal.ConsoleCommandsContainer;
import org.openhab.core.io.console.rfc147.internal.ConsoleSupportRfc147;

/**
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class HelpConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private ConsoleCommandsContainer commandsContainer;

    public HelpConsoleCommandExtension() {
        super("help", "Get help for all available commands.");
    }

    public void setConsoleCommandsContainer(final ConsoleCommandsContainer commandsContainer) {
        this.commandsContainer = commandsContainer;
    }

    // Add a method that name is equal to our command
    public void help(String[] args) {
        execute(args, ConsoleSupportRfc147.CONSOLE);
    }

    @Override
    public void execute(String[] args, Console console) {
        if (this.commandsContainer != null) {
            ConsoleInterpreter.printHelp(console, ConsoleSupportRfc147.CONSOLE.getBase(), ":",
                    this.commandsContainer.getConsoleCommandExtensions());
        }
    }

    @Override
    public List<String> getUsages() {
        return Collections.singletonList(buildCommandUsage(getDescription()));
    }

}
