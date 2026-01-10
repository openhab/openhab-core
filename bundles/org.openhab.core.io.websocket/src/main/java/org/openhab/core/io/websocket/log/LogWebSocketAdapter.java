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
package org.openhab.core.io.websocket.log;

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.openhab.core.io.websocket.WebSocketAdapter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;

import com.google.gson.Gson;

/**
 * The {@link LogWebSocketAdapter} allows subscription to log events over WebSocket
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { WebSocketAdapter.class })
public class LogWebSocketAdapter implements WebSocketAdapter {
    public static final String ADAPTER_ID = "logs";
    private final Gson gson = new Gson();
    private final Set<LogWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private final LogReaderService logReaderService;

    @Activate
    public LogWebSocketAdapter(@Reference LogReaderService logReaderService) {
        this.logReaderService = logReaderService;
    }

    @Deactivate
    public void deactivate() {
        webSockets.forEach(logReaderService::removeLogListener);
    }

    public void registerListener(LogWebSocket logWebSocket) {
        webSockets.add(logWebSocket);
        logReaderService.addLogListener(logWebSocket);
    }

    public void unregisterListener(LogWebSocket logWebSocket) {
        logReaderService.removeLogListener(logWebSocket);
        webSockets.remove(logWebSocket);
    }

    @Override
    public String getId() {
        return ADAPTER_ID;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
            ServletUpgradeResponse servletUpgradeResponse) {
        return new LogWebSocket(gson, LogWebSocketAdapter.this);
    }

    public Enumeration<LogEntry> getLog() {
        return logReaderService.getLog();
    }
}
