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

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO that is used on the REST API to provide details about LLM tools.
 *
 * @author Florian Hotze - Initial contribution
 */
@Schema(name = "LLMTool")
@NonNullByDefault
public record LLMToolDTO(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label, String description) {
}
