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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link PCMWebSocketConnection} represents a WebSocket connection used to transmit pcm audio.
 * <p>
 * The websocket uses the text protocol for send commands represented by {@link WebSocketCommand} and the binary
 * protocol to transmit the audio data.
 * <p>
 * The websocket supports only one line for the {@link PCMWebSocketAudioSource} (the audio is shared on the server).
 * The websocket supports multiple lines for the {@link PCMWebSocketAudioSink} (to accomplis that the outgoing data
 * chucks contain a header).
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@WebSocket
@NonNullByDefault
@SuppressWarnings("unused")
public class PCMWebSocketConnection implements WebSocketListener {
    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketConnection.class);
    protected final Map<String, ServiceRegistration<?>> audioComponentRegistrations = new ConcurrentHashMap<>();
    private volatile @Nullable Session session;
    private @Nullable RemoteEndpoint remote;
    private final PCMWebSocketAdapter wsAdapter;
    private final ScheduledExecutorService executor;
    private @Nullable ScheduledFuture<?> scheduledDisconnection;

    private boolean initialized = false;
    private @Nullable Runnable dialogTrigger = null;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private String id = "";
    private @Nullable PCMWebSocketAudioSource audioSource = null;

    public PCMWebSocketConnection(PCMWebSocketAdapter wsAdapter, ScheduledExecutorService executor) {
        this.wsAdapter = wsAdapter;
        this.executor = executor;
    }

    public void sendAudio(byte[] id, byte[] b) {
        try {
            var remote = getRemote();
            if (remote != null) {
                // concat stream identifier and send
                ByteBuffer buff = ByteBuffer.wrap(new byte[id.length + b.length]);
                buff.put(id);
                buff.put(b);
                remote.sendBytesByFuture(ByteBuffer.wrap(buff.array()));
            }
        } catch (IllegalStateException ignored) {
            logger.warn("Unable to send audio buffer");
        }
    }

    public void setListening(boolean listening) {
        sendClientCommand(new WebSocketCommand(listening ? WebSocketCommand.OutputCommands.START_LISTENING
                : WebSocketCommand.OutputCommands.STOP_LISTENING));
    }

    public void disconnect() {
        var session = getSession();
        if (session != null) {
            try {
                session.disconnect();
            } catch (IOException ignored) {
            }
        }
    }

    private void spot() {
        onRemoteSpot();
    }

    @Override
    public void onWebSocketConnect(@Nullable Session sess) {
        if (sess == null) {
            // never
            return;
        }
        this.session = sess;
        this.remote = sess.getRemote();
        logger.info("New client connected.");
        scheduledDisconnection = executor.schedule(() -> {
            try {
                sess.disconnect();
            } catch (IOException ignored) {
            }
        }, 5, TimeUnit.SECONDS);
    }

    private <T extends WebSocketCommand> void sendClientCommand(T msg) {
        var remote = getRemote();
        if (remote != null) {
            try {
                remote.sendStringByFuture(new ObjectMapper().writeValueAsString(msg));
            } catch (JsonProcessingException e) {
                logger.warn("JsonProcessingException writing JSON message: ", e);
            }
        }
    }

    @Override
    public void onWebSocketBinary(byte @Nullable [] payload, int offset, int len) {
        logger.trace("Received binary data of length {}", len);
        PCMWebSocketAudioSource audioSource = this.audioSource;
        if (payload != null && audioSource != null) {
            audioSource.writeToStreams(payload);
        }
    }

    @Override
    public void onWebSocketText(@Nullable String message) {
        try {
            var rootMessageNode = jsonMapper.readTree(message);
            if (rootMessageNode.has("cmd")) {
                try {
                    var cmd = rootMessageNode.get("cmd").asText().trim().toUpperCase();
                    logger.debug("Handling msg '{}'", cmd);
                    var messageType = WebSocketCommand.InputCommands.valueOf(cmd);
                    switch (messageType) {
                        case INITIALIZE -> {
                            wsAdapter.onSpeakerConnected(this);
                            JsonNode argsNode = rootMessageNode.get("args");
                            var initializeArgs = jsonMapper.treeToValue(argsNode,
                                    WebSocketCommand.InitializeArgs.class);
                            var scheduledDisconnection = this.scheduledDisconnection;
                            if (scheduledDisconnection != null) {
                                scheduledDisconnection.cancel(true);
                            }
                            // update connection settings
                            id = initializeArgs.id;
                            registerSpeakerComponents(id, initializeArgs.sourceSampleRate, initializeArgs.sampleRate,
                                    initializeArgs.runDialog, initializeArgs.listeningItem,
                                    initializeArgs.locationItem);
                            sendClientCommand(new WebSocketCommand(WebSocketCommand.OutputCommands.INITIALIZED));
                        }
                        case ON_SPOT -> onRemoteSpot();
                    }

                } catch (IOException | IllegalStateException e) {
                    logger.warn("Disconnecting client: {}", e.getMessage());
                    disconnect();
                }
            }
        } catch (JsonProcessingException e) {
            logger.warn("Exception parsing JSON message: ", e);
        }
    }

    @Override
    public void onWebSocketError(@Nullable Throwable cause) {
        logger.warn("WebSocket Error: ", cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, @Nullable String reason) {
        this.session = null;
        this.remote = null;
        logger.debug("Session closed with code {}: {}", statusCode, reason);
        wsAdapter.onSpeakerDisconnected(this);
        unregisterSpeakerComponents(id);
    }

    public void setSinkVolume(int value) {
        if (initialized) {
            sendClientCommand(
                    new WebSocketCommand(WebSocketCommand.OutputCommands.SINK_VOLUME, Map.of("value", value)));
        }
    }

    public void setSourceVolume(int value) {
        if (initialized) {
            sendClientCommand(
                    new WebSocketCommand(WebSocketCommand.OutputCommands.SOURCE_VOLUME, Map.of("value", value)));
        }
    }

    public @Nullable RemoteEndpoint getRemote() {
        return this.remote;
    }

    public @Nullable Session getSession() {
        return this.session;
    }

    public boolean isConnected() {
        Session sess = this.session;
        return sess != null && sess.isOpen();
    }

    private synchronized void registerSpeakerComponents(String id, int sourceSampleRate, int prefSampleRate,
            boolean runDialog, String listeningItem, String locationItem) throws IOException {
        if (id.isBlank()) {
            throw new IOException("Unable to register audio components");
        }
        String label = "UI (" + id + ")";
        logger.debug("Registering dialog components for '{}' (source sample rate {})", id, sourceSampleRate);
        this.initialized = true;
        // register source
        var audioSource = this.audioSource = new PCMWebSocketAudioSource(getSourceId(id), label, sourceSampleRate,
                this);
        logger.debug("Registering audio source {}", this.audioSource.getId());
        audioComponentRegistrations.put(this.audioSource.getId(), wsAdapter.bundleContext
                .registerService(AudioSource.class.getName(), this.audioSource, new Hashtable<>()));
        // register sink
        var audioSink = new PCMWebSocketAudioSink(getSinkId(id), label, this, prefSampleRate);
        logger.debug("Registering audio sink {}", audioSink.getId());
        audioComponentRegistrations.put(audioSink.getId(),
                wsAdapter.bundleContext.registerService(AudioSink.class.getName(), audioSink, new Hashtable<>()));
        // init dialog
        if (runDialog) {
            var dialogProvider = this.wsAdapter.dialogProvider;
            if (dialogProvider == null) {
                throw new IOException("Voice functionality is not ready");
            }
            dialogTrigger = dialogProvider.startDialog(this, audioSink, audioSource,
                    !locationItem.isBlank() ? locationItem : null, !listeningItem.isBlank() ? listeningItem : null);
        } else {
            dialogTrigger = null;
        }
    }

    private synchronized void unregisterSpeakerComponents(String id) {
        initialized = false;
        dialogTrigger = null;
        var source = wsAdapter.audioManager.getSource(getSourceId(id));
        if (source instanceof PCMWebSocketAudioSource hsAudioSource) {
            try {
                hsAudioSource.close();
            } catch (Exception ignored) {
            }
        }
        if (source != null) {
            ServiceRegistration<?> sourceReg = audioComponentRegistrations.remove(source.getId());
            if (sourceReg != null) {
                logger.debug("Unregistering audio source {}", source.getId());
                sourceReg.unregister();
            }
        }
        var sink = wsAdapter.audioManager.getSink(getSinkId(id));
        if (sink != null) {
            ServiceRegistration<?> sinkReg = audioComponentRegistrations.remove(sink.getId());
            if (sinkReg != null) {
                logger.debug("Unregistering audio sink {}", sink.getId());
                sinkReg.unregister();
            }
        }
    }

    private void onRemoteSpot() {
        var dialogTrigger = this.dialogTrigger;
        if (dialogTrigger != null) {
            dialogTrigger.run();
        }
    }

    private String getSinkId(String id) {
        return "pcm::" + id + "::sink";
    }

    private String getSourceId(String id) {
        return "pcm::" + id + "::source";
    }

    public String getId() {
        return id;
    }

    private static class WebSocketCommand {
        public String cmd = "";
        public Map<String, Object> args;

        public WebSocketCommand(OutputCommands cmd) {
            this(cmd, new HashMap<>());
        }

        public WebSocketCommand(OutputCommands cmd, Map<String, Object> args) {
            this.cmd = cmd.name();
            this.args = args;
        }

        public enum OutputCommands {
            INITIALIZED,
            START_LISTENING,
            STOP_LISTENING,
            SINK_VOLUME,
            SOURCE_VOLUME,
        }

        public enum InputCommands {
            INITIALIZE,
            ON_SPOT,
        }

        public static class InitializeArgs {
            /**
             * Identifier to concatenate to the audio source/sink id.
             */
            public String id = "";
            /**
             * Sample rate of the source audio line.
             */
            public int sourceSampleRate;
            /**
             * Client preferred sample rate.
             */
            public int sampleRate;
            /**
             * Start a dialog processor using the registered audio components.
             */
            public boolean runDialog = false;
            /**
             * Listening item for the dialog.
             */
            public String listeningItem = "";
            /**
             * Location item for the dialog.
             */
            public String locationItem = "";

            public InitializeArgs() {
            }
        }
    }
}
