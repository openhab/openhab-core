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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A DTO to store information about a tool call.
 * 
 * @param tool the UID of the {@link LLMTool}
 * @param params the params of the tool call
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public record LLMToolCall(String tool, Map<String, Object> params) {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMToolCall.class);

    public static LLMToolCall map(LLMTool tool, Map<String, Object> params) {
        return new LLMToolCall(tool.getUID(), params);
    }

    /**
     * Serializes this instance to JSON.
     * 
     * @return the JSON or null if serialization failed
     */
    public @Nullable String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize tool call '{}: {}'", tool, params, e);
        }
        return null;
    }

    /**
     * Deserializes a tool call from JSON.
     * 
     * @param json the JSON to deserialize
     * @return the deserialized tool call or null if deserialization failed
     */
    public static @Nullable LLMToolCall fromJson(@Nullable String json) {
        if (json == null) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, LLMToolCall.class);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to deserialize tool call '{}'", json, e);
        }
        return null;
    }
}
