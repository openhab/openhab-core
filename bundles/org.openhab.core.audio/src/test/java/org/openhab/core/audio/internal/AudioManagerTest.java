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
package org.openhab.core.audio.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.audio.internal.fake.AudioSinkFake;
import org.openhab.core.audio.internal.utils.BundledSoundFileHandler;
import org.openhab.core.common.Disposable;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.library.types.PercentType;

/**
 * OSGi test for {@link AudioManagerImpl}
 *
 * @author Petar Valchev - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 * @author Henning Treu - Convert to plain java tests
 */
@NonNullByDefault
public class AudioManagerTest {

    private @NonNullByDefault({}) AudioManagerImpl audioManager;
    private @NonNullByDefault({}) AudioSinkRegistryImpl audioSinkRegistry;
    private @NonNullByDefault({}) AudioSourceRegistryImpl audioSourceRegistry;
    private @NonNullByDefault({}) AudioSinkFake audioSink;
    private @NonNullByDefault({}) AudioSource audioSource;
    private @NonNullByDefault({}) BundledSoundFileHandler fileHandler;

    @BeforeEach
    public void setup() throws IOException {
        fileHandler = new BundledSoundFileHandler();
        audioSinkRegistry = new AudioSinkRegistryImpl();
        audioSourceRegistry = new AudioSourceRegistryImpl();
        audioManager = new AudioManagerImpl(audioSinkRegistry, audioSourceRegistry);
        audioSink = new AudioSinkFake();

        audioSource = mock(AudioSource.class);
        when(audioSource.getId()).thenReturn("audioSourceId");
        when(audioSource.getUID()).thenReturn("audioSourceId");
        when(audioSource.getLabel(any(Locale.class))).thenReturn("audioSourceLabel");
    }

    @AfterEach
    public void tearDown() throws IOException {
        fileHandler.close();
    }

    @Test
    public void audioManagerPlaysByteArrayAudioStream() {
        audioSinkRegistry.addAudioSink(audioSink);
        ByteArrayAudioStream audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE,
                AudioFormat.CODEC_PCM_SIGNED);

        audioManager.play(audioStream, audioSink.getId());

        AudioFormat expectedAudioFormat = audioStream.getFormat();

