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
package org.openhab.core.audio.transcode;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.utils.AudioWaveUtils;

/**
 * Unit tests for {@link AudioTranscoder}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioTranscoderTest {

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

    private byte[] createWavBytes(int sampleRate, int bitDepth, int channels, int durationMs) throws IOException {
        byte[] pcm = createPcmBytes(sampleRate, bitDepth, channels, durationMs);
        int frameSize = (bitDepth / 8) * channels;
        javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitDepth, channels, frameSize,
                sampleRate, false);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm), jFormat, pcm.length / frameSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
        return baos.toByteArray();
    }

    @Test
    public void testSupportedFormats() {
        Set<AudioFormat> sourceSupported = AudioTranscoder.getSupportedSourceFormats();
        Set<AudioFormat> targetSupported = AudioTranscoder.getSupportedTargetFormats();

        assertTrue(sourceSupported.contains(AudioFormat.WAV));
        assertTrue(sourceSupported.contains(AudioFormat.PCM_SIGNED));
        assertTrue(sourceSupported.contains(AudioFormat.FLAC));

        assertTrue(targetSupported.contains(AudioFormat.WAV));
        assertTrue(targetSupported.contains(AudioFormat.PCM_SIGNED));
        assertTrue(targetSupported.contains(AudioFormat.FLAC));

        assertTrue(AudioTranscoder.isSupported(AudioFormat.WAV));
        assertTrue(AudioTranscoder.isSupported(AudioFormat.PCM_SIGNED));
        assertTrue(AudioTranscoder.isSupported(AudioFormat.FLAC));

        assertTrue(AudioTranscoder.isSourceSupported(AudioFormat.WAV));
        assertTrue(AudioTranscoder.isSourceSupported(AudioFormat.PCM_SIGNED));
        assertTrue(AudioTranscoder.isSourceSupported(AudioFormat.FLAC));

        assertTrue(AudioTranscoder.isTargetSupported(AudioFormat.WAV));
        assertTrue(AudioTranscoder.isTargetSupported(AudioFormat.PCM_SIGNED));
        assertTrue(AudioTranscoder.isTargetSupported(AudioFormat.FLAC));

        assertFalse(AudioTranscoder.isSupported(AudioFormat.MP3));
        assertFalse(AudioTranscoder.isSupported(AudioFormat.OGG));
        assertFalse(AudioTranscoder.isSupported(AudioFormat.AAC));

        assertFalse(AudioTranscoder.isSourceSupported(AudioFormat.MP3));
        assertFalse(AudioTranscoder.isTargetSupported(AudioFormat.MP3));
    }

    @Test
    public void testDirectionalSupportValidation() {
        AudioFormat pcm = AudioFormat.PCM_SIGNED;
        AudioFormat mp3 = AudioFormat.MP3;

        // MP3 cannot be decoded (source) or encoded (target)
        assertFalse(AudioTranscoder.canTranscode(mp3, pcm));
        assertFalse(AudioTranscoder.canTranscode(pcm, mp3));

        // Encoding to MP3 throws
        byte[] pcmData = createPcmBytes(44100, 16, 1, 50);
        ByteArrayAudioStream stream = new ByteArrayAudioStream(pcmData, pcm);
        assertThrows(AudioTranscodingException.class, () -> {
            AudioTranscoder.transcode(stream, mp3);
        });

        // Decoding from MP3 throws
        ByteArrayAudioStream mp3Stream = new ByteArrayAudioStream(new byte[10], mp3);
        assertThrows(AudioTranscodingException.class, () -> {
            AudioTranscoder.transcode(mp3Stream, pcm);
        });
    }

    @Test
    public void testCanTranscode() {
        AudioFormat pcm44100 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        AudioFormat pcm48000 = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                768000, 48000L, 1);

        assertTrue(AudioTranscoder.canTranscode(pcm44100, pcm48000));
        assertTrue(AudioTranscoder.canTranscode(pcm44100, AudioFormat.WAV));
        assertTrue(AudioTranscoder.canTranscode(AudioFormat.WAV, pcm44100));
        assertTrue(AudioTranscoder.canTranscode(pcm44100, AudioFormat.FLAC));
        assertTrue(AudioTranscoder.canTranscode(AudioFormat.FLAC, pcm44100));
        assertTrue(AudioTranscoder.canTranscode(AudioFormat.WAV, AudioFormat.FLAC));

        // Unsupported formats
        assertFalse(AudioTranscoder.canTranscode(AudioFormat.MP3, pcm44100));
        assertFalse(AudioTranscoder.canTranscode(pcm44100, AudioFormat.MP3));

        // Different channel counts
        AudioFormat stereoPcm = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                1411200, 44100L, 2);
        assertFalse(AudioTranscoder.canTranscode(pcm44100, stereoPcm));
    }

    @Test
    public void testPassthroughWhenCompatible() throws AudioTranscodingException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat format = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream stream = new ByteArrayAudioStream(pcmData, format);

        AudioStream result = AudioTranscoder.transcode(stream, format);
        assertSame(stream, result);
    }

    @Test
    public void testResamplePcmFrequency() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                null, 48000L, 1);

        AudioStream resampled = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertNotNull(resampled);

        AudioFormat resultFormat = resampled.getFormat();
        assertEquals(AudioFormat.CONTAINER_NONE, resultFormat.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, resultFormat.getCodec());
        assertEquals(48000L, resultFormat.getFrequency());
        assertEquals(16, resultFormat.getBitDepth());
        assertEquals(1, resultFormat.getChannels());

        byte[] output = resampled.readAllBytes();
        // 100 ms at 16 bit at 48 kHz => 4800 samples, sample size = 2 byte: 4800 * 2 = 9600 (+/- filter, alignment,
        // ...)
        assertTrue(output.length >= 9500 && output.length <= 9700);
        resampled.close();
    }

    @Test
    public void testConvertPcmBitDepth() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 44100L, 1);

        AudioStream converted = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertNotNull(converted);
        assertEquals(24, converted.getFormat().getBitDepth());
        assertEquals(44100L, converted.getFormat().getFrequency());

        byte[] output = converted.readAllBytes();
        // sample size increased from 2 to 3 bytes (16 to 24 bit)
        assertEquals((pcmData.length / 2) * 3, output.length);
        converted.close();
    }

    @Test
    public void testConvertPcmEndianness() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, true, 16,
                null, 44100L, 1);

        AudioStream converted = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertNotNull(converted);
        assertEquals(Boolean.TRUE, converted.getFormat().isBigEndian());

        byte[] output = converted.readAllBytes();
        assertEquals(pcmData.length, output.length);
        // Verify byte swapped on sample where high and low bytes differ
        int sampleOffset = -1;
        for (int i = 0; i < pcmData.length; i += 2) {
            if (pcmData[i] != pcmData[i + 1]) {
                sampleOffset = i;
                break;
            }
        }
        assertTrue(sampleOffset >= 0);
        assertEquals(pcmData[sampleOffset], output[sampleOffset + 1]);
        assertEquals(pcmData[sampleOffset + 1], output[sampleOffset]);
        converted.close();
    }

    @Test
    public void testSimultaneousResamplingAndBitDepth() throws AudioTranscodingException, IOException {
        byte[] pcmData = createPcmBytes(44100, 16, 1, 100);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 24,
                null, 48000L, 1);

        AudioStream converted = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertNotNull(converted);
        assertEquals(24, converted.getFormat().getBitDepth());
        assertEquals(48000L, converted.getFormat().getFrequency());

        byte[] output = converted.readAllBytes();
        // 100 ms at 24 bit at 48 kHz => 4800 samples, sample site = 3 byte: 4800 * 3 = 14400 (+/- filter, alignment,
        // ...)
        assertTrue(output.length >= 14200 && output.length <= 14600);
        converted.close();
    }

    @Test
    public void testWavToPcm() throws AudioTranscodingException, IOException {
        byte[] wavBytes = createWavBytes(44100, 16, 1, 100);
        AudioFormat wavFormat = new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream wavStream = new ByteArrayAudioStream(wavBytes, wavFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                null, 44100L, 1);

        AudioStream pcmStream = AudioTranscoder.transcode(wavStream, targetFormat);
        assertNotNull(pcmStream);
        assertEquals(AudioFormat.CONTAINER_NONE, pcmStream.getFormat().getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, pcmStream.getFormat().getCodec());

        byte[] pcmBytes = pcmStream.readAllBytes();
        assertTrue(pcmBytes.length > 0);
        // WAV RIFF container has 44 bytes header, so PCM payload should be wavBytes.length - 44
        assertEquals(wavBytes.length - 44, pcmBytes.length);
        pcmStream.close();
    }

    @Test
    public void testPcmToWav() throws Exception {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream pcmStream = new ByteArrayAudioStream(pcmBytes, pcmFormat);

        AudioFormat targetFormat = AudioFormat.WAV;

        AudioStream wavStream = AudioTranscoder.transcode(pcmStream, targetFormat);
        assertNotNull(wavStream);
        assertEquals(AudioFormat.CONTAINER_WAVE, wavStream.getFormat().getContainer());

        byte[] wavData = wavStream.readAllBytes();
        assertTrue(wavData.length > pcmBytes.length);
        // Parse header using openHAB utility to confirm valid WAV
        AudioFormat parsed = AudioWaveUtils.parseWavFormat(new ByteArrayInputStream(wavData));
        assertEquals(AudioFormat.CONTAINER_WAVE, parsed.getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, parsed.getCodec());
        assertEquals(44100L, parsed.getFrequency());
        wavStream.close();
    }

    @Test
    public void testTranscodeToSupported() throws Exception {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 50);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream pcmStream = new ByteArrayAudioStream(pcmBytes, pcmFormat);

        Set<AudioFormat> candidates = Set.of(AudioFormat.MP3, AudioFormat.WAV);
        AudioStream result = AudioTranscoder.transcodeToSupported(pcmStream, candidates);
        assertNotNull(result);
        assertEquals(AudioFormat.CONTAINER_WAVE, result.getFormat().getContainer());
        result.close();
    }

    @Test
    public void testTranscodeToSupportedThrowsWhenNoMatch() {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 50);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream pcmStream = new ByteArrayAudioStream(pcmBytes, pcmFormat);

        Set<AudioFormat> candidates = Set.of(AudioFormat.MP3, AudioFormat.AAC);
        assertThrows(AudioTranscodingException.class, () -> {
            AudioTranscoder.transcodeToSupported(pcmStream, candidates);
        });
    }

    @Test
    public void testClosingStreamClosesSource() throws Exception {
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

        AudioStream resampledStream = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertFalse(closed.get());

        resampledStream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testPreservesStreamId() throws Exception {
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
                return "test-stream-id-123";
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

        AudioStream resampledStream = AudioTranscoder.transcode(sourceStream, targetFormat);
        assertEquals("test-stream-id-123", resampledStream.getId());
        resampledStream.close();
    }
}
