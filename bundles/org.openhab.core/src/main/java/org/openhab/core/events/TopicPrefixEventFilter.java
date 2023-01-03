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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TopicPrefixEventFilter} is a default openHAB {@link EventFilter} implementation that ensures filtering
 * of events based on the prefix of an event topic.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class TopicPrefixEventFilter implements EventFilter {

    private final String topicPrefix;

    /**
     * Constructs a new topic event filter.
     *
     * @param topicRegex the prefix event topics must start with
     */
    public TopicPrefixEventFilter(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    @Override
    public boolean apply(Event event) {
        return event.getTopic().startsWith(topicPrefix);
    }
}
