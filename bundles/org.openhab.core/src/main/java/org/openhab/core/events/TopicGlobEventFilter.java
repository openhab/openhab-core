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

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TopicGlobalEventFilter} is a default openHAB {@link EventFilter} implementation that ensures filtering
 * of events based on an event topic.
 * 
 * The syntax for the filter is the glob syntax documented at
 * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class TopicGlobEventFilter implements EventFilter {

    private final PathMatcher topicMatcher;

    /**
     * Constructs a new topic event filter.
     *
     * @param topicGlob the glob
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">Java
     *      Glob</a>
     */
    public TopicGlobEventFilter(String topicGlob) {
        this.topicMatcher = FileSystems.getDefault().getPathMatcher("glob:" + topicGlob);
    }

    @Override
    public boolean apply(Event event) {
        return topicMatcher.matches(Paths.get(event.getTopic()));
    }
}
