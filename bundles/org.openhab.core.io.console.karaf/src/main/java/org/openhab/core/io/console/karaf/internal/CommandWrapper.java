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
package org.openhab.core.io.console.karaf.internal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleInterpreter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 * This class wraps OH ConsoleCommandExtensions to commands for Apache Karaf
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Henning Treu - implement help command
 */
@Service
@org.apache.karaf.shell.api.action.Command(name = "help", scope = "smarthome", description = "Print the full usage information of the 'smarthome' commands.")
public class CommandWrapper implements Command, Action {

    // Define a scope for all commands.
    public static final String SCOPE = "smarthome";

    private final ConsoleCommandExtension command;

    /**
     * The registry is injected when a CommandWrapper is instantiated by Karaf (see {@link CommandWrapper} default
     * constructor).
     */
    @Reference
    private Registry registry;

    /**
     * The constructor for the "help" instance of this class. This instance will be created by
     * org.apache.karaf.shell.impl.action.command.ManagerImpl.instantiate(Class<? extends T>, Registry) and
     * is used to print all usages from the `smarthome` scope.
     * The wrapped command is unused here because the karaf ifrastructure will call the {@link #execute()} method.
     */
    public CommandWrapper() {
        this(null);
    }

    public CommandWrapper(final ConsoleCommandExtension command) {
        this.command = command;
    }

    @Override
    public Object execute(Session session, List<Object> argList) throws Exception {
        String[] args = argList.stream().map(a -> a.toString()).collect(Collectors.toList()).toArray(new String[0]);

        final Console console = new OSGiConsole(getScope());

        if (args.length == 1 && "--help".equals(args[0])) {
            for (final String usage : command.getUsages()) {
                console.printUsage(usage);
            }
        } else {
            ConsoleInterpreter.execute(console, command, args);
        }
        return null;
    }

    @Override
    public Completer getCompleter(boolean arg0) {
        return null;
    }

    @Override
    public String getDescription() {
        return command.getDescription();
    }

    @Override
    public String getName() {
        return command.getCommand();
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    /**
     * {@link Action}.{@link #execute()} will be called on the CommandWrapper instance created by Karaf (see
     * {@link CommandWrapper} default constructor).
     */
    @Override
    public Object execute() throws Exception {
        List<Command> commands = registry.getCommands();
        for (Command command : commands) {
            if (SCOPE.equals(command.getScope()) && command instanceof CommandWrapper) {
                command.execute(null, Arrays.asList(new Object[] { "--help" }));
            }
        }

        return null;
    }
}
