/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.rest.sse.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.io.rest.sse.internal.dto.EventDTO;

/**
 * Utility class containing helper methods for the SSE implementation.
 *
 * @author Ivan Iliev - Initial contribution
 * @author Dennis Nobel - Changed EventBean
 * @author Markus Rathgeb - Don't depend on specific application but use APIs if possible
 */
@NonNullByDefault
public class SseUtil {
    static final String TOPIC_VALIDATE_PATTERN = "(\\w*\\*?\\/?,?:?-?\\s*)*";

    public static EventDTO buildDTO(final Event event) {
        EventDTO dto = new EventDTO();
        dto.topic = event.getTopic();
        dto.type = event.getType();
        dto.payload = event.getPayload();
        return dto;
    }

    /**
     * Creates a new {@link OutboundSseEvent} object containing an {@link EventDTO} created for the given {@link Event}.
     *
     * @param eventBuilder the builder that should be used
     * @param event the event data transfer object
     * @return a new OutboundEvent
     */
    public static OutboundSseEvent buildEvent(OutboundSseEvent.Builder eventBuilder, EventDTO event) {
        final OutboundSseEvent sseEvent = eventBuilder.name("message") //
                .mediaType(MediaType.APPLICATION_JSON_TYPE) //
                .data(event) //
                .build();

        return sseEvent;
    }

    /**
     * Validates the given topicFilter
     *
     * @param topicFilter
     * @return true if the given input filter is empty or a valid topic filter string
     *
     */
    public static boolean isValidTopicFilter(@Nullable String topicFilter) {
        return topicFilter == null || topicFilter.isEmpty() || topicFilter.matches(TOPIC_VALIDATE_PATTERN);
    }

    /**
     * Splits the given topicFilter at any commas (",") and for each token replaces any wildcards(*) with the regex
     * pattern (.*)
     *
     * @param topicFilter
     * @return
     */
    public static List<String> convertToRegex(@Nullable String topicFilter) {
        List<String> filters = new ArrayList<>();

        if (topicFilter == null || topicFilter.isEmpty()) {
            filters.add(".*");
        } else {
            StringTokenizer tokenizer = new StringTokenizer(topicFilter, ",");
            while (tokenizer.hasMoreElements()) {
                String regex = tokenizer.nextToken().trim().replace("*", ".*") + ".*";
                filters.add(regex);
            }
        }

        return filters;
    }
}
