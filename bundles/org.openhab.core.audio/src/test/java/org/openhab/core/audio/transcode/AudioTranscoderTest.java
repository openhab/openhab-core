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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
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

    private byte[] createFlacBytes(int sampleRate, int bitDepth, int channels, int durationMs)
            throws AudioTranscodingException, IOException {
        byte[] pcm = createPcmBytes(sampleRate, bitDepth, channels, durationMs);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                bitDepth, sampleRate * (bitDepth / 8) * channels * 8, (long) sampleRate, channels);
        ByteArrayAudioStream stream = new ByteArrayAudioStream(pcm, pcmFormat);
        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_FLAC, AudioFormat.CODEC_FLAC, null, bitDepth,
                null, (long) sampleRate, channels);
        AudioStream flacStream = AudioTranscoder.transcode(stream, targetFormat, true);
        byte[] flacBytes = flacStream.readAllBytes();
        flacStream.close();
        return flacBytes;
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

    static Stream<Arguments> sampleRatePairs() {
        int[] rates = { 8000, 11025, 16000, 22050, 44100, 48000 };
        List<Arguments> pairs = new ArrayList<>();
        for (int sourceRate : rates) {
            for (int targetRate : rates) {
                if (sourceRate != targetRate) {
                    pairs.add(Arguments.of(sourceRate, targetRate));
                }
            }
        }
        return pairs.stream();
    }

    @ParameterizedTest(name = "from {0} Hz to {1} Hz")
    @MethodSource("sampleRatePairs")
    void testResamplePcmFrequency(int sourceRate, int targetRate) throws AudioTranscodingException, IOException {
        byte[] pcmBytes = createPcmBytes(sourceRate, 16, 1, 50);
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                sourceRate * 16, (long) sourceRate, 1);
        ByteArrayAudioStream srcStream = new ByteArrayAudioStream(pcmBytes, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                targetRate * 16, (long) targetRate, 1);

        try (AudioStream resampled = AudioTranscoder.transcode(srcStream, targetFormat)) {
            assertNotNull(resampled);

            AudioFormat resultFormat = resampled.getFormat();
            assertEquals(AudioFormat.CONTAINER_NONE, resultFormat.getContainer());
            assertEquals(AudioFormat.CODEC_PCM_SIGNED, resultFormat.getCodec());
            assertEquals(targetRate, resultFormat.getFrequency());
            assertEquals(16, resultFormat.getBitDepth());
            assertEquals(1, resultFormat.getChannels());

            byte[] output = resampled.readAllBytes();
            assertTrue(output.length > 0);

            double expectedSamples = 50.0 * targetRate / 1000.0;
            int expectedBytes = (int) Math.round(expectedSamples * 2);
            assertTrue(output.length >= expectedBytes * 0.7 && output.length <= expectedBytes * 1.3,
                    "Resampling from " + sourceRate + " to " + targetRate + " gave unexpected length: " + output.length
                            + " vs expected " + expectedBytes);
        }
    }

    static Stream<Arguments> bitDepthPairs() {
        int[] bitDepths = { 8, 16, 24, 32 };
        List<Arguments> pairs = new ArrayList<>();
        for (int sourceDepth : bitDepths) {
            for (int targetDepth : bitDepths) {
                if (sourceDepth != targetDepth) {
                    pairs.add(Arguments.of(sourceDepth, targetDepth));
                }
            }
        }
        return pairs.stream();
    }

    @ParameterizedTest(name = "from {0}-bit to {1}-bit")
    @MethodSource("bitDepthPairs")
    void testConvertPcmBitDepth(int sourceDepth, int targetDepth) throws AudioTranscodingException, IOException {
        int sampleRate = 44100;
        int durationMs = 100;
        int channels = 1;

        byte[] pcmData = createPcmBytes(sampleRate, sourceDepth, channels, durationMs);
        int bitrate = sampleRate * sourceDepth * channels;

        AudioFormat sourceFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                sourceDepth, bitrate, (long) sampleRate, channels);
        ByteArrayAudioStream sourceStream = new ByteArrayAudioStream(pcmData, sourceFormat);

        AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                targetDepth, null, (long) sampleRate, channels);

        try (AudioStream converted = AudioTranscoder.transcode(sourceStream, targetFormat)) {
            assertNotNull(converted);

            AudioFormat resultFormat = converted.getFormat();
            assertEquals(targetDepth, resultFormat.getBitDepth());
            assertEquals(sampleRate, resultFormat.getFrequency());
            assertEquals(channels, resultFormat.getChannels());

            byte[] output = converted.readAllBytes();

            int sourceBytesPerSample = sourceDepth / 8;
            int targetBytesPerSample = targetDepth / 8;
            int expectedBytes = (pcmData.length / sourceBytesPerSample) * targetBytesPerSample;

            assertEquals(expectedBytes, output.length,
                    "Converting from " + sourceDepth + "-bit to " + targetDepth + "-bit yielded unexpected byte count");
        }
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
    public void testPcmToWav() throws AudioTranscodingException, IOException {
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
    public void testWavAlwaysSizeable() throws AudioTranscodingException, IOException {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream stream1 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        AudioStream wavStream = AudioTranscoder.transcode(stream1, AudioFormat.WAV, false);
        assertInstanceOf(FileAudioStream.class, wavStream);
        assertInstanceOf(SizeableAudioStream.class, wavStream);
        byte[] wavBytes = wavStream.readAllBytes();
        assertTrue(wavBytes.length > 0);
        wavStream.close();

        ByteArrayAudioStream stream2 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        AudioStream fileWav = AudioTranscoder.transcode(stream2, AudioFormat.WAV, true);
        assertInstanceOf(FileAudioStream.class, fileWav);
        assertInstanceOf(SizeableAudioStream.class, fileWav);
        byte[] fileWavBytes = fileWav.readAllBytes();
        assertTrue(fileWavBytes.length > 0);
        fileWav.close();
    }

    @Test
    public void testPcmToFlacStream() throws AudioTranscodingException, IOException {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream pcmStream = new ByteArrayAudioStream(pcmBytes, pcmFormat);

        AudioStream flacStream = AudioTranscoder.transcode(pcmStream, AudioFormat.FLAC);
        assertNotNull(flacStream);
        assertEquals(AudioFormat.CONTAINER_FLAC, flacStream.getFormat().getContainer());
        byte[] flacData = flacStream.readAllBytes();
        assertTrue(flacData.length > 0);
        assertEquals((byte) 'f', flacData[0]);
        assertEquals((byte) 'L', flacData[1]);
        assertEquals((byte) 'a', flacData[2]);
        assertEquals((byte) 'C', flacData[3]);
        flacStream.close();
    }

    @Test
    public void testPcmToFlacToPcm() throws AudioTranscodingException, IOException {
        byte[] originalPcm = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream srcStream = new ByteArrayAudioStream(originalPcm, pcmFormat);
        AudioStream flacStream = AudioTranscoder.transcode(srcStream, AudioFormat.FLAC, true);
        byte[] flacBytes = flacStream.readAllBytes();
        flacStream.close();

        ByteArrayAudioStream flacIn = new ByteArrayAudioStream(flacBytes, AudioFormat.FLAC);
        AudioStream pcmStream = AudioTranscoder.transcode(flacIn, pcmFormat);
        assertNotNull(pcmStream);
        assertEquals(AudioFormat.CONTAINER_NONE, pcmStream.getFormat().getContainer());
        assertEquals(AudioFormat.CODEC_PCM_SIGNED, pcmStream.getFormat().getCodec());
        assertEquals(44100L, pcmStream.getFormat().getFrequency());

        byte[] decodedPcm = pcmStream.readAllBytes();
        assertEquals(originalPcm.length, decodedPcm.length);
        pcmStream.close();
    }

    @Test
    public void testFlacToWav() throws AudioTranscodingException, IOException {
        byte[] flacBytes = createFlacBytes(44100, 16, 1, 100);
        ByteArrayAudioStream flacStream = new ByteArrayAudioStream(flacBytes, AudioFormat.FLAC);

        AudioStream wavStream = AudioTranscoder.transcode(flacStream, AudioFormat.WAV);
        assertNotNull(wavStream);
        assertEquals(AudioFormat.CONTAINER_WAVE, wavStream.getFormat().getContainer());

        byte[] wavBytes = wavStream.readAllBytes();
        assertTrue(wavBytes.length > 0);
        AudioFormat parsed = AudioWaveUtils.parseWavFormat(new ByteArrayInputStream(wavBytes));
        assertEquals(AudioFormat.CONTAINER_WAVE, parsed.getContainer());
        assertEquals(44100L, parsed.getFrequency());
        wavStream.close();
    }

    @Test
    public void testWavToFlac() throws AudioTranscodingException, IOException {
        byte[] wavBytes = createWavBytes(44100, 16, 1, 100);
        ByteArrayAudioStream wavStream = new ByteArrayAudioStream(wavBytes, AudioFormat.WAV);

        AudioStream flacStream = AudioTranscoder.transcode(wavStream, AudioFormat.FLAC);
        assertNotNull(flacStream);
        assertEquals(AudioFormat.CONTAINER_FLAC, flacStream.getFormat().getContainer());
        byte[] flacBytes = flacStream.readAllBytes();
        assertTrue(flacBytes.length > 0);
        assertEquals((byte) 'f', flacBytes[0]);
        assertEquals((byte) 'L', flacBytes[1]);
        assertEquals((byte) 'a', flacBytes[2]);
        assertEquals((byte) 'C', flacBytes[3]);
        flacStream.close();
    }

    @Test
    public void testFlacStreamingVsSizeable() throws AudioTranscodingException, IOException {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream stream1 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        AudioStream pipedFlac = AudioTranscoder.transcode(stream1, AudioFormat.FLAC, false);
        assertInstanceOf(PipedAudioStream.class, pipedFlac);
        byte[] pipedFlacBytes = pipedFlac.readAllBytes();
        assertTrue(pipedFlacBytes.length > 0);
        pipedFlac.close();

        ByteArrayAudioStream stream2 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        AudioStream fileFlac = AudioTranscoder.transcode(stream2, AudioFormat.FLAC, true);
        assertInstanceOf(FileAudioStream.class, fileFlac);
        assertInstanceOf(SizeableAudioStream.class, fileFlac);
        byte[] fileFlacBytes = fileFlac.readAllBytes();
        assertTrue(fileFlacBytes.length > 0);
        fileFlac.close();
    }

    @Test
    public void testCorruptFlacThrowsException() throws IOException {
        byte[] invalidFlac = new byte[] { 'f', 'L', 'a', 'C', 0, 1, 2, 3, 4, 5 };

        try (ByteArrayAudioStream stream = new ByteArrayAudioStream(invalidFlac, AudioFormat.FLAC)) {
            AudioFormat targetFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                    16, null, 44100L, 1);
            assertThrows(AudioTranscodingException.class, () -> {
                AudioTranscoder.transcode(stream, targetFormat);
            });
        }
    }

    @Test
    public void testTranscodeToSupported() throws AudioTranscodingException, IOException {
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
    public void testTranscodeToSupportedWithSupportedStreams() throws AudioTranscodingException, IOException {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 100);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        ByteArrayAudioStream stream1 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        Set<Class<? extends AudioStream>> pipedSupported = Set.of(AudioStream.class);
        AudioStream piped = AudioTranscoder.transcodeToSupported(stream1, Set.of(AudioFormat.FLAC), pipedSupported);
        assertInstanceOf(PipedAudioStream.class, piped);
        piped.close();

        ByteArrayAudioStream stream2 = new ByteArrayAudioStream(pcmBytes, pcmFormat);
        Set<Class<? extends AudioStream>> fileSupported = Set.of(FileAudioStream.class);
        AudioStream sizeable = AudioTranscoder.transcodeToSupported(stream2, Set.of(AudioFormat.FLAC), fileSupported);
        assertInstanceOf(FileAudioStream.class, sizeable);
        assertInstanceOf(SizeableAudioStream.class, sizeable);
        sizeable.close();
    }

    @Test
    public void testTranscodeToSupportedThrowsWhenNoMatch() throws IOException {
        byte[] pcmBytes = createPcmBytes(44100, 16, 1, 50);
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16,
                705600, 44100L, 1);
        try (ByteArrayAudioStream pcmStream = new ByteArrayAudioStream(pcmBytes, pcmFormat)) {
            Set<AudioFormat> candidates = Set.of(AudioFormat.MP3, AudioFormat.AAC);
            assertThrows(AudioTranscodingException.class, () -> {
                AudioTranscoder.transcodeToSupported(pcmStream, candidates);
            });
        }
    }

    @Test
    public void testClosingStreamClosesSource() throws AudioTranscodingException, IOException {
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
    public void testPreservesStreamId() throws AudioTranscodingException, IOException {
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
