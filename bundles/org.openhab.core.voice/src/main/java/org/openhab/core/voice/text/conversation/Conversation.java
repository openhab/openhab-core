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
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Conversation} class contains a list of messages in between the user and a LanguageInterpreter.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class Conversation {
    public static final int DEFAULT_MAX_MESSAGES = 50;

    private final List<Message> messages;
    private final String id;
    private final CopyOnWriteArrayList<ConversationListener> listeners = new CopyOnWriteArrayList<>();
    private int maxMessages = DEFAULT_MAX_MESSAGES;

    public Conversation(String id) {
        this.id = id;
        this.messages = new ArrayList<>();
    }

    public Conversation(String id, List<Message> messages) {
        this.id = id;
        this.messages = new ArrayList<>(messages);
    }

    /**
     * Adds a {@link ConversationListener}.
     *
     * @param listener the listener
     */
    public void addListener(ConversationListener listener) {
        listeners.addIfAbsent(listener);
    }

    /**
     * Removes a {@link ConversationListener}.
     *
     * @param listener the listener
     */
    public void removeListener(ConversationListener listener) {
        listeners.remove(listener);
    }

    private void notifyMessageAdded(Message message) {
        listeners.forEach(l -> l.onMessageAdded(this, message));
    }

    private void notifyMessagesRemoved(int removedSinceMessagesId) {
        listeners.forEach(l -> l.onMessagesRemoved(this, removedSinceMessagesId));
    }

    /**
     * Set the maximum number of messages to keep in history.
     * At least 5 messages must be kept in the history.
     *
     * @param maxMessages the maximum number of messages
     */
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = Math.max(5, maxMessages);
        synchronized (messages) {
            while (messages.size() > this.maxMessages) {
                messages.removeFirst();
            }
        }
    }

    /**
     * Get a copy of the list of messages.
     *
     * @return list of messages
     */
    public List<Message> getMessages() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    /**
     * Get the last message (if any).
     *
     * @return the last message or null
     */
    public @Nullable Message getLastMessage() {
        synchronized (messages) {
            if (messages.isEmpty()) {
                return null;
            }
            return messages.getLast();
        }
    }

    /**
     * Adds a new message to the end of the conversation.
     *
     * @param role the role that authored this message
     * @param content the content of the message
     * @return the UID of the newly added message
     * @throws ConversationException see {@link #addMessage(ConversationRole, String, Integer)}
     */
    public int addMessage(ConversationRole role, String content) throws ConversationException {
        return addMessage(role, content, null);
    }

    /**
     * Adds a new message to the end of the conversation.
     *
     * <p>
     * The following conditions are enforced:
     * <ul>
     * <li>If <code>prevMessageUID</code> is provided: The last message has the expected UID, so the conversation hasn't
     * changed unexpectedly.</li>
     * <li>A tool return message must come after a tool call message.</li>
     * <li>The first message is expected to be a user message.</li>
     * </ul>
     *
     * @param role the role that authored this message
     * @param content the content of the message
     * @param prevMessageID the ID of the (assumed) previous message
     * @return the UID of the newly added message
     * @throws ConversationException when any of the above conditions is violated
     */
    public int addMessage(ConversationRole role, String content, @Nullable Integer prevMessageID)
            throws ConversationException {
        Message message;
        synchronized (messages) {
            @Nullable
            Message lastMessage = messages.isEmpty() ? null : messages.getLast();
            if (lastMessage != null) {
                if (prevMessageID != null && prevMessageID != lastMessage.id()) {
                    throw new ConversationException("Conversation has changed");
                }
                if (role == ConversationRole.TOOL_RETURN) {
                    if (lastMessage.role() != ConversationRole.TOOL_CALL) {
                        throw new ConversationException("Tool result should be after tool call");
                    }
                }
            } else if (!role.equals(ConversationRole.USER)) {
                throw new ConversationException("First message should be a user message");
            }
            int id = lastMessage != null ? lastMessage.id() + 1 : 0;
            message = new Message(id, role, content);
            while (messages.size() >= maxMessages) {
                messages.removeFirst();
            }
            messages.add(message);
        }
        notifyMessageAdded(message);
        return message.id();
    }

    /**
     * Removes a specific messages and all subsequent messages from the conversation.
     *
     * @param id the ID of the message and its successors to remove
     * @return whether an actual removal happened
     */
    public boolean removeSinceMessage(int id) {
        boolean removed = false;
        synchronized (messages) {
            var messageOptional = messages.stream().filter(m -> m.id() == id).findFirst();
            if (messageOptional.isPresent()) {
                int index = messages.indexOf(messageOptional.get());
                messages.subList(index, messages.size()).clear();
                removed = true;
            }
        }
        if (removed) {
            notifyMessagesRemoved(id);
        }
        return removed;
    }

    /**
     * Remove all messages from the conversation.
     */
    public void removeMessages() {
        boolean removed = false;
        synchronized (messages) {
            if (!messages.isEmpty()) {
                messages.clear();
                removed = true;
            }
        }
        if (removed) {
            notifyMessagesRemoved(0);
        }
    }

    public String getId() {
        return id;
    }

    /**
     * A message.
     *
     * @param id the ID of the message
     * @param role the role that authored the message
     * @param content the content of the message
     */
    public record Message(int id, ConversationRole role, String content) {
    }
}
