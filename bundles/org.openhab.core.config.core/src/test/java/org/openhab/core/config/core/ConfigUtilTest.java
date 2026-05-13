/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.openhab.core.config.core.ConfigDescriptionParameter.Type.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * @author Simon Kaufmann - Initial contribution
 * @author Florian Hotze - Add tests for env variable substitution
 */
@Execution(ExecutionMode.SAME_THREAD) // Force sequential test execution to avoid multi-threaded access to
                                      // ConfigUtil::setEnvProvider
@ResourceLock(value = "org.openhab.core.config.core.ConfigUtil", mode = ResourceAccessMode.READ_WRITE)
@NonNullByDefault
public class ConfigUtilTest {

    private final URI configUri = URI.create("system:ephemeris");
    private final ConfigUtil.EnvProvider mockEnv = mock(ConfigUtil.EnvProvider.class);

    private final ConfigDescriptionParameterBuilder configDescriptionParameterBuilder1 = ConfigDescriptionParameterBuilder
            .create("p1", DECIMAL).withMultiple(true).withMultipleLimit(7);
    private final ConfigDescriptionParameterBuilder configDescriptionParameterBuilder2 = ConfigDescriptionParameterBuilder
            .create("p2", TEXT).withMultiple(true).withMultipleLimit(2);

    @BeforeEach
    public void setup() {
        reset(mockEnv);
        when(mockEnv.get("HOSTNAME")).thenReturn("openhab-host");
        when(mockEnv.get("PATH")).thenReturn("openhab-path");
        ConfigUtil.setEnvProvider(mockEnv);
    }

