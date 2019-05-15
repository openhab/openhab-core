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
package org.eclipse.smarthome.io.rest.sse.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.io.rest.sse.SseResource;
import org.junit.Test;

/**
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 * @author David Graeff - Migrated to new APIs
 */
public class SseResourceTest {

    @Test
    public void testValidInvalidFilters() {
        // invalid
        assertThat(SseResource.isValidTopic("smarthome/.*"), is(false));
        assertThat(SseResource.isValidTopic("smarthome/\\w*/"), is(false));
        assertThat(SseResource.isValidTopic("sm.*/test/"), is(false));
        assertThat(SseResource.isValidTopic("smarthome.*"), is(false));

        // valid
        assertThat(SseResource.isValidTopic("smarthome"), is(true));
        assertThat(SseResource.isValidTopic(""), is(true));
        assertThat(SseResource.isValidTopic(", smarthome/*"), is(true));
        assertThat(SseResource.isValidTopic("smarthome,qivicon"), is(true));
        assertThat(SseResource.isValidTopic("smarthome , qivicon"), is(true));
        assertThat(SseResource.isValidTopic("smarthome,    qivicon"), is(true));
        assertThat(SseResource.isValidTopic("smarthome/test"), is(true));
        assertThat(SseResource.isValidTopic("smarthome/test/test/test/test/test"), is(true));
        assertThat(
                SseResource.isValidTopic("smarthome/test/test/test/test/test,    smarthome/test/test/test/test/test"),
                is(true));
        assertThat(
                SseResource.isValidTopic(
                        "smarthome/test/test/test/test/test,    smarthome/test/test/test/test/test, smarthome,qivicon"),
                is(true));
        assertThat(SseResource.isValidTopic("////////////"), is(true));
        assertThat(SseResource.isValidTopic("*/added"), is(true));
        assertThat(SseResource.isValidTopic("*added"), is(true));
    }

    @Test
    public void testFilterMatchers() {
        String regex1 = "smarthome/*/test/test/test/test";
        String regex2 = "smarthome/test/*/test/test/test";
        String regex3 = "smarthome";
        String regex4 = "qivicon";

        assertThat("smarthome/test/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regex1), is(false));

        assertThat("smarthome/test/test/test/test/test".matches(regex2), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex2), is(false));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regex2), is(false));

        assertThat("smarthome/test/test/test/test/test".matches(regex3), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex3), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regex3), is(true));

        assertThat("smarthome/test/test/test/test/test".matches(regex4), is(false));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex4), is(false));
        assertThat("qivicon/asdf/ASDF/test/test/test".matches(regex4), is(true));
    }

    @Test
    public void testMoreFilterMatchers() {
        String regex1 = "*";
        String regex2 = "smarthome/items/*/added";
        String regex3 = "smarthome/items/*/added, smarthome/items";

        assertThat("smarthome/test/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regex1), is(true));

        assertThat("smarthome/test/test/test/test/test".matches(regex2), is(false));
        assertThat("smarthome/items/anyitem/added".matches(regex2), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regex2), is(false));

        assertThat("smarthome/items/anyitem/added".matches(regex3), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regex3), is(true));
        assertThat("smarthome/items/anyitem/updated".matches(regex3), is(true));
        assertThat("smarthome/things/anything/updated".matches(regex3), is(false));
    }

    @Test
    public void testEvenMoreFilterMatchers() {
        String regex1 = "";

        assertThat("smarthome/test/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/test/test/test/test".matches(regex1), is(true));
        assertThat("smarthome/asdf/ASDF/test/test/test".matches(regex1), is(true));

        regex1 = (".*/added");
        assertThat("smarthome/items/anyitem/added".matches(regex1), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regex1), is(false));

        regex1 = (".*added");
        assertThat("smarthome/items/anyitem/added".matches(regex1), is(true));
        assertThat("smarthome/items/anyitem/removed".matches(regex1), is(false));
    }
}
