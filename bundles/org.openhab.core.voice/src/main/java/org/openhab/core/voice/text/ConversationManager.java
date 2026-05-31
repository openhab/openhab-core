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
package org.openhab.core.voice.text;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ConversationManager} is responsible for managing the lifecycle and persistence of {@link Conversation}s.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface ConversationManager {

    /**
     * Gets a conversation by its identifier.
     *
     * <p>
     * If id is null or blank, return a conversation with blank id that can not be stored.
     * If no conversation with that id exists, create a new conversation.
     *
     * @param id the conversation identifier
     * @return the conversation
     */
    Conversation getConversation(String id);

    /**
     * Persists the conversation state to storage.
     *
     * <p>
     * If the conversation is empty (no messages), it should be removed from storage.
     * If the id is blank, it should not be persisted to storage.
     *
     * @param conversation the conversation to save
     */
    void storeConversation(Conversation conversation);

    /**
     * Explicitly removes a conversation from memory and storage.
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
}
