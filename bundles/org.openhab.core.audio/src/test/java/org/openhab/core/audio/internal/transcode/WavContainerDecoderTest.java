/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.audio.internal.transcode;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.transcode.AudioTranscodingException;

/**
 * Unit tests for {@link WavContainerDecoder}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class WavContainerDecoderTest {

    private @NonNullByDefault({}) WavContainerDecoder transcoder;

    @BeforeEach
    public void setup() {
        transcoder = new WavContainerDecoder();
    }

    private byte[] createTestWavBytes(int sampleRate, int bitDepth, int channels, int durationMs) throws IOException {
        int frameSize = (bitDepth / 8) * channels;
        int numFrames = (sampleRate * durationMs) / 1000;
        byte[] pcmData = new byte[numFrames * frameSize];
        for (int i = 0; i < pcmData.length; i++) {
            pcmData[i] = (byte) (i % 128);
        }
        javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitDepth, channels, frameSize,
                sampleRate, false);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcmData), jFormat, numFrames);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    private byte[] createTestWavBytes() throws IOException {
        return createTestWavBytes(44100, 16, 1, 100);
    }

    @Test
    public void testCanTranscode() {
        assertTrue(transcoder.canTranscode(AudioFormat.WAV, AudioFormat.PCM_SIGNED));

        AudioFormat concreteWav = new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        AudioFormat concretePcm = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        assertTrue(transcoder.canTranscode(concreteWav, concretePcm));

        AudioFormat diffFreqPcm = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);
        assertFalse(transcoder.canTranscode(concreteWav, diffFreqPcm));

        assertFalse(transcoder.canTranscode(AudioFormat.MP3, AudioFormat.PCM_SIGNED));
        assertFalse(transcoder.canTranscode(AudioFormat.WAV, AudioFormat.FLAC));
        assertFalse(transcoder.canTranscode(AudioFormat.WAV, AudioFormat.MP3));
    }

    @Test
    public void unpackWavToPcm() throws Exception {
        AudioFormat concreteWav = new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        byte[] wavBytes = createTestWavBytes(44100, 16, 1, 100);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(wavBytes, concreteWav);

        AudioStream pcmStream = transcoder.transcode(sourceStream, AudioFormat.PCM_SIGNED);
        assertNotNull(pcmStream);

        AudioFormat format = pcmStream.getFormat();
        assertEquals(AudioFormat.CONTAINER_NONE, format.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, format.getCodec());
        assertEquals(Boolean.FALSE, format.isBigEndian());
        assertEquals(16, format.getBitDepth());
        assertEquals(44100L, format.getFrequency());
        assertEquals(1, format.getChannels());

        byte[] pcmData = pcmStream.readAllBytes();
        assertTrue(pcmData.length > 0);
        assertTrue(pcmData.length < wavBytes.length); // stripped WAV header

        pcmStream.close();
    }

    @Test
    public void unpackInvalidStreamThrowsException() {
        byte[] invalidBytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(invalidBytes, AudioFormat.WAV);

        assertThrows(AudioTranscodingException.class, () -> {
            transcoder.transcode(sourceStream, AudioFormat.PCM_SIGNED);
        });
    }

    @Test
    public void closesUnderlyingSourceOnClose() throws Exception {
        byte[] wavBytes = createTestWavBytes();
        AtomicBoolean sourceClosed = new AtomicBoolean(false);

        AudioStream sourceStream = new AudioStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(wavBytes);

            @Override
            public AudioFormat getFormat() {
                return AudioFormat.WAV;
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte @Nullable [] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                sourceClosed.set(true);
                in.close();
            }
        };

        AudioStream pcmStream = transcoder.transcode(sourceStream, AudioFormat.PCM_SIGNED);
        assertFalse(sourceClosed.get());

        pcmStream.close();
        assertTrue(sourceClosed.get());
    }

    @Test
    public void preservesStreamId() throws Exception {
        byte[] wavBytes = createTestWavBytes();
        AudioStream sourceStream = new AudioStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(wavBytes);

            @Override
            public AudioFormat getFormat() {
                return AudioFormat.WAV;
            }

            @Override
            public @Nullable String getId() {
                return "test-stream-123";
            }

            @Override
            public int read() {
                return in.read();
            }

            @Override
            public int read(byte @Nullable [] b, int off, int len) {
                return in.read(b, off, len);
            }
        };

        AudioStream pcmStream = transcoder.transcode(sourceStream, AudioFormat.PCM_SIGNED);
        assertEquals("test-stream-123", pcmStream.getId());
        pcmStream.close();
    }

    @Test
    public void markAndResetSupportedOnUnpackedPcmStream() throws Exception {
        byte[] wavBytes = createTestWavBytes();
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(wavBytes, AudioFormat.WAV);

        AudioStream pcmStream = transcoder.transcode(sourceStream, AudioFormat.PCM_SIGNED);
        assertTrue(pcmStream.markSupported());

        byte[] frame1 = new byte[2];
        byte[] frame2 = new byte[2];
        byte[] frameReset = new byte[2];

        assertEquals(2, pcmStream.read(frame1));
        pcmStream.mark(100);
        assertEquals(2, pcmStream.read(frame2));
        pcmStream.reset();
        assertEquals(2, pcmStream.read(frameReset));
        assertArrayEquals(frame2, frameReset);

        pcmStream.close();
    }
}
