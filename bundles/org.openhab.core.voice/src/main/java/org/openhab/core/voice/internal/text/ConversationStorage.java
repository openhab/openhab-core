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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.text.Conversation;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConversationStorage} class stores active conversations.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class ConversationStorage {
    private final Storage<ConversationDTO> conversationStorageService;
    private final Map<String, Conversation> activeConversations = new HashMap<>();
    private final EventPublisher eventPublisher;
    private final Logger logger = LoggerFactory.getLogger(ConversationStorage.class);

    @Activate
    public ConversationStorage(final StorageService storageService, final EventPublisher eventPublisher) {
        this.conversationStorageService = storageService.getStorage(Conversation.class.getName(),
                this.getClass().getClassLoader());
        this.eventPublisher = eventPublisher;
    }

    public synchronized Conversation getConversation(String id) {
        Conversation conversation = activeConversations.get(id);
        if (conversation != null) {
            // return same reference when possible
            return conversation;
        }
        ConversationDTO conversationDTO = conversationStorageService.get(id);
        if (conversationDTO != null) {
            logger.debug("Conversation '{}' found", id);
            conversation = new Conversation(id, eventPublisher,
                    new ArrayList<>(conversationDTO.messageDTOs.stream().map(MessageDTO::toMessage).toList()));
        } else {
            logger.debug("Creating new conversation '{}'", id);
            conversation = new Conversation(id, eventPublisher);
        }
        activeConversations.put(conversation.getId(), conversation);
        return conversation;
    }

    public synchronized void storeConversation(Conversation conversation) {
        logger.debug("Storing conversation '{}' with {} messages...", conversation.getId(),
                conversation.getMessages().size());
        conversationStorageService.put(conversation.getId(), new ConversationDTO(
                new ArrayList<>(conversation.getMessages().stream().map(MessageDTO::fromMessage).toList())));
    }

    public synchronized void removeConversation(Conversation conversation) {
        logger.debug("Removing conversation '{}'", conversation.getId());
        activeConversations.remove(conversation.getId());
        conversationStorageService.remove(conversation.getId());
    }
}