    @AfterEach
    public void tearDown() {
        ConfigUtil.setEnvProvider(System::getenv);
    }

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
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configUri)
                .withParameter(configDescriptionParameterBuilder2.build()).build();

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        assertThat(configuration.get("p2"), is(nullValue()));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithASingleValues() {
        configDescriptionParameterBuilder1.withDefault("2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configUri)
                .withParameter(configDescriptionParameterBuilder1.build()).build();

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 1, List.of(new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithMultipleValues() {
        configDescriptionParameterBuilder1.withDefault("2.3,2.4,2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configUri)
                .withParameter(configDescriptionParameterBuilder1.build()).build();

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 3,
                List.of(new BigDecimal("2.3"), new BigDecimal("2.4"), new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationIgnoresWrongTypes() {
        configDescriptionParameterBuilder1.withDefault("2.3,2.4,foo,2.5");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configUri)
                .withParameter(configDescriptionParameterBuilder1.build()).build();

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p1"), 3,
                List.of(new BigDecimal("2.3"), new BigDecimal("2.4"), new BigDecimal("2.5")));
    }

    @Test
    public void verifyApplyDefaultConfigurationReturnsAListWithTrimmedValues() {
        configDescriptionParameterBuilder2.withDefault("first value,  second value  ,third value,,,");

        Configuration configuration = new Configuration();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configUri)
                .withParameter(configDescriptionParameterBuilder2.build()).build();

        ConfigUtil.applyDefaultConfiguration(configuration, configDescription);
        verifyValuesOfConfiguration(configuration.get("p2"), 3, List.of("first value", "second value", "third value"));
    }

    private void verifyValuesOfConfiguration(Object subject, int expectedSize, List<?> expectedValues) {
        assertThat(subject, is(notNullValue()));
        assertThat(subject, is(instanceOf(List.class)));
        assertThat(((List<?>) subject).size(), is(expectedSize));
        assertThat(((List<?>) subject), is(expectedValues));
    }

    @Test
    public void firstDescriptionWinsForNormalization() {
        ConfigDescription configDescriptionInteger = ConfigDescriptionBuilder.create(URI.create("thing:fooThing"))
                .withParameter(ConfigDescriptionParameterBuilder.create("foo", INTEGER).build()).build();

        ConfigDescription configDescriptionString = ConfigDescriptionBuilder.create(URI.create("thingType:fooThing"))
                .withParameter(ConfigDescriptionParameterBuilder.create("foo", TEXT).build()).build();

        assertThat(ConfigUtil.normalizeTypes(Map.of("foo", "1"), List.of(configDescriptionInteger)).get("foo"),
                is(instanceOf(BigDecimal.class)));
        assertThat(ConfigUtil.normalizeTypes(Map.of("foo", "1"), List.of(configDescriptionString)).get("foo"),
                is(instanceOf(String.class)));
        assertThat(ConfigUtil
                .normalizeTypes(Map.of("foo", "1"), List.of(configDescriptionInteger, configDescriptionString))
                .get("foo"), is(instanceOf(BigDecimal.class)));
        assertThat(ConfigUtil
                .normalizeTypes(Map.of("foo", "1"), List.of(configDescriptionString, configDescriptionInteger))
                .get("foo"), is(instanceOf(String.class)));
    }

    @Test
    public void resolveVariablesResolvesSingleEnvVariable() {
        String hostname = mockEnv.get("HOSTNAME");

        assertEquals(hostname, ConfigUtil.resolveVariables("${ENV:HOSTNAME}"));
        assertEquals("prefix-" + hostname + "-suffix", ConfigUtil.resolveVariables("prefix-${ENV:HOSTNAME}-suffix"));
    }

    @Test
    public void resolveVariablesResolvesMultipleEnvVariables() {
        String hostname = mockEnv.get("HOSTNAME");
        String path = mockEnv.get("PATH");

        assertEquals(hostname + ":" + path, ConfigUtil.resolveVariables("${ENV:HOSTNAME}:${ENV:PATH}"));
    }

    @Test
    public void resolveVariablesThrowsIAEForUnknownEnvVariable() {
        assertThrows(IllegalArgumentException.class, () -> ConfigUtil.resolveVariables("${ENV:UNKNOWN_VAR}"));
    }

    @Test
    public void resolveVariablesResolvesList() {
        String hostname = mockEnv.get("HOSTNAME");
        List<Object> input = List.of("plain", "${ENV:HOSTNAME}", true, 42, 3.14159);
        List<Object> expected = List.of("plain", hostname, true, 42, 3.14159);

        assertEquals(expected, ConfigUtil.resolveVariables(input));
    }

    @Test
    public void resolveVariablesPassesThroughPrimitivesNotString() {
        assertEquals(42, ConfigUtil.resolveVariables(42));
        assertEquals(3.14159, ConfigUtil.resolveVariables(3.14159));
        assertEquals(true, ConfigUtil.resolveVariables(true));
        assertEquals(false, ConfigUtil.resolveVariables(false));
    }

    @Test
    public void resolveVariablesPassesThroughIncorrectPatterns() {
        String noEnv = "${HOSTNAME}";
        String noBraces = "$ENV:HOSTNAME";
        String unclosedBraces = "${ENV:HOSTNAME";

        assertEquals(noEnv, ConfigUtil.resolveVariables(noEnv));
        assertEquals(noBraces, ConfigUtil.resolveVariables(noBraces));
        assertEquals(unclosedBraces, ConfigUtil.resolveVariables(unclosedBraces));
    }

    @Test
    public void resolveVariablesResolvesEnvVariablesInConfiguration() {
        String hostname = mockEnv.get("HOSTNAME");
        Map<String, @Nullable Object> config = Map.of("p1", "plain", "p2", "${ENV:HOSTNAME}", "p3", true, "p4", 42,
                "p5", 3.14159);

        Map<String, @Nullable Object> resolvedConfig = ConfigUtil.resolveVariables(config);
        assertEquals("plain", resolvedConfig.get("p1"));
        assertEquals(hostname, resolvedConfig.get("p2"));
        assertEquals(true, resolvedConfig.get("p3"));
        assertEquals(42, resolvedConfig.get("p4"));
        assertEquals(3.14159, resolvedConfig.get("p5"));
    }

    @Test
    public void resolveVariablesAndNormalizeTypesResolvesThenNormalizedConfiguration() {
        String hostname = mockEnv.get("HOSTNAME");
        when(mockEnv.get("BOOLEAN")).thenReturn("true");
        when(mockEnv.get("INTEGER")).thenReturn("42");
        when(mockEnv.get("DECIMAL")).thenReturn("3.14159");

        Map<String, @Nullable Object> config = Map.of("p1", "plain", "p2", "${ENV:HOSTNAME}", "p3", "${ENV:BOOLEAN}",
                "p4", "${ENV:INTEGER}", "p5", "${ENV:DECIMAL}");
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(URI.create("thingType:fooThing"))
                .withParameter(ConfigDescriptionParameterBuilder.create("p1", TEXT).build())
                .withParameter(ConfigDescriptionParameterBuilder.create("p2", TEXT).build())
                .withParameter(ConfigDescriptionParameterBuilder.create("p3", BOOLEAN).build())
                .withParameter(ConfigDescriptionParameterBuilder.create("p4", INTEGER).build())
                .withParameter(ConfigDescriptionParameterBuilder.create("p5", DECIMAL).build()).build();

        Configuration normalizedAndResolvedConfig = ConfigUtil
                .resolveVariablesAndNormalizeTypes(new Configuration(config), List.of(configDescription));

        assertEquals("plain", normalizedAndResolvedConfig.get("p1"));
        assertEquals(hostname, normalizedAndResolvedConfig.get("p2"));
        assertEquals(true, normalizedAndResolvedConfig.get("p3"));
        assertEquals(new BigDecimal("42"), normalizedAndResolvedConfig.get("p4"));
        assertEquals(new BigDecimal("3.14159"), normalizedAndResolvedConfig.get("p5"));
    }
}
