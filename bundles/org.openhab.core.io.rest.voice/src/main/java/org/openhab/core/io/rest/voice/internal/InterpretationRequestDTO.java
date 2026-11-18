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
package org.openhab.core.io.rest.voice.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO representing a text interpretation request on the REST API.
 *
 * @author Florian Hotze - Initial contribution
 */
@Schema(name = "InterpretationRequest")
@NonNullByDefault
public record InterpretationRequestDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Text to interpret", example = "Turn on the kitchen lights") String text,
        @Schema(description = "Ordered list of interpreter IDs to try", example = "[\"standard\", \"gemini\"]") @Nullable List<String> interpreters,
        @Schema(description = "Conversation ID for multi-turn context") @Nullable String conversationId,
        @Schema(description = "List of LLM tool IDs, or [\"*\"] for all", defaultValue = "[\"*\"]") @Nullable List<String> llmTools,
        @Schema(description = "Location item ID to contextualize the command") @Nullable String locationItem) {
}
