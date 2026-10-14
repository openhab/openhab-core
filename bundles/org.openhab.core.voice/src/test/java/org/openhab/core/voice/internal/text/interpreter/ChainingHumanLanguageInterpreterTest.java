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
package org.openhab.core.voice.internal.text.interpreter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.openhab.core.voice.text.interpreter.llm.UnrecoverableLLMToolException;

/**
 * Tests for {@link ChainingHumanLanguageInterpreter}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class ChainingHumanLanguageInterpreterTest {

    private @Mock @NonNullByDefault({}) HumanLanguageInterpreter primaryMock;
    private @Mock @NonNullByDefault({}) HumanLanguageInterpreter secondaryMock;
    private @Mock @NonNullByDefault({}) HumanLanguageInterpreter tertiaryMock;

    private @NonNullByDefault({}) ChainingHumanLanguageInterpreter chainedInterpreter;

    @BeforeEach
    public void setUp() {
        lenient().when(primaryMock.getId()).thenReturn("primary");
        lenient().when(primaryMock.getLabel(any())).thenReturn("Primary");
        lenient().when(primaryMock.getSupportedLocales()).thenReturn(Set.of());

        lenient().when(secondaryMock.getId()).thenReturn("secondary");
        lenient().when(secondaryMock.getLabel(any())).thenReturn("Secondary");
        lenient().when(secondaryMock.getSupportedLocales()).thenReturn(Set.of());

        lenient().when(tertiaryMock.getId()).thenReturn("tertiary");
        lenient().when(tertiaryMock.getLabel(any())).thenReturn("Tertiary");
        lenient().when(tertiaryMock.getSupportedLocales()).thenReturn(Set.of());

        chainedInterpreter = new ChainingHumanLanguageInterpreter(Map.of("primaryInterpreter", "primary",
                "secondaryInterpreter", "secondary", "tertiaryInterpreter", "tertiary"));

        chainedInterpreter.addHumanLanguageInterpreter(primaryMock);
        chainedInterpreter.addHumanLanguageInterpreter(secondaryMock);
        chainedInterpreter.addHumanLanguageInterpreter(tertiaryMock);
    }

    @Test
    public void primarySucceedsDoesNotInvokeSecondaryOrTertiary() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light")).thenReturn("Ok");

        String result = chainedInterpreter.interpret(Locale.ENGLISH, "turn on light");

        assertEquals("Ok", result);
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, never()).interpret(any(), anyString());
        verify(tertiaryMock, never()).interpret(any(), anyString());
    }

    @Test
    public void primaryFailsWithRecoverableSecondarySucceeds() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Not understood"));
        when(secondaryMock.interpret(Locale.ENGLISH, "turn on light")).thenReturn("Done");

        String result = chainedInterpreter.interpret(Locale.ENGLISH, "turn on light");

        assertEquals("Done", result);
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(tertiaryMock, never()).interpret(any(), anyString());
    }

    @Test
    public void primaryAndSecondaryFailTertiarySucceeds() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Not understood"));
        when(secondaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Parse error"));
        when(tertiaryMock.interpret(Locale.ENGLISH, "turn on light")).thenReturn("Handled by LLM");

        String result = chainedInterpreter.interpret(Locale.ENGLISH, "turn on light");

        assertEquals("Handled by LLM", result);
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(tertiaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
    }

    @Test
    public void allFailRethrowsLastException() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("First failed"));
        when(secondaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Second failed"));
        when(tertiaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Third failed"));

        InterpretationException ex = assertThrows(InterpretationException.class,
                () -> chainedInterpreter.interpret(Locale.ENGLISH, "turn on light"));

        assertEquals("Third failed", ex.getMessage());
    }

    @Test
    public void primaryThrowsUnrecoverableAbortsImmediately() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new UnrecoverableInterpretationException("Item is read-only"));

        UnrecoverableInterpretationException ex = assertThrows(UnrecoverableInterpretationException.class,
                () -> chainedInterpreter.interpret(Locale.ENGLISH, "turn on light"));

        assertEquals("Item is read-only", ex.getMessage());
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, never()).interpret(any(), anyString());
        verify(tertiaryMock, never()).interpret(any(), anyString());
    }

    @Test
    public void secondaryThrowsUnrecoverableAbortsImmediately() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Not understood"));
        when(secondaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new UnrecoverableInterpretationException("Access denied"));

        UnrecoverableInterpretationException ex = assertThrows(UnrecoverableInterpretationException.class,
                () -> chainedInterpreter.interpret(Locale.ENGLISH, "turn on light"));

        assertEquals("Access denied", ex.getMessage());
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(tertiaryMock, never()).interpret(any(), anyString());
    }

    @Test
    public void toolUnrecoverableCauseAbortsImmediately() throws InterpretationException {
        when(primaryMock.interpret(Locale.ENGLISH, "turn on light"))
                .thenThrow(new InterpretationException("Tool failed", new UnrecoverableLLMToolException("read-only")));

        InterpretationException ex = assertThrows(InterpretationException.class,
                () -> chainedInterpreter.interpret(Locale.ENGLISH, "turn on light"));

        assertEquals("Tool failed", ex.getMessage());
        assertInstanceOf(UnrecoverableLLMToolException.class, ex.getCause());
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, "turn on light");
        verify(secondaryMock, never()).interpret(any(), anyString());
        verify(tertiaryMock, never()).interpret(any(), anyString());
    }

    @Test
    public void contextInterpretationDelegation() throws InterpretationException, ConversationException {
        Conversation conversation = new Conversation("c1");
        conversation.addMessage(ConversationRole.USER, "turn on light");
        InterpreterContext context = new InterpreterContext(conversation, List.of(), null, null);

        when(primaryMock.interpret(Locale.ENGLISH, context)).thenReturn("");

        String result = chainedInterpreter.interpret(Locale.ENGLISH, context);

        assertEquals("", result);
        verify(primaryMock, times(1)).interpret(Locale.ENGLISH, context);
        verify(secondaryMock, never()).interpret(any(), any(InterpreterContext.class));
    }

    @Test
    public void unsupportedLocaleSkipsToNextInterpreter() throws InterpretationException {
        when(primaryMock.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN));
        when(secondaryMock.getSupportedLocales()).thenReturn(Set.of(Locale.FRENCH));
        when(secondaryMock.interpret(Locale.FRENCH, "allume la lumière")).thenReturn("Oui");

        String result = chainedInterpreter.interpret(Locale.FRENCH, "allume la lumière");

        assertEquals("Oui", result);
        verify(primaryMock, never()).interpret(any(), anyString());
        verify(secondaryMock, times(1)).interpret(Locale.FRENCH, "allume la lumière");
    }

    @Test
    public void emptyChainThrowsInterpretationException() {
        ChainingHumanLanguageInterpreter emptyInterpreter = new ChainingHumanLanguageInterpreter(Map.of());

        assertThrows(InterpretationException.class, () -> emptyInterpreter.interpret(Locale.ENGLISH, "test"));
    }

    @Test
    public void getParameterOptionsReturnsAvailableInterpreters() {
        URI uri = URI.create(ChainingHumanLanguageInterpreter.CONFIG_URI);

        Collection<ParameterOption> options = chainedInterpreter.getParameterOptions(uri,
                ChainingHumanLanguageInterpreter.CONFIG_PRIMARY_INTERPRETER, null, Locale.ENGLISH);

        assertNotNull(options);
        assertEquals(3, options.size());
        assertTrue(options.stream().anyMatch(opt -> "primary".equals(opt.getValue())));
        assertTrue(options.stream().anyMatch(opt -> "secondary".equals(opt.getValue())));
        assertTrue(options.stream().anyMatch(opt -> "tertiary".equals(opt.getValue())));
    }
}
