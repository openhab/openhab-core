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
package org.openhab.core.voice.internal.text.conversation;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.text.conversation.Conversation;

/**
 * The {@link ConversationDTO} class contains a list of messages in between the users and a LanguageInterpreter.
 * It is used to store {@link Conversation}s using a
 * {@link org.openhab.core.storage.StorageService}.
 * 
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public record ConversationDTO(List<MessageDTO> messages) {
}
