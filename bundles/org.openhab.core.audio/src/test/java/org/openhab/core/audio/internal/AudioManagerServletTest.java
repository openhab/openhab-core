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

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.internal.fake.AudioSinkFake;

/**
 * OSGi test for {@link AudioManagerImpl}
 *
 * @author Petar Valchev - Initial contribution
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 * @author Henning Treu - extract servlet tests
 */
public class AudioManagerServletTest extends AbstractAudioServletTest {

    private AudioManagerImpl audioManager;

    private AudioSinkFake audioSink;

    @Before
    public void setup() {
        audioManager = new AudioManagerImpl();
        audioSink = new AudioSinkFake();
    }

    @Test
    public void audioManagerProcessesMultitimeStreams() throws Exception {
        audioManager.addAudioSink(audioSink);
        int streamTimeout = 10;
        assertServedStream(streamTimeout);
    }

    @Test
    public void audioManagerProcessesOneTimeStream() throws Exception {
        audioManager.addAudioSink(audioSink);
        assertServedStream(null);
    }

    @Test
    public void audioManagerDoesNotProcessStreamsIfThereIsNoRegisteredSink() throws Exception {
        int streamTimeout = 10;
        assertServedStream(streamTimeout);
    }

    private void assertServedStream(Integer timeInterval) throws Exception {
        AudioStream audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED);
        String url = serveStream(audioStream, timeInterval);

        audioManager.stream(url, audioSink.getId());

        if (audioManager.getSink() == audioSink) {
            assertThat("The streamed url was not as expected", ((URLAudioStream) audioSink.audioStream).getURL(),
                    is(url));
        } else {
            assertThat(String.format("The sink %s received an unexpected stream", audioSink.getId()),
                    audioSink.audioStream, is(nullValue()));
        }
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
