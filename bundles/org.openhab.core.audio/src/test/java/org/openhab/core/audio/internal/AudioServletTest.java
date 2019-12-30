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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.internal.utils.BundledSoundFileHandler;

/**
 * Test cases for {@link AudioServlet}
 *
 * @author Petar Valchev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AudioServletTest extends AbstractAudioServletTest {

    private static final String MEDIA_TYPE_AUDIO_WAV = "audio/wav";
    private static final String MEDIA_TYPE_AUDIO_OGG = "audio/ogg";
    private static final String MEDIA_TYPE_AUDIO_MPEG = "audio/mpeg";

    private final byte[] testByteArray = new byte[] { 0, 1, 2 };

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
        try (BundledSoundFileHandler fileHandler = new BundledSoundFileHandler()) {
            AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

            ContentResponse response = getHttpResponse(audioStream);

            assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
            assertThat("The response media type was not as expected", response.getMediaType(),
                    is(MEDIA_TYPE_AUDIO_WAV));
        }
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
        final int streamTimeout = 1;

        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_MP3);

        final long beg = System.currentTimeMillis();

        String url = serveStream(audioStream, streamTimeout);

        Request request = getHttpRequest(url);

        ContentResponse response = request.send();

        final long end = System.currentTimeMillis();

        if (response.getStatus() == HttpStatus.NOT_FOUND_404) {
            assertThat("Response status 404 is only allowed if streamTimeout is exceeded.",
                    TimeUnit.MILLISECONDS.toSeconds(end - beg), greaterThan((long) streamTimeout));
        } else {
            assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
            assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
            assertThat("The response media type was not as expected", response.getMediaType(),
                    is(MEDIA_TYPE_AUDIO_MPEG));

            assertThat("The audio stream was not added to the multitime streams",
                    audioServlet.getMultiTimeStreams().containsValue(audioStream), is(true));
        }

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
