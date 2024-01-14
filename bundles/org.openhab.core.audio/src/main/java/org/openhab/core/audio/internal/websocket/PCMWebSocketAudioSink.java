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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.audio.utils.AudioWaveUtils;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an {@link AudioSink} implementation connected to the {@link PCMWebSocketConnection} that allow to
 * transmit concurrent pcm audio lines through the websocket.
 * <p>
 * To identify the different audio lines the data chucks are prefixed by a header added by the
 * {@link PCMWebSocketOutputStream} class.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketAudioSink implements AudioSink {
    /**
     * Byte send to the sink after last chunk to indicate that streaming has ended.
     * Should try to be sent event on and error as the client should be aware that data transmission has ended.
     */
    private static byte terminationByte = (byte) 254;
    private final HashSet<AudioFormat> supportedFormats = new HashSet<>();
    private final HashSet<Class<? extends AudioStream>> supportedStreams = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(PCMWebSocketAudioSink.class);

    private final String sinkId;
    private final String sinkLabel;
    private final PCMWebSocketConnection websocket;
    private final int channelNumber;
    private final long clientSampleRate;
    private PercentType sinkVolume = new PercentType(100);

    public PCMWebSocketAudioSink(String id, String label, PCMWebSocketConnection websocket, int channelNumber,
            long clientSampleRate) {
        this.sinkId = id;
        this.sinkLabel = label;
        this.websocket = websocket;
        this.channelNumber = channelNumber;
        this.clientSampleRate = clientSampleRate;
        supportedStreams.add(FixedLengthAudioStream.class);
        supportedStreams.add(PipedAudioStream.class);
        for (var container : List.of(AudioFormat.CONTAINER_NONE, AudioFormat.CONTAINER_WAVE)) {
            for (var sampleRate : List.of(8000L, 16000L, 32000L, 44100L, 48000L)) {
                for (var bitDepth : List.of(16, 32)) {
                    for (var channels : List.of(1, 2)) {
                        supportedFormats.add(new AudioFormat( //
                                container, //
                                AudioFormat.CODEC_PCM_SIGNED, //
                                false, //
                                bitDepth, //
                                Math.toIntExact(sampleRate * bitDepth * channelNumber), //
                                sampleRate, //
                                channels //
                        ));
                    }
                }
            }
        }
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
        var format = audioStream.getFormat();
        InputStream convertedAudioStream = null;
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
            if (audioStream instanceof SizeableAudioStream sizeableAudioStream) {
                long length = sizeableAudioStream.length();
                var audioFormat = audioStream.getFormat();
                long byteRate = (Objects.requireNonNull(audioFormat.getBitDepth()) / 8)
                        * Objects.requireNonNull(audioFormat.getFrequency())
                        * Objects.requireNonNull(audioFormat.getChannels());
                float durationInSeconds = (float) length / byteRate;
                duration = Math.round(durationInSeconds * 1000);
                logger.debug("Duration of input stream : {}", duration);
            }
            if (audioStream instanceof PipedAudioStream && isDirectStreamSupported(format)) {
                // TODO: review
                outputStream = new PCMWebSocketOutputStream(websocket, format);
                convertedAudioStream = getPCMStreamNormalized(audioStream, audioStream.getFormat());
                transferAudio(convertedAudioStream, outputStream, -1);
            } else {
                outputStream = new PCMWebSocketOutputStream(websocket, format);
                convertedAudioStream = getPCMStreamNormalized(audioStream, audioStream.getFormat());
                transferAudio(convertedAudioStream, outputStream, duration);
            }
        } catch (InterruptedIOException ignored) {
        } catch (IOException e) {
            logger.warn("IOException: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("InterruptedException: {}", e.getMessage());
        } finally {
            if (convertedAudioStream != null) {
                try {
                    convertedAudioStream.close();
                } catch (IOException e) {
                    logger.warn("IOException: {}", e.getMessage(), e);
                }
            }
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

    private boolean isDirectStreamSupported(AudioFormat format) {
        var bigEndian = format.isBigEndian();
        var bitDepth = format.getBitDepth();
        return AudioFormat.PCM_SIGNED.isCompatible(format) && //
                bitDepth != null && bitDepth == 16 && //
                bigEndian != null && !bigEndian;
    }

    private InputStream getPCMStreamNormalized(InputStream pcmInputStream, AudioFormat format) {
        if (format.getChannels() != channelNumber || format.getBitDepth() != 16
                || format.getFrequency() != clientSampleRate) {
            logger.debug("Sound is not in the target format. Trying to re-encode it");
            javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat( //
                    (float) format.getFrequency(), //
                    format.getBitDepth(), //
                    format.getChannels(), //
                    true, //
                    false //
            );
            javax.sound.sampled.AudioFormat fixedJFormat = new javax.sound.sampled.AudioFormat( //
                    (float) clientSampleRate, //
                    16, //
                    channelNumber, //
                    true, //
                    false //
            );
            return AudioSystem.getAudioInputStream(fixedJFormat, new AudioInputStream(pcmInputStream, jFormat, -1));
        } else {
            return pcmInputStream;
        }
    }

    private void transferAudio(InputStream inputStream, OutputStream outputStream, long duration)
            throws IOException, InterruptedException {
        Instant start = Instant.now();
        try {
            inputStream.transferTo(outputStream);
        } finally {
            try {
                // send a byte indicating this stream has ended, so it can be tear down on the client
                outputStream.write(new byte[] { terminationByte }, 0, 1);
            } catch (IOException e) {
                logger.warn("Unable to send termination byte to sink {}", sinkId);
            }
        }
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
        return supportedFormats;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return supportedStreams;
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
     * will prefix each chunk with a header composed of 6 bytes.
     * Header: 3 bytes (stream id) + 1 byte (stream sample rate) + 1 byte (stream bit depth) + 1 byte (channels).
     */
    protected static class PCMWebSocketOutputStream extends OutputStream {
        private final byte[] header;
        private final PCMWebSocketConnection websocket;
        private boolean closed = false;

        public PCMWebSocketOutputStream(PCMWebSocketConnection websocket, AudioFormat audioFormat) {
            this.websocket = websocket;
            this.header = generateId(audioFormat);
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

        private static byte[] generateId(AudioFormat audioFormat) {
            SecureRandom sr = new SecureRandom();
            byte[] rndBytes = new byte[6];
            sr.nextBytes(rndBytes);
            rndBytes[4] = StreamSampleRate.fromValue(Objects.requireNonNull(audioFormat.getFrequency()).intValue())
                    .get();
            rndBytes[5] = StreamBitDepth.fromValue(Objects.requireNonNull(audioFormat.getBitDepth())).get();
            rndBytes[6] = StreamChannels.fromValue(Objects.requireNonNull(audioFormat.getChannels())).get();
            return rndBytes;
        }
    }

    /**
     * Byte sent to indicate the sample rate
     */
    private enum StreamSampleRate {
        // 16bit int 1 channel little-endian
        S8000((byte) 1),
        // 16bit int 2 channel little-endian
        S16000((byte) 2),
        // 16bit int 2 channel little-endian
        S32000((byte) 3),
        // 16bit int 2 channel little-endian
        S44100((byte) 4),
        // 16bit int 2 channel little-endian
        S48000((byte) 5);

        private final byte b;

        StreamSampleRate(byte b) {
            this.b = b;
        }

        public byte get() {
            return this.b;
        }

        public static StreamSampleRate fromValue(int sampleRate) {
            return switch (sampleRate) {
                case 8000 -> StreamSampleRate.S8000;
                case 16000 -> StreamSampleRate.S16000;
                case 32000 -> StreamSampleRate.S32000;
                case 44100 -> StreamSampleRate.S44100;
                case 48000 -> StreamSampleRate.S48000;
                default -> throw new IllegalArgumentException("Invalid sample rate");
            };
        }
    }

    /**
     * Byte sent to indicate the stream bit depth
     */
    private enum StreamBitDepth {
        // 16bit audio
        B16((byte) 1),
        // 32bit audio
        B32((byte) 2);

        private final byte b;

        StreamBitDepth(byte b) {
            this.b = b;
        }

        public byte get() {
            return this.b;
        }

        public static StreamBitDepth fromValue(int bitDepth) {
            return switch (bitDepth) {
                case 16 -> StreamBitDepth.B16;
                case 32 -> StreamBitDepth.B32;
                default -> throw new IllegalArgumentException("Invalid bit depth");
            };
        }
    }

    /**
     * Byte sent to indicate the stream channels
     */
    private enum StreamChannels {
        // 16bit int 2 channel little-endian
        C1((byte) 1),
        C2((byte) 2);

        private final byte b;

        StreamChannels(byte b) {
            this.b = b;
        }

        public byte get() {
            return this.b;
        }

        public static StreamChannels fromValue(int channels) {
            return switch (channels) {
                case 1 -> StreamChannels.C1;
                case 2 -> StreamChannels.C2;
                default -> throw new IllegalArgumentException("Invalid channels");
            };
        }
    }
}
