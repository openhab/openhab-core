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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.voice.text.conversation.ConversationRole;

/**
 * Test class for {@link ConversationEventFactory}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationEventFactoryTest {
    ConversationEventFactory factory = new ConversationEventFactory();

    @Test
    public void createConversationCreatedEvent() {
        String id = "conv-1";
        ConversationCreatedEvent event = ConversationEventFactory.createConversationCreatedEvent(id, null);

        assertEquals(ConversationCreatedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/added", event.getTopic());
        assertEquals(id, event.getConversationId());
    }

    @Test
    public void serializeDeserializeConversationCreatedEvent() {
        String id = "conv-1";
        ConversationCreatedEvent event = ConversationEventFactory.createConversationCreatedEvent(id, null);

        Event deserialized = factory.createEventByType(event.getType(), event.getTopic(), event.getPayload(),
                event.getSource());
        assertInstanceOf(ConversationCreatedEvent.class, deserialized);
        assertEquals(ConversationCreatedEvent.TYPE, deserialized.getType());
        assertEquals(event.getTopic(), deserialized.getTopic());
        assertEquals(event.getPayload(), deserialized.getPayload());
        assertEquals(event.getSource(), deserialized.getSource());
        assertEquals(id, ((ConversationCreatedEvent) deserialized).getConversationId());
    }

    @Test
    public void createConversationRemovedEvent() {
        String id = "conv-1";
        ConversationRemovedEvent event = ConversationEventFactory.createConversationRemovedEvent(id, null);

        assertEquals(ConversationRemovedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/removed", event.getTopic());
        assertEquals(id, event.getConversationId());
    }

    @Test
    public void serializeDeserializeConversationRemovedEvent() {
        String id = "conv-1";
        ConversationRemovedEvent event = ConversationEventFactory.createConversationRemovedEvent(id, null);

        Event deserialized = factory.createEventByType(event.getType(), event.getTopic(), event.getPayload(),
                event.getSource());
        assertInstanceOf(ConversationRemovedEvent.class, deserialized);
        assertEquals(ConversationRemovedEvent.TYPE, deserialized.getType());
        assertEquals(event.getTopic(), deserialized.getTopic());
        assertEquals(event.getPayload(), deserialized.getPayload());
        assertEquals(event.getSource(), deserialized.getSource());
        assertEquals(id, ((ConversationRemovedEvent) deserialized).getConversationId());
    }

    @Test
    public void createConversationMessageAddedEvent() {
        String convId = "conv-1";
        int msgId = 1;
        ConversationRole role = ConversationRole.USER;
        String text = "Hello";
        ConversationMessageAddedEvent event = ConversationEventFactory.createConversationMessageAddedEvent(convId,
                msgId, role, text, null);

        assertEquals(ConversationMessageAddedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/messageadded", event.getTopic());
        assertEquals(convId, event.getConversationId());
        assertEquals(msgId, event.getMessageId());
        assertEquals(role, event.getRole());
        assertEquals(text, event.getText());
    }

    @Test
    public void serializeDeserializeConversationMessageAddedEvent() {
        String convId = "conv-1";
        int msgId = 1;
        ConversationRole role = ConversationRole.USER;
        String text = "Hello";
        ConversationMessageAddedEvent event = ConversationEventFactory.createConversationMessageAddedEvent(convId,
                msgId, role, text, null);

        Event deserialized = factory.createEventByType(event.getType(), event.getTopic(), event.getPayload(),
                event.getSource());
        assertInstanceOf(ConversationMessageAddedEvent.class, deserialized);
        assertEquals(ConversationMessageAddedEvent.TYPE, deserialized.getType());
        assertEquals(event.getTopic(), deserialized.getTopic());
        assertEquals(event.getPayload(), deserialized.getPayload());
        assertEquals(event.getSource(), deserialized.getSource());
        assertEquals(convId, ((ConversationMessageAddedEvent) deserialized).getConversationId());
        assertEquals(msgId, ((ConversationMessageAddedEvent) deserialized).getMessageId());
        assertEquals(role, ((ConversationMessageAddedEvent) deserialized).getRole());
        assertEquals(text, ((ConversationMessageAddedEvent) deserialized).getText());
    }

    @Test
    public void createConversationMessagesRemovedEvent() {
        String convId = "conv-1";
        int removedSinceMessagesId = 1;
        ConversationMessagesRemovedEvent event = ConversationEventFactory.createConversationMessagesRemovedEvent(convId,
                removedSinceMessagesId, null);

        assertEquals(ConversationMessagesRemovedEvent.TYPE, event.getType());
        assertEquals("openhab/conversations/conv-1/messagesremoved", event.getTopic());
        assertEquals(convId, event.getConversationId());
        assertEquals(removedSinceMessagesId, event.getRemovedSinceMessagesId());
    }

    @Test
    public void serializeDeserializeConversationMessagesRemovedEvent() {
        String convId = "conv-1";
        int removedSinceMessagesId = 1;
        ConversationMessagesRemovedEvent event = ConversationEventFactory.createConversationMessagesRemovedEvent(convId,
                removedSinceMessagesId, null);

        Event deserialized = factory.createEventByType(event.getType(), event.getTopic(), event.getPayload(),
                event.getSource());
        assertInstanceOf(ConversationMessagesRemovedEvent.class, deserialized);
        assertEquals(ConversationMessagesRemovedEvent.TYPE, deserialized.getType());
        assertEquals(event.getTopic(), deserialized.getTopic());
        assertEquals(event.getPayload(), deserialized.getPayload());
        assertEquals(event.getSource(), deserialized.getSource());
        assertEquals(convId, ((ConversationMessagesRemovedEvent) deserialized).getConversationId());
        assertEquals(removedSinceMessagesId,
                ((ConversationMessagesRemovedEvent) deserialized).getRemovedSinceMessagesId());
    }
}
