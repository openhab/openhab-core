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
package org.eclipse.smarthome.config.core;

import static org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class ConfigUtilTest {

    @Test
    public void verifyNormalizeDefaultTypeForTextReturnsString() {
        assertThat(ConfigUtil.normalizeDefaultType(TEXT, "foo"), is("foo"));
        assertThat(ConfigUtil.normalizeDefaultType(TEXT, "1.0"), is("1.0"));
    }

    @Test
    public void verifyNormalizeDefaultTypeForBooleanReturnsBoolean() {
        assertThat(ConfigUtil.normalizeDefaultType(BOOLEAN, "true"), is(Boolean.TRUE));
        assertThat(ConfigUtil.normalizeDefaultType(BOOLEAN, "YES"), is(Boolean.FALSE));
    }

    @Test
    public void verifyNormalizeDefaultTypeForIntegerReturnsIntegerOrNull() {
        assertThat(ConfigUtil.normalizeDefaultType(INTEGER, "1"), is(BigInteger.ONE));
        assertThat(ConfigUtil.normalizeDefaultType(INTEGER, "1.2"), is(nullValue()));
        assertThat(ConfigUtil.normalizeDefaultType(INTEGER, "foo"), is(nullValue()));
    }

    @Test
    public void verifyNormalizeDefaultTypeForDecimalReturnsimalBigDecOrNull() {
        assertThat(ConfigUtil.normalizeDefaultType(DECIMAL, "1"), is(BigDecimal.ONE));
        assertThat(ConfigUtil.normalizeDefaultType(DECIMAL, "1.2"), is(new BigDecimal("1.2")));
        assertThat(ConfigUtil.normalizeDefaultType(DECIMAL, "foo"), is(nullValue()));
    }

    @Test
    public void firstDesciptionWinsForNormalization() throws URISyntaxException {
        ConfigDescription configDescriptionInteger = new ConfigDescription(new URI("thing:fooThing"),
                Arrays.asList(new ConfigDescriptionParameter("foo", INTEGER)));

        ConfigDescription configDescriptionString = new ConfigDescription(new URI("thingType:fooThing"),
                Arrays.asList(new ConfigDescriptionParameter("foo", TEXT)));

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
