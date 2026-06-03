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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.openhab.core.voice.text.conversation.events.ConversationCreatedEvent;
import org.openhab.core.voice.text.conversation.events.ConversationMessageAddedEvent;
import org.openhab.core.voice.text.conversation.events.ConversationMessagesRemovedEvent;
import org.openhab.core.voice.text.conversation.events.ConversationRemovedEvent;

/**
 * Test class for {@link ConversationManagerImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationManagerImplTest {

    private final StorageService storageService = mock(StorageService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    @SuppressWarnings("unchecked")
    private final Storage<ConversationDTO> storage = mock(Storage.class);

    private @NonNullByDefault({}) ConversationManagerImpl conversationManager;

    @BeforeEach
    public void setUp() {
        doReturn(storage).when(storageService).getStorage(eq(Conversation.class.getName()), any());
        conversationManager = new ConversationManagerImpl(storageService, eventPublisher);
    }

    @BeforeEach
    public void tearDown() {
        clearInvocations(storageService, eventPublisher, storage);
    }

    @Test
    public void getConversationCreatesNewConversation() {
        String id = "test-conv";
        when(storage.get(id)).thenReturn(null);

        Conversation conversation = conversationManager.getConversation(id);

        assertNotNull(conversation);
        assertEquals(id, conversation.getId());
        assertTrue(conversation.getMessages().isEmpty());
    }

    @Test
    public void getConversationEmitsEventOnCreatingNewConversation() {
        String id = "test-conv";
        when(storage.get(id)).thenReturn(null);

        conversationManager.getConversation(id);

        verify(eventPublisher).post(any(ConversationCreatedEvent.class));
    }

    @Test
    public void getConversationLoadsConversationFromStorage() {
        String id = "stored-conv";
        MessageDTO messageDTO = new MessageDTO(1, ConversationRole.USER, "Hello");
        ArrayList<MessageDTO> messages = new ArrayList<>(List.of(messageDTO));
        ConversationDTO conversationDTO = new ConversationDTO(messages);
        when(storage.get(id)).thenReturn(conversationDTO);

        Conversation conversation = conversationManager.getConversation(id);

        assertNotNull(conversation);
        assertEquals(id, conversation.getId());
        assertEquals(1, conversation.getMessages().size());
        assertEquals("Hello", conversation.getMessages().getFirst().content());

        // Verify it was put into active conversations map by checking that subsequent call returns the same instance
        assertSame(conversation, conversationManager.getConversation(id));
    }

    @Test
    public void removeConversationRemovesFromStorage() {
        String id = "to-remove";
        conversationManager.removeConversation(id);

        verify(storage).remove(id);
        verify(eventPublisher).post(any(ConversationRemovedEvent.class));
    }

    @Test
    public void removeConversationEmitsEvent() {
        String id = "to-remove";
        conversationManager.removeConversation(id);

        verify(eventPublisher).post(any(ConversationRemovedEvent.class));
    }

    @Test
    public void getConversationsReturnsAllFromStorage() {
        when(storage.getKeys()).thenReturn(Set.of("conv1", "conv2"));
        when(storage.get("conv1")).thenReturn(new ConversationDTO(new ArrayList<>()));
        when(storage.get("conv2")).thenReturn(new ConversationDTO(new ArrayList<>()));

        Collection<Conversation> conversations = conversationManager.getConversations();

        assertEquals(2, conversations.size());
    }

    @Test
    public void addMessageRespectsHistoryLimit() throws ConversationException {
        conversationManager.setHistoryLimit(2);
        Conversation conversation = conversationManager.getConversation("limit-test");

        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.OPENHAB, "2");
        conversation.addMessage(ConversationRole.USER, "3");

        assertEquals(2, conversation.getMessages().size());
        assertEquals("2", conversation.getMessages().get(0).content());
        assertEquals("3", conversation.getMessages().get(1).content());
    }

    @Test
    public void addMessageToAConversationEmitsEvent() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        clearInvocations(eventPublisher); // clear events from creating conversation

        conversation.addMessage(ConversationRole.USER, "Hello");

        verify(eventPublisher).post(any(ConversationMessageAddedEvent.class));
    }

    @Test
    public void addMessageToAConversationPersistsToStorage() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);

        conversation.addMessage(ConversationRole.USER, "Hello");

        verify(storage).put(eq(id), any(ConversationDTO.class));
    }

    @Test
    public void removeMessagesFromAConversationEmitsEvent() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.USER, "2");
        clearInvocations(eventPublisher); // clear events from creating conversation

        conversation.removeSinceMessage(0);

        verify(eventPublisher).post(any(ConversationMessagesRemovedEvent.class));
    }

    @Test
    public void removeMessagesFromAConversationPersistsToStorage() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.USER, "2");
        clearInvocations(storage, storageService); // clear stores from setting up conversation

        conversation.removeSinceMessage(1);
        verify(storage).put(eq(id), any(ConversationDTO.class));
        clearInvocations(storage, storageService);

        conversation.removeSinceMessage(0);
        verify(storage).remove(eq(id));
    }
}
