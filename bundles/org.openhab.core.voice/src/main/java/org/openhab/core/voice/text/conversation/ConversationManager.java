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

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ConversationManager} is responsible for managing the lifecycle and persistence of {@link Conversation}s.
 *
 * <p>
 * Implementations should:
 * <ul>
 * <li>Automatically persist a persistable {@link Conversation} whenever a message is added or messages are
 * removed.</li>
 * <li>Emit {@link org.openhab.core.voice.text.conversation.events.ConversationEvent} implementations accordingly.</li>
 * <li>If a conversation has a blank ID, it must not be persisted and events must not be emitted.</li>
 * </ul>
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface ConversationManager {
    /**
     * Gets a conversation by its identifier.
     *
     * <p>
     * If no conversation with that ID exists, create a new conversation.
     *
     * @param id the unique identifier of the conversation
     * @return the conversation
     */
    default Conversation getConversation(String id) {
        return Objects.requireNonNull(getConversation(id, true));
    }

    /**
     * Gets a conversation by its unique ID.
     *
     * @param id the unique identifier of the conversation
     * @param createIfMissing whether to create a conversation with the provided <code>id</code> if none exists
     * @return the conversation
     */
    @Nullable
    Conversation getConversation(String id, boolean createIfMissing);

    /**
     * Explicitly removes a conversation storage.
     *
     * @param id the conversation identifier
     */
    void removeConversation(String id);

    /**
     * Returns all currently active or stored conversations.
     *
     * @return a collection of all conversations
     */
    Collection<Conversation> getConversations();

    /**
     * Sets the maximum number of messages to keep in a conversation history.
     *
     * @param limit the maximum number of messages
     */
    void setHistoryLimit(int limit);
}
