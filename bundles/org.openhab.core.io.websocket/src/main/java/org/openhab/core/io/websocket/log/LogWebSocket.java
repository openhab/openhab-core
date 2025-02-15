/*
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link LogWebSocket} is the WebSocket implementation for logs.
 *
 * This supports sending of history, and provides a method of managing message cadence.
 * When a client connects, it must send a filter request before the server will send any logs. This triggers the sending
 * of history.
 *
 * Live logs are sent as individual messages if they are received with sufficient spacing. When logs come in very
 * quickly, they are clustered together and sent as an array after up to 100mS.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Chris Jackson - Add history and improve performance using arrays
 */
@WebSocket
@NonNullByDefault
@SuppressWarnings("unused")
public class LogWebSocket implements LogListener {
    @SuppressWarnings("unchecked")
    private static final TypeToken<List<String>> STRING_LIST_TYPE = (TypeToken<List<String>>) TypeToken
            .getParameterized(List.class, String.class);

    private static final int SEND_PERIOD = 100; // Minimum allowable time between log packets (in milliseconds)
    private static final long FIRST_SEQUENCE = 0;

    private final Logger logger = LoggerFactory.getLogger(LogWebSocket.class);

    private final LogWebSocketAdapter wsAdapter;
    private final Gson gson;

    private @Nullable Session session;
    private @Nullable RemoteEndpoint remoteEndpoint;

    private final ScheduledExecutorService scheduledExecutorService;
    private @Nullable ScheduledFuture<?> commitScheduledFuture;

    private long lastSentTime = 0;
    private List<LogDTO> deferredLogs = new ArrayList<>();

    private boolean enabled = false;
    private long lastSequence = FIRST_SEQUENCE;

    private List<Pattern> loggerPatterns = List.of();

    public LogWebSocket(Gson gson, LogWebSocketAdapter wsAdapter) {
        this.wsAdapter = wsAdapter;
        this.gson = gson;

        scheduledExecutorService = ThreadPoolManager.getScheduledPool("LogWebSocket");
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        if (enabled) {
            this.wsAdapter.unregisterListener(this);
        }
        stopDeferredScheduledFuture();
        this.session = null;
        this.remoteEndpoint = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        RemoteEndpoint remoteEndpoint = session.getRemote();
        this.remoteEndpoint = remoteEndpoint;
    }

    @OnWebSocketMessage
    public void onText(String message) {
        // Detect empty message (keepalive) and ignore
        if ("{}".equals(message)) {
            return;
        }

        // Defer sending live logs while we process the history
        lastSentTime = Long.MAX_VALUE;
        stopDeferredScheduledFuture();

        // Enable log messages
        if (!enabled) {
            this.wsAdapter.registerListener(this);
            enabled = true;
        }

        RemoteEndpoint remoteEndpoint = this.remoteEndpoint;
        if (session == null || remoteEndpoint == null) {
            // no connection or no remote endpoint , do nothing this is possible due to async behavior
            return;
        }

        LogFilterDTO logFilterDto;
        try {
            logFilterDto = gson.fromJson(message, LogFilterDTO.class);
        } catch (JsonParseException e) {
            logger.warn("Failed to parse '{}' to a valid log filter object", message);
            return;
        }

        loggerPatterns = logFilterDto.loggerNames == null ? List.of()
                : logFilterDto.loggerNames.stream().map(Pattern::compile).toList();

        Long timeStart;
        Long timeStop;
        if (logFilterDto.timeStart != null) {
            timeStart = logFilterDto.timeStart;
        } else {
            timeStart = Long.MIN_VALUE;
        }
        if (logFilterDto.timeStop != null) {
            timeStop = logFilterDto.timeStop;
        } else {
            timeStop = Long.MAX_VALUE;
        }

        Long sequenceStart;
        if (logFilterDto.sequenceStart != null) {
            sequenceStart = logFilterDto.sequenceStart;
        } else {
            sequenceStart = lastSequence;
        }

        List<LogEntry> logs = new ArrayList<>();
        for (Enumeration<LogEntry> history = wsAdapter.getLog(); history.hasMoreElements();) {
            logs.add(history.nextElement());
        }

        if (logs.isEmpty()) {
            lastSentTime = 0;
            return;
        }

        Predicate<LogEntry> withinTimeRange = log -> (log.getTime() >= timeStart) && (log.getTime() <= timeStop);
        Predicate<LogEntry> withinSequence = log -> log.getSequence() > sequenceStart;
        Predicate<LogEntry> nameMatchesAnyPattern = log -> loggerPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(log.getLoggerName()).matches());

        List<LogEntry> filteredEvents = logs.stream().filter(withinTimeRange.and(withinSequence))
                .collect(Collectors.toList());
        // List<LogEntry> filteredEvents = logs.stream().filter(withinTimeRange.and(nameMatchesAnyPattern))
        // .collect(Collectors.toList());
        List<LogDTO> dtoList = filteredEvents.stream().map(this::map).collect(Collectors.toList());
        Collections.sort(dtoList);

        try {
            sendMessage(gson.toJson(dtoList));
        } catch (IOException e) {
        }
        lastSentTime = System.currentTimeMillis();

        // Remove any duplicates from the live log buffer
        long newestSequence = logs.get(0).getSequence();
        synchronized (deferredLogs) {
            Iterator<LogDTO> iterator = deferredLogs.iterator();
            while (iterator.hasNext()) {
                LogDTO value = iterator.next();
                if (value.sequence <= newestSequence) {
                    iterator.remove();
                }
            }
        }

        // Continue with live logs...
        flush();
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
            return;
        }
        remoteEndpoint.sendString(message);
    }

    @Override
    public void logged(@NonNullByDefault({}) LogEntry logEntry) {
        if (!loggerPatterns.isEmpty() && loggerPatterns.stream().noneMatch(logPatternMatch(logEntry))) {
            return;
        }

        LogDTO logDTO = map(logEntry);
        lastSequence = logEntry.getSequence();

        // If the last message sent was less than SEND_PERIOD ago, then we just buffer
        if (lastSentTime > System.currentTimeMillis() - SEND_PERIOD) {
            // Start the timer if this is the first deferred log
            synchronized (deferredLogs) {
                if (deferredLogs.isEmpty()) {
                    commitScheduledFuture = scheduledExecutorService.schedule(this::flush,
                            lastSentTime + SEND_PERIOD - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                }

                deferredLogs.add(logDTO);
            }
        } else {
            lastSentTime = System.currentTimeMillis();
            try {
                sendMessage(gson.toJson(logDTO));
            } catch (IOException e) {
                // Fail silently!
            }
        }
    }

    private static Predicate<Pattern> logPatternMatch(LogEntry logEntry) {
        return pattern -> pattern.matcher(logEntry.getLoggerName()).matches();
    }

    private LogDTO map(LogEntry logEntry) {
        return new LogDTO(logEntry.getSequence(), logEntry.getLoggerName(), logEntry.getLogLevel(), logEntry.getTime(),
                logEntry.getMessage());
    }

    private void stopDeferredScheduledFuture() {
        // Stop any existing scheduled commit
        ScheduledFuture<?> commitScheduledFuture = this.commitScheduledFuture;
        if (commitScheduledFuture != null) {
            commitScheduledFuture.cancel(false);
            commitScheduledFuture = null;
        }
    }

    private synchronized void flush() {
        stopDeferredScheduledFuture();

        synchronized (deferredLogs) {
            if (!deferredLogs.isEmpty()) {
                try {
                    sendMessage(gson.toJson(deferredLogs));
                } catch (IOException e) {
                }

                deferredLogs.clear();
            }
        }
    }
}
