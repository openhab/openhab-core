/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.internal.fake.AudioSinkFake;
import org.openhab.core.audio.internal.utils.BundledSoundFileHandler;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.console.Console;

/**
 * OSGi test for {@link AudioConsoleCommandExtension}
 *
 * @author Petar Valchev - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AudioConsoleTest extends AbstractAudioServletTest {

    private BundledSoundFileHandler fileHandler;

    private AudioConsoleCommandExtension audioConsoleCommandExtension;

    private AudioManagerImpl audioManager;

    private AudioSinkFake audioSink;

    private final byte[] testByteArray = new byte[] { 0, 1, 2 };

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
    public void setUp() throws IOException {
        fileHandler = new BundledSoundFileHandler();
        audioSink = new AudioSinkFake();

        audioManager = new AudioManagerImpl();
        audioManager.addAudioSink(audioSink);

        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.getDefault());

        audioConsoleCommandExtension = new AudioConsoleCommandExtension(audioManager, localeProvider);
    }

    @After
    public void tearDown() {
        fileHandler.close();
    }

    @Test
    public void testUsages() {
        assertThat("Could not get AudioConsoleCommandExtension's usages", audioConsoleCommandExtension.getUsages(),
                is(notNullValue()));
    }

    @Test
    public void audioConsolePlaysFile() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, fileHandler.wavFileName() };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSink() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(),
                fileHandler.wavFileName() };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSinkWithASpecifiedVolume() throws AudioException, IOException {
        AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(),
                fileHandler.wavFileName(), "25" };
        audioConsoleCommandExtension.execute(args, consoleMock);

        assertThat(audioSink.audioFormat.isCompatible(audioStream.getFormat()), is(true));
        audioStream.close();
    }

    @Test
    public void audioConsolePlaysFileForASpecifiedSinkWithAnInvalidVolume() {
        String[] args = new String[] { AudioConsoleCommandExtension.SUBCMD_PLAY, audioSink.getId(),
                fileHandler.wavFileName(), "invalid" };
        audioConsoleCommandExtension.execute(args, consoleMock);

        waitForAssert(() -> assertThat("The given volume was invalid", consoleOutput,
                is("Specify volume as percentage between 0 and 100")));
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
