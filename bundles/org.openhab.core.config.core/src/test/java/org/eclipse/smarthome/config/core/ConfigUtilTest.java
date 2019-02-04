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
package org.eclipse.smarthome.config.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class ConfigUtilTest {

    @Test
    public void firstDesciptionWinsForNormalization() throws URISyntaxException {
        ConfigDescription configDescriptionInteger = new ConfigDescription(new URI("thing:fooThing"),
                Arrays.asList(new ConfigDescriptionParameter("foo", Type.INTEGER)));

        ConfigDescription configDescriptionString = new ConfigDescription(new URI("thingType:fooThing"),
                Arrays.asList(new ConfigDescriptionParameter("foo", Type.TEXT)));

        assertThat(
                ConfigUtil.normalizeTypes(Collections.singletonMap("foo", "1"), Arrays.asList(configDescriptionInteger))
                        .get("foo"),
                is(instanceOf(BigDecimal.class)));
        assertThat(
                ConfigUtil.normalizeTypes(Collections.singletonMap("foo", "1"), Arrays.asList(configDescriptionString))
                        .get("foo"),
                is(instanceOf(String.class)));
        assertThat(
                ConfigUtil.normalizeTypes(Collections.singletonMap("foo", "1"),
                        Arrays.asList(configDescriptionInteger, configDescriptionString)).get("foo"),
                is(instanceOf(BigDecimal.class)));
        assertThat(
                ConfigUtil.normalizeTypes(Collections.singletonMap("foo", "1"),
                        Arrays.asList(configDescriptionString, configDescriptionInteger)).get("foo"),
                is(instanceOf(String.class)));
    }
}
