/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.sse.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.io.rest.sse.beans.EventBean;
import org.glassfish.jersey.media.sse.OutboundEvent;

/**
 * Utility class containing helper methods for the SSE implementation.
 * 
 * @author Ivan Iliev - Initial Contribution and API
 * @author Dennis Nobel - Changed EventBean
 */
public class SseUtil {
    static final String TOPIC_VALIDATE_PATTERN = "(\\w*\\*?\\/?,?\\s*)*";

    static {
        boolean servlet3 = false;
        try {
            servlet3 = ServletRequest.class.getMethod("startAsync") != null;
        } catch (Exception e) {
        } finally {
            SERVLET3_SUPPORT = servlet3;
        }
    }

    /**
     * True if the {@link ServletRequest} class has a "startAsync" method,
     * otherwise false.
     */
    public static final boolean SERVLET3_SUPPORT;

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
     * Used to mark our current thread(request processing) that SSE blocking
     * should be enabled.
     */
    private static ThreadLocal<Boolean> blockingSseEnabled = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * Returns true if the current thread is processing an SSE request that
     * should block.
     * 
     * @return
     */
    public static boolean shouldAsyncBlock() {
        return blockingSseEnabled.get().booleanValue();
    }

    /**
     * Marks the current thread as processing a blocking sse request.
     */
    public static void enableBlockingSse() {
        blockingSseEnabled.set(true);
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
        List<String> filters = new ArrayList<String>();

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
