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
package org.openhab.core.io.websocket.audio.internal;

import static java.nio.ByteBuffer.wrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.openhab.core.audio.AudioDialogProvider;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.websocket.WebSocketAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PCMWebSocketAdapter} creates instances of {@link PCMWebSocketConnection} to handle PCM audio streams.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { PCMWebSocketAdapter.class, WebSocketAdapter.class })
public class PCMWebSocketAdapter implements WebSocketAdapter {
    public static final String ADAPTER_ID = "audio-pcm";

    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAdapter.class);
    private final ScheduledExecutorService executor = ThreadPoolManager.getScheduledPool("audio-pcm-websocket");
    protected final BundleContext bundleContext;
    protected final AudioManager audioManager;
    protected final AudioDialogProvider audioDialogProvider;
    private final ScheduledFuture<?> pingTask;
    private final Set<PCMWebSocketConnection> webSocketConnections = Collections.synchronizedSet(new HashSet<>());

    @Activate
    public PCMWebSocketAdapter(BundleContext bundleContext, final @Reference AudioManager audioManager,
            final @Reference AudioDialogProvider audioDialogProvider) {
        this.bundleContext = bundleContext;
        this.audioManager = audioManager;
        this.audioDialogProvider = audioDialogProvider;
        this.pingTask = executor.scheduleWithFixedDelay(this::pingHandlers, 10, 5, TimeUnit.SECONDS);
    }

    protected void onSpeakerConnected(PCMWebSocketConnection speaker) throws IllegalStateException {
        synchronized (webSocketConnections) {
            if (getSpeakerConnection(speaker.getId()) != null) {
                throw new IllegalStateException("Another speaker with the same id is already connected");
            }
            webSocketConnections.add(speaker);
            logger.debug("connected speakers {}", webSocketConnections.size());
        }
    }

    protected void onClientDisconnected(PCMWebSocketConnection connection) {
        logger.debug("speaker disconnected '{}'", connection.getId());
        synchronized (webSocketConnections) {
            webSocketConnections.remove(connection);
            logger.debug("connected speakers {}", webSocketConnections.size());
        }
    }

    public @Nullable PCMWebSocketConnection getSpeakerConnection(String id) {
        synchronized (webSocketConnections) {
            return webSocketConnections.stream()
                    .filter(speakerConnection -> speakerConnection.getId().equalsIgnoreCase(id)).findAny().orElse(null);
        }
    }

    @Override
    public String getId() {
        return ADAPTER_ID;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
            ServletUpgradeResponse servletUpgradeResponse) {
        logger.debug("creating connection");
        return new PCMWebSocketConnection(this, executor);
    }

    @Deactivate
    @SuppressWarnings("unused")
    public synchronized void deactivate() {
        logger.debug("stopping connection check");
        pingTask.cancel(true);
        disconnectAll();
    }

    private void pingHandlers() {
        ArrayList<PCMWebSocketConnection> handlers = new ArrayList<>(webSocketConnections);
        for (var handler : handlers) {
            if (handler != null) {
                boolean pinged = false;
                var remote = handler.getRemote();
                if (remote != null) {
                    try {
                        remote.sendPing(wrap("oh".getBytes(StandardCharsets.UTF_8)));
                        pinged = true;
                    } catch (IOException ignored) {
                    }
                }
                if (!pinged) {
                    logger.debug("Ping failed, disconnecting speaker '{}'", handler.getId());
                    var session = handler.getSession();
                    if (session != null) {
                        session.close();
                    }
                }
            }

        }
    }

    private void disconnectAll() {
        logger.debug("Disconnecting {} clients...", webSocketConnections.size());
        ArrayList<PCMWebSocketConnection> connections = new ArrayList<>(webSocketConnections);
        for (var connection : connections) {
            onClientDisconnected(connection);
            connection.disconnect();
        }
    }
}
