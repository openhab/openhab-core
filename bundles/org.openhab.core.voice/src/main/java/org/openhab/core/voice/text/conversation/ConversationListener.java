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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Listener interface for receiving events from a {@link Conversation}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface ConversationListener {

    /**
     * Called when a new message is added to the conversation.
     *
     * @param conversation the conversation
     * @param message the added message
     */
    void onMessageAdded(Conversation conversation, Conversation.Message message);

    /**
     * Called when messages are removed from the conversation.
     *
     * @param conversation the conversation
     */
    void onMessagesRemoved(Conversation conversation);
}
