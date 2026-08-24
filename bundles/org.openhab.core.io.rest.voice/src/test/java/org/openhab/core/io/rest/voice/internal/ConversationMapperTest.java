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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;

/**
 * Test class for {@link ConversationMapper}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationMapperTest {
    @Test
    public void testMap() throws ConversationException {
        String id = "test-conversation";
        Conversation conversation = new Conversation(id);
        conversation.addMessage(ConversationRole.USER, "Hello");
        conversation.addMessage(ConversationRole.OPENHAB, "Hi there!");

        ConversationDTO dto = ConversationMapper.map(conversation);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals(conversation.getCreated(), dto.created());
        assertEquals(conversation.getLastUpdated(), dto.lastUpdated());
        assertEquals(2, dto.messages().size());
        assertEquals(ConversationRole.USER, dto.messages().get(0).role());
        assertEquals("Hello", dto.messages().get(0).content());
        assertEquals(ConversationRole.OPENHAB, dto.messages().get(1).role());
        assertEquals("Hi there!", dto.messages().get(1).content());
    }

    @Test
    public void testMapInfo() {
        String id = "test-conversation-info";
        Conversation conversation = new Conversation(id);

        ConversationInfoDTO dto = ConversationMapper.mapInfo(conversation);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals(conversation.getCreated(), dto.created());
        assertEquals(conversation.getLastUpdated(), dto.lastUpdated());
    }
}
