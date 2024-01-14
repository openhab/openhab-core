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
package org.openhab.core.audio.internal.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an {@link AudioSource} implementation connected to the {@link PCMWebSocketConnection} that allow to
 * a single pcm audio line through the websocket which is shared across the active {@link AudioStream} instances.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketAudioSource implements AudioSource {
    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAudioSource.class);
    public static int supportedBitDepth = 16;
    public static int supportedSampleRate = 16000;
    public static int supportedChannels = 1;
    public static AudioFormat SUPPORTED_FORMAT = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, false, supportedBitDepth, null, (long) supportedSampleRate,
            supportedChannels);
    private final String sourceId;
    private final String sourceLabel;
    private final PCMWebSocketConnection websocket;
    private @Nullable PipedOutputStream sourceAudioPipedOutput;
    private @Nullable PipedInputStream sourceAudioPipedInput;
    private @Nullable InputStream sourceAudioStream;
    private final PipedAudioStream.Group streamGroup = PipedAudioStream.newGroup(SUPPORTED_FORMAT);
    private final int streamSampleRate;

    public PCMWebSocketAudioSource(String id, String label, int streamSampleRate, PCMWebSocketConnection websocket) {
        this.sourceId = id;
        this.sourceLabel = label;
        this.websocket = websocket;
        this.streamSampleRate = streamSampleRate;
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
        return Set.of(SUPPORTED_FORMAT);
    }

    @Override
    public AudioStream getInputStream(AudioFormat audioFormat) throws AudioException {
        try {
            final PipedAudioStream stream = streamGroup.getAudioStreamInGroup();
            stream.onClose(this::onStreamClose);
            onStreamCreated();
            return stream;
        } catch (IOException e) {
            throw new AudioException(e);
        }
    }

    public void close() throws Exception {
        streamGroup.close();
    }

    public void writeToStreams(byte[] payload) {
        if (this.sourceAudioStream == null || this.sourceAudioPipedOutput == null) {
            logger.debug("Source already disposed ignoring data");
            return;
        }
        byte[] convertedPayload;
        try {
            this.sourceAudioPipedOutput.write(payload);
            int resampledLength = (payload.length) / (streamSampleRate / supportedSampleRate);
            logger.trace("resampling payload size {} => {}", payload.length, resampledLength);
            convertedPayload = this.sourceAudioStream.readNBytes(resampledLength);
        } catch (IOException e) {
            logger.error("Error writing source audio", e);
            return;
        }
        streamGroup.write(convertedPayload);
    }

    private void onStreamCreated() {
        logger.debug("Registering source stream for '{}'", getId());
        synchronized (streamGroup) {
            if (this.streamGroup.isEmpty()) {
                try {
                    var pipedOutput = new PipedOutputStream();
                    this.sourceAudioPipedOutput = pipedOutput;
                    var pipedInput = new PipedInputStream(pipedOutput, 4096 * 4);
                    var sourceAudioPipedInput = this.sourceAudioPipedInput = pipedInput;
                    if (streamSampleRate != PCMWebSocketAudioSource.supportedSampleRate) {
                        logger.debug("Enabling audio resampling for the audio source stream: {} => {}",
                                streamSampleRate, PCMWebSocketAudioSource.supportedSampleRate);
                        this.sourceAudioStream = getPCMStreamNormalized(sourceAudioPipedInput);
                    } else {
                        logger.debug("Audio source stream sample rate {}, no resampling needed", supportedSampleRate);
                        this.sourceAudioStream = pipedInput;
                    }
                } catch (IOException e) {
                    logger.error("Unable to setup audio source stream", e);
                }
                logger.debug("Send start listening {}", getId());
                websocket.setListening(true);
            }
        }
    }

    /**
     * Ensure right PCM format by converting if needed (sample rate, channel)
     *
     * @param stream PCM input stream
     * @return A PCM normalized stream at the desired format
     */
    private AudioInputStream getPCMStreamNormalized(InputStream stream) {
        javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat( //
                (float) streamSampleRate, //
                supportedBitDepth, //
                supportedChannels, //
                true, //
                false //
        );
        javax.sound.sampled.AudioFormat fixedJFormat = new javax.sound.sampled.AudioFormat( //
                (float) supportedSampleRate, //
                supportedBitDepth, //
                supportedChannels, //
                true, //
                false //
        );
        logger.debug("Sound is not in the target format. Trying to re-encode it");
        return AudioSystem.getAudioInputStream(fixedJFormat, new AudioInputStream(stream, jFormat, -1));
    }

    private void onStreamClose() {
        logger.debug("Unregister source audio stream for '{}'", getId());
        synchronized (streamGroup) {
            if (streamGroup.isEmpty()) {
                logger.debug("Send stop listening {}", getId());
                websocket.setListening(false);
                logger.debug("Disposing audio source internal resources for '{}'", getId());
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
            }
        }
    }
}
