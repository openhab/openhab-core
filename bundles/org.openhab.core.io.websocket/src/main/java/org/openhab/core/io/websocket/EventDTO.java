/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.io.websocket;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;

/**
 * The {@link EventDTO} is used for serialization and deserialization of events
 *
 * @author Jan N. Klug - Initial contribution
 */
public class EventDTO {
    public @Nullable String type;
    public @Nullable String topic;
    public @Nullable String payload;
    public @Nullable String source;

    public @Nullable String eventId;

    public EventDTO() {
    }

    public EventDTO(String type, String topic, String payload, @Nullable String source, @Nullable String eventId) {
        this.type = type;
        this.topic = topic;
        this.payload = payload;
        this.source = source;
        this.eventId = eventId;
    }

    public EventDTO(Event event) {
        type = event.getType();
        topic = event.getTopic();
        source = event.getSource();
        payload = event.getPayload();
    }
}
