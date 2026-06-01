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
import org.openhab.core.voice.text.conversation.Conversation;

/**
 * Mapper class that maps {@link Conversation} instanced to their respective DTOs.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class ConversationMapper {

    /**
     * Maps a {@link Conversation} to a {@link ConversationDTO}.
     *
     * @param conversation the conversation
     *
     * @return the corresponding DTO
     */
    public static ConversationDTO map(Conversation conversation) {
        return new ConversationDTO(conversation.getId(), conversation.getMessages().stream()
                .map(m -> new ConversationDTO.MessageDTO(m.getUID(), m.getRole().name(), m.getContent())).toList());
    }
}
