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
package org.eclipse.smarthome.io.rest.sse.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.smarthome.io.rest.sse.internal.util.SseUtil;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Test;

/**
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class SseResourceOSGiTest extends JavaOSGiTest {

    @Test
    public void testValidInvalidFilters() {
        // invalid
        assertThat(SseUtil.isValidTopicFilter("smarthome/.*"), is(false));
        assertThat(SseUtil.isValidTopicFilter("smarthome/\\w*/"), is(false));
        assertThat(SseUtil.isValidTopicFilter("sm.*/test/"), is(false));
        assertThat(SseUtil.isValidTopicFilter("smarthome.*"), is(false));

        // valid
        assertThat(SseUtil.isValidTopicFilter("smarthome"), is(true));
        assertThat(SseUtil.isValidTopicFilter(""), is(true));
        assertThat(SseUtil.isValidTopicFilter(", smarthome/*"), is(true));
        assertThat(SseUtil.isValidTopicFilter("smarthome,qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("smarthome , qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("smarthome,    qivicon"), is(true));
        assertThat(SseUtil.isValidTopicFilter("smarthome/test"), is(true));
        assertThat(SseUtil.isValidTopicFilter("smarthome/test/test/test/test/test"), is(true));
        assertThat(
                SseUtil.isValidTopicFilter("smarthome/test/test/test/test/test,    smarthome/test/test/test/test/test"),
                is(true));
        assertThat(
                SseUtil.isValidTopicFilter(
                        "smarthome/test/test/test/test/test,    smarthome/test/test/test/test/test, smarthome,qivicon"),
                is(true));
        assertThat(SseUtil.isValidTopicFilter("////////////"), is(true));
        assertThat(SseUtil.isValidTopicFilter("*/added"), is(true));
        assertThat(SseUtil.isValidTopicFilter("*added"), is(true));
    }

    @Test
    public void testFilterMatchers() {
        List<String> regexes = SseUtil.convertToRegex(
                "smarthome/*/test/test/test/test,    smarthome/test/*/test/test/test, smarthome,qivicon");

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(false));

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(1)), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(1)), is(false));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regexes.get(1)), is(false));

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(2)), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(2)), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regexes.get(2)), is(true));

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(3)), is(false));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(3)), is(false));
        assertThat("qivicon/asdf/ASDF/test/test/test".matches(regexes.get(3)), is(true));
    }

    @Test
    public void testMoreFilterMatchers() {
        List<String> regexes = SseUtil.convertToRegex(",    *, smarthome/items/*/added, smarthome/items");

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(true));

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(1)), is(false));
        assertThat("smarthome/items/anyitem/added".matches(regexes.get(1)), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regexes.get(1)), is(false));

        assertThat("smarthome/items/anyitem/added".matches(regexes.get(2)), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regexes.get(2)), is(true));
        assertThat("smarthome/items/anyitem/updated".matches(regexes.get(2)), is(true));
        assertThat("smarthome/things/anything/updated".matches(regexes.get(2)), is(false));
    }

    @Test
    public void testEvenMoreFilterMatchers() {
        List<String> regexes = SseUtil.convertToRegex("");

        assertThat("smarthome/test/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regexes.get(0)), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regexes.get(0)), is(true));

        regexes = SseUtil.convertToRegex("*/added");
        assertThat("smarthome/items/anyitem/added".matches(regexes.get(0)), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regexes.get(0)), is(false));

        regexes = SseUtil.convertToRegex("*added");
        assertThat("smarthome/items/anyitem/added".matches(regexes.get(0)), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regexes.get(0)), is(false));
    }
}
