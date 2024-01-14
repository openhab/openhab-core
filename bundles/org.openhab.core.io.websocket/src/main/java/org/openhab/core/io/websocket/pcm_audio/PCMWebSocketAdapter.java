/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.websocket.pcm_audio;

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
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.websocket.WebSocketAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PCMWebSocketAdapter} creates instances of {@link PCMWebSocketConnection} to handle pcm audio.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { PCMWebSocketAdapter.class, WebSocketAdapter.class })
public class PCMWebSocketAdapter implements WebSocketAdapter {
    public static final String ADAPTER_ID = "pcm-audio";

    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAdapter.class);
    private final ScheduledExecutorService executor = ThreadPoolManager.getScheduledPool("pcm-audio-websocket");
    protected final BundleContext bundleContext;
    protected final AudioManager audioManager;
    protected PCMWebSocketAdapter.@Nullable DialogProvider dialogProvider = null;
    private final ScheduledFuture<?> pingTask;
    private final Set<PCMWebSocketConnection> speakerConnections = Collections.synchronizedSet(new HashSet<>());

    @Activate
    public PCMWebSocketAdapter(BundleContext bundleContext, final @Reference AudioManager audioManager) {
        this.bundleContext = bundleContext;
        this.audioManager = audioManager;
        this.pingTask = executor.scheduleWithFixedDelay(this::pingHandlers, 10, 5, TimeUnit.SECONDS);
    }

    protected void onSpeakerConnected(PCMWebSocketConnection speaker) throws IllegalStateException {
        synchronized (speakerConnections) {
            if (getSpeakerConnection(speaker.getId()) != null) {
                throw new IllegalStateException("Another speaker with the same id is already connected");
            }
            speakerConnections.add(speaker);
            logger.debug("connected speakers {}", speakerConnections.size());
        }
    }

    private void onSpeakerDisconnected(PCMWebSocketConnection connection) {
        logger.debug("speaker disconnected '{}'", connection.getId());
        synchronized (speakerConnections) {
            speakerConnections.remove(connection);
            logger.debug("connected speakers {}", speakerConnections.size());
        }
    }

    public @Nullable PCMWebSocketConnection getSpeakerConnection(String id) {
        synchronized (speakerConnections) {
            return speakerConnections.stream()
                    .filter(speakerConnection -> speakerConnection.getId().equalsIgnoreCase(id)).findAny().orElse(null);
        }
    }

    public void setDialogProvider(DialogProvider dialogProvider) {
        this.dialogProvider = dialogProvider;
    }

    @Override
    public String getId() {
        return ADAPTER_ID;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
            ServletUpgradeResponse servletUpgradeResponse) {
        logger.debug("creating connection!");
        return new PCMWebSocketConnection(this, executor);
    }

    public synchronized void dispose() {
        logger.debug("Unregistering protocols");
        pingTask.cancel(true);
        disconnectHandlers();
    }

    private void pingHandlers() {
        var handlers = new ArrayList<>(speakerConnections);
        for (var handler : handlers) {
            if (handler != null) {
                boolean pinned = false;
                var remote = handler.getRemote();
                if (remote != null) {
                    try {
                        remote.sendPing(wrap("oh".getBytes(StandardCharsets.UTF_8)));
                        pinned = true;
                    } catch (IOException ignored) {
                    }
                }
                if (!pinned) {
                    logger.warn("ping failed, disconnecting speaker {}", handler.getId());
                    var session = handler.getSession();
                    if (session != null) {
                        session.close();
                    }
                }
            }

        }
    }

    private void disconnectHandlers() {
        logger.debug("Disconnecting {} clients...", speakerConnections.size());
        var handlers = new ArrayList<>(speakerConnections);
        for (var handler : handlers) {
            onSpeakerDisconnected(handler);
            var session = handler.getSession();
            if (session != null) {
                try {
                    session.disconnect();
                } catch (IOException e) {
                    logger.debug("Disconnect failed: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * These interface is meant to be implemented in the voice bundle for providing the dialog initialization
     * functionality to these websocket connections.
     */
    public interface DialogProvider {
        /**
         * Starts a dialog and returns a runnable that triggers it.
         *
         * @param audioSink the audio sink to play sound
         * @param audioSource the audio source to capture sound
         * @param locationItem an Item name to scope dialog commands
         * @param listeningItem an Item name to toggle while dialog is listening
         * @return a {@link Runnable} instance to trigger dialog processing
         */
        Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
                @Nullable String listeningItem);
    }
}
