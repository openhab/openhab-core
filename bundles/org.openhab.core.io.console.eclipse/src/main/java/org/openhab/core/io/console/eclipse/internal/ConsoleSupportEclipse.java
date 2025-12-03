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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class provides access to openHAB functionality through the OSGi console
 * of Equinox. Unfortunately, there these command providers are not standardized
 * for OSGi, so we need different implementations for different OSGi containers.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Split console interface and specific implementation
 */
@Component
public class ConsoleSupportEclipse implements CommandProvider {

    private static final String BASE = "openhab";

    private final SortedMap<String, ConsoleCommandExtension> consoleCommandExtensions = Collections
            .synchronizedSortedMap(new TreeMap<>());

    /**
     * Constructs a new ConsoleSupportEclipse instance.
     */
    public ConsoleSupportEclipse() {
    }

    /**
     * Adds a console command extension to the registry.
     * This method is called dynamically by OSGi when a new console command extension is registered.
     *
     * @param consoleCommandExtension the console command extension to add
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        consoleCommandExtensions.put(consoleCommandExtension.getCommand(), consoleCommandExtension);
    }

    /**
     * Removes a console command extension from the registry.
     * This method is called dynamically by OSGi when a console command extension is unregistered.
     *
     * @param consoleCommandExtension the console command extension to remove
     */
    public void removeConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        consoleCommandExtensions.remove(consoleCommandExtension.getCommand());
    }

    /**
     * Gets a console command extension by command name.
     *
     * @param cmd the command name
     * @return the console command extension, or null if not found
     */
    private ConsoleCommandExtension getConsoleCommandExtension(final String cmd) {
        return consoleCommandExtensions.get(cmd);
    }

    /**
     * Gets all registered console command extensions.
     *
     * @return a collection of all console command extensions
     */
    private Collection<ConsoleCommandExtension> getConsoleCommandExtensions() {
        return new HashSet<>(consoleCommandExtensions.values());
    }

    /**
     * Methods staring with "_" will be used as commands. We only define one command "openhab" to make
     * sure we do not get into conflict with other existing commands. The different functionalities
     * can then be used by the first argument.
     *
     * @param interpreter the equinox command interpreter
     * @return null, return parameter is not used
     */
    public Object _openhab(final CommandInterpreter interpreter) {
        final Console console = new OSGiConsole(BASE, interpreter);

        final String cmd = interpreter.nextArgument();
        if (cmd == null) {
            ConsoleInterpreter.printHelp(console, BASE, " ", getConsoleCommandExtensions());
        } else {
            final ConsoleCommandExtension extension = getConsoleCommandExtension(cmd);
            if (extension == null) {
                console.println(String.format("No handler for command '%s' was found.", cmd));
            } else {
                // Build argument list
                final List<String> argsList = new ArrayList<>();
                while (true) {
                    final String narg = interpreter.nextArgument();
                    if (narg != null && !narg.isEmpty()) {
                        argsList.add(narg);
                    } else {
                        break;
                    }
                }
                final String[] args = new String[argsList.size()];
                argsList.toArray(args);

                ConsoleInterpreter.execute(console, extension, args);
            }
        }

        return null;
    }

    /**
     * Gets the help text for all registered openHAB console commands.
     *
     * @return the help text listing all available commands and their usage
     */
    @Override
    public String getHelp() {
        return ConsoleInterpreter.getHelp(BASE, " ", getConsoleCommandExtensions());
    }
}
