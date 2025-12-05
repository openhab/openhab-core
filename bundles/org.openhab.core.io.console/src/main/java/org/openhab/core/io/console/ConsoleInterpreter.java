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
package org.openhab.core.io.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides generic methods for handling console input (i.e. pure strings).
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Change interface
 */
@NonNullByDefault
public class ConsoleInterpreter {

    /**
     * Generates a formatted help message listing all available console commands.
     * This method creates a comprehensive help text that includes all command extensions
     * with their usage information, formatted with the specified base command and separator.
     *
     * @param base the base command string (e.g., "openhab")
     * @param separator the separator between base and command (e.g., ":")
     * @param extensions the collection of console command extensions to include in the help
     * @return a formatted string containing all command usages
     */
    public static String getHelp(final String base, final String separator,
            Collection<ConsoleCommandExtension> extensions) {
        final List<String> usages = ConsoleInterpreter.getUsages(extensions);
        final StringBuilder buffer = new StringBuilder();

        buffer.append("---openHAB commands---\n\t");
        for (int i = 0; i < usages.size(); i++) {
            final String usageString = usages.get(i);
            buffer.append(String.format("%s%s%s\n", base, separator, usageString));
            if (usages.size() > i + 1) {
                buffer.append("\t");
            }
        }

        return buffer.toString();
    }

    /**
     * Prints the formatted help message to the console.
     * This is a convenience method that generates and prints the help text for all
     * available console commands.
     *
     * @param console the console to print the help message to
     * @param base the base command string (e.g., "openhab")
     * @param separator the separator between base and command (e.g., ":")
     * @param extensions the collection of console command extensions to include in the help
     */
    public static void printHelp(final Console console, final String base, final String separator,
            Collection<ConsoleCommandExtension> extensions) {
        console.println(getHelp(base, separator, extensions));
    }

    /**
     * Executes a console command extension with the given arguments.
     * This method wraps the execution with error handling, logging any exceptions that occur
     * and providing user-friendly error messages to the console.
     *
     * @param console the console for command output
     * @param extension the console command extension to execute
     * @param args the arguments to pass to the command
     */
    public static void execute(Console console, ConsoleCommandExtension extension, String[] args) {
        try {
            extension.execute(args, console);
        } catch (final Exception ex) {
            final Logger logger = LoggerFactory.getLogger(ConsoleInterpreter.class);
            logger.error("An error occurred while executing the console command.", ex);
            console.println("An unexpected error occurred during execution.");
        }
    }

    /**
     * Returns a newline-separated list of usage texts for all available commands.
     * Each usage text is on its own line, providing a complete list of all command
     * usages from the given console command extensions.
     *
     * @param consoleCommandExtensions the collection of console command extensions
     * @return a newline-separated string containing all command usage texts
     */
    public static String getUsage(Collection<ConsoleCommandExtension> consoleCommandExtensions) {
        StringBuilder sb = new StringBuilder();
        for (String usage : ConsoleInterpreter.getUsages(consoleCommandExtensions)) {
            sb.append(usage + "\n");
        }
        return sb.toString();
    }

    /**
     * Returns a list of usage texts for all available commands.
     * This method collects all usage strings from the given console command extensions
     * and returns them as a list.
     *
     * @param consoleCommandExtensions the collection of console command extensions
     * @return a list containing all command usage texts
     */
    public static List<String> getUsages(Collection<ConsoleCommandExtension> consoleCommandExtensions) {
        List<String> usages = new ArrayList<>();
        for (ConsoleCommandExtension consoleCommandExtension : consoleCommandExtensions) {
            usages.addAll(consoleCommandExtension.getUsages());
        }
        return usages;
    }
}
