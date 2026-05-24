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
package org.openhab.core.voice.internal.text.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.events.Event;
import org.openhab.core.voice.text.ConversationRole;

/**
 * The {@link ConversationEvent} defines a {@link Event} implementation that emits conversation changes.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class ConversationEvent extends AbstractEvent {
    /**
     * The extension event type.
     */
    public static final String TYPE = ConversationEvent.class.getSimpleName();
    private final String uid;
    private final ConversationRole role;
    private final String text;

    /**
     * Must be called in subclass constructor to create a new event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     */
    protected ConversationEvent(String topic, String payload, @Nullable String source, String uid,
            ConversationRole role, String text) {
        super(topic, payload, source);
        this.uid = uid;
        this.role = role;
        this.text = text;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getUid() {
        return uid;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public static class ConversationMessageDTO {
        public String uid = "";
        public ConversationRole role = ConversationRole.USER;
        public String text = "";

        public ConversationMessageDTO withUID(String uid) {
            this.uid = uid;
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
