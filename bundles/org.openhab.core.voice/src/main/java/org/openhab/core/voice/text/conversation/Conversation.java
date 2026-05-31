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
package org.openhab.core.voice.text.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.voice.text.conversation.events.ConversationEventFactory;

/**
 * The {@link Conversation} class contains a list of messages in between the users and a LanguageInterpreter.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class Conversation {

    public static final int DEFAULT_MAX_MESSAGES = 50;

    private final List<Message> messages;
    private final String id;
    private final @Nullable EventPublisher eventPublisher;
    private int maxMessages = DEFAULT_MAX_MESSAGES;

    public Conversation(String id, @Nullable EventPublisher eventPublisher) {
        this.id = id;
        this.eventPublisher = eventPublisher;
        this.messages = new ArrayList<>();
    }

    public Conversation(String id, @Nullable EventPublisher eventPublisher, List<Message> messages) {
        this.id = id;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    /**
     * Set the maximum number of messages to keep in history
     *
     * @param maxMessages the maximum number of messages
     */
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
        synchronized (messages) {
            while (messages.size() > maxMessages) {
                messages.removeFirst();
            }
        }
    }

    /**
     * Get a copy of the list of messages
     *
     * @return list of messages
     */
    public List<Message> getMessages() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    /**
     * Get last message or null
     *
     * @return nullable last message
     */
    public @Nullable Message getLastMessage() {
        synchronized (messages) {
            if (messages.isEmpty()) {
                return null;
            }
            return messages.getLast();
        }
    }

    public String addMessage(ConversationRole role, String content) throws ConversationException {
        return addMessage(role, content, null);
    }

    public String addMessage(ConversationRole role, String content, @Nullable String prevMessageUID)
            throws ConversationException {
        Message lastMessage = getLastMessage();
        if (lastMessage != null) {
            if (prevMessageUID != null && !prevMessageUID.equals(lastMessage.getUID())) {
                throw new ConversationException("Conversation has changed");
            }
            switch (role) {
                case TOOL_RETURN -> {
                    if (lastMessage.getRole() != ConversationRole.TOOL_CALL) {
                        throw new ConversationException("Tool result should be after tool call");
                    }
                }
            }
        } else if (!role.equals(ConversationRole.USER)) {
            throw new ConversationException("First message should be an user message");
        }
        String uid = UUID.randomUUID().toString();
        synchronized (messages) {
            while (messages.size() >= maxMessages) {
                messages.removeFirst();
            }
            messages.add(new Message(uid, role, content));
        }
        if (eventPublisher != null) {
            eventPublisher
                    .post(ConversationEventFactory.createConversationMessageEvent(getId(), uid, role, content, null));
        }
        return uid;
    }

    public boolean removeSinceMessage(String uid) {
        synchronized (messages) {
            var messageOptional = messages.stream().filter(m -> m.getUID().equals(uid)).findFirst();
            if (messageOptional.isPresent()) {
                int index = messages.indexOf(messageOptional.get());
                messages.subList(index, messages.size()).clear();
                return true;
            }
        }
        return false;
    }

    public void removeMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public String getId() {
        return id;
    }

    public static final class Message {
        private final String uid;
        private final ConversationRole role;
        private final String content;

        public Message(String uid, ConversationRole role, String content) {
            this.uid = uid;
            this.role = role;
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public ConversationRole getRole() {
            return role;
        }

        public String getUID() {
            return uid;
        }
    }
}
