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
 * The {@link ConversationMessageEvent} defines a {@link org.openhab.core.events.Event} implementation that emits
 * messages of a conversation.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class ConversationMessageEvent extends ConversationEvent {
    /**
     * The conversation message event type.
     */
    public static final String TYPE = ConversationMessageEvent.class.getSimpleName();

    private final int messageId;
    private final ConversationRole role;
    private final String text;

    public ConversationMessageEvent(String topic, String payload, @Nullable String source, String conversationUID,
            int messageId, ConversationRole role, String text) {
        super(topic, payload, source, conversationUID);
        this.messageId = messageId;
        this.role = role;
        this.text = text;
    }

    @Override
    public String getType() {
        return TYPE;
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

    public static class ConversationMessageDTO extends ConversationEvent.ConversationDTO {
        public int id = 0;
        public ConversationRole role = ConversationRole.USER;
        public String text = "";

        @Override
        public ConversationMessageDTO withConversationUID(String uid) {
            return (ConversationMessageDTO) super.withConversationUID(uid);
        }

        public ConversationMessageDTO withMessageId(int id) {
            this.id = id;
            return this;
        }

        public ConversationMessageDTO withParticipant(ConversationRole role) {
            this.role = role;
            return this;
        }

        public ConversationMessageDTO withText(String text) {
            this.text = text;
            return this;
        }
    }
}
