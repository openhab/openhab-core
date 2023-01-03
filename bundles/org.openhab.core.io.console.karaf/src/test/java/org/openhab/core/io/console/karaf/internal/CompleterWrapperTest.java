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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 * @author Cody Cutrer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class CompleterWrapperTest {
    private @Mock @NonNullByDefault({}) ConsoleCommandExtension commandExtension;
    private @Mock @NonNullByDefault({}) ConsoleCommandCompleter completer;
    private @Mock @NonNullByDefault({}) Session session;
    private @Mock @NonNullByDefault({}) CommandLine commandLine;
    private @NonNullByDefault({}) CommandWrapper commandWrapper;
    private @NonNullByDefault({}) Completer completerWrapper;

    @BeforeEach
    public void setup() {
        when(commandExtension.getCommand()).thenReturn("command");
        when(commandExtension.getCompleter()).thenReturn(completer);
        when(commandExtension.getDescription()).thenReturn("description");
        commandWrapper = new CommandWrapper(commandExtension);
    }

    @Test
    public void fillsCommandDescriptionsLocalOnly() {
        completerWrapper = commandWrapper.getCompleter(true);
        var candidates = new ArrayList<Candidate>();
        when(commandLine.getArguments()).thenReturn(new String[] { "" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(0);
        when(commandLine.getArgumentPosition()).thenReturn(0);

        completerWrapper.completeCandidates(session, commandLine, candidates);
        assertEquals(1, candidates.size());
        assertEquals("command", candidates.get(0).value());
        assertEquals("description", candidates.get(0).descr());
    }

    @Test
    public void fillsCommandDescriptionsLocalAndGlobal() {
        completerWrapper = commandWrapper.getCompleter(false);
        var candidates = new ArrayList<Candidate>();
        when(commandLine.getArguments()).thenReturn(new String[] { "" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(0);
        when(commandLine.getArgumentPosition()).thenReturn(0);

        completerWrapper.completeCandidates(session, commandLine, candidates);
        assertEquals(2, candidates.size());
        assertEquals("command", candidates.get(0).value());
        assertEquals("description", candidates.get(0).descr());

        assertEquals("openhab:command", candidates.get(1).value());
        assertEquals("description", candidates.get(1).descr());
    }

    @Test
    public void completeCandidatesCompletesArguments() {
        completerWrapper = commandWrapper.getCompleter(true);
        var candidates = new ArrayList<Candidate>();
        when(commandLine.getArguments()).thenReturn(new String[] { "command", "subcmd" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(1);
        when(commandLine.getArgumentPosition()).thenReturn(6);
        when(commandLine.getBufferPosition()).thenReturn(14);
        when(completer.complete(new String[] { "subcmd" }, 0, 6, new ArrayList<>())).thenReturn(false);

        completerWrapper.completeCandidates(session, commandLine, candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    public void doesntCallCompleterForOtherCommands() {
        completerWrapper = commandWrapper.getCompleter(true);
        var candidates = new ArrayList<String>();
        when(commandLine.getArguments()).thenReturn(new String[] { "somethingElse", "" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(1);
        verifyNoInteractions(completer);

        assertEquals(-1, completerWrapper.complete(session, commandLine, candidates));
        assertTrue(candidates.isEmpty());
    }

    @Test
    public void callsCompleterWithProperlyScopedArguments() {
        completerWrapper = commandWrapper.getCompleter(true);
        var candidates = new ArrayList<String>();
        when(commandLine.getArguments()).thenReturn(new String[] { "command", "subcmd" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(1);
        when(commandLine.getArgumentPosition()).thenReturn(6);
        when(completer.complete(new String[] { "subcmd" }, 0, 6, new ArrayList<>())).thenReturn(false);

        assertEquals(-1, completerWrapper.complete(session, commandLine, candidates));
        assertTrue(candidates.isEmpty());
    }

    @Test
    public void callsCompleterForGlobalForm() {
        completerWrapper = commandWrapper.getCompleter(false);
        var candidates = new ArrayList<String>();
        when(commandLine.getArguments()).thenReturn(new String[] { "openhab:command", "subcmd" });
        when(commandLine.getCursorArgumentIndex()).thenReturn(1);
        when(commandLine.getArgumentPosition()).thenReturn(6);
        when(completer.complete(new String[] { "subcmd" }, 0, 6, new ArrayList<>())).thenReturn(false);

        assertEquals(-1, completerWrapper.complete(session, commandLine, candidates));
        assertTrue(candidates.isEmpty());
    }
}
