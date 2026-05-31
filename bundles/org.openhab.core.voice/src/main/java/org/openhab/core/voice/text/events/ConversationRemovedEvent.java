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
package org.openhab.core.voice.text.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ConversationRemovedEvent} defines a {@link org.openhab.core.events.Event} that notifies about conversation
 * removal.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationRemovedEvent extends ConversationEvent {
    /**
     * The conversation removed event type.
     */
    public static final String TYPE = ConversationRemovedEvent.class.getSimpleName();

    public ConversationRemovedEvent(String topic, String payload, @Nullable String source, String uid) {
        super(topic, payload, source, uid);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
