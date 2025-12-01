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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.*;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an {@link AudioSource} implementation connected to a {@link PCMWebSocketConnection} supporting
 * a single PCM audio line through a WebSocket connection and shared across all active {@link AudioStream} instances.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketAudioSource implements AudioSource {
    private static final int SUPPORTED_BIT_DEPTH = 16;
    private static final int SUPPORTED_SAMPLE_RATE = 16000;
    private static final int SUPPORTED_CHANNELS = 1;
    public static AudioFormat supportedFormat = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, false, SUPPORTED_BIT_DEPTH, null, (long) SUPPORTED_SAMPLE_RATE,
            SUPPORTED_CHANNELS);
    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAudioSource.class);
    private final String sourceId;
    private final String sourceLabel;
    private final PCMWebSocketConnection websocket;
    private @Nullable PipedOutputStream sourceAudioPipedOutput;
    private @Nullable PipedInputStream sourceAudioPipedInput;
    private @Nullable InputStream sourceAudioStream;
    private final PipedAudioStream.Group streamGroup = PipedAudioStream.newGroup(supportedFormat);
    private @Nullable Future<?> sourceWriteTask;
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("pcm-audio-source");
    private byte @Nullable [] streamId;

    public PCMWebSocketAudioSource(String id, String label, PCMWebSocketConnection websocket) {
        this.sourceId = id;
        this.sourceLabel = label;
        this.websocket = websocket;
    }

    @Override
    public String getId() {
        return this.sourceId;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return this.sourceLabel;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return Set.of(supportedFormat);
    }

    @Override
    public AudioStream getInputStream(AudioFormat audioFormat) throws AudioException {
        try {
            final PipedAudioStream stream = streamGroup.getAudioStreamInGroup();
            synchronized (streamGroup) {
                if (this.streamGroup.size() == 1) {
                    logger.debug("Send start listening {}", getId());
                    this.streamId = null;
                    websocket.setListening(true);
                }
            }
            stream.onClose(this::onStreamClose);
            return stream;
        } catch (IOException e) {
            throw new AudioException(e);
        }
    }

    public void close() throws Exception {
        streamGroup.close();
    }

    public void writeToStreams(byte[] id, int sampleRate, int bitDepth, int channels, byte[] payload) {
        if (streamGroup.isEmpty()) {
            logger.debug("Source already disposed, ignoring data");
            return;
        }
        if (this.streamId == null) {
            this.streamId = id;
        } else if (!Arrays.equals(this.streamId, id)) {
            logger.warn("Only one concurrent data line is supported, ignoring data from source stream {}", id);
            return;
        }
        boolean needsConvert = sampleRate != SUPPORTED_SAMPLE_RATE || bitDepth != SUPPORTED_BIT_DEPTH
                || channels != SUPPORTED_CHANNELS;
        if (!needsConvert) {
            streamGroup.write(payload);
            return;
        }
        if (this.sourceAudioPipedOutput == null || this.sourceAudioStream == null) {
            try {
                this.sourceAudioPipedOutput = new PipedOutputStream();
                var sourceAudioPipedInput = this.sourceAudioPipedInput = new PipedInputStream(
                        this.sourceAudioPipedOutput, (sampleRate * (bitDepth / 8) * channels) * 2);
                logger.debug(
                        "Enabling converting pcm audio for the audio source stream: sample rate {}, bit depth {}, channels {} => sample rate {}, bit depth {}, channels {}",
                        sampleRate, bitDepth, channels, SUPPORTED_SAMPLE_RATE, SUPPORTED_BIT_DEPTH, SUPPORTED_CHANNELS);
                this.sourceAudioStream = PCMWebSocketAudioUtil.getPCMStreamNormalized(sourceAudioPipedInput, sampleRate,
                        bitDepth, channels, SUPPORTED_SAMPLE_RATE, SUPPORTED_BIT_DEPTH, SUPPORTED_CHANNELS);
                sourceWriteTask = scheduler.submit(() -> {
                    int bytesPer250ms = (SUPPORTED_SAMPLE_RATE * (SUPPORTED_BIT_DEPTH / 8) * SUPPORTED_CHANNELS) / 4;
                    while (true) {
                        byte[] convertedPayload;
                        try {
                            convertedPayload = this.sourceAudioStream.readNBytes(bytesPer250ms);
                            Thread.sleep(0);
                        } catch (InterruptedIOException | InterruptedException e) {
                            continue;
                        } catch (IOException e) {
                            if (e.getMessage().contains("Pipe closed")) {
                                return;
                            }
                            logger.error("Error reading converted audio data", e);
                            continue;
                        }
                        streamGroup.write(convertedPayload);
                    }
                });
            } catch (IOException e) {
                logger.error("Unable to setup audio source stream", e);
                return;
            }
        }
        try {
            this.sourceAudioPipedOutput.write(payload);
        } catch (IOException e) {
            logger.error("Error converting source audio format", e);
        }
    }

    private void onStreamClose() {
        logger.debug("Unregister source audio stream for '{}'", getId());
        synchronized (streamGroup) {
            if (streamGroup.isEmpty()) {
                logger.debug("Send stop listening {}", getId());
                websocket.setListening(false);
                if (this.sourceWriteTask != null) {
                    this.sourceWriteTask.cancel(true);
                    this.sourceWriteTask = null;
                }
                if (this.sourceAudioStream != null) {
                    try {
                        this.sourceAudioStream.close();
                    } catch (IOException ignored) {
                    }
                    this.sourceAudioStream = null;
                }
                if (this.sourceAudioPipedOutput != null) {
                    try {
                        this.sourceAudioPipedOutput.close();
                    } catch (IOException ignored) {
                    }
                    this.sourceAudioPipedOutput = null;
                }
                if (this.sourceAudioPipedInput != null) {
                    try {
                        this.sourceAudioPipedInput.close();
                    } catch (IOException ignored) {
                    }
                    this.sourceAudioPipedInput = null;
                }
                this.streamId = null;
            }
        }
    }
}
