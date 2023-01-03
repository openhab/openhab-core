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
package org.openhab.core.events;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TopicEventFilter} is a default openHAB {@link EventFilter} implementation that ensures filtering
 * of events based on an event topic.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class TopicEventFilter implements EventFilter {

    private final Pattern topicRegex;

    /**
     * Constructs a new topic event filter.
     *
     * @param topicRegex the regular expression of a topic
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html">Java Regex</a>
     */
    public TopicEventFilter(String topicRegex) {
        this.topicRegex = Pattern.compile(topicRegex);
    }

    @Override
    public boolean apply(Event event) {
        return topicRegex.matcher(event.getTopic()).matches();
    }
}
