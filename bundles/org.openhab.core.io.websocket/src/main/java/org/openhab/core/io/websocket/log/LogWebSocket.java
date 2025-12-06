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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // All access must be guarded by "this"
    private @Nullable Session session;

    // All access must be guarded by "this"
    private @Nullable RemoteEndpoint remoteEndpoint;

    private final ScheduledExecutorService scheduledExecutorService;

    // All access must be guarded by "this"
    private @Nullable ScheduledFuture<?> commitScheduledFuture;

    // All access must be guarded by "this"
    private long lastSentTime = 0;

    // All access must be guarded by "this"
    private List<LogDTO> deferredLogs = new ArrayList<>();

    // All access must be guarded by "this"
    private boolean enabled = false;

    // All access must be guarded by "this"
    private long lastSequence = FIRST_SEQUENCE;

    private final List<Pattern> loggerPatterns = new CopyOnWriteArrayList<>();

    public LogWebSocket(Gson gson, LogWebSocketAdapter wsAdapter) {
        this.wsAdapter = wsAdapter;
        this.gson = gson;

        scheduledExecutorService = ThreadPoolManager.getScheduledPool("LogWebSocket");
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        stopDeferredScheduledFuture();
        synchronized (this) {
            if (enabled) {
                this.wsAdapter.unregisterListener(this);
                enabled = false;
            }
            this.session = null;
            this.remoteEndpoint = null;
        }
    }

    @OnWebSocketConnect
    public synchronized void onConnect(Session session) {
        this.session = session;
        this.remoteEndpoint = session.getRemote();
    }

    @OnWebSocketMessage
    public void onText(String message) {
        // Detect empty message (keepalive) and ignore
        if ("{}".equals(message)) {
            return;
        }

        Session session;
        RemoteEndpoint remoteEndpoint;
        synchronized (this) {
            // Defer sending live logs while we process the history
            lastSentTime = Long.MAX_VALUE;
            stopDeferredScheduledFuture();

            // Enable log messages
            if (!enabled) {
                this.wsAdapter.registerListener(this);
                enabled = true;
            }

            session = this.session;
            remoteEndpoint = this.remoteEndpoint;
        }
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

        if (!loggerPatterns.isEmpty()) {
            loggerPatterns.clear();
        }
        List<String> loggerNames;
        if (logFilterDto != null && (loggerNames = logFilterDto.loggerNames) != null) {
            List<Pattern> filters = loggerNames.stream().map(Pattern::compile).toList();
            if (!filters.isEmpty()) {
                loggerPatterns.addAll(filters);
            }
        }

        Long timeStart;
        Long timeStop;
        if (logFilterDto != null && logFilterDto.timeStart != null) {
            timeStart = logFilterDto.timeStart;
        } else {
            timeStart = Long.MIN_VALUE;
        }
        if (logFilterDto != null && logFilterDto.timeStop != null) {
            timeStop = logFilterDto.timeStop;
        } else {
            timeStop = Long.MAX_VALUE;
        }

        Long sequenceStart;
        if (logFilterDto != null && logFilterDto.sequenceStart != null) {
            sequenceStart = logFilterDto.sequenceStart;
        } else {
            synchronized (this) {
                sequenceStart = lastSequence;
            }
        }

        List<LogEntry> logs = new ArrayList<>();
        for (Enumeration<LogEntry> history = wsAdapter.getLog(); history.hasMoreElements();) {
            logs.add(history.nextElement());
        }

        if (logs.isEmpty()) {
            synchronized (this) {
                lastSentTime = 0;
            }
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

        long sentTime;
        try {
            sendMessage(gson.toJson(dtoList));
            sentTime = System.currentTimeMillis();
        } catch (IOException e) {
            sentTime = Long.MIN_VALUE;
        }

        // Remove any duplicates from the live log buffer
        long newestSequence = logs.getFirst().getSequence();
        synchronized (this) {
            Iterator<LogDTO> iterator = deferredLogs.iterator();
            while (iterator.hasNext()) {
                LogDTO value = iterator.next();
                if (value.sequence <= newestSequence) {
                    iterator.remove();
                }
            }
        }
        synchronized (this) {
            lastSentTime = sentTime == Long.MIN_VALUE ? 0L : sentTime;
        }

        // Continue with live logs...
        flush();
    }

    @OnWebSocketError
    public void onError(@Nullable Session session, @Nullable Throwable error) {
        if (session != null) {
            session.close();
        }

        String message = error == null ? "<null>" : Objects.requireNonNullElse(error.getMessage(), "<null>");
        logger.info("WebSocket error: {}", message);
        onClose(StatusCode.NO_CODE, message);
    }

    private void sendMessage(String message) throws IOException {
        RemoteEndpoint remoteEndpoint;
        synchronized (this) {
            remoteEndpoint = this.remoteEndpoint;
        }
        if (remoteEndpoint == null) {
            return;
        }
        remoteEndpoint.sendString(message);
    }

    /**
     * @implNote Under no circumstances must this method result in something being logged, since that
     *           causes an "endless circle".
     */
    @Override
    public void logged(@NonNullByDefault({}) LogEntry logEntry) {
        if (!loggerPatterns.isEmpty() && loggerPatterns.stream().noneMatch(logPatternMatch(logEntry))) {
            return;
        }

        LogDTO logDTO = map(logEntry);
        boolean bufferEmpty;
        synchronized (this) {
            lastSequence = logEntry.getSequence();

            // If the buffer isn't empty or the last message was sent less than SEND_PERIOD ago, then we just buffer
            if (!(bufferEmpty = deferredLogs.isEmpty()) || lastSentTime > System.currentTimeMillis() - SEND_PERIOD) {
                if (bufferEmpty) {
                    stopDeferredScheduledFuture();
                    commitScheduledFuture = scheduledExecutorService.schedule(this::flush,
                            lastSentTime + SEND_PERIOD - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                }

                deferredLogs.add(logDTO);
            } else {
                lastSentTime = System.currentTimeMillis();
                scheduledExecutorService.submit(() -> {
                    try {
                        sendMessage(gson.toJson(logDTO));
                    } catch (IOException e) {
                        // Fail silently!
                    }
                });
            }
        }
    }

    private static Predicate<Pattern> logPatternMatch(LogEntry logEntry) {
        return pattern -> pattern.matcher(logEntry.getLoggerName()).matches();
    }

    private LogDTO map(LogEntry logEntry) {
        String stackTrace;
        if (logEntry.getException() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            logEntry.getException().printStackTrace(pw);
            stackTrace = sw.toString();
        } else {
            stackTrace = "";
        }

        return new LogDTO(logEntry.getSequence(), logEntry.getLoggerName(), logEntry.getLogLevel(), logEntry.getTime(),
                logEntry.getMessage(), stackTrace);
    }

    private void stopDeferredScheduledFuture() {
        // Stop any existing scheduled commit
        ScheduledFuture<?> commitScheduledFuture;
        synchronized (this) {
            commitScheduledFuture = this.commitScheduledFuture;
            this.commitScheduledFuture = null;
        }
        if (commitScheduledFuture != null && !commitScheduledFuture.isDone()) {
            commitScheduledFuture.cancel(false);
        }
    }

    private void flush() {
        stopDeferredScheduledFuture();

        List<LogDTO> logs;
        synchronized (this) {
            if (deferredLogs.isEmpty()) {
                logs = null;
            } else {
                logs = List.copyOf(deferredLogs);
                deferredLogs.clear();
            }
        }
        if (logs != null) {
            try {
                sendMessage(gson.toJson(logs));
            } catch (IOException e) {
            }
        }
    }
}
