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
package org.eclipse.smarthome.io.transport.mqtt.internal;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for the {@link TopicSubscriber} class
 *
 * Checks for several topics that the matcher is as expected and the original topic is really matched
 *
 * @author Jan N. Klug - Initial contribution
 */
public class TopicSubscriberTests {

    @Test
    public void simpleTopic() {
        String testTopic = "foo/bar";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/bar", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void whiteSpaceTopic() {
        String testTopic = "foo/b ar";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/b ar", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterStarTopic() {
        String testTopic = "foo/bar*";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/bar\\*", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterDollarTopic() {
        String testTopic = "foo/$bar";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/\\$bar", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterCaretTopic() {
        String testTopic = "foo/b^ar";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/b\\^ar", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterDotTopic() {
        String testTopic = "foo/ba.r";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/ba\\.r", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterParenthesesTopic() {
        String testTopic = "foo/b(a)r";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/b\\(a\\)r", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterBracesTopic() {
        String testTopic = "foo/b{a}r";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/b\\{a\\}r", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void specialCharacterSquareBracketsTopic() {
        String testTopic = "foo/b[a]r";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/b\\[a\\]r", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch(testTopic));
    }

    @Test
    public void singleLevelWildcardTopic() {
        String testTopic = "foo/+/bar";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/[^/]*/bar", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch("foo/silly/bar"));
        assertFalse(subscribers.topicMatch("foo/silly/two/bar"));
    }

    @Test
    public void multiLevelWildcardTopic() {
        String testTopic = "foo/#";
        TopicSubscribers subscribers = new TopicSubscribers(testTopic);

        assertEquals("foo/.*", subscribers.getTopicRegexPattern());
        assertTrue(subscribers.topicMatch("foo/bar"));
        assertTrue(subscribers.topicMatch("foo/silly/bar"));
    }

}
