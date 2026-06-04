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
 * The {@link ConversationCreatedEvent} defines a {@link org.openhab.core.events.Event} that notifies about a new
 * conversation.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationCreatedEvent extends ConversationEvent {
    /**
     * The conversation added event type.
     */
    public static final String TYPE = ConversationCreatedEvent.class.getSimpleName();

    public ConversationCreatedEvent(String topic, String payload, @Nullable String source, String conversationId) {
        super(topic, payload, source, conversationId);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
