/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.io.transport.mqtt.internal;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;

/**
 * A list of all subscribers for a given topic. This object also stores a regex pattern for the topic, where
 * the MQTT wildcards and special regex-characters got replaced.
 *
 * @author David Graeff - Initial contribution
 * @author Jan N. Klug - refactored for special cases and performance
 */
@NonNullByDefault
public class TopicSubscribers extends ArrayList<MqttMessageSubscriber> {
    private static final long serialVersionUID = -2969599983479371961L;

    // matches all regex-special characters "(){}[].*$^" (i.e. all except + and \)
    private static final Pattern REPLACE_SPECIAL_CHAR_PATTERN = Pattern
            .compile("([\\(\\)\\{\\}\\[\\]\\.\\*\\$\\^]{1})");

    private final Pattern topicRegexPattern;

    public TopicSubscribers(String topic) {
        // replace special characters
        String patternString = REPLACE_SPECIAL_CHAR_PATTERN.matcher(topic).replaceAll("\\\\$1");

        // replace single-level-wildcard (+) and multi-level-wildcard (#)
        patternString = StringUtils.replace(patternString, "+", "[^/]*");
        patternString = StringUtils.replace(patternString, "#", ".*");

        this.topicRegexPattern = Pattern.compile(patternString);
    }

    /**
     * check if topic matches this subscriber list
     *
     * @param topic a string representing the topic
     * @return true if matches
     */
    public boolean topicMatch(String topic) {
        return topicRegexPattern.matcher(topic).matches();
    }

    /**
     * get the regex matching pattern of this subcriber list
     *
     * @return the pattern as a string
     */
    public String getTopicRegexPattern() {
        return topicRegexPattern.pattern();
    }
}
