/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.config.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.openhab.core.config.core.ConfigDescriptionParameter.Type.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class ConfigUtilTest {

    private final URI configUri = URI.create("system:ephemeris");

    private final ConfigDescriptionParameterBuilder configDescriptionParameterBuilder1 = ConfigDescriptionParameterBuilder
            .create("p1", DECIMAL).withMultiple(true).withMultipleLimit(7);
    private final ConfigDescriptionParameterBuilder configDescriptionParameterBuilder2 = ConfigDescriptionParameterBuilder
            .create("p2", TEXT).withMultiple(true).withMultipleLimit(2);

    @Test
    public void verifyNormalizeDefaultTypeForTextReturnsString() {
        assertThat(ConfigUtil.getDefaultValueAsCorrectType(
                ConfigDescriptionParameterBuilder.create("test", TEXT).withDefault("foo").build()), is("foo"));
        assertThat(ConfigUtil.getDefaultValueAsCorrectType(
                ConfigDescriptionParameterBuilder.create("test", TEXT).withDefault("1.0").build()), is("1.0"));
    }

    @Test
    public void verifyNormalizeDefaultTypeForBooleanReturnsBoolean() {
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", BOOLEAN).withDefault("true").build()),
                is(Boolean.TRUE));
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", BOOLEAN).withDefault("YES").build()),
                is(Boolean.FALSE));
    }

    @Test
    public void verifyNormalizeDefaultTypeForIntegerReturnsIntegerOrNull() {
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", INTEGER).withDefault("1").build()),
                is(BigDecimal.ONE));
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", INTEGER).withDefault("1.2").build()),
                is(BigDecimal.ONE));
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", INTEGER).withDefault("foo").build()),
                is(nullValue()));
    }

    @Test
    public void verifyNormalizeDefaultTypeForDecimalReturnsimalBigDecOrNull() {
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", DECIMAL).withDefault("1").build()),
                is(BigDecimal.ONE));
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", DECIMAL).withDefault("1.2").build()),
                is(new BigDecimal("1.2")));
        assertThat(
                ConfigUtil.getDefaultValueAsCorrectType(
                        ConfigDescriptionParameterBuilder.create("test", DECIMAL).withDefault("foo").build()),
                is(nullValue()));
    }

    @Test
    public void verifyGetNumberOfDecimalPlacesWorksCorrectly() {
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("0.001")), is(3));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("0.01")), is(2));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("0.1")), is(1));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("1.000")), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("1.00")), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("1.0")), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(BigDecimal.ONE), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("10")), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("100")), is(0));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("100.1")), is(1));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("100.01")), is(2));
        assertThat(ConfigUtil.getNumberOfDecimalPlaces(new BigDecimal("100.001")), is(3));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsNullIfNotSet() {
        Configuration configuration = new Configuration();
        ConfigDescription configDescription = new ConfigDescription(configUri,
                Collections.singletonList(configDescriptionParameterBuilder2.build()));

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        assertThat(configuration.get("p2"), is(nullValue()));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithASingleValues() {
        configDescriptionParameterBuilder1.withDefault("2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = new ConfigDescription(configUri,
                Collections.singletonList(configDescriptionParameterBuilder1.build()));

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 1, Collections.singletonList(new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithMultipleValues() {
        configDescriptionParameterBuilder1.withDefault("2.3,2.4,2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = new ConfigDescription(configUri,
                Collections.singletonList(configDescriptionParameterBuilder1.build()));

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 3,
                Arrays.asList(new BigDecimal("2.3"), new BigDecimal("2.4"), new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationIgnoresWrongTypes() {
        configDescriptionParameterBuilder1.withDefault("2.3,2.4,foo,2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = new ConfigDescription(configUri,
                Collections.singletonList(configDescriptionParameterBuilder1.build()));

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 3,
                Arrays.asList(new BigDecimal("2.3"), new BigDecimal("2.4"), new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithTrimmedValues() {
        configDescriptionParameterBuilder2.withDefault("first value,  second value  ,third value,,,");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = new ConfigDescription(configUri,
                Collections.singletonList(configDescriptionParameterBuilder2.build()));

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p2"), 3,
                Arrays.asList("first value", "second value", "third value"));
    }

    private void verifyValuesOfConfiguration(Object subject, int expectedSize, List<?> expectedValues) {
        assertThat(subject, is(notNullValue()));
        assertThat(subject, is(instanceOf(List.class)));
        assertThat(((List<?>) subject).size(), is(expectedSize));
        assertThat(((List<?>) subject), is(expectedValues));
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
