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

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO for listing {@link org.openhab.core.voice.text.conversation.Conversation}s on the REST API.
 *
 * @author Florian Hotze - Initial contribution
 */
@Schema(name = "ConversationInfo")
@NonNullByDefault
public record ConversationInfoDTO(String id, Instant created, Instant lastUpdated) {
}
