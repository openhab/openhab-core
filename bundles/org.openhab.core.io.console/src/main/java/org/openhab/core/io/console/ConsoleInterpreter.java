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
package org.openhab.core.io.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides generic methods for handling console input (i.e. pure strings).
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Change interface
 */
public class ConsoleInterpreter {

    public static String getHelp(final String base, final String separator,
            Collection<ConsoleCommandExtension> extensions) {
        final List<String> usages = ConsoleInterpreter.getUsages(extensions);
        final StringBuffer buffer = new StringBuffer();

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

    public static void printHelp(final Console console, final String base, final String separator,
            Collection<ConsoleCommandExtension> extensions) {
        console.println(getHelp(base, separator, extensions));
    }

    public static void execute(Console console, ConsoleCommandExtension extension, String[] args) {
        try {
            extension.execute(args, console);
        } catch (final Exception ex) {
            final Logger logger = LoggerFactory.getLogger(ConsoleInterpreter.class);
            logger.error("An error occurred while executing the console command.", ex);
            console.println("An unexpected error occurred during execution.");
        }
    }

    /** returns a CR-separated list of usage texts for all available commands */
    public static String getUsage(Collection<ConsoleCommandExtension> consoleCommandExtensions) {
        StringBuilder sb = new StringBuilder();
        for (String usage : ConsoleInterpreter.getUsages(consoleCommandExtensions)) {
            sb.append(usage + "\n");
        }
        return sb.toString();
    }

    /** returns an array of the usage texts for all available commands */
    public static List<String> getUsages(Collection<ConsoleCommandExtension> consoleCommandExtensions) {
        List<String> usages = new ArrayList<>();
        for (ConsoleCommandExtension consoleCommandExtension : consoleCommandExtensions) {
            usages.addAll(consoleCommandExtension.getUsages());
        }
        return usages;
    }
}
