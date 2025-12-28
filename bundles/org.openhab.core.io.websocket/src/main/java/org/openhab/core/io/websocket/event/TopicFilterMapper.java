/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.websocket.event;

import static org.openhab.core.io.websocket.event.EventWebSocket.WEBSOCKET_EVENT_TYPE;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.TopicEventFilter;

/**
 * The {@link TopicFilterMapper} is used for mapping topic filter expression from the topic filter WebSocketEvent to
 * {@link TopicEventFilter}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public final class TopicFilterMapper {
    private static final Pattern TOPIC_VALIDATE_PATTERN = Pattern.compile("^(\\w*\\*?/?)+$");

    private TopicFilterMapper() {
    }

    private static String mapTopicToRegEx(String topic) {
        if (TOPIC_VALIDATE_PATTERN.matcher(topic).matches()) {
            // convert to regex: replace any wildcard (*) with the regex pattern (.*)
            return "^" + topic.trim().replace("*", ".*") + "$";
        }
        // assume is already a regex
        return topic;
    }

    /**
     * Maps the topic expressions to a {@link TopicEventFilter} for event inclusion.
     * 
     * @param topics the topic expressions
     * @return the {@link TopicEventFilter} or `null` if there are no inclusions defined
     * @throws EventProcessingException if a topic expression is invalid, i.e. neither a valid topic value, expression
     *             using the * wildcard or regular expression
     */
    public static @Nullable TopicEventFilter mapTopicsToIncludeFilter(List<String> topics)
            throws EventProcessingException {
        List<String> includeTopics = topics.stream() //
                .filter(t -> !t.startsWith("!")) // include topics (expressions) only
                .map(TopicFilterMapper::mapTopicToRegEx) // map topics (expressions) to RegEx
                .toList();
        if (includeTopics.isEmpty()) {
            return null;
        }
        TopicEventFilter filter;
        try {
            filter = new TopicEventFilter(includeTopics);
        } catch (PatternSyntaxException e) {
            throw new EventProcessingException("Invalid topic expression in topic filter " + WEBSOCKET_EVENT_TYPE, e);
        }
        return filter;
    }

    /**
     * Maps the topic expressions to a {@link TopicEventFilter} for event exclusion.
     * 
     * @param topics the topic expressions
     * @return the {@link TopicEventFilter} or `null` if there are no exclusions defined
     * @throws EventProcessingException if a topic expression is invalid, i.e. neither a valid topic value, expression
     *             using the * wildcard or regular expression
     */
    public static @Nullable TopicEventFilter mapTopicsToExcludeFilter(List<String> topics)
            throws EventProcessingException {
        List<String> excludeTopics = topics.stream() //
                .filter(t -> t.startsWith("!")) // exclude topics (expressions) only
                .map(t -> t.substring(1)) // remove the exclamation mark
                .map(TopicFilterMapper::mapTopicToRegEx) // map topics (expressions) to RegEx
                .toList();
        if (excludeTopics.isEmpty()) {
            return null;
        }
        TopicEventFilter filter;
        try {
            filter = new TopicEventFilter(excludeTopics);
        } catch (PatternSyntaxException e) {
            throw new EventProcessingException("Invalid topic expression in topic filter " + WEBSOCKET_EVENT_TYPE, e);
        }
        return filter;
    }
}
