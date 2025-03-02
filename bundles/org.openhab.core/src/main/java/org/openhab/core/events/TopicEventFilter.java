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
package org.openhab.core.events;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link TopicEventFilter} is a default openHAB {@link EventFilter} implementation that ensures filtering
 * of events based on a single event topic or multiple event topics.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Florian Hotze - Add support for filtering of events by multiple event topics
 */
@NonNullByDefault
public class TopicEventFilter implements EventFilter {

    private final @Nullable Pattern topicRegex;
    private final List<Pattern> topicsRegexes = new ArrayList<>();

    /**
     * Constructs a new topic event filter.
     *
     * @param topicRegex the regular expression of a topic
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">Java
     *      Regex</a>
     */
    public TopicEventFilter(String topicRegex) {
        this.topicRegex = Pattern.compile(topicRegex);
    }

    /**
     * Constructs a new topic event filter.
     *
     * @param topicsRegexes the regular expressions of multiple topics
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">Java
     *      Regex</a>
     */
    public TopicEventFilter(List<String> topicsRegexes) {
        this.topicRegex = null;
        for (String topicRegex : topicsRegexes) {
            this.topicsRegexes.add(Pattern.compile(topicRegex));
        }
    }

    @Override
    public boolean apply(Event event) {
        String topic = event.getTopic();
        Pattern topicRegex = this.topicRegex;
        if (topicRegex != null) {
            return topicRegex.matcher(topic).matches();
        } else {
            return topicsRegexes.stream().anyMatch(p -> p.matcher(topic).matches());
        }
    }
}
