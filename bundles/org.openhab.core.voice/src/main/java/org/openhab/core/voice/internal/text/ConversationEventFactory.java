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
package org.openhab.core.voice.internal.text;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.openhab.core.voice.text.ConversationRole;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ConversationEventFactory} defines a {@link Event} implementation that emits conversation changes.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@Component(immediate = true, service = EventFactory.class)
@NonNullByDefault
public class ConversationEventFactory extends AbstractEventFactory {
    static final String TOPIC_TEMPLATE = "openhab/conversations/{id}";

    /**
     * Constructs a new ConversationEventFactory.
     */
    public ConversationEventFactory() {
        super(Set.of(ConversationEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception {
        ConversationEvent.ConversationMessageDTO messageDTO = deserializePayload(payload,
                ConversationEvent.ConversationMessageDTO.class);
        return new ConversationEvent(topic, payload, source, messageDTO.uid, messageDTO.role, messageDTO.text);
    }

    public static ConversationEvent createConversationEvent(String conversationId, String messageId,
            ConversationRole role, String text) {
        String payload = serializePayload(
                new ConversationEvent.ConversationMessageDTO().withUID(messageId).withParticipant(role).withText(text));
        return new ConversationEvent(buildTopic(conversationId), payload, null, messageId, role, text);
    }

    static String buildTopic(String id) {
        return TOPIC_TEMPLATE.replace("{id}", id);
    }

    /**
     * The {@link ConversationEvent} defines a {@link Event} implementation that emits conversation changes.
     *
     * @author Miguel Álvarez Díez - Initial contribution
     */
    @NonNullByDefault
    public static class ConversationEvent extends AbstractEvent {
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
}
