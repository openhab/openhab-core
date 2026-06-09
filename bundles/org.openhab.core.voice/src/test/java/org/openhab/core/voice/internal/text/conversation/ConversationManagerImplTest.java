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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.internal.VoiceConfiguration;
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
    private final StorageService storageServiceMock = mock(StorageService.class);
    private final EventPublisher eventPublisherMock = mock(EventPublisher.class);
    @SuppressWarnings("unchecked")
    private final Storage<PersistedConversationDTO> storageMock = mock(Storage.class);

    private @NonNullByDefault({}) ConversationManagerImpl conversationManager;

    @BeforeEach
    public void setUp() {
        doReturn(storageMock).when(storageServiceMock).getStorage(eq(Conversation.class.getName()), any());
        conversationManager = new ConversationManagerImpl(storageServiceMock, eventPublisherMock,
                Map.of(VoiceConfiguration.CONFIG_CONVERSATION_HISTORY_LIMIT, "10"));
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(storageServiceMock, eventPublisherMock, storageMock);
    }

    @Test
    public void getConversationCreatesNewConversationIfCreateIfMissingIsTrue() {
        String id = "test-conv";
        when(storageMock.get(id)).thenReturn(null);

        Conversation conversation = conversationManager.getConversation(id, true);

        assertNotNull(conversation);
        assertEquals(id, conversation.getId());
        assertTrue(conversation.getMessages().isEmpty());
    }

    @Test
    public void getConversationDoesNotCreateNewConversationIfCreateIfMissingIsFalse() {
        String id = "test-conv";
        when(storageMock.get(id)).thenReturn(null);

        Conversation conversation = conversationManager.getConversation(id, false);

        assertNull(conversation);
    }

    @Test
    public void getConversationEmitsEventOnCreatingNewConversation() {
        String id = "test-conv";
        when(storageMock.get(id)).thenReturn(null);

        conversationManager.getConversation(id);

        verify(eventPublisherMock).post(any(ConversationCreatedEvent.class));
    }

    @Test
    public void getConversationDoesNotEmitEventOnCreatingConversationWithBlankId() {
        String id = "";

        conversationManager.getConversation(id);

        verify(eventPublisherMock, never()).post(any(ConversationCreatedEvent.class));
    }

    @Test
    public void getConversationLoadsConversationFromStorage() {
        String id = "stored-conv";
        Instant created = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant lastUpdated = Instant.now();
        PersistedMessageDTO persistedMessageDTO = new PersistedMessageDTO(1, ConversationRole.USER, "Hello");
        ArrayList<PersistedMessageDTO> messages = new ArrayList<>(List.of(persistedMessageDTO));
        PersistedConversationDTO persistedConversationDTO = new PersistedConversationDTO(id, created, lastUpdated,
                messages);
        when(storageMock.get(id)).thenReturn(persistedConversationDTO);

        Conversation conversation = conversationManager.getConversation(id);

        assertNotNull(conversation);
        assertEquals(id, conversation.getId());
        assertEquals(created, conversation.getCreated());
        assertEquals(lastUpdated, conversation.getLastUpdated());
        assertEquals(1, conversation.getMessages().size());
        assertEquals("Hello", conversation.getMessages().getFirst().content());

        // Verify it was put into active conversations map by checking that subsequent call returns the same instance
        assertSame(conversation, conversationManager.getConversation(id));
    }

    @Test
    public void removeConversationRemovesFromStorage() {
        String id = "to-remove";
        conversationManager.removeConversation(id);

        verify(storageMock).remove(id);
        verify(eventPublisherMock).post(any(ConversationRemovedEvent.class));
    }

    @Test
    public void removeConversationEmitsEvent() {
        String id = "to-remove";
        conversationManager.removeConversation(id);

        verify(eventPublisherMock).post(any(ConversationRemovedEvent.class));
    }

    @Test
    public void getConversationsReturnsAllFromStorage() {
        Instant created = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant lastUpdated = Instant.now();
        when(storageMock.getKeys()).thenReturn(Set.of("conv1", "conv2"));
        when(storageMock.get("conv1"))
                .thenReturn(new PersistedConversationDTO("conv1", created, lastUpdated, new ArrayList<>()));
        when(storageMock.get("conv2"))
                .thenReturn(new PersistedConversationDTO("conv2", created, lastUpdated, new ArrayList<>()));

        Collection<Conversation> conversations = conversationManager.getConversations();

        assertEquals(2, conversations.size());
    }

    @Test
    public void addMessageToAConversationEmitsEvent() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        clearInvocations(eventPublisherMock); // clear events from creating conversation

        conversation.addMessage(ConversationRole.USER, "Hello");

        verify(eventPublisherMock).post(any(ConversationMessageAddedEvent.class));
    }

    @Test
    public void addMessageToAConversationPersistsToStorage() throws ConversationException {
        String id = "storage-test";
        Conversation conversation = conversationManager.getConversation(id);

        conversation.addMessage(ConversationRole.USER, "Hello");

        verify(storageMock).put(eq(id), any(PersistedConversationDTO.class));
    }

    @Test
    void addMessageToAConversationUpdatesLastUpdated() throws ConversationException {
        String id = "last-updated-test";
        Conversation conversation = conversationManager.getConversation(id);
        Instant oldLastUpdated = conversation.getLastUpdated();

        conversation.addMessage(ConversationRole.USER, "Hello");

        assertTrue(conversation.getLastUpdated().isAfter(conversation.getCreated()));
        assertTrue(conversation.getLastUpdated().isAfter(oldLastUpdated));
    }

    @Test
    public void addMessageToAConversationWithBlankIdDoesNotPersistToStorage() throws ConversationException {
        String id = "";
        Conversation conversation = conversationManager.getConversation(id);

        conversation.addMessage(ConversationRole.USER, "Hello");

        verify(storageMock, never()).put(eq(id), any(PersistedConversationDTO.class));
    }

    @Test
    public void removeMessagesFromAConversationEmitsEvent() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.USER, "2");
        clearInvocations(eventPublisherMock); // clear events from creating conversation

        conversation.removeSinceMessage(1);
        verify(eventPublisherMock).post(any(ConversationMessagesRemovedEvent.class));

        conversation.removeSinceMessage(0);
        verify(eventPublisherMock).post(any(ConversationRemovedEvent.class));
    }

    @Test
    public void removeMessagesFromAConversationPersistsToStorage() throws ConversationException {
        String id = "event-test";
        Conversation conversation = conversationManager.getConversation(id);
        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.USER, "2");
        clearInvocations(storageMock, storageServiceMock); // clear stores from setting up conversation

        conversation.removeSinceMessage(1);
        verify(storageMock).put(eq(id), any(PersistedConversationDTO.class));
        clearInvocations(storageMock, storageServiceMock);

        conversation.removeSinceMessage(0);
        verify(storageMock).remove(eq(id));
    }

    @Test
    public void removeMessagesFromAConversationWithBlankIdDoesNotPersistToStorage() throws ConversationException {
        String id = "";
        Conversation conversation = conversationManager.getConversation(id);
        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.USER, "2");
        conversation.addMessage(ConversationRole.USER, "3");
        conversation.addMessage(ConversationRole.USER, "4");
        conversation.addMessage(ConversationRole.USER, "5");
        clearInvocations(storageMock, storageServiceMock); // clear stores from setting up conversation

        conversation.removeSinceMessage(3);
        conversation.removeSinceMessage(0);
        verify(storageMock, never()).put(eq(id), any(PersistedConversationDTO.class));
    }
}
