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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ConversationEventFactory} defines a {@link Event} implementation that emits conversation changes.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 * @author Florian Hotze - Differ between created, removed, message added and messages removed events
 */
@Component(immediate = true, service = EventFactory.class)
@NonNullByDefault
public class ConversationEventFactory extends AbstractEventFactory {
    private static final String CONVERSATION_ADDED_TOPIC = "openhab/conversations/{id}/added";
    private static final String CONVERSATION_REMOVED_TOPIC = "openhab/conversations/{id}/removed";
    private static final String CONVERSATION_MESSAGE_ADDED_TOPIC = "openhab/conversations/{id}/messageadded";
    private static final String CONVERSATION_MESSAGES_REMOVED_TOPIC = "openhab/conversations/{id}/messagesremoved";

    /**
     * Constructs a new ConversationEventFactory.
     */
    public ConversationEventFactory() {
        super(Set.of(ConversationCreatedEvent.TYPE, ConversationRemovedEvent.TYPE, ConversationMessageAddedEvent.TYPE,
                ConversationMessagesRemovedEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source) {
        if (ConversationCreatedEvent.TYPE.equals(eventType)) {
            return createConversationCreatedEvent(topic, payload, source);
        } else if (ConversationRemovedEvent.TYPE.equals(eventType)) {
            return createConversationRemovedEvent(topic, payload, source);
        } else if (ConversationMessageAddedEvent.TYPE.equals(eventType)) {
            return createConversationMessageAddedEvent(topic, payload, source);
        } else if (ConversationMessagesRemovedEvent.TYPE.equals(eventType)) {
            return createConversationMessagesRemovedEvent(topic, payload, source);
        }

        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    private Event createConversationCreatedEvent(String topic, String payload, @Nullable String source) {
        ConversationEvent.ConversationDTO conversationDTO = deserializePayload(payload,
                ConversationEvent.ConversationDTO.class);
        return new ConversationCreatedEvent(topic, payload, source, conversationDTO.conversationId);
    }

    private Event createConversationRemovedEvent(String topic, String payload, @Nullable String source) {
        ConversationEvent.ConversationDTO conversationDTO = deserializePayload(payload,
                ConversationEvent.ConversationDTO.class);
        return new ConversationRemovedEvent(topic, payload, source, conversationDTO.conversationId);
    }

    private Event createConversationMessageAddedEvent(String topic, String payload, @Nullable String source) {
        ConversationMessageAddedEvent.ConversationMessageAddedDTO messageDTO = deserializePayload(payload,
                ConversationMessageAddedEvent.ConversationMessageAddedDTO.class);
        return new ConversationMessageAddedEvent(topic, payload, source, messageDTO.conversationId,
                messageDTO.messageId, messageDTO.role, messageDTO.text);
    }

    private Event createConversationMessagesRemovedEvent(String topic, String payload, @Nullable String source) {
        ConversationMessagesRemovedEvent.ConversationMessagesRemovedDTO messageDTO = deserializePayload(payload,
                ConversationMessagesRemovedEvent.ConversationMessagesRemovedDTO.class);
        return new ConversationMessagesRemovedEvent(topic, payload, source, messageDTO.conversationId,
                messageDTO.removedSinceMessagesId);
    }

    private static String buildTopic(String template, String id) {
        return template.replace("{id}", id);
    }

    public static ConversationCreatedEvent createConversationCreatedEvent(String conversationId,
            @Nullable String source) {
        String payload = serializePayload(new ConversationEvent.ConversationDTO().withConversationId(conversationId));
        return new ConversationCreatedEvent(buildTopic(CONVERSATION_ADDED_TOPIC, conversationId), payload, source,
                conversationId);
    }

    public static ConversationRemovedEvent createConversationRemovedEvent(String conversationId,
            @Nullable String source) {
        String payload = serializePayload(new ConversationEvent.ConversationDTO().withConversationId(conversationId));
        return new ConversationRemovedEvent(buildTopic(CONVERSATION_REMOVED_TOPIC, conversationId), payload, source,
                conversationId);
    }

    public static ConversationMessageAddedEvent createConversationMessageAddedEvent(String conversationId,
            int messageId, ConversationRole role, String text, @Nullable String source) {
        String payload = serializePayload(new ConversationMessageAddedEvent.ConversationMessageAddedDTO()
                .withConversationId(conversationId).withId(messageId).withParticipant(role).withText(text));
        return new ConversationMessageAddedEvent(buildTopic(CONVERSATION_MESSAGE_ADDED_TOPIC, conversationId), payload,
                source, conversationId, messageId, role, text);
    }

    public static ConversationMessagesRemovedEvent createConversationMessagesRemovedEvent(String conversationId,
            int removedSinceMessagesId, @Nullable String source) {
        String payload = serializePayload(new ConversationMessagesRemovedEvent.ConversationMessagesRemovedDTO()
                .withConversationId(conversationId).withRemovedSinceMessagesId(removedSinceMessagesId));
        return new ConversationMessagesRemovedEvent(buildTopic(CONVERSATION_MESSAGES_REMOVED_TOPIC, conversationId),
                payload, source, conversationId, removedSinceMessagesId);
    }
}
