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
import org.openhab.core.voice.text.conversation.Conversation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO for serialising {@link Conversation}s on the REST API.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@Schema(name = "Conversation")
@NonNullByDefault
public class ConversationDTO {
    public String id;
    public List<MessageDTO> messages;

    public ConversationDTO(String id, List<MessageDTO> messages) {
        this.id = id;
        this.messages = messages;
    }

    public static class MessageDTO {
        public int id;
        public String role;
        public String content;

        public MessageDTO(int id, String role, String content) {
            this.id = id;
            this.role = role;
            this.content = content;
        }
    }
}
