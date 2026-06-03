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

/**
 * The {@link ConversationMessagesRemovedEvent} defines a {@link org.openhab.core.events.Event} implementation that
 * emits on removal of messages from a conversation.
 */
@NonNullByDefault
public class ConversationMessagesRemovedEvent extends ConversationEvent {
    /**
     * The conversation messages removed event type.
     */
    public static final String TYPE = ConversationMessagesRemovedEvent.class.getSimpleName();

    private final int removedSinceMessagesId;

    public ConversationMessagesRemovedEvent(String topic, String payload, @Nullable String source,
            String conversationId, int removedSinceMessagesId) {
        super(topic, payload, source, conversationId);
        this.removedSinceMessagesId = removedSinceMessagesId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public int getremovedSinceMessagesId() {
        return removedSinceMessagesId;
    }

    public static class ConversationMessagesRemovedDTO extends ConversationEvent.ConversationDTO {
        public int removedSinceMessagesId = 0;

        @Override
        public ConversationMessagesRemovedDTO withConversationId(String conversationId) {
            return (ConversationMessagesRemovedDTO) super.withConversationId(conversationId);
        }

        public ConversationMessagesRemovedDTO withRemovedSinceMessagesId(int removedSinceMessagesId) {
            this.removedSinceMessagesId = removedSinceMessagesId;
            return this;
        }
    }
}
