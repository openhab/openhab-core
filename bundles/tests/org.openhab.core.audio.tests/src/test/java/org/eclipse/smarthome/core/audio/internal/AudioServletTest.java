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

import java.io.File;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FileAudioStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link AudioServlet}
 *
 * @author Petar Valchev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AudioServletTest extends AbstractAudioServeltTest {

    private final String MEDIA_TYPE_AUDIO_WAV = "audio/wav";
    private final String MEDIA_TYPE_AUDIO_OGG = "audio/ogg";
    private final String MEDIA_TYPE_AUDIO_MPEG = "audio/mpeg";

    private static final String CONFIGURATION_DIRECTORY_NAME = "configuration";

    private static final String WAV_FILE_NAME = "wavAudioFile.wav";
    private static final String WAV_FILE_PATH = CONFIGURATION_DIRECTORY_NAME + "/sounds/" + WAV_FILE_NAME;

    private final byte[] testByteArray = new byte[] { 0, 1, 2 };

    @Before
    public void setup() {
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, CONFIGURATION_DIRECTORY_NAME);
    }

    @After
    public void tearDown() {
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, ConfigConstants.DEFAULT_CONFIG_FOLDER);
    }

    @Test
    public void audioServletProcessesByteArrayStream() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_MP3);

        ContentResponse response = getHttpResponse(audioStream);

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
        assertThat("The response media type was not as expected", response.getMediaType(),
                is(equalTo(MEDIA_TYPE_AUDIO_MPEG)));
    }

    @Test
    public void audioServletProcessesStreamFromWavFile() throws Exception {
        AudioStream audioStream = new FileAudioStream(new File(WAV_FILE_PATH));

        ContentResponse response = getHttpResponse(audioStream);

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response media type was not as expected", response.getMediaType(), is(MEDIA_TYPE_AUDIO_WAV));
    }

    @Test
    public void audioServletProcessesStreamFromOggContainer() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_OGG,
                AudioFormat.CODEC_PCM_SIGNED);

        ContentResponse response = getHttpResponse(audioStream);

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
        assertThat("The response media type was not as expected", response.getMediaType(), is(MEDIA_TYPE_AUDIO_OGG));
    }

    @Test
    public void mimeTypeIsNullWhenNoContainerAndTheAudioFormatIsNotMp3() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_PCM_SIGNED);

        ContentResponse response = getHttpResponse(audioStream);

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response media type was not as expected", response.getMediaType(), is(nullValue()));
    }

    @Test
    public void onlyOneRequestToOneTimeStreamsCanBeMade() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_MP3);

        String url = serveStream(audioStream);

        Request request = getHttpRequest(url);

        ContentResponse response = request.send();

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
        assertThat("The response media type was not as expected", response.getMediaType(), is(MEDIA_TYPE_AUDIO_MPEG));

        response = request.send();

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.NOT_FOUND_404));
    }

    @Test
    public void requestToMultitimeStreamCannotBeDoneAfterTheTimeoutOfTheStreamHasExipred() throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_MP3);

        int streamTimeout = 1;
        String url = serveStream(audioStream, streamTimeout);

        Request request = getHttpRequest(url);

        ContentResponse response = request.send();

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
        assertThat("The response media type was not as expected", response.getMediaType(), is(MEDIA_TYPE_AUDIO_MPEG));

        assertThat("The audio stream was not added to the multitime streams",
                audioServlet.getMultiTimeStreams().containsValue(audioStream), is(true));

        waitForAssert(() -> {
            try {
                request.send();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            assertThat("The audio stream was not removed from multitime streams",
                    audioServlet.getMultiTimeStreams().containsValue(audioStream), is(false));
        });

        response = request.send();
        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.NOT_FOUND_404));
    }

}
