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

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.openhab.core.events.Event;
import org.openhab.core.io.rest.sse.beans.EventBean;

/**
 * Utility class containing helper methods for the SSE implementation.
 *
 * @author Ivan Iliev - Initial contribution
 * @author Dennis Nobel - Changed EventBean
 */
public class SseUtil {
    static final String TOPIC_VALIDATE_PATTERN = "(\\w*\\*?\\/?,?:?-?\\s*)*";

    /**
     * Creates a new {@link OutboundEvent} object containing an {@link EventBean} created for the given Eclipse
     * SmartHome {@link Event}.
     *
     * @param event the event
     *
     * @return a new OutboundEvent
     */
    public static OutboundEvent buildEvent(Event event) {
        EventBean eventBean = new EventBean();
        eventBean.topic = event.getTopic();
        eventBean.type = event.getType();
        eventBean.payload = event.getPayload();

        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        OutboundEvent outboundEvent = eventBuilder.name("message").mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(eventBean).build();

        return outboundEvent;
    }

    /**
     * Validates the given topicFilter
     *
     * @param topicFilter
     * @return true if the given input filter is empty or a valid topic filter string
     *
     */
    public static boolean isValidTopicFilter(String topicFilter) {
        return StringUtils.isEmpty(topicFilter) || topicFilter.matches(TOPIC_VALIDATE_PATTERN);
    }

    /**
     * Splits the given topicFilter at any commas (",") and for each token replaces any wildcards(*) with the regex
     * pattern (.*)
     *
     * @param topicFilter
     * @return
     */
    public static List<String> convertToRegex(String topicFilter) {
        List<String> filters = new ArrayList<>();

        if (StringUtils.isEmpty(topicFilter)) {
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
