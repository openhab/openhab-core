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
package org.openhab.core.voice.internal.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationArguments;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;

/**
 * Unit tests for {@link InterpretationService}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class InterpretationServiceTest {

    private @NonNullByDefault({}) InterpretationService service;

    private @NonNullByDefault({}) Conversation conversation;
    private @NonNullByDefault({}) HumanLanguageInterpreter hli1;
    private @NonNullByDefault({}) HumanLanguageInterpreter hli2;

    @BeforeEach
    public void setUp() {
        service = new InterpretationService();

        conversation = mock(Conversation.class);
        hli1 = mock(HumanLanguageInterpreter.class);
        hli2 = mock(HumanLanguageInterpreter.class);
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getId()).thenReturn("hli2");
    }

    @Test
    public void interpretSuccessOnFirstInterpreter() throws Exception {
        when(hli1.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class))).thenReturn("success answer");

        String result = service.interpret("turn on light", List.of(hli1, hli2), conversation, List.of(), Locale.ENGLISH,
                null, "base");

        assertEquals("success answer", result);
        verify(conversation).addMessage(ConversationRole.USER, "turn on light");
        verify(hli1).interpret(eq(Locale.ENGLISH), any(InterpreterContext.class));
        verify(hli2, never()).interpret(any(Locale.class), any(InterpreterContext.class));
    }

    @Test
    public void interpretFallsBackOnExceptionToNextInterpreter() throws Exception {
        when(hli1.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("hli1 failed"));
        when(hli2.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class))).thenReturn("hli2 answer");

        String result = service.interpret("turn on light", List.of(hli1, hli2), conversation, List.of(), Locale.ENGLISH,
                null, "base");

        assertEquals("hli2 answer", result);
        verify(hli1).interpret(eq(Locale.ENGLISH), any(InterpreterContext.class));
        verify(hli2).interpret(eq(Locale.ENGLISH), any(InterpreterContext.class));
    }

    @Test
    public void interpretUnrecoverableExceptionAbortsImmediatelyAndRecordsError() throws Exception {
        when(hli1.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class)))
                .thenThrow(new UnrecoverableInterpretationException("fatal error"));

        assertThrows(UnrecoverableInterpretationException.class, () -> service.interpret("turn on light",
                List.of(hli1, hli2), conversation, List.of(), Locale.ENGLISH, null, "base"));

        verify(hli2, never()).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(conversation).addMessage(ConversationRole.OPENHAB, "fatal error");
    }

    @Test
    public void interpretAllFailRecordsExceptionAndThrows() throws Exception {
        when(hli1.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("error1"));
        when(hli2.interpret(eq(Locale.ENGLISH), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("error2"));

        assertThrows(InterpretationException.class, () -> service.interpret("turn on light", List.of(hli1, hli2),
                conversation, List.of(), Locale.ENGLISH, null, "base"));

        verify(conversation).addMessage(ConversationRole.OPENHAB, "error2");
    }
}
