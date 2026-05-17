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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO that is used on the REST API to provide infos about {@link org.openhab.core.voice.text.Conversation} to UIs.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@Schema(name = "Conversation")
public class ConversationDTO {
    public String id;
    public List<MessageDTO> messages;

    public static class MessageDTO {
        public String uid;
        public String rol;
        public String content;
    }
}
