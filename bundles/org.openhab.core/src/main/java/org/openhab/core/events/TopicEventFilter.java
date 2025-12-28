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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TopicEventFilter} is a default openHAB {@link EventFilter} implementation that ensures filtering
 * of events based on a single event topic or multiple event topics.
 * <p>
 * Thread-safe.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Florian Hotze - Add support for filtering of events by multiple event topics
 */
@NonNullByDefault
public class TopicEventFilter implements EventFilter {

    private final List<Pattern> topicsRegexes;

    /**
     * Constructs a new topic event filter.
     *
     * @param topicRegex the regular expression of a topic
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">Java
     *      Regex</a>
     */
    public TopicEventFilter(String topicRegex) {
        this.topicsRegexes = List.of(Pattern.compile(topicRegex));
    }

    /**
     * Constructs a new topic event filter.
     *
     * @param topicsRegexes the regular expressions of multiple topics
     * @throws PatternSyntaxException indicate a syntax error in a any of the regular-expression patterns
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">Java
     *      Regex</a>
     */
    public TopicEventFilter(List<String> topicsRegexes) throws PatternSyntaxException {
        List<Pattern> tmpTopicsRegexes = new ArrayList<>();
        for (String topicRegex : topicsRegexes) {
            tmpTopicsRegexes.add(Pattern.compile(topicRegex));
        }
        this.topicsRegexes = Collections.unmodifiableList(tmpTopicsRegexes);
    }

    @Override
    public boolean apply(Event event) {
        return topicsRegexes.stream().anyMatch(p -> p.matcher(event.getTopic()).matches());
    }
}
