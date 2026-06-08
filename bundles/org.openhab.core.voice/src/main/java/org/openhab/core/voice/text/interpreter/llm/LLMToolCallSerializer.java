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
 * Serializes a tool call so it can be added as
 * {@link org.openhab.core.voice.text.conversation.ConversationRole#TOOL_CALL} message to a
 * {@link org.openhab.core.voice.text.conversation.Conversation}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolCallSerializer {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMToolCallSerializer.class);

    private static LLMToolCall map(LLMTool tool, Map<String, Object> params) {
        return new LLMToolCall(tool.getUID(), params);
    }

    /**
     * Serializes a {@link LLMTool} call.
     * 
     * @param tool the tool that has been called
     * @param params the parameters of the call
     * @return the JSON or null if serialization failed
     */
    public static @Nullable String serialize(LLMTool tool, Map<String, Object> params) {
        try {
            return JSON_MAPPER.writeValueAsString(map(tool, params));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize tool call '{}: {}': {}", tool.getUID(), params.toString(), e.getMessage());
        }
        return null;
    }

    record LLMToolCall(String tool, Map<String, Object> params) {
    }
}
