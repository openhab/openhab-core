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
package org.openhab.core.voice.text.conversation.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.voice.text.conversation.ConversationRole;

/**
 * Test class for {@link ConversationEventFactory}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationEventFactoryTest {

    @Test
    public void createConversationCreatedEvent() {
        String id = "conv-1";
        ConversationCreatedEvent event = ConversationEventFactory.createConversationCreatedEvent(id, null);

        assertEquals(ConversationCreatedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/added", event.getTopic());
        assertTrue(event.getPayload().contains(id));
    }

    @Test
    public void createConversationRemovedEvent() {
        String id = "conv-1";
        ConversationRemovedEvent event = ConversationEventFactory.createConversationRemovedEvent(id, null);

        assertEquals(ConversationRemovedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/removed", event.getTopic());
        assertTrue(event.getPayload().contains(id));
    }

    @Test
    public void createConversationMessageEvent() {
        String convId = "conv-1";
        int msgId = 1;
        String text = "Hello";
        ConversationMessageEvent event = ConversationEventFactory.createConversationMessageEvent(convId, msgId,
                ConversationRole.USER, text, null);

        assertEquals(ConversationMessageEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/message", event.getTopic());
        assertTrue(event.getPayload().contains(String.valueOf(msgId)));
        assertTrue(event.getPayload().contains(text));
        assertTrue(event.getPayload().contains(ConversationRole.USER.name()));
    }
}
