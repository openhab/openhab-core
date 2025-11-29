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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.*;
import org.openhab.core.audio.utils.AudioWaveUtils;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an {@link AudioSink} implementation connected to the {@link PCMWebSocketConnection} that allows to
 * transmit concurrent PCM audio lines through WebSocket.
 * <p>
 * To identify the different audio lines the data chucks are prefixed by a header added by the
 * {@link PCMWebSocketOutputStream} class.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketAudioSink implements AudioSink {
    /**
     * Byte sent after the last chunk for a stream to indicate the stream has ended, so the client can dispose resources
     * associated with the stream.
     * It's sent on a finally clause that covers the audio transmission execution so it gets sent even if some
     * exception interrupts the audio transmission.
     */
    private static final byte STREAM_TERMINATION_BYTE = (byte) 254;
    private static final Set<AudioFormat> SUPPORTED_FORMATS = Set.of(AudioFormat.WAV, AudioFormat.PCM_SIGNED);
    private static final Set<Class<? extends AudioStream>> SUPPORTED_STREAMS = Set.of(FixedLengthAudioStream.class,
            PipedAudioStream.class);

    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAudioSink.class);

    private final String sinkId;
    private final String sinkLabel;
    private final PCMWebSocketConnection websocket;
    private PercentType sinkVolume = new PercentType(100);
    @Nullable
    private Integer forceSampleRate;
    @Nullable
    private Integer forceBitDepth;
    @Nullable
    private Integer forceChannels;

    public PCMWebSocketAudioSink(String id, String label, PCMWebSocketConnection websocket,
            @Nullable Integer forceSampleRate, @Nullable Integer forceBitDepth, @Nullable Integer forceChannels) {
        this.sinkId = id;
        this.sinkLabel = label;
        this.websocket = websocket;
        this.forceSampleRate = forceSampleRate;
        this.forceBitDepth = forceBitDepth;
        this.forceChannels = forceChannels;
    }

    @Override
    public String getId() {
        return this.sinkId;
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return this.sinkLabel;
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        if (audioStream == null) {
            return;
        }
        OutputStream outputStream = null;
        try {
            long duration = -1;
            if (AudioFormat.CONTAINER_WAVE.equals(audioStream.getFormat().getContainer())) {
                logger.debug("Removing wav container from data");
                try {
                    AudioWaveUtils.removeFMT(audioStream);
                } catch (IOException e) {
                    logger.warn("IOException trying to remove wav header: {}", e.getMessage());
                }
            }
            var audioFormat = audioStream.getFormat();
            if (audioStream instanceof SizeableAudioStream sizeableAudioStream) {
                long byteLength = sizeableAudioStream.length();
                long bytesPerSecond = (Objects.requireNonNull(audioFormat.getBitDepth()) / 8)
                        * Objects.requireNonNull(audioFormat.getFrequency())
                        * Objects.requireNonNull(audioFormat.getChannels());
                float durationInSeconds = (float) byteLength / bytesPerSecond;
                duration = Math.round(durationInSeconds * 1000);
                logger.debug("Duration of input stream : {}", duration);
            }
            AtomicBoolean transferenceAborted = new AtomicBoolean(false);
            if (audioStream instanceof PipedAudioStream pipedAudioStream) {
                pipedAudioStream.onClose(() -> transferenceAborted.set(true));
            }
            int sampleRate = Objects.requireNonNull(audioFormat.getFrequency()).intValue();
            int bitDepth = Objects.requireNonNull(audioFormat.getBitDepth());
            int channels = Objects.requireNonNull(audioFormat.getChannels());
            int targetSampleRate = Objects.requireNonNullElse(forceSampleRate, sampleRate);
            Integer targetBitDepth = Objects.requireNonNullElse(forceBitDepth, bitDepth);
            Integer targetChannels = Objects.requireNonNullElse(forceChannels, channels);
            outputStream = new PCMWebSocketOutputStream(websocket, targetSampleRate, targetBitDepth.byteValue(),
                    targetChannels.byteValue());
            InputStream finalAudioStream;
            if ( //
            (forceSampleRate != null && !forceSampleRate.equals(sampleRate)) || //
                    (forceBitDepth != null && !forceBitDepth.equals(bitDepth)) || //
                    (forceChannels != null && !forceChannels.equals(channels)) //
            ) {
                logger.debug("Sound is not in the target format. Trying to re-encode it");
                finalAudioStream = PCMWebSocketAudioUtil.getPCMStreamNormalized(audioStream, sampleRate, bitDepth,
                        channels, targetSampleRate, targetBitDepth, targetChannels);
            } else {
                finalAudioStream = audioStream;
            }
            int bytesPer500ms = (targetSampleRate * (targetBitDepth / 8) * targetChannels) / 2;
            transferAudio(finalAudioStream, outputStream, bytesPer500ms, duration, transferenceAborted);
        } catch (InterruptedIOException ignored) {
        } catch (IOException e) {
            logger.warn("IOException: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("InterruptedException: {}", e.getMessage());
        } finally {
            try {
                audioStream.close();
            } catch (IOException e) {
                logger.warn("IOException: {}", e.getMessage(), e);
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                logger.warn("IOException: {}", e.getMessage(), e);
            }
        }
    }

    private void transferAudio(InputStream inputStream, OutputStream outputStream, int chunkSize, long duration,
            AtomicBoolean aborted) throws IOException, InterruptedException {
        Instant start = Instant.now();
        long transferred = 0;
        try {
            byte[] buffer = new byte[chunkSize];
            int read;
            while (!aborted.get() && (read = inputStream.read(buffer, 0, chunkSize)) >= 0) {
                outputStream.write(buffer, 0, read);
                transferred += read;
            }
        } finally {
            try {
                // send a byte indicating this stream has ended, so it can be tear down on the client
                outputStream.write(new byte[] { STREAM_TERMINATION_BYTE }, 0, 1);
            } catch (IOException e) {
                logger.warn("Unable to send termination byte to sink {}", sinkId);
            }
        }
        logger.debug("Sent {} bytes of audio", transferred);
        if (duration != -1) {
            Instant end = Instant.now();
            long millisSecondTimedToSendAudioData = Duration.between(start, end).toMillis();
            if (millisSecondTimedToSendAudioData < duration) {
                long timeToSleep = duration - millisSecondTimedToSendAudioData;
                logger.debug("Sleep time to let the system play sound : {}ms", timeToSleep);
                Thread.sleep(timeToSleep);
            }
        }
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_STREAMS;
    }

    @Override
    public PercentType getVolume() throws IOException {
        return this.sinkVolume;
    }

    @Override
    public void setVolume(PercentType percentType) throws IOException {
        this.sinkVolume = percentType;
        websocket.setSinkVolume(percentType.intValue());
    }

    /**
     * This is an {@link OutputStream} implementation for writing binary data to the websocket that
     * will prefix each chunk with a header composed of 8 bytes.
     * Header: 2 bytes (stream id) + 4 byte (stream sample rate) + 1 byte (stream bit depth) + 1 byte (channels).
     */
    protected static class PCMWebSocketOutputStream extends OutputStream {
        private final byte[] header;
        private final PCMWebSocketConnection websocket;
        private boolean closed = false;

        public PCMWebSocketOutputStream(PCMWebSocketConnection websocket, int sampleRate, byte bitDepth,
                byte channels) {
            this.websocket = websocket;
            this.header = PCMWebSocketStreamIdUtil.generateAudioPacketHeader(sampleRate, bitDepth, channels).array();
        }

        @Override
        public void write(int b) throws IOException {
            write(ByteBuffer.allocate(4).putInt(b).array());
        }

        @Override
        public void write(byte @Nullable [] b) throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
            if (b != null) {
                websocket.sendAudio(header, b);
            }
        }

        @Override
        public void write(byte @Nullable [] b, int off, int len) throws IOException {
            if (b != null) {
                write(Arrays.copyOfRange(b, off, off + len));
            }
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
