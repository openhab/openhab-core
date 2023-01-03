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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link EventWebSocket} is the WebSocket implementation that extends the event bus
 *
 * @author Jan N. Klug - Initial contribution
 */
@WebSocket
@NonNullByDefault
@SuppressWarnings("unused")
public class EventWebSocket {
    public static final String WEBSOCKET_EVENT_TYPE = "WebSocketEvent";
    public static final String WEBSOCKET_TOPIC_PREFIX = "openhab/websocket/";

    private static final Type STRING_LIST_TYPE = TypeToken.getParameterized(List.class, String.class).getType();

    private final Logger logger = LoggerFactory.getLogger(EventWebSocket.class);

    private final EventWebSocketServlet servlet;
    private final Gson gson;
    private final EventPublisher eventPublisher;
    private final ItemEventUtility itemEventUtility;

    private @Nullable Session session;
    private @Nullable RemoteEndpoint remoteEndpoint;
    private String remoteIdentifier = "<unknown>";

    private List<String> typeFilter = List.of();
    private List<String> sourceFilter = List.of();

    public EventWebSocket(Gson gson, EventWebSocketServlet servlet, ItemEventUtility itemEventUtility,
            EventPublisher eventPublisher) {
        this.servlet = servlet;
        this.gson = gson;
        this.itemEventUtility = itemEventUtility;
        this.eventPublisher = eventPublisher;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        this.servlet.unregisterListener(this);
        remoteIdentifier = "<unknown>";
        this.session = null;
        this.remoteEndpoint = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        RemoteEndpoint remoteEndpoint = session.getRemote();
        this.remoteEndpoint = remoteEndpoint;
        this.remoteIdentifier = remoteEndpoint.getInetSocketAddress().toString();
        this.servlet.registerListener(this);
    }

    @OnWebSocketMessage
    public void onText(String message) {
        RemoteEndpoint remoteEndpoint = this.remoteEndpoint;
        if (session == null || remoteEndpoint == null) {
            // no connection or no remote endpoint , do nothing this is possible due to async behavior
            return;
        }

        EventDTO responseEvent;

        try {
            EventDTO eventDTO = gson.fromJson(message, EventDTO.class);
            try {
                if (eventDTO == null) {
                    throw new EventProcessingException("Deserialized event must not be null");
                }
                String type = eventDTO.type;
                if (type == null) {
                    throw new EventProcessingException("Event type must not be null.");
                }

                switch (type) {
                    case "ItemCommandEvent":
                        Event itemCommandEvent = itemEventUtility.createCommandEvent(eventDTO);
                        eventPublisher.post(itemCommandEvent);
                        responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/success",
                                "", null, eventDTO.eventId);
                        break;
                    case "ItemStateEvent":
                        Event itemStateEvent = itemEventUtility.createStateEvent(eventDTO);
                        eventPublisher.post(itemStateEvent);
                        responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/success",
                                "", null, eventDTO.eventId);
                        break;
                    case WEBSOCKET_EVENT_TYPE:
                        if ((WEBSOCKET_TOPIC_PREFIX + "heartbeat").equals(eventDTO.topic)
                                && "PING".equals(eventDTO.payload)) {
                            responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "heartbeat",
                                    "PONG", null, eventDTO.eventId);
                        } else if ((WEBSOCKET_TOPIC_PREFIX + "filter/type").equals(eventDTO.topic)) {
                            typeFilter = Objects.requireNonNullElse(gson.fromJson(eventDTO.payload, STRING_LIST_TYPE),
                                    List.of());
                            logger.debug("Setting type filter for connection to {}: {}",
                                    remoteEndpoint.getInetSocketAddress(), typeFilter);
                            responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/type",
                                    eventDTO.payload, null, eventDTO.eventId);
                        } else if ((WEBSOCKET_TOPIC_PREFIX + "filter/source").equals(eventDTO.topic)) {
                            sourceFilter = Objects.requireNonNullElse(gson.fromJson(eventDTO.payload, STRING_LIST_TYPE),
                                    List.of());
                            logger.debug("Setting source filter for connection to {}: {}",
                                    remoteEndpoint.getInetSocketAddress(), typeFilter);
                            responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "filter/source",
                                    eventDTO.payload, null, eventDTO.eventId);
                        } else {
                            throw new EventProcessingException("Invalid topic or payload in WebSocketEvent");
                        }
                        break;
                    default:
                        throw new EventProcessingException("Unknown event type '" + eventDTO.type + "'");
                }
                if (!WEBSOCKET_EVENT_TYPE.equals(type) && responseEvent.eventId == null) {
                    // skip only for successful processing of state/command, always send response if processing failed
                    logger.trace("Not sending response event {}, because no eventId present.", responseEvent);
                    return;
                }
            } catch (EventProcessingException | JsonParseException e) {
                logger.warn("Failed to process deserialized event '{}': {}", message, e.getMessage());
                responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/failed",
                        "Processing error: " + e.getMessage(), null, eventDTO != null ? eventDTO.eventId : "");

            }
        } catch (JsonParseException e) {
            logger.warn("Could not deserialize '{}'", message);
            responseEvent = new EventDTO(WEBSOCKET_EVENT_TYPE, WEBSOCKET_TOPIC_PREFIX + "response/failed",
                    "Deserialization error: " + e.getMessage(), null, null);
        }

        try {
            sendMessage(gson.toJson(responseEvent));
        } catch (IOException e) {
            logger.debug("Failed to send WebSocketResponseEvent event {} to {}: {}", responseEvent, remoteIdentifier,
                    e.getMessage());
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        if (session != null) {
            session.close();
        }

        String message = error == null ? "<null>" : Objects.requireNonNullElse(error.getMessage(), "<null>");
        logger.info("WebSocket error: {}", message);
        onClose(StatusCode.NO_CODE, message);
    }

    public void processEvent(Event event) {
        try {
            String source = event.getSource();
            if ((source == null || !sourceFilter.contains(event.getSource()))
                    && (typeFilter.isEmpty() || typeFilter.contains(event.getType()))) {
                sendMessage(gson.toJson(new EventDTO(event)));
            }
        } catch (IOException e) {
            logger.debug("Failed to send event {} to {}: {}", event, remoteIdentifier, e.getMessage());
        }
    }

    private synchronized void sendMessage(String message) throws IOException {
        RemoteEndpoint remoteEndpoint = this.remoteEndpoint;
        if (remoteEndpoint == null) {
            logger.warn("Could not determine remote endpoint, failed to send '{}'.", message);
            return;
        }
        remoteEndpoint.sendString(message);
    }
}
