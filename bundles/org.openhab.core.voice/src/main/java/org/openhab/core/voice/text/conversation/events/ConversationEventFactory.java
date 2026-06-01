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
 * @author Florian Hotze - Differ between added, removed & message events
 */
@Component(immediate = true, service = EventFactory.class)
@NonNullByDefault
public class ConversationEventFactory extends AbstractEventFactory {
    private static final String CONVERSATION_ADDED_TOPIC = "openhab/conversations/{id}/added";
    private static final String CONVERSATION_REMOVED_TOPIC = "openhab/conversations/{id}/removed";
    private static final String CONVERSATION_MESSAGE_TOPIC = "openhab/conversations/{id}/message";

    /**
     * Constructs a new ConversationEventFactory.
     */
    public ConversationEventFactory() {
        super(Set.of(ConversationMessageEvent.TYPE, ConversationAddedEvent.TYPE, ConversationRemovedEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source) {
        if (ConversationMessageEvent.TYPE.equals(eventType)) {
            return createConversationMessageEvent(topic, payload, source);
        } else if (ConversationAddedEvent.TYPE.equals(eventType)) {
            return createConversationAddedEvent(topic, payload, source);
        } else if (ConversationRemovedEvent.TYPE.equals(eventType)) {
            return createConversationRemovedEvent(topic, payload, source);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    private Event createConversationMessageEvent(String topic, String payload, @Nullable String source) {
        ConversationMessageEvent.ConversationMessageDTO messageDTO = deserializePayload(payload,
                ConversationMessageEvent.ConversationMessageDTO.class);
        return new ConversationMessageEvent(topic, payload, source, messageDTO.uid, messageDTO.role, messageDTO.text);
    }

    private Event createConversationAddedEvent(String topic, String payload, @Nullable String source) {
        ConversationEvent.ConversationDTO addedDTO = deserializePayload(payload,
                ConversationEvent.ConversationDTO.class);
        return new ConversationAddedEvent(topic, payload, source, addedDTO.uid);
    }

    private Event createConversationRemovedEvent(String topic, String payload, @Nullable String source) {
        ConversationEvent.ConversationDTO removedDTO = deserializePayload(payload,
                ConversationEvent.ConversationDTO.class);
        return new ConversationRemovedEvent(topic, payload, source, removedDTO.uid);
    }

    public static ConversationMessageEvent createConversationMessageEvent(String conversationId, String messageId,
            ConversationRole role, String text, @Nullable String source) {
        String payload = serializePayload(new ConversationMessageEvent.ConversationMessageDTO().withUID(messageId)
                .withParticipant(role).withText(text));
        return new ConversationMessageEvent(buildTopic(CONVERSATION_MESSAGE_TOPIC, conversationId), payload, source,
                messageId, role, text);
    }

    public static ConversationAddedEvent createConversationAddedEvent(String conversationId, @Nullable String source) {
        String payload = serializePayload(new ConversationEvent.ConversationDTO().withUID(conversationId));
        return new ConversationAddedEvent(buildTopic(CONVERSATION_ADDED_TOPIC, conversationId), payload, source,
                conversationId);
    }

    public static ConversationRemovedEvent createConversationRemovedEvent(String conversationId,
            @Nullable String source) {
        String payload = serializePayload(new ConversationEvent.ConversationDTO().withUID(conversationId));
        return new ConversationRemovedEvent(buildTopic(CONVERSATION_REMOVED_TOPIC, conversationId), payload, source,
                conversationId);
    }

    static String buildTopic(String template, String id) {
        return template.replace("{id}", id);
    }
}
