/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.function.BiFunction;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.ByteArrayAudioStream;
import org.eclipse.smarthome.core.audio.FileAudioStream;
import org.eclipse.smarthome.core.audio.UnsupportedAudioStreamException;
import org.eclipse.smarthome.core.audio.internal.fake.AudioSinkFake;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * OSGi test for {@link AudioManagerImpl}
 *
 * @author Petar Valchev - Initial contribution and API
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 * @author Henning Treu - Convert to plain java tests
 */
public class AudioManagerTest {

    private AudioManagerImpl audioManager;

    private AudioSinkFake audioSink;
    private AudioSource audioSource;

    private static final String CONFIGURATION_DIRECTORY_NAME = "configuration";

    private static final String MP3_FILE_NAME = "mp3AudioFile.mp3";
    private static final String MP3_FILE_PATH = CONFIGURATION_DIRECTORY_NAME + "/sounds/" + MP3_FILE_NAME;

    private static final String WAV_FILE_NAME = "wavAudioFile.wav";
    private static final String WAV_FILE_PATH = CONFIGURATION_DIRECTORY_NAME + "/sounds/" + WAV_FILE_NAME;

    @Before
    public void setup() {
        audioManager = new AudioManagerImpl();
        audioSink = new AudioSinkFake();

        audioSource = mock(AudioSource.class);
        when(audioSource.getId()).thenReturn("audioSourceId");
        when(audioSource.getLabel(any(Locale.class))).thenReturn("audioSourceLabel");

        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, CONFIGURATION_DIRECTORY_NAME);
    }

    @After
    public void tearDown() {
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, ConfigConstants.DEFAULT_CONFIG_FOLDER);
    }

    @Test
    public void audioManagerPlaysByteArrayAudioStream() throws AudioException {
        audioManager.addAudioSink(audioSink);
        ByteArrayAudioStream audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_MP3);

        audioManager.play(audioStream, audioSink.getId());

        AudioFormat expectedAudioFormat = audioStream.getFormat();

        assertThat(audioSink.audioFormat.isCompatible(expectedAudioFormat), is(true));
    }

    @Test
    public void nullStreamsAreProcessed() {
        audioManager.addAudioSink(audioSink);
        audioManager.play(null, audioSink.getId());

        assertThat(audioSink.isStreamProcessed, is(true));
        assertThat(audioSink.isStreamStopped, is(true));
    }

    @Test
    public void audioManagerPlaysStreamFromWavAudioFiles() throws AudioException {
        audioManager.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));

        audioManager.play(audioStream, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
    }

    @Test
    public void audioManagerPlaysStreamFromMp3AudioFiles() throws AudioException {
        audioManager.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(MP3_FILE_PATH));

        audioManager.play(audioStream, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
    }

    @Test
    public void audioManagerPlaysWavAudioFiles() throws AudioException, IOException {
        audioManager.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));
        audioManager.playFile(WAV_FILE_NAME, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioManagerPlaysMp3AudioFiles() throws AudioException, IOException {
        audioManager.addAudioSink(audioSink);
        AudioStream audioStream = new FileAudioStream(new File(MP3_FILE_PATH));
        audioManager.playFile(MP3_FILE_NAME, audioSink.getId());

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void fileIsNotProcessedIfThereIsNoRegisteredSink() throws AudioException {
        File file = new File(MP3_FILE_PATH);

        audioManager.playFile(file.getName(), audioSink.getId());

        assertThat(audioSink.isStreamProcessed, is(false));
    }

    @Test
    public void audioManagerHandlesUnsupportedAudioFormatException() throws AudioException {
        audioManager.addAudioSink(audioSink);
        audioSink.isUnsupportedAudioFormatExceptionExpected = true;
        try {
            audioManager.playFile(MP3_FILE_NAME, audioSink.getId());
        } catch (UnsupportedAudioStreamException e) {
            fail("An exception " + e + " was thrown, while trying to process a stream");
        }
    }

    @Test
    public void audioManagerHandlesUnsupportedAudioStreamException() throws AudioException {
        audioManager.addAudioSink(audioSink);
        audioSink.isUnsupportedAudioStreamExceptionExpected = true;
        try {
            audioManager.playFile(MP3_FILE_NAME, audioSink.getId());
        } catch (UnsupportedAudioStreamException e) {
            fail("An exception " + e + " was thrown, while trying to process a stream");
        }
    }

    @Test
    public void audioManagerSetsTheVolumeOfASink() throws IOException {
        audioManager.addAudioSink(audioSink);
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
        audioManager.addAudioSink(audioSink);

        Collection<ParameterOption> parameterOptions = audioManager.getParameterOptions(URI.create("wrong.uri"),
                AudioManagerImpl.CONFIG_DEFAULT_SINK, Locale.US);
        assertThat("The parameter options were not as expected", parameterOptions, is(nullValue()));
    }

    private void assertRegisteredSource(boolean isSourceDefault) {
        audioManager.addAudioSource(audioSource);

        if (isSourceDefault) {
            audioManager
                    .modified(Collections.singletonMap(AudioManagerImpl.CONFIG_DEFAULT_SOURCE, audioSource.getId()));
        } else {
            // just to make sure there is no default source
            audioManager.modified(Collections.emptyMap());
        }

        assertThat(String.format("The source %s was not registered", audioSource.getId()), audioManager.getSource(),
                is(audioSource));
        assertThat(String.format("The source %s was not added to the set of sources", audioSource.getId()),
                audioManager.getAllSources().contains(audioSource), is(true));
        assertThat(String.format("The source %s was not added to the set of sources", audioSource.getId()),
                audioManager.getSourceIds(audioSource.getId()).contains(audioSource.getId()), is(true));
    }

    private void assertRegisteredSink(boolean isSinkDefault) {
        audioManager.addAudioSink(audioSink);

        if (isSinkDefault) {
            audioManager.modified(Collections.singletonMap(AudioManagerImpl.CONFIG_DEFAULT_SINK, audioSink.getId()));
        } else {
            // just to make sure there is no default sink
            audioManager.modified(Collections.emptyMap());
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
     * @param param - either default source or default sink
     */
    private void assertAddedParameterOption(String param, Locale locale) {
        String id = "";
        String label = "";

        switch (param) {
            case AudioManagerImpl.CONFIG_DEFAULT_SINK:
                audioManager.addAudioSink(audioSink);
                id = audioSink.getId();
                label = audioSink.getLabel(locale);
                break;
            case AudioManagerImpl.CONFIG_DEFAULT_SOURCE:
                audioManager.addAudioSource(audioSource);
                id = audioSource.getId();
                label = audioSource.getLabel(locale);
                break;
            default:
                fail("The parameter must be either default sink or default source");
        }

        Collection<ParameterOption> parameterOptions = audioManager
                .getParameterOptions(URI.create(AudioManagerImpl.CONFIG_URI), param, locale);

        @SuppressWarnings("null")
        BiFunction<String, String, Boolean> isParameterOptionAdded = (v, l) -> parameterOptions.stream()
                .anyMatch(po -> po.getValue().equals(v) && po.getLabel().equals(l));

        assertThat(param + " was not added to the parameter options", isParameterOptionAdded.apply(id, label),
                is(true));
    }

    private ByteArrayAudioStream getByteArrayAudioStream(String container, String codec) {
        int bitDepth = 16;
        int bitRate = 1000;
        long frequency = 16384;
        byte[] testByteArray = new byte[] { 0, 1, 2 };

        AudioFormat audioFormat = new AudioFormat(container, codec, true, bitDepth, bitRate, frequency);

        return new ByteArrayAudioStream(testByteArray, audioFormat);
    }

}
