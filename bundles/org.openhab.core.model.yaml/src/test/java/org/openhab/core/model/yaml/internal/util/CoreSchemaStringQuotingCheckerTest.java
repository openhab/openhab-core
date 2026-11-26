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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Tests that Strings written with {@link CoreSchemaStringQuotingChecker} are read back as Strings by a mapper using
 * the YAML 1.2 core schema, as done by {@code YamlModelRepositoryImpl}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class CoreSchemaStringQuotingCheckerTest {

    private final ObjectMapper mapper = YAMLMapper
            .builder(YAMLFactory.builder().loadSettings(LeadingZeroStringCoreSchema.loadSettings())
                    .stringQuotingChecker(new CoreSchemaStringQuotingChecker()).build())
            .configureForJackson2().disable(YAMLReadFeature.EMPTY_STRING_AS_NULL)
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES).enable(YAMLWriteFeature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS).build();

    @ParameterizedTest
    @ValueSource(strings = { "0x1F", "0o17", ".NaN", ".inf", "-.5e-3", ".5", "+12", "1e3", "007", "~", "null", "Null",
            "True", "FALSE", "yes", "ON", "", "12:30", "1_000", "plain text" })
    public void stringsSurviveWriteAndRead(String value) {
        String yaml = mapper.writeValueAsString(Map.of("key", value));
        JsonNode node = mapper.readTree(yaml).get("key");

        assertThat(yaml, node.isString(), is(true));
        assertThat(node.asString(), is(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "plain text", "ON", "12:30", "2026-11-24" })
    public void plainStringsStayUnquoted(String value) {
        // MINIMIZE_QUOTES: only quote where needed; ON is quoted by the default checker (YAML 1.1 keyword)
        String yaml = mapper.writeValueAsString(Map.of("key", value));

        assertThat(yaml, is("ON".equals(value) ? "---\nkey: \"ON\"\n" : "---\nkey: " + value + "\n"));
    }
}
