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

import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationManager;

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

    private VoiceResource voiceResource = new VoiceResource(localeService, audioManager, voiceManager,
            conversationManager);

    @SuppressWarnings("unchecked")
    @Test
    public void testGetConversations() {
        Conversation conversation1 = mock(Conversation.class);
        when(conversation1.getId()).thenReturn("conv-1");
        when(conversation1.getCreated()).thenReturn(Instant.ofEpochMilli(1000));
        when(conversation1.getLastUpdated()).thenReturn(Instant.ofEpochMilli(2000));

        Conversation conversation2 = mock(Conversation.class);
        when(conversation2.getId()).thenReturn("conv-2");
        when(conversation2.getCreated()).thenReturn(Instant.ofEpochMilli(3000));
        when(conversation2.getLastUpdated()).thenReturn(Instant.ofEpochMilli(4000));

        when(conversationManager.getConversations()).thenReturn(List.of(conversation1, conversation2));

        Response response = voiceResource.getConversations();
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
}
