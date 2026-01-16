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
package org.openhab.core.io.websocket.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.io.websocket.WebSocketAdapter;
import org.openhab.core.items.ItemRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;

/**
 * The {@link EventWebSocketAdapter} allows subscription to oh events over WebSocket
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { EventSubscriber.class, WebSocketAdapter.class })
public class EventWebSocketAdapter implements EventSubscriber, WebSocketAdapter {
    public static final String ADAPTER_ID = "events";
    private final Gson gson = new Gson();
    private final EventPublisher eventPublisher;

    private final ItemEventUtility itemEventUtility;
    private final Set<EventWebSocket> webSockets = new CopyOnWriteArraySet<>();

    @Activate
    public EventWebSocketAdapter(@Reference EventPublisher eventPublisher, @Reference ItemRegistry itemRegistry) {
        this.eventPublisher = eventPublisher;
        itemEventUtility = new ItemEventUtility(gson, itemRegistry);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(EventSubscriber.ALL_EVENT_TYPES);
    }

    @Override
    public void receive(Event event) {
        webSockets.forEach(ws -> ws.processEvent(event));
    }

    public void registerListener(EventWebSocket eventWebSocket) {
        webSockets.add(eventWebSocket);
    }

    public void unregisterListener(EventWebSocket eventWebSocket) {
        webSockets.remove(eventWebSocket);
    }

    @Override
    public String getId() {
        return ADAPTER_ID;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
            ServletUpgradeResponse servletUpgradeResponse) {
        return new EventWebSocket(gson, EventWebSocketAdapter.this, itemEventUtility, eventPublisher);
    }
}
