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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.text.conversation.ConversationRole;

/**
 * The {@link ConversationMessageAddedEvent} defines a {@link org.openhab.core.events.Event} implementation that emits
 * on addition of a message to a conversation.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class ConversationMessageAddedEvent extends ConversationEvent {
    /**
     * The conversation message added event type.
     */
    public static final String TYPE = ConversationMessageAddedEvent.class.getSimpleName();

    private final int messageId;
    private final ConversationRole role;
    private final String text;

    public ConversationMessageAddedEvent(String topic, String payload, @Nullable String source, String conversationId,
            int messageId, ConversationRole role, String text) {
        super(topic, payload, source, conversationId);
        this.messageId = messageId;
        this.role = role;
        this.text = text;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Message '" + messageId + "' of role '" + role + "' has been added to conversation '"
                + getConversationId() + "'.";
    }

    public int getMessageId() {
        return messageId;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public static class ConversationMessageAddedDTO extends ConversationEvent.ConversationDTO {
        public int messageId = 0;
        public ConversationRole role = ConversationRole.USER;
        public String text = "";

        @Override
        public ConversationMessageAddedDTO withConversationId(String conversationId) {
            return (ConversationMessageAddedDTO) super.withConversationId(conversationId);
        }

        public ConversationMessageAddedDTO withId(int id) {
            this.messageId = id;
            return this;
        }

        public ConversationMessageAddedDTO withParticipant(ConversationRole role) {
            this.role = role;
            return this;
        }

        public ConversationMessageAddedDTO withText(String text) {
            this.text = text;
            return this;
        }
    }
}
