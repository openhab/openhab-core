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
package org.openhab.core.model.yaml.internal.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Tests that {@link LeadingZeroStringCoreSchema} reads plain scalars with a leading zero like YAML 1.1 did.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class LeadingZeroStringCoreSchemaTest {

    private final ObjectMapper mapper = YAMLMapper
            .builder(YAMLFactory.builder().loadSettings(LeadingZeroStringCoreSchema.loadSettings()).build()).build();

    @ParameterizedTest
    @ValueSource(strings = { "09", "08", "09123", "-09", "+08", "0089" })
    public void leadingZeroNonOctalIsString(String value) {
        JsonNode node = mapper.readTree("key: " + value).get("key");

        assertThat(node.isString(), is(true));
        assertThat(node.asString(), is(value));
    }

    @ParameterizedTest
    @CsvSource({ "0755,493", "007,7", "00,0", "0,0", "12,12", "-12,-12" })
    public void integersAreNumbers(String value, long expected) {
        JsonNode node = mapper.readTree("key: " + value).get("key");

        assertThat(node.isIntegralNumber(), is(true));
        assertThat(node.asLong(), is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = { "'09'", "\"09\"" })
    public void quotedValuesStayStrings(String value) {
        assertThat(mapper.readTree("key: " + value).get("key").asString(), is("09"));
    }
}
