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
package org.openhab.core.io.rest.voice.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationArguments;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;

import jakarta.ws.rs.core.Response;

/**
 * Test class for {@link VoiceResource}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class VoiceResourceTest {

    private LocaleService localeService = mock(LocaleService.class);
    private AudioManager audioManager = mock(AudioManager.class);
    private VoiceManager voiceManager = mock(VoiceManager.class);
    private ConversationManager conversationManager = mock(ConversationManager.class);
    private LLMToolRegistry llmToolRegistry = mock(LLMToolRegistry.class);

    private VoiceResource voiceResource = new VoiceResource(localeService, audioManager, voiceManager,
            conversationManager, llmToolRegistry);

    @SuppressWarnings("unchecked")
    @Test
    public void testListConversations() {
        Conversation conversation1 = mock(Conversation.class);
        when(conversation1.getId()).thenReturn("conv-1");
        when(conversation1.getCreated()).thenReturn(Instant.ofEpochMilli(1000));
        when(conversation1.getLastUpdated()).thenReturn(Instant.ofEpochMilli(2000));

        Conversation conversation2 = mock(Conversation.class);
        when(conversation2.getId()).thenReturn("conv-2");
        when(conversation2.getCreated()).thenReturn(Instant.ofEpochMilli(3000));
        when(conversation2.getLastUpdated()).thenReturn(Instant.ofEpochMilli(4000));

        when(conversationManager.getConversations()).thenReturn(List.of(conversation1, conversation2));

        Response response = voiceResource.listConversations();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<ConversationInfoDTO> entity = (List<ConversationInfoDTO>) response.getEntity();
        assertNotNull(entity);
        assertEquals(2, entity.size());

        ConversationInfoDTO dto1 = entity.get(0);
        assertEquals("conv-1", dto1.id());
        assertEquals(Instant.ofEpochMilli(1000), dto1.created());
        assertEquals(Instant.ofEpochMilli(2000), dto1.lastUpdated());

        ConversationInfoDTO dto2 = entity.get(1);
        assertEquals("conv-2", dto2.id());
        assertEquals(Instant.ofEpochMilli(3000), dto2.created());
        assertEquals(Instant.ofEpochMilli(4000), dto2.lastUpdated());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLLMTools() {
        LLMTool tool1 = mock(LLMTool.class);
        when(tool1.getUID()).thenReturn("tool-1");
        when(tool1.getLabel(any())).thenReturn("Tool 1");
        when(tool1.getDescription(any())).thenReturn("Description of tool 1");

        LLMTool tool2 = mock(LLMTool.class);
        when(tool2.getUID()).thenReturn("tool-2");
        when(tool2.getLabel(any())).thenReturn("Tool 2");
        when(tool2.getDescription(any())).thenReturn("Description of tool 2");

        when(llmToolRegistry.getAll()).thenReturn(List.of(tool1, tool2));

        Response response = voiceResource.getLLMTools(null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<LLMToolDTO> entity = (List<LLMToolDTO>) response.getEntity();
        assertNotNull(entity);
        assertEquals(2, entity.size());

        LLMToolDTO dto1 = entity.get(0);
        assertEquals("tool-1", dto1.id());
        assertEquals("Tool 1", dto1.label());
        assertEquals("Description of tool 1", dto1.description());

        LLMToolDTO dto2 = entity.get(1);
        assertEquals("tool-2", dto2.id());
        assertEquals("Tool 2", dto2.label());
        assertEquals("Description of tool 2", dto2.description());
    }

    @Test
    public void testInterpretJsonNullOrBlankTextReturnsBadRequest() {
        Response nullResponse = voiceResource.interpret(null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), nullResponse.getStatus());

        InterpretationRequestDTO blankRequest = new InterpretationRequestDTO("   ", null, null, null, null);
        Response blankResponse = voiceResource.interpret(null, blankRequest);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), blankResponse.getStatus());
    }

    @Test
    public void testInterpretJsonDefaultInterpreterAndTools() throws InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(voiceManager.getHLI()).thenReturn(hli);
        when(localeService.getLocale(null)).thenReturn(Locale.ENGLISH);
        when(voiceManager.interpret("Turn on the kitchen lights",
                new InterpretationArguments("", "", "*", null, Locale.ENGLISH))).thenReturn("Done");

        InterpretationRequestDTO request = new InterpretationRequestDTO("Turn on the kitchen lights", null, null, null,
                null);
        Response response = voiceResource.interpret(null, request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Done", response.getEntity());

        request = new InterpretationRequestDTO("Turn on the kitchen lights", List.of(), null, List.of(), null);
        response = voiceResource.interpret(null, request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Done", response.getEntity());
    }

    @Test
    public void testInterpretJsonDefaultInterpreterNotFound() {
        when(voiceManager.getHLI()).thenReturn(null);

        InterpretationRequestDTO request = new InterpretationRequestDTO("Turn on the kitchen lights", null, null, null,
                null);
        Response response = voiceResource.interpret(null, request);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInterpretJsonWithAllParameters() throws InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(voiceManager.getHLIsByIds(List.of("standard", "gemini"))).thenReturn(List.of(hli));
        when(localeService.getLocale("en")).thenReturn(Locale.ENGLISH);
        when(voiceManager.interpret("Turn on the kitchen lights",
                new InterpretationArguments("standard,gemini", "conv-1", "tool1,tool2", "Kitchen", Locale.ENGLISH)))
                .thenReturn("Lights turned on");

        InterpretationRequestDTO request = new InterpretationRequestDTO("Turn on the kitchen lights",
                List.of("standard", "gemini"), "conv-1", List.of("tool1", "tool2"), "Kitchen");

        Response response = voiceResource.interpret("en", request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Lights turned on", response.getEntity());
    }

    @Test
    public void testInterpretJsonSpecifiedInterpretersNotFound() {
        when(voiceManager.getHLIsByIds(List.of("unknown"))).thenReturn(List.of());

        InterpretationRequestDTO request = new InterpretationRequestDTO("Turn on the kitchen lights",
                List.of("unknown"), null, null, null);

        Response response = voiceResource.interpret(null, request);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInterpretJsonInterpretationExceptionReturnsBadRequest() throws InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(voiceManager.getHLI()).thenReturn(hli);
        when(localeService.getLocale(null)).thenReturn(Locale.ENGLISH);
        when(voiceManager.interpret(eq("Invalid"), any(InterpretationArguments.class)))
                .thenThrow(new InterpretationException("Cannot interpret"));

        InterpretationRequestDTO request = new InterpretationRequestDTO("Invalid", null, null, null, null);

        Response response = voiceResource.interpret(null, request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
