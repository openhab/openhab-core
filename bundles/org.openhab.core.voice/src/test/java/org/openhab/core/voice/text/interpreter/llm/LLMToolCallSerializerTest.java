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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/**
 * Tests for {@link LLMToolCallSerializer}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolCallSerializerTest {
    private final Gson gson = new Gson();

    private LLMTool tool = new LLMTool() {
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

    @Test
    public void serializesWithoutParams() {
        LLMToolCallSerializer.LLMToolCall expected = new LLMToolCallSerializer.LLMToolCall(tool.getUID(),
                Collections.emptyMap());
        String serialized = LLMToolCallSerializer.serialize(tool, Collections.emptyMap());

        assertNotNull(serialized);
        assertEquals(gson.toJson(expected), serialized);
    }

    @Test
    public void serializesWithParams() {
        Map<String, Object> params = Collections.singletonMap("param1", "value1");
        LLMToolCallSerializer.LLMToolCall expected = new LLMToolCallSerializer.LLMToolCall(tool.getUID(), params);
        String serialized = LLMToolCallSerializer.serialize(tool, params);

        assertNotNull(serialized);
        assertEquals(gson.toJson(expected), serialized);
    }
}
