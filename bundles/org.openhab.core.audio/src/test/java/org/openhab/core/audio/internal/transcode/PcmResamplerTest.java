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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.transcode.AudioTranscodingException;

/**
 * Unit tests for {@link PcmResampler}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class PcmResamplerTest {

    private @NonNullByDefault({}) PcmResampler transcoder;

    @BeforeEach
    public void setup() {
        transcoder = new PcmResampler();
    }

    private byte[] createPcmBytes(int sampleRate, int bitDepth, int channels, int durationMs) {
        int totalSamples = (sampleRate * durationMs) / 1000;
        int bytesPerSample = bitDepth / 8;
        int frameSize = bytesPerSample * channels;
        byte[] pcmData = new byte[totalSamples * frameSize];

        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / sampleRate;
            int sampleValue = (int) (Math.sin(angle) * ((1L << (bitDepth - 1)) - 1));
            for (int ch = 0; ch < channels; ch++) {
                int offset = i * frameSize + ch * bytesPerSample;
                for (int b = 0; b < bytesPerSample; b++) {
                    pcmData[offset + b] = (byte) ((sampleValue >> (b * 8)) & 0xFF);
                }
            }
        }

        return pcmData;
    }

    @Test
    public void testCanTranscode() {
        AudioFormat pcm44100 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        AudioFormat pcm48000 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);

        // Different frequencies -> can transcode
        assertTrue(transcoder.canTranscode(pcm44100, pcm48000));
        assertTrue(transcoder.canTranscode(pcm48000, pcm44100));

        // Same frequency -> no resampling needed
        assertFalse(transcoder.canTranscode(pcm44100, pcm44100));

        // Generic target without frequency -> cannot resample
        assertFalse(transcoder.canTranscode(pcm44100, AudioFormat.PCM_SIGNED));

        // Generic source without frequency -> can transcode to a target with frequency
        assertTrue(transcoder.canTranscode(AudioFormat.PCM_SIGNED, pcm48000));

        // Different channel counts -> unsupported
        AudioFormat stereoPcm48000 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                16, 1536000, 48000L, 2);
        assertFalse(transcoder.canTranscode(pcm44100, stereoPcm48000));

        // Different bit depth -> can transcode
        AudioFormat pcm24bit = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 44100L, 1);
        assertTrue(transcoder.canTranscode(pcm44100, pcm24bit));
        assertTrue(transcoder.canTranscode(pcm24bit, pcm44100));

        // Different bit depth and frequency -> can transcode
        AudioFormat pcm24bit48k = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 48000L, 1);
        assertTrue(transcoder.canTranscode(pcm44100, pcm24bit48k));

        // Unsupported bit depth
        AudioFormat pcm12bit = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 12,
                null, 44100L, 1);
        assertFalse(transcoder.canTranscode(pcm44100, pcm12bit));

        // Incompatible container/codec
        assertFalse(transcoder.canTranscode(AudioFormat.WAV, pcm48000));
        assertFalse(transcoder.canTranscode(pcm44100, AudioFormat.FLAC));
    }

    @Test
    public void validatesMetadata() {
        // TODO
    }

    @Test
    public void upsample44100To48000() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);

        AudioStream resampledStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(resampledStream);

        AudioFormat resultFormat = resampledStream.getFormat();
        assertEquals(AudioFormat.CONTAINER_NONE, resultFormat.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, resultFormat.getCodec());
        assertEquals(48000L, resultFormat.getFrequency());
        assertEquals(16, resultFormat.getBitDepth());
        assertEquals(1, resultFormat.getChannels());

        byte[] outputBytes = resampledStream.readAllBytes();
        assertTrue(outputBytes.length > 0);
        // At 48 kHz, 100ms has 4800 samples = 9600 bytes (+/- filter padding)
        assertTrue(outputBytes.length >= 9500 && outputBytes.length <= 9700);

        resampledStream.close();
    }

    @Test
    public void downsample48000To16000() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(48000, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                256000, 16000L, 1);

        AudioStream resampledStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(resampledStream);

        assertEquals(16000L, resampledStream.getFormat().getFrequency());

        byte[] outputBytes = resampledStream.readAllBytes();
        assertTrue(outputBytes.length > 0);
        // At 16 kHz, 100ms has 1600 samples = 3200 bytes (+/- filter padding)
        assertTrue(outputBytes.length >= 3100 && outputBytes.length <= 3300);

        resampledStream.close();
    }

    @Test
    public void upsampleStereo() throws Exception {
        byte[] pcmData = createPcmBytes(44100, 16, 2, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                1411200, 44100L, 2);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                1536000, 48000L, 2);

        AudioStream resampledStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(resampledStream);

        assertEquals(48000L, resampledStream.getFormat().getFrequency());
        assertEquals(2, resampledStream.getFormat().getChannels());

        byte[] outputBytes = resampledStream.readAllBytes();
        assertTrue(outputBytes.length > 0);
        // At 48 kHz stereo, 100ms has 4800 frames * 4 bytes = 19200 bytes (+/- filter padding)
        assertTrue(outputBytes.length >= 19000 && outputBytes.length <= 19400);

        resampledStream.close();
    }

    @Test
    public void passThroughIfSameFrequency() throws Exception {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioStream resultStream = transcoder.transcode(sourceStream, sourceFormat);
        assertSame(sourceStream, resultStream);

        sourceStream.close();
    }

    @Test
    public void closesSourceOnClose() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 50);
        AtomicBoolean closed = new AtomicBoolean(false);

        AudioStream sourceStream = new AudioStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(pcmData);

            @Override
            public AudioFormat getFormat() {
                return new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16, 705600,
                        44100L, 1);
            }

            @Override
            public int read() {
                return in.read();
            }

            @Override
            public int read(byte @Nullable [] b, int off, int len) {
                return in.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                closed.set(true);
                in.close();
            }
        };

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);

        AudioStream resampledStream = transcoder.transcode(sourceStream, targetFormat);
        assertFalse(closed.get());

        resampledStream.close();
        assertTrue(closed.get());
    }

    @Test
    public void preservesStreamId() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 50);
        AudioStream sourceStream = new AudioStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(pcmData);

            @Override
            public AudioFormat getFormat() {
                return new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16, 705600,
                        44100L, 1);
            }

            @Override
            public @Nullable String getId() {
                return "resample-test-stream-id";
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

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);

        AudioStream resampledStream = transcoder.transcode(sourceStream, targetFormat);
        assertEquals("resample-test-stream-id", resampledStream.getId());
        resampledStream.close();
    }

    @Test
    public void transcodeBitDepth16To24() throws Exception {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 44100L, 1);

        AudioStream convertedStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(convertedStream);

        AudioFormat resultFormat = convertedStream.getFormat();
        assertEquals(AudioFormat.CONTAINER_NONE, resultFormat.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, resultFormat.getCodec());
        assertEquals(44100L, resultFormat.getFrequency());
        assertEquals(24, resultFormat.getBitDepth());
        assertEquals(1, resultFormat.getChannels());

        byte[] outputBytes = convertedStream.readAllBytes();
        assertEquals((pcmData.length / 2) * 3, outputBytes.length); // 2 bytes/sample -> 3 bytes/sample

        convertedStream.close();
    }

    @Test
    public void transcodeBitDepth24To16() throws Exception {
        byte[] pcmData = createPcmBytes(44100, 24, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);

        AudioStream convertedStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(convertedStream);
        assertEquals(16, convertedStream.getFormat().getBitDepth());
        assertEquals(44100L, convertedStream.getFormat().getFrequency());

        byte[] outputBytes = convertedStream.readAllBytes();
        assertEquals((pcmData.length / 3) * 2, outputBytes.length); // 3 bytes/sample -> 2 bytes/sample

        convertedStream.close();
    }

    @Test
    public void transcodeBitDepthAndSampleRateSimultaneously() throws Exception {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 48000L, 1);

        AudioStream convertedStream = transcoder.transcode(sourceStream, targetFormat);
        assertNotNull(convertedStream);

        assertEquals(24, convertedStream.getFormat().getBitDepth());
        assertEquals(48000L, convertedStream.getFormat().getFrequency());

        byte[] outputBytes = convertedStream.readAllBytes();
        assertTrue(outputBytes.length > 0);
        // At 48 kHz 24-bit mono, 100ms has 4800 samples * 3 bytes = 14400 bytes (+/- filter padding)
        assertTrue(outputBytes.length >= 14200 && outputBytes.length <= 14600);

        convertedStream.close();
    }
}