        assertThat(audioSink.audioFormat.isCompatible(expectedAudioFormat), is(true));
    }

    @Test
    public void audioManagerPlaysByteArrayAudioStreamDirectlyToAudioSink() {
        ByteArrayAudioStream audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE,
                AudioFormat.CODEC_PCM_SIGNED);

        audioManager.play(audioStream, audioSink);

        AudioFormat expectedAudioFormat = audioStream.getFormat();

        assertThat(audioSink.audioFormat.isCompatible(expectedAudioFormat), is(true));
    }

    @Test
    public void nullStreamsAreProcessed() {
        audioSinkRegistry.addAudioSink(audioSink);
        audioManager.play(null, audioSink.getId());

        assertThat(audioSink.isStreamProcessed, is(true));
        assertThat(audioSink.isStreamStopped, is(true));
    }

    @Test
    public void audioManagerPlaysStreamFromWavAudioFiles() throws AudioException {
        audioSinkRegistry.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

        audioManager.play(audioStream, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
    }

    @Test
    public void audioManagerPlaysStreamFromMp3AudioFiles() throws AudioException {
        audioSinkRegistry.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.mp3FilePath()));

        audioManager.play(audioStream, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
    }

    @Test
    public void audioManagerPlaysWavAudioFiles() throws AudioException, IOException {
        audioSinkRegistry.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));
        audioManager.playFile(fileHandler.wavFileName(), audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioManagerPlaysMp3AudioFiles() throws AudioException, IOException {
        audioSinkRegistry.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.mp3FilePath()));
        audioManager.playFile(fileHandler.mp3FileName(), audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void fileIsNotProcessedIfThereIsNoRegisteredSink() throws AudioException {
        File file = new File(fileHandler.mp3FilePath());

        audioManager.playFile(file.getName(), audioSink.getId());

        assertThat(audioSink.isStreamProcessed, is(false));
    }

    @Test
    public void audioManagerHandlesUnsupportedAudioFormatException() throws AudioException {
        audioSinkRegistry.addAudioSink(audioSink);
        audioSink.isUnsupportedAudioFormatExceptionExpected = true;
        try {
            audioManager.playFile(fileHandler.mp3FileName(), audioSink.getId());
        } catch (UnsupportedAudioStreamException e) {
            fail("An exception " + e + " was thrown, while trying to process a stream");
        }
    }

    @Test
    public void audioManagerHandlesUnsupportedAudioStreamException() throws AudioException {
        audioSinkRegistry.addAudioSink(audioSink);
        audioSink.isUnsupportedAudioStreamExceptionExpected = true;
        try {
            audioManager.playFile(fileHandler.mp3FileName(), audioSink.getId());
        } catch (UnsupportedAudioStreamException e) {
            fail("An exception " + e + " was thrown, while trying to process a stream");
        }
    }

    @Test
    public void audioManagerSetsTheVolumeOfASink() throws IOException {
        audioSinkRegistry.addAudioSink(audioSink);
        PercentType initialVolume = new PercentType(67);
        PercentType sinkMockVolume = getSinkMockVolume(initialVolume);

        assertThat(sinkMockVolume, is(initialVolume));
    }

    @Test
    public void theVolumeOfANullSinkIsZero() throws IOException {
        assertThat(audioManager.getVolume(null), is(PercentType.ZERO));
    }

    @Test
    public void audioManagerSetsTheVolumeOfNotRegisteredSinkToZero() throws IOException {
        PercentType initialVolume = new PercentType(67);
        PercentType sinkMockVolume = getSinkMockVolume(initialVolume);

        assertThat(sinkMockVolume, is(PercentType.ZERO));
    }

    @Test
    public void sourceIsRegistered() {
        assertRegisteredSource(false);
    }

    @Test
    public void defaultSourceIsRegistered() {
        assertRegisteredSource(true);
    }

    @Test
    public void sinkIsRegistered() {
        assertRegisteredSink(false);
    }

    @Test
    public void defaultSinkIsRegistered() {
        assertRegisteredSink(true);
    }

    @Test
    public void sinkIsAddedInParameterOptions() {
        assertAddedParameterOption(AudioManagerImpl.CONFIG_DEFAULT_SINK, Locale.getDefault());
    }

    @Test
    public void sourceIsAddedInParameterOptions() {
        assertAddedParameterOption(AudioManagerImpl.CONFIG_DEFAULT_SOURCE, Locale.getDefault());
    }

    @Test
    public void inCaseOfWrongUriNoParameterOptionsAreAdded() {
        audioSinkRegistry.addAudioSink(audioSink);

        Collection<ParameterOption> parameterOptions = audioManager.getParameterOptions(URI.create("wrong.uri"),
                AudioManagerImpl.CONFIG_DEFAULT_SINK, null, Locale.US);
        assertThat("The parameter options were not as expected", parameterOptions, is(nullValue()));
    }

    private void assertRegisteredSource(boolean isSourceDefault) {
        audioSourceRegistry.addAudioSource(audioSource);

        if (isSourceDefault) {
            audioManager.modified(Map.of(AudioManagerImpl.CONFIG_DEFAULT_SOURCE, audioSource.getId()));
        } else {
            // just to make sure there is no default source
            audioManager.modified(Map.of());
        }

        assertThat(String.format("The source %s was not registered", audioSource.getId()), audioManager.getSource(),
                is(audioSource));
        assertThat(String.format("The source %s was not added to the set of sources", audioSource.getId()),
                audioManager.getAllSources().contains(audioSource), is(true));
        assertThat(String.format("The source %s was not added to the set of sources", audioSource.getId()),
                audioManager.getSourceIds(audioSource.getId()).contains(audioSource.getId()), is(true));
    }

    private void assertRegisteredSink(boolean isSinkDefault) {
        audioSinkRegistry.addAudioSink(audioSink);

        if (isSinkDefault) {
            audioManager.modified(Map.of(AudioManagerImpl.CONFIG_DEFAULT_SINK, audioSink.getId()));
        } else {
            // just to make sure there is no default sink
            audioManager.modified(Map.of());
        }

        assertThat(String.format("The sink %s was not registered", audioSink.getId()), audioManager.getSink(),
                is(audioSink));
        assertThat(String.format("The sink %s was not added to the set of sinks", audioSink.getId()),
                audioManager.getAllSinks().contains(audioSink), is(true));
        assertThat(String.format("The sink %s was not added to the set of sinks", audioSink.getId()),
                audioManager.getSinkIds(audioSink.getId()).contains(audioSink.getId()), is(true));
    }

    private PercentType getSinkMockVolume(PercentType initialVolume) throws IOException {
        audioManager.setVolume(initialVolume, audioSink.getId());

        String sinkMockId = audioSink.getId();
        return audioManager.getVolume(sinkMockId);
    }

    /**
     *
     * @param param either default source or default sink
     */
    private void assertAddedParameterOption(String param, Locale locale) {
        String id = "";
        String label = "";

        switch (param) {
            case AudioManagerImpl.CONFIG_DEFAULT_SINK:
                audioSinkRegistry.addAudioSink(audioSink);
                id = audioSink.getId();
                label = Objects.requireNonNull(audioSink.getLabel(locale));
                break;
            case AudioManagerImpl.CONFIG_DEFAULT_SOURCE:
                audioSourceRegistry.addAudioSource(audioSource);
                id = audioSource.getId();
                label = Objects.requireNonNull(audioSource.getLabel(locale));
                break;
            default:
                fail("The parameter must be either default sink or default source");
        }

        Collection<ParameterOption> parameterOptions = audioManager
                .getParameterOptions(URI.create(AudioManagerImpl.CONFIG_URI), param, null, locale);

        @SuppressWarnings("null")
        BiFunction<String, String, Boolean> isParameterOptionAdded = (v, l) -> parameterOptions.stream()
                .anyMatch(po -> po.getValue().equals(v) && po.getLabel().equals(l));

        assertThat(param + " was not added to the parameter options", isParameterOptionAdded.apply(id, label),
                is(true));
    }

    @Test
    public void disposableStreamIsDisposedOnSuccessfulPlayback() {
        audioSinkRegistry.addAudioSink(audioSink);
        AtomicBoolean disposed = new AtomicBoolean(false);
        DisposableAudioStream stream = new DisposableAudioStream(new byte[] { 0, 1, 2 },
                new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, true, 16, 1000, 16384L, 1),
                () -> disposed.set(true));

        audioManager.play(stream, audioSink.getId());

        assertTrue(disposed.get());
    }

    @Test
    public void disposableStreamIsDisposedOnFailedPlayback() {
        audioSinkRegistry.addAudioSink(audioSink);
        audioSink.isUnsupportedAudioFormatExceptionExpected = true;
        AtomicBoolean disposed = new AtomicBoolean(false);
        DisposableAudioStream stream = new DisposableAudioStream(new byte[] { 0, 1, 2 },
                new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, true, 16, 1000, 16384L, 1),
                () -> disposed.set(true));

        audioManager.play(stream, audioSink.getId());

        assertTrue(disposed.get());
    }

    @Test
    public void playTranscodesAudioStreamWhenFormatNotDirectlySupportedBySink() throws IOException {
        audioSink.supportedAudioFormats = Set.of(AudioFormat.PCM_SIGNED);
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink);

        assertTrue(audioSink.isStreamProcessed);
        assertNotNull(audioSink.audioFormat);
        assertTrue(AudioFormat.PCM_SIGNED.isCompatible(audioSink.audioFormat));
        assertEquals(AudioFormat.CONTAINER_NONE, audioSink.audioFormat.getContainer());
    }

    @Test
    public void playWithSinkIdTranscodesAudioStreamWhenFormatNotDirectlySupportedBySink() throws IOException {
        audioSinkRegistry.addAudioSink(audioSink);
        audioSink.supportedAudioFormats = Set.of(AudioFormat.PCM_SIGNED);
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink.getId());

        assertTrue(audioSink.isStreamProcessed);
        assertNotNull(audioSink.audioFormat);
        assertTrue(AudioFormat.PCM_SIGNED.isCompatible(audioSink.audioFormat));
        assertEquals(AudioFormat.CONTAINER_NONE, audioSink.audioFormat.getContainer());
    }

    @Test
    public void playTranscodesAudioStreamWhenSinkRequiresSizeableStream() throws IOException {
        audioSink.supportedAudioFormats = Set.of(AudioFormat.WAV);
        audioSink.supportedAudioStreams = Set.of(FileAudioStream.class);
        ByteArrayAudioStream wavStream = createWavAudioStream();
        AudioStream nonSizeableStream = new AudioStream() {
            @Override
            public AudioFormat getFormat() {
                return wavStream.getFormat();
            }

            @Override
            public int read() throws IOException {
                return wavStream.read();
            }

            @Override
            public int read(byte @Nullable [] b, int off, int len) throws IOException {
                return wavStream.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                wavStream.close();
            }
        };

        audioManager.play(nonSizeableStream, audioSink);

        assertTrue(audioSink.isStreamProcessed);
        assertInstanceOf(SizeableAudioStream.class, audioSink.audioStream);
    }

    @Test
    public void playPassesStreamDirectlyWhenFormatAndStreamTypeAreSupported() throws IOException {
        audioSink.supportedAudioFormats = Set.of(AudioFormat.WAV);
        audioSink.supportedAudioStreams = Set.of(ByteArrayAudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink);

        assertTrue(audioSink.isStreamProcessed);
        assertSame(audioStream, audioSink.audioStream);
    }

    @Test
    public void playDoesNotSendStreamToSinkWhenTranscodingFailsForUnsupportedFormat() throws IOException {
        audioSink.supportedAudioFormats = Set.of(AudioFormat.AAC);
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink);

        assertFalse(audioSink.isStreamProcessed);
        assertNull(audioSink.audioStream);
    }

    @Test
    public void playDoesNotSendStreamToSinkWhenSinkSupportsNoFormats() throws IOException {
        audioSink.supportedAudioFormats = Set.of();
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink);

        assertFalse(audioSink.isStreamProcessed);
        assertNull(audioSink.audioStream);
    }

    @Test
    public void playClosesSourceStreamWhenTranscodingFails() throws IOException {
        audioSink.supportedAudioFormats = Set.of(AudioFormat.AAC);
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AtomicBoolean closed = new AtomicBoolean(false);
        ByteArrayAudioStream wavStream = createWavAudioStream();
        AudioStream audioStream = new AudioStream() {
            @Override
            public AudioFormat getFormat() {
                return wavStream.getFormat();
            }

            @Override
            public int read() throws IOException {
                return wavStream.read();
            }

            @Override
            public int read(byte @Nullable [] b, int off, int len) throws IOException {
                return wavStream.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                closed.set(true);
                wavStream.close();
            }
        };

        audioManager.play(audioStream, audioSink);

        assertTrue(closed.get());
        assertFalse(audioSink.isStreamProcessed);
    }

    @Test
    public void playWithSinkIdDoesNotSendStreamToSinkWhenTranscodingFails() throws IOException {
        audioSinkRegistry.addAudioSink(audioSink);
        audioSink.supportedAudioFormats = Set.of(AudioFormat.AAC);
        audioSink.supportedAudioStreams = Set.of(AudioStream.class);
        AudioStream audioStream = createWavAudioStream();

        audioManager.play(audioStream, audioSink.getId());

        assertFalse(audioSink.isStreamProcessed);
        assertNull(audioSink.audioStream);
    }

    private ByteArrayAudioStream createWavAudioStream() throws IOException {
        int sampleRate = 44100;
        int bitDepth = 16;
        int channels = 1;

        int durationMs = 50;
        int totalSamples = (sampleRate * durationMs) / 1000;
        int bytesPerSample = bitDepth / 8;
        int frameSize = bytesPerSample * channels;

        byte[] pcmData = new byte[totalSamples * frameSize];
        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / sampleRate;
            int sampleValue = (int) (Math.sin(angle) * ((1L << (bitDepth - 1)) - 1));
            pcmData[i * 2] = (byte) (sampleValue & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((sampleValue >> 8) & 0xFF);
        }

        javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitDepth, channels, frameSize,
                sampleRate, false);

        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcmData), jFormat,
                pcmData.length / frameSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);

        return new ByteArrayAudioStream(baos.toByteArray(), AudioFormat.WAV);
    }

    private ByteArrayAudioStream getByteArrayAudioStream(String container, String codec) {
        int bitDepth = 16;
        int bitRate = 1000;
        long frequency = 16384;
        int channels = 1;
        byte[] testByteArray = new byte[] { 0, 1, 2 };

        AudioFormat audioFormat = new AudioFormat(container, codec, true, bitDepth, bitRate, frequency, channels);

        return new ByteArrayAudioStream(testByteArray, audioFormat);
    }

    private static class DisposableAudioStream extends ByteArrayAudioStream implements Disposable {
        private final Runnable onDispose;

        DisposableAudioStream(byte[] bytes, AudioFormat format, Runnable onDispose) {
            super(bytes, format);
            this.onDispose = onDispose;
        }

        @Override
        public void dispose() {
            onDispose.run();
        }
    }
}
