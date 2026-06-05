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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationListener;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.conversation.events.ConversationEventFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConversationManagerImpl} class manages active conversations.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 * @author Florian Hotze - Initial contribution
 */
@Component(service = ConversationManager.class)
@NonNullByDefault
public class ConversationManagerImpl implements ConversationManager, ConversationListener {
    private final Storage<ConversationDTO> conversationStorage;
    private final Map<String, Conversation> activeConversations = new ConcurrentHashMap<>();
    private final EventPublisher eventPublisher;
    private final Logger logger = LoggerFactory.getLogger(ConversationManagerImpl.class);
    private int historyLimit = Conversation.DEFAULT_MAX_MESSAGES;

    @Activate
    public ConversationManagerImpl(final @Reference StorageService storageService,
            final @Reference EventPublisher eventPublisher) {
        this.conversationStorage = storageService.getStorage(Conversation.class.getName(),
                this.getClass().getClassLoader());
        this.eventPublisher = eventPublisher;
    }

    @Override
    public @Nullable Conversation getConversation(String id, boolean createIfMissing) {
        Conversation conversation;
        if (id.isBlank() && createIfMissing) {
            logger.debug("Creating new unpersisted conversation");
            conversation = new Conversation("");
            conversation.setMaxMessages(historyLimit);
            return conversation;
        }

        conversation = activeConversations.get(id);
        if (conversation != null) {
            // return same reference when possible
            return conversation;
        }
        synchronized (this) {
            // re-check whether conversation became active since last check
            conversation = activeConversations.get(id);
            if (conversation != null) {
                conversation.setMaxMessages(historyLimit);
                // return same reference when possible
                return conversation;
            }
            // load conversation from storage or create a new one
            ConversationDTO conversationDTO = conversationStorage.get(id);
            if (conversationDTO != null) {
                logger.debug("Conversation '{}' found", id);
                conversation = new Conversation(id,
                        conversationDTO.messages().stream().map(MessageDTO::toMessage).toList());
            } else if (createIfMissing) {
                logger.debug("Creating new conversation '{}'", id);
                conversation = new Conversation(id);
                eventPublisher.post(ConversationEventFactory.createConversationCreatedEvent(id, null));
            } else {
                return null;
            }
            conversation.addListener(this);
            conversation.setMaxMessages(historyLimit);
            activeConversations.put(conversation.getId(), conversation);
            return conversation;
        }
    }

    /**
     * Persists the conversation state to storage.
     *
     * <p>
     * If the conversation is empty (no messages), it should be removed from storage.
     * If the ID is blank, it should not be persisted to storage.
     *
     * @param conversation the conversation to save
     */
    private void storeConversation(Conversation conversation) {
        String id = conversation.getId();
        if (id.isBlank()) {
            return;
        }
        if (conversation.getMessages().isEmpty()) {
            removeConversation(id);
        } else {
            logger.debug("Storing conversation '{}' with {} messages...", id, conversation.getMessages().size());
            conversationStorage.put(id, new ConversationDTO(
                    new ArrayList<>(conversation.getMessages().stream().map(MessageDTO::fromMessage).toList())));
            activeConversations.put(id, conversation);
        }
    }

    @Override
    public void removeConversation(String id) {
        logger.debug("Removing conversation '{}'", id);
        Conversation conversation = activeConversations.remove(id);
        if (conversation != null) {
            conversation.removeListener(this);
        }
        if (!id.isBlank()) {
            conversationStorage.remove(id);
            eventPublisher.post(ConversationEventFactory.createConversationRemovedEvent(id, null));
        }
    }

    @Override
    public Collection<Conversation> getConversations() {
        // Ensure all stored conversations are in the active map
        for (String id : conversationStorage.getKeys()) {
            if (!activeConversations.containsKey(id)) {
                getConversation(id);
            }
        }
        return activeConversations.values();
    }

    @Override
    public void setHistoryLimit(int limit) {
        this.historyLimit = limit;
        activeConversations.values().forEach(c -> c.setMaxMessages(limit));
    }

    @Override
    public void onMessageAdded(Conversation conversation, Conversation.Message message) {
        eventPublisher.post(ConversationEventFactory.createConversationMessageAddedEvent(conversation.getId(),
                message.id(), message.role(), message.content(), null));
        storeConversation(conversation);
    }

    @Override
    public void onMessagesRemoved(Conversation conversation, int sinceRemovedMessagesId) {
        if (conversation.getMessages().isEmpty()) {
            removeConversation(conversation.getId());
        } else {
            eventPublisher.post(ConversationEventFactory.createConversationMessagesRemovedEvent(conversation.getId(),
                    sinceRemovedMessagesId, null));
            storeConversation(conversation);
        }
    }
}
