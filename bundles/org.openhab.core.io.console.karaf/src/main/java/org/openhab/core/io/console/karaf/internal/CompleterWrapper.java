/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault({})
public class CompleterWrapper implements Completer {

    private final @Nullable ConsoleCommandCompleter completer;
    private final String command;
    private final String commandDescription;
    private final @Nullable String globalCommand;

    public CompleterWrapper(final ConsoleCommandExtension command, boolean scoped) {
        this.completer = command.getCompleter();
        this.command = command.getCommand();
        this.commandDescription = command.getDescription();
        if (!scoped) {
            globalCommand = CommandWrapper.SCOPE + ":" + this.command;
        } else {
            globalCommand = null;
        }
    }

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        String localGlobalCommand = globalCommand;
        if (commandLine.getCursorArgumentIndex() == 0) {
            StringsCompleter stringsCompleter = new StringsCompleter();
            stringsCompleter.getStrings().add(command);
            if (localGlobalCommand != null) {
                stringsCompleter.getStrings().add(localGlobalCommand);
            }
            return stringsCompleter.complete(session, commandLine, candidates);
        }

        if (commandLine.getArguments().length > 1) {
            String arg = commandLine.getArguments()[0];
            if (!arg.equals(command) && !arg.equals(localGlobalCommand)) {
                return -1;
            }
        }

        if (commandLine.getCursorArgumentIndex() < 0) {
            return -1;
        }

        var localCompleter = completer;
        if (localCompleter == null) {
            return -1;
        }

        String[] args = commandLine.getArguments();
        boolean result = localCompleter.complete(Arrays.copyOfRange(args, 1, args.length),
                commandLine.getCursorArgumentIndex() - 1, commandLine.getArgumentPosition(), candidates);
        return result ? commandLine.getBufferPosition() - commandLine.getArgumentPosition() : -1;
    }

    // Override this method to give command descriptions if completing the command name
    @Override
    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        if (commandLine.getCursorArgumentIndex() == 0) {
            String arg = commandLine.getArguments()[0];
            arg = arg.substring(0, commandLine.getArgumentPosition());

            if (command.startsWith(arg)) {
                candidates.add(new Candidate(command, command, null, commandDescription, null, null, true));
            }
            String localGlobalCommand = globalCommand;
            if (localGlobalCommand != null && localGlobalCommand.startsWith(arg)) {
                candidates.add(new Candidate(localGlobalCommand, localGlobalCommand, null, commandDescription, null,
                        null, true));
            }

            return;
        }

        org.apache.karaf.shell.api.console.Completer.super.completeCandidates(session, commandLine, candidates);
    }
}
