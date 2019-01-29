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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FileAudioStream;
import org.eclipse.smarthome.core.audio.URLAudioStream;
import org.eclipse.smarthome.core.audio.internal.fake.AudioSinkFake;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.io.console.Console;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * OSGi test for {@link AudioConsoleCommandExtension}
 *
 * @author Petar Valchev - Initial contribution and API
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AudioConsoleTest extends AbstractAudioServeltTest {

    private AudioConsoleCommandExtension audioConsoleCommandExtension;

    private AudioManagerImpl audioManager;

    private AudioSinkFake audioSink;

    private final byte[] testByteArray = new byte[] { 0, 1, 2 };

    private static final String CONFIGURATION_DIRECTORY_NAME = "configuration";

    protected static final String MP3_FILE_NAME = "mp3AudioFile.mp3";
    protected static final String MP3_FILE_PATH = CONFIGURATION_DIRECTORY_NAME + "/sounds/" + MP3_FILE_NAME;

    protected static final String WAV_FILE_NAME = "wavAudioFile.wav";
    protected static final String WAV_FILE_PATH = CONFIGURATION_DIRECTORY_NAME + "/sounds/" + WAV_FILE_NAME;

    private String consoleOutput;
    private final Console consoleMock = new Console() {

        @Override
        public void println(String s) {
            consoleOutput = s;
        }

        @Override
        public void printUsage(String s) {
        }

        @Override
        public void print(String s) {
            consoleOutput = s;
        }
    };

    private final int testTimeout = 1;

    @Before
    public void setUp() {
        audioManager = new AudioManagerImpl();
        audioSink = new AudioSinkFake();
        audioManager.addAudioSink(audioSink);

        audioConsoleCommandExtension = new AudioConsoleCommandExtension();
        audioConsoleCommandExtension.setAudioManager(audioManager);

        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.getDefault());
        audioConsoleCommandExtension.setLocaleProvider(localeProvider);

        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, CONFIGURATION_DIRECTORY_NAME);
    }

    @After
    public void tearDown() {
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, ConfigConstants.DEFAULT_CONFIG_FOLDER);
    }

    @Test
    public void testUsages() {
        assertThat("Could not get AudioConsoleCommandExtension's usages", audioConsoleCommandExtension.getUsages(),
                is(notNullValue()));
    }

    @Test
    public void audioConsolePlaysFile() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, WAV_FILE_NAME };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSink() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(), WAV_FILE_NAME };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSinkWithASpecifiedVolume() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(), WAV_FILE_NAME,
                "25" };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSinkWithAnInvalidVolume() {
        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(), WAV_FILE_NAME,
                "invalid" };
        audioConsoleCommandExtension.execute(args, consoleMock);

        waitForAssert(() -> assertThat("The given volume was invalid", consoleOutput, nullValue()));
    }

    @Test
    public void audioConsolePlaysStream() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_WAVE,
                AudioFormat.CODEC_PCM_SIGNED);

        String url = serveStream(audioStream, testTimeout);

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_STREAM, url };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat("The streamed URL was not as expected", ((URLAudioStream) audioSink.audioStream).getURL(), is(url));
    }

    @Test
    public void audioConsolePlaysStreamForASpecifiedSink() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_WAVE,
                AudioFormat.CODEC_PCM_SIGNED);

        String url = serveStream(audioStream, testTimeout);

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_STREAM, audioSink.getId(), url };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat("The streamed URL was not as expected", ((URLAudioStream) audioSink.audioStream).getURL(), is(url));
    }

    @Test
    public void audioConsoleListsSinks() {
        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_SINKS };
        audioConsoleCommandExtension.execute(args, consoleMock);

        waitForAssert(() -> assertThat("The listed sink was not as expected", consoleOutput,
                is(String.format("* %s (%s)", audioSink.getLabel(Locale.getDefault()), audioSink.getId()))));
    }

    @Test
    public void audioConsoleListsSources() {
        AudioSource audioSource = mock(AudioSource.class);
        when(audioSource.getId()).thenReturn("sourceId");
        audioManager.addAudioSource(audioSource);

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_SOURCES };
        audioConsoleCommandExtension.execute(args, consoleMock);

        waitForAssert(() -> assertThat("The listed source was not as expected", consoleOutput,
                is(String.format("* %s (%s)", audioSource.getLabel(Locale.getDefault()), audioSource.getId()))));
    }

}
