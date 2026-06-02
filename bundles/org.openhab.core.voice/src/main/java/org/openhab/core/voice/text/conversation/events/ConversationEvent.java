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
import org.openhab.core.events.AbstractEvent;

/**
 * Abstract implementation of a conversation event which will be posted for conversation changes.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public abstract class ConversationEvent extends AbstractEvent {
    private final String uid;

    /**
     * Must be called in subclass constructor to create a new event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     */
    protected ConversationEvent(String topic, String payload, @Nullable String source, String uid) {
        super(topic, payload, source);
        this.uid = uid;
    }

    @Override
    abstract public String getType();

    public String getUid() {
        return uid;
    }

    public static class ConversationDTO {
        public String conversationUID = "";

        public ConversationDTO withConversationUID(String uid) {
            this.conversationUID = uid;
            return this;
        }
    }
}
