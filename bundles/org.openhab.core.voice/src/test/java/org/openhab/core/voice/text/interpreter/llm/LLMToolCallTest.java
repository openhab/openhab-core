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
package org.openhab.core.voice.text.interpreter.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.Gson;

/**
 * Tests for {@link LLMToolCall}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolCallTest {
    private final Gson gson = new Gson();

    private final LLMTool tool = new LLMTool() {
        @Override
        public String getUID() {
            return "sample-llm-tool";
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            return "Sample LLM Tool";
        }

        @Override
        public String getShortDescription(@Nullable Locale locale) {
            return "";
        }

        @Override
        public String getDescription(@Nullable Locale locale) {
            return "";
        }

        @Override
        public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
            return List.of(new LLMToolParam("param1", LLMToolParamType.STRING, "A text parameter",
                    Collections.emptyList(), false));
        }

        @Override
        public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
            return "Sample LLM Tool has been called.";
        }
    };

    private static Stream<Arguments> provideToolCallScenarios() {
        return Stream.of(Arguments.of("No Parameters", Collections.emptyMap()),
                Arguments.of("Single Parameter", Collections.singletonMap("param1", "value1")),
                Arguments.of("Multiple Parameters", Map.of("p1", "v1", "p2", 42)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideToolCallScenarios")
    public void testSerialization(String description, Map<String, Object> params) {
        LLMToolCall call = LLMToolCall.map(tool, params);
        String serialized = call.toJson();

        assertNotNull(serialized);
        assertEquals(gson.toJson(call), serialized);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideToolCallScenarios")
    public void testDeserialization(String description, Map<String, Object> params) {
        LLMToolCall original = LLMToolCall.map(tool, params);
        String json = gson.toJson(original);
        assertNotNull(json);

        LLMToolCall deserialized = LLMToolCall.fromJson(json);

        assertNotNull(deserialized);
        assertEquals(original.tool(), deserialized.tool());
        assertEquals(original.params(), deserialized.params());
    }
}
