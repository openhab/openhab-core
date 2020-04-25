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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final String BASE = "smarthome";

    private final SortedMap<String, ConsoleCommandExtension> consoleCommandExtensions = Collections
            .synchronizedSortedMap(new TreeMap<>());

    public ConsoleSupportEclipse() {
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        consoleCommandExtensions.put(consoleCommandExtension.getCommand(), consoleCommandExtension);
    }

    public void removeConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        consoleCommandExtensions.remove(consoleCommandExtension.getCommand());
    }

    private ConsoleCommandExtension getConsoleCommandExtension(final String cmd) {
        return consoleCommandExtensions.get(cmd);
    }

    private Collection<ConsoleCommandExtension> getConsoleCommandExtensions() {
        final Set<ConsoleCommandExtension> set = new HashSet<>();
        set.addAll(consoleCommandExtensions.values());
        return set;
    }

    /**
     * Methods staring with "_" will be used as commands. We only define one command "smarthome" to make
     * sure we do not get into conflict with other existing commands. The different functionalities
     * can then be used by the first argument.
     *
     * @param interpreter the equinox command interpreter
     * @return null, return parameter is not used
     */
    public Object _smarthome(final CommandInterpreter interpreter) {
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

    @Override
    public String getHelp() {
        return ConsoleInterpreter.getHelp(BASE, " ", getConsoleCommandExtensions());
    }
}
