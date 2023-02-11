/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;

/**
 * The {@link EventDTO} is used for serialization and deserialization of events
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class EventDTO {
    public @Nullable String type;
    public @Nullable String topic;
    public @Nullable String payload;
    public @Nullable String source;

    public @Nullable String eventId;

    public EventDTO() {
    }

    public EventDTO(String type, String topic, @Nullable String payload, @Nullable String source,
            @Nullable String eventId) {
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventDTO eventDTO = (EventDTO) o;
        return Objects.equals(type, eventDTO.type) && Objects.equals(topic, eventDTO.topic)
                && Objects.equals(payload, eventDTO.payload) && Objects.equals(source, eventDTO.source)
                && Objects.equals(eventId, eventDTO.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, topic, payload, source, eventId);
    }

    @Override
    public String toString() {
        return "EventDTO{type='" + type + "', topic='" + topic + "', payload='" + payload + "', source='" + source
                + "', eventId='" + eventId + "'}";
    }
}
