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
package org.openhab.core.voice.internal.text;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.text.Conversation;
import org.openhab.core.voice.text.ConversationRole;

/**
 * The {@link MessageDTO} class contains a list of messages in between the users and a LanguageInterpreter
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class MessageDTO {
    public String uid;
    public ConversationRole r;
    public String c;

    public MessageDTO(String uid, ConversationRole r, String c) {
        this.uid = uid;
        this.r = r;
        this.c = c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, r, c);
    }

    @Override
    public String toString() {
        return "MessageRecord[" + "uid=" + uid + ", " + "r=" + r + ", " + "c=" + c + ']';
    }

    public Conversation.Message toMessage() {
        return new Conversation.Message(uid, r, c);
    }

    public static MessageDTO fromMessage(Conversation.Message messageRecord) {
        return new MessageDTO(messageRecord.getUID(), messageRecord.getRole(), messageRecord.getContent());
    }
}
