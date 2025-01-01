/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link LogWebSocket} is the WebSocket implementation for logs
 *
 * @author Jan N. Klug - Initial contribution
 */
@WebSocket
@NonNullByDefault
@SuppressWarnings("unused")
public class LogWebSocket implements LogListener {
    @SuppressWarnings("unchecked")
    private static final TypeToken<List<String>> STRING_LIST_TYPE = (TypeToken<List<String>>) TypeToken
            .getParameterized(List.class, String.class);

    private final Logger logger = LoggerFactory.getLogger(LogWebSocket.class);

    private final LogWebSocketAdapter wsAdapter;
    private final Gson gson;

    private @Nullable Session session;
    private @Nullable RemoteEndpoint remoteEndpoint;
    private String remoteIdentifier = "<unknown>";

    private List<Pattern> loggerPatterns = List.of();

    public LogWebSocket(Gson gson, LogWebSocketAdapter wsAdapter) {
        this.wsAdapter = wsAdapter;
        this.gson = gson;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        this.wsAdapter.unregisterListener(this);
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
        this.wsAdapter.registerListener(this);
    }

    @OnWebSocketMessage
    public void onText(String message) {
        RemoteEndpoint remoteEndpoint = this.remoteEndpoint;
        if (session == null || remoteEndpoint == null) {
            // no connection or no remote endpoint , do nothing this is possible due to async behavior
            return;
        }

        try {
            loggerPatterns = gson.fromJson(message, STRING_LIST_TYPE).stream().map(Pattern::compile).toList();
        } catch (JsonParseException e) {
            logger.warn("Failed to parse '{}' to a list of subscribed loggers", message);
        }
    }

    @OnWebSocketError
    public void onError(Session session, @Nullable Throwable error) {
        if (session != null) {
            session.close();
        }

        String message = error == null ? "<null>" : Objects.requireNonNullElse(error.getMessage(), "<null>");
        logger.info("WebSocket error: {}", message);
        onClose(StatusCode.NO_CODE, message);
    }

    private synchronized void sendMessage(String message) throws IOException {
        RemoteEndpoint remoteEndpoint = this.remoteEndpoint;
        if (remoteEndpoint == null) {
            logger.warn("Could not determine remote endpoint, failed to send '{}'.", message);
            return;
        }
        remoteEndpoint.sendString(message);
    }

    @Override
    public void logged(@NonNullByDefault({}) LogEntry logEntry) {
        if (!loggerPatterns.isEmpty() && loggerPatterns.stream().noneMatch(logMatch(logEntry))) {
            return;
        }
        try {
            LogDTO logDTO = map(logEntry);
            sendMessage(gson.toJson(logDTO));
        } catch (IOException e) {
            logger.debug("Failed to send log {} to {}: {}", logEntry, remoteIdentifier, e.getMessage());
        }
    }

    private static Predicate<Pattern> logMatch(LogEntry logEntry) {
        return pattern -> pattern.matcher(logEntry.getLoggerName()).matches();
    }

    private static LogDTO map(LogEntry logEntry) {
        LogDTO logDTO = new LogDTO();
        logDTO.loggerName = logEntry.getLoggerName();
        logDTO.level = logEntry.getLogLevel();
        logDTO.unixtime = logEntry.getTime();
        logDTO.timestamp = new Date(logEntry.getTime());
        logDTO.message = logEntry.getMessage();
        return logDTO;
    }
}
