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
package org.openhab.core.io.rest.sse.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class SseUtilTest {

    @Test
    public void testValidInvalidFilters() {
        // invalid
        assertThat(SseUtil.isValidTopicFilter("openhab/.*"), is(false));
        assertThat(SseUtil.isValidTopicFilter("openhab/\\w*/"), is(false));
        assertThat(SseUtil.isValidTopicFilter("sm.*/test/"), is(false));
        assertThat(SseUtil.isValidTopicFilter("openhab.*"), is(false));

        // valid
        assertThat(SseUtil.isValidTopicFilter("openhab"), is(true));
        assertThat(SseUtil.isValidTopicFilter(""), is(true));
        assertThat(SseUtil.isValidTopicFilter(", openhab/*"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab,qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab , qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab,    qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab/test"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab/test/test/test/test/test"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab/test/test/test/test/test,    openhab/test/test/test/test/test"),
                is(true));
        assertThat(
                SseUtil.isValidTopicFilter(
                        "openhab/test/test/test/test/test,    openhab/test/test/test/test/test, openhab,qivicon"),
                is(true));
        assertThat(SseUtil.isValidTopicFilter("////////////"), is(true));
        assertThat(SseUtil.isValidTopicFilter("*/added"), is(true));
        assertThat(SseUtil.isValidTopicFilter("*added"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab/test/test:test:123/test"), is(true));
        assertThat(SseUtil.isValidTopicFilter("openhab/test/test-test-123-test:test:123/test"), is(true));
    }

    @Test
    public void testFilterMatchers() {
        List<String> regexes = SseUtil
                .convertToRegex("openhab/*/test/test/test/test,    openhab/test/*/test/test/test, openhab/*,qivicon/*");

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(false));

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(1)), is(true));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(1)), is(false));
        assertThat("openhab/asdf/ASDF/test/test/test".matches(regexes.get(1)), is(false));

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(2)), is(true));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(2)), is(true));
        assertThat("openhab/asdf/ASDF/test/test/test".matches(regexes.get(2)), is(true));

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(3)), is(false));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(3)), is(false));
        assertThat("qivicon/asdf/ASDF/test/test/test".matches(regexes.get(3)), is(true));
    }

    @Test
    public void testMoreFilterMatchers() {
        List<String> regexes = SseUtil.convertToRegex(",    *, openhab/items/*/added, openhab/items/*/*");

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(true));

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(1)), is(false));
        assertThat("openhab/items/anyitem/added".matches(regexes.get(1)), is(true));
        assertThat("openhab/items/anyitem/removed".matches(regexes.get(1)), is(false));

        assertThat("openhab/items/anyitem/added".matches(regexes.get(2)), is(true));
        assertThat("openhab/items/anyitem/removed".matches(regexes.get(2)), is(true));
        assertThat("openhab/items/anyitem/updated".matches(regexes.get(2)), is(true));
        assertThat("openhab/things/anything/updated".matches(regexes.get(2)), is(false));
    }

    @Test
    public void testEvenMoreFilterMatchers() {
        List<String> regexes = SseUtil.convertToRegex("");

        assertThat("openhab/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("openhab/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(true));

        regexes = SseUtil.convertToRegex("*/added");
        assertThat("openhab/items/anyitem/added".matches(regexes.get(0)), is(true));
        assertThat("openhab/items/anyitem/removed".matches(regexes.get(0)), is(false));

        regexes = SseUtil.convertToRegex("*added");
        assertThat("openhab/items/anyitem/added".matches(regexes.get(0)), is(true));
        assertThat("openhab/items/anyitem/removed".matches(regexes.get(0)), is(false));

        regexes = SseUtil.convertToRegex("openhab/items/*/state");
        assertThat("openhab/items/anyitem/state".matches(regexes.get(0)), is(true));
        assertThat("openhab/items/anyitem/statechanged".matches(regexes.get(0)), is(false));
    }
}
