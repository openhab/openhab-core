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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
    private static final Gson GSON = new Gson();

    public static LLMToolCall map(LLMTool tool, Map<String, Object> params) {
        return new LLMToolCall(tool.getUID(), params);
    }

    /**
     * Serializes this instance to JSON.
     *
     * @return the JSON
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes a tool call from JSON.
     *
     * @param json the JSON to deserialize
     * @return the deserialized tool call
     * @throws JsonSyntaxException if <code>json</code> is not a valid representation of {@link LLMToolCall}
     */
    public static LLMToolCall fromJson(String json) throws JsonSyntaxException {
        LLMToolCall call = GSON.fromJson(json, LLMToolCall.class);
        if (call == null) {
            throw new JsonSyntaxException("Deserialized LLMToolCall is null.");
        }
        return call;
    }
}
