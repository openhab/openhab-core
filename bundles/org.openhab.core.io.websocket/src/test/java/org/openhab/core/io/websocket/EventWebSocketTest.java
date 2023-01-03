/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.io.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.io.websocket.EventWebSocket.WEBSOCKET_EVENT_TYPE;
import static org.openhab.core.io.websocket.EventWebSocket.WEBSOCKET_TOPIC_PREFIX;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;

import com.google.gson.Gson;

/**
 * The {@link EventWebSocketTest} contains tests for the {@link EventWebSocket}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EventWebSocketTest {
    private static final String REMOTE_WEBSOCKET_IMPLEMENTATION = "fooWebsocket";

    private static final String TEST_ITEM_NAME = "testItem";
    private static final NumberItem TEST_ITEM = new NumberItem(TEST_ITEM_NAME);

    private Gson gson = new Gson();

    private @Mock @NonNullByDefault({}) EventWebSocketServlet servlet;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisher;
    private @Mock @NonNullByDefault({}) Session session;
    private @Mock @NonNullByDefault({}) RemoteEndpoint remoteEndpoint;

    private @NonNullByDefault({}) ItemEventUtility itemEventUtility;
    private @NonNullByDefault({}) EventWebSocket eventWebSocket;

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        itemEventUtility = new ItemEventUtility(gson, itemRegistry);
        eventWebSocket = new EventWebSocket(gson, servlet, itemEventUtility, eventPublisher);

        when(session.getRemote()).thenReturn(remoteEndpoint);
        when(remoteEndpoint.getInetSocketAddress()).thenReturn(new InetSocketAddress(47115));

        when(itemRegistry.getItem(eq(TEST_ITEM_NAME))).thenReturn(TEST_ITEM);

        eventWebSocket.onConnect(session);
        verify(servlet).registerListener(eventWebSocket);
    }

    @Test
    public void listenerCorrectlyUnregisteredOnClose() {
        eventWebSocket.onClose(StatusCode.NORMAL, "Normal close.");

        verify(servlet).unregisterListener(eventWebSocket);
    }

    @Test
    public void sessionClosesOnErrorAndOnCloseCalled() {
        eventWebSocket.onError(session, new IllegalStateException());

        verify(session).close();
        verify(servlet).unregisterListener(eventWebSocket);
    }

    @Test
    public void stateEventWithIdFromWebsocketIsPublishedAndConfirmed() throws IOException {
        Event expectedEvent = ItemEventFactory.createStateEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);
        EventDTO eventDTO = new EventDTO(expectedEvent);
        eventDTO.eventId = "id-1";
        EventDTO expectedResponse = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/success", "",
                null, eventDTO.eventId);

        assertEventProcessing(eventDTO, expectedEvent, expectedResponse);
    }

    @Test
    public void stateEventWithoutIdFromWebsocketIsPublished() throws IOException {
        Event expectedEvent = ItemEventFactory.createStateEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);
        EventDTO eventDTO = new EventDTO(expectedEvent);

        assertEventProcessing(eventDTO, expectedEvent, null);
    }

    @Test
    public void commandEventWithIdFromWebsocketIsPublishedAndConfirmed() throws IOException {
        Event expectedEvent = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);

        EventDTO eventDTO = new EventDTO(expectedEvent);
        eventDTO.eventId = "id-1";
        EventDTO expectedResponse = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/success", "",
                null, eventDTO.eventId);

        assertEventProcessing(eventDTO, expectedEvent, expectedResponse);
    }

    @Test
    public void commandEventWithoutIdFromWebsocketIsPublished() throws IOException {
        Event expectedEvent = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);

        EventDTO eventDTO = new EventDTO(expectedEvent);
        assertEventProcessing(eventDTO, expectedEvent, null);
    }

    @Test
    public void illegalStateEventNotPublishedAndResponseSent() throws IOException {
        Event expectedEvent = ItemEventFactory.createStateEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);

        EventDTO eventDTO = new EventDTO(expectedEvent);
        eventDTO.payload = "";

        EventDTO expectedResponse = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/failed",
                "Processing error: Failed to deserialize payload \u0027\u0027.", null, null);

        assertEventProcessing(eventDTO, null, expectedResponse);
    }

    @Test
    public void illegalCommandEventNotPublishedAndResponseSent() throws IOException {
        Event expectedEvent = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);

        EventDTO eventDTO = new EventDTO(expectedEvent);
        eventDTO.eventId = "id-1";
        eventDTO.topic = "";

        EventDTO expectedResponse = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/failed",
                "Processing error: Topic must follow the format {namespace}/{entityType}/{entity}/{action}.", null,
                eventDTO.eventId);

        assertEventProcessing(eventDTO, null, expectedResponse);
    }

    @Test
    public void heartBeat() throws IOException {
        EventDTO eventDTO = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "heartbeat", "PING", null,
                null);
        EventDTO expectedResponse = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "heartbeat", "PONG",
                null, null);

        assertEventProcessing(eventDTO, null, expectedResponse);
    }

    @Test
    public void eventFromBusSent() throws IOException {
        Event event = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);

        eventWebSocket.processEvent(event);
        EventDTO eventDTO = new EventDTO(event);

        verify(remoteEndpoint).sendString(gson.toJson(eventDTO));
    }

    @Test
    public void eventFromBusFilterType() throws IOException {
        EventDTO eventDTO = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/type",
                "[\"ItemCommandEvent\"]", null, null);
        EventDTO responseEventDTO = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/type",
                eventDTO.payload, null, null);
        eventWebSocket.onText(gson.toJson(eventDTO));
        verify(remoteEndpoint).sendString(gson.toJson(responseEventDTO));

        // subscribed type is sent
        Event event = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO,
                REMOTE_WEBSOCKET_IMPLEMENTATION);
        eventWebSocket.processEvent(event);
        verify(remoteEndpoint).sendString(gson.toJson(new EventDTO(event)));

        // not subscribed event not sent
        event = ItemEventFactory.createStateEvent(TEST_ITEM_NAME, DecimalType.ZERO, REMOTE_WEBSOCKET_IMPLEMENTATION);
        eventWebSocket.processEvent(event);
        verify(remoteEndpoint, times(2)).sendString(any());
    }

    @Test
    public void eventFromBusFilterSource() throws IOException {
        EventDTO eventDTO = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/source",
                "[\"" + REMOTE_WEBSOCKET_IMPLEMENTATION + "\"]", null, null);
        EventDTO responseEventDTO = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/source",
                eventDTO.payload, null, null);
        eventWebSocket.onText(gson.toJson(eventDTO));
        verify(remoteEndpoint).sendString(gson.toJson(responseEventDTO));

        // non-matching is sent
        Event event = ItemEventFactory.createCommandEvent(TEST_ITEM_NAME, DecimalType.ZERO);
        eventWebSocket.processEvent(event);
        verify(remoteEndpoint).sendString(gson.toJson(new EventDTO(event)));

        // matching is not sent
        event = ItemEventFactory.createStateEvent(TEST_ITEM_NAME, DecimalType.ZERO, REMOTE_WEBSOCKET_IMPLEMENTATION);
        eventWebSocket.processEvent(event);
        verify(remoteEndpoint, times(2)).sendString(any());
    }

    private void assertEventProcessing(EventDTO incoming, @Nullable Event expectedEvent,
            @Nullable EventDTO expectedResponse) throws IOException {
        eventWebSocket.onText(gson.toJson(incoming));

        if (expectedEvent != null) {
            verify(eventPublisher).post(eq(Objects.requireNonNull(expectedEvent)));
        } else {
            verify(eventPublisher, never()).post(any());
        }

        if (expectedResponse != null) {
            String expectedResponseString = gson.toJson(expectedResponse);
            verify(remoteEndpoint).sendString(eq(expectedResponseString));
        } else {
            verify(remoteEndpoint, never()).sendString(any());
        }
    }
}
