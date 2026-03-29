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
package org.openhab.core.io.console.rfc147.internal.extension;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.rfc147.internal.ConsoleCommandsContainer;
import org.openhab.core.io.console.rfc147.internal.ConsoleSupportRfc147;

/**
 * Console command extension that provides help information for all available commands.
 * This command displays usage information for all registered console commands in the
 * OSGi RFC 147 console.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class HelpConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private @Nullable ConsoleCommandsContainer commandsContainer;

    /**
     * Constructs a new help console command extension.
     * Registers the "help" command with a description.
     */
    public HelpConsoleCommandExtension() {
        super("help", "Get help for all available commands.");
    }

    /**
     * Sets the container that provides access to all registered console commands.
     * This is used to retrieve the list of commands to display help for.
     *
     * @param commandsContainer the commands container, or null to clear the reference
     */
    public void setConsoleCommandsContainer(final @Nullable ConsoleCommandsContainer commandsContainer) {
        this.commandsContainer = commandsContainer;
    }

    /**
     * Command entry point that matches the command name "help".
     * This method is invoked by the OSGi RFC 147 command infrastructure when the help command is called.
     *
     * @param args the command arguments
     */
    public void help(String[] args) {
        execute(args, ConsoleSupportRfc147.CONSOLE);
    }

    @Override
    public void execute(String[] args, Console console) {
        ConsoleCommandsContainer commandsContainer = this.commandsContainer;
        if (commandsContainer != null) {
            ConsoleInterpreter.printHelp(console, ConsoleSupportRfc147.CONSOLE.getBase(), ":",
                    commandsContainer.getConsoleCommandExtensions());
        }
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(getDescription()));
    }
}
