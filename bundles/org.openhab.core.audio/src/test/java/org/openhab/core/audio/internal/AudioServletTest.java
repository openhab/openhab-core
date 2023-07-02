/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.FileAudioStream;
import org.openhab.core.audio.StreamServed;
import org.openhab.core.audio.internal.utils.BundledSoundFileHandler;

/**
 * Test cases for {@link AudioServlet}
 *
 * @author Petar Valchev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
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
    public void audioServletProcessesStreamFromWavFileWithoutAcceptHeader() throws Exception {
        try (BundledSoundFileHandler fileHandler = new BundledSoundFileHandler()) {
            AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

            ContentResponse response = getHttpResponse(audioStream);

            assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
            assertThat("The response media type was not as expected", response.getMediaType(),
                    is(MEDIA_TYPE_AUDIO_WAV));
        }
    }

    @Test
    public void audioServletProcessesStreamFromWavFileWithAcceptHeader() throws Exception {
        try (BundledSoundFileHandler fileHandler = new BundledSoundFileHandler()) {
            AudioStream audioStream = new FileAudioStream(new File(fileHandler.wavFilePath()));

            ContentResponse response = getHttpResponseWithAccept(audioStream, "audio/invalid, audio/x-wav");

            assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
            assertThat("The response media type was not as expected", response.getMediaType(), is("audio/x-wav"));
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

        ContentResponse response = getHttpRequest(url).send();

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
        assertThat("The response media type was not as expected", response.getMediaType(), is(MEDIA_TYPE_AUDIO_MPEG));

        response = getHttpRequest(url).send();

        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.NOT_FOUND_404));
    }

    @Test
    public void requestToMultitimeStreamCannotBeDoneAfterTheTimeoutOfTheStreamHasExpired() throws Exception {
        final int streamTimeout = 3;

        AudioStream audioStream = getByteArrayAudioStream(testByteArray, AudioFormat.CONTAINER_NONE,
                AudioFormat.CODEC_MP3);

        final long beg = System.currentTimeMillis();

        String url = serveStream(audioStream, streamTimeout);

        ContentResponse response = getHttpRequest(url).send();

        final long end = System.currentTimeMillis();

        if (response.getStatus() == HttpStatus.NOT_FOUND_404) {
            assertThat("Response status 404 is only allowed if streamTimeout is exceeded.",
                    TimeUnit.MILLISECONDS.toSeconds(end - beg), greaterThan((long) streamTimeout));
        } else {
            assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.OK_200));
            assertThat("The response content was not as expected", response.getContent(), is(testByteArray));
            assertThat("The response media type was not as expected", response.getMediaType(),
                    is(MEDIA_TYPE_AUDIO_MPEG));

            assertThat("The audio stream was not added to the multitime streams", audioServlet.getServedStreams()
                    .values().stream().map(StreamServed::audioStream).toList().contains(audioStream), is(true));
        }

        waitForAssert(() -> {
            try {
                getHttpRequest(url).send();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            assertThat("The audio stream was not removed from multitime streams", audioServlet.getServedStreams()
                    .values().stream().map(StreamServed::audioStream).toList().contains(audioStream), is(false));
        });

        response = getHttpRequest(url).send();
        assertThat("The response status was not as expected", response.getStatus(), is(HttpStatus.NOT_FOUND_404));
    }

    @Test
    public void oneTimeStreamIsRecreatedAsAClonable() throws Exception {
        AudioStream audioStream = mock(AudioStream.class);
        AudioFormat audioFormat = mock(AudioFormat.class);
        when(audioStream.getFormat()).thenReturn(audioFormat);
        when(audioFormat.getCodec()).thenReturn(AudioFormat.CODEC_MP3);
        when(audioStream.readNBytes(anyInt())).thenReturn(testByteArray);

        String url = serveStream(audioStream, 10);
        String uuid = url.substring(url.lastIndexOf("/") + 1);
        StreamServed servedStream = audioServlet.getServedStreams().get(uuid);

        // does not contain directly the stream because it is now a new stream wrapper
        assertThat(servedStream.audioStream(), not(audioStream));
        // it is now a ByteArrayAudioStream wrapper :
        assertThat(servedStream.audioStream(), instanceOf(ByteArrayAudioStream.class));

        ContentResponse response = getHttpRequest(url).send();
        assertThat("The response content was not as expected", response.getContent(), is(testByteArray));

        verify(audioStream).close();
    }

    @Test
    public void oneTimeStreamIsClosedAndRemovedAfterServed() throws Exception {
        AudioStream audioStream = mock(AudioStream.class);
        AudioFormat audioFormat = mock(AudioFormat.class);
        when(audioStream.getFormat()).thenReturn(audioFormat);
        when(audioFormat.getCodec()).thenReturn(AudioFormat.CODEC_MP3);
        when(audioStream.readNBytes(anyInt())).thenReturn(new byte[] { 1, 2, 3 });

        String url = serveStream(audioStream);
        assertThat(audioServlet.getServedStreams().values().stream().map(StreamServed::audioStream).toList(),
                contains(audioStream));

        getHttpRequest(url).send();

        verify(audioStream).close();
        assertThat(audioServlet.getServedStreams().values().stream().map(StreamServed::audioStream).toList(),
                not(contains(audioStream)));
    }

    @Test
    public void multiTimeStreamIsClosedAfterExpired() throws Exception {
        AtomicInteger cloneCounter = new AtomicInteger();
        ByteArrayAudioStream audioStream = mock(ByteArrayAudioStream.class);
        AudioStream clonedStream = mock(AudioStream.class);
        AudioFormat audioFormat = mock(AudioFormat.class);
        when(audioStream.getFormat()).thenReturn(audioFormat);
        when(audioStream.getClonedStream()).thenAnswer(answer -> {
            cloneCounter.getAndIncrement();
            return clonedStream;
        });
        when(audioStream.readNBytes(anyInt())).thenReturn(new byte[] { 1, 2, 3 });
        when(clonedStream.readNBytes(anyInt())).thenReturn(new byte[] { 1, 2, 3 });
        when(audioFormat.getCodec()).thenReturn(AudioFormat.CODEC_MP3);

        String url = serveStream(audioStream, 2);
        assertThat(audioServlet.getServedStreams().values().stream().map(StreamServed::audioStream).toList(),
                contains(audioStream));

        waitForAssert(() -> {
            try {
                ContentResponse resp = getHttpRequest(url).send();
                assertThat(resp.getStatus(), is(404));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

        });

        verify(audioStream).close();
        assertThat(audioServlet.getServedStreams().values().stream().map(StreamServed::audioStream).toList(),
                not(contains(audioStream)));

        verify(clonedStream, times(cloneCounter.get())).close();
    }

    @Test
    public void streamsAreClosedOnDeactivate() throws Exception {
        AudioStream oneTimeStream = mock(AudioStream.class);
        ByteArrayAudioStream multiTimeStream = mock(ByteArrayAudioStream.class);

        serveStream(oneTimeStream);
        serveStream(multiTimeStream, 10);

        audioServlet.deactivate();

        verify(oneTimeStream).close();
        verify(multiTimeStream).close();
    }
}
