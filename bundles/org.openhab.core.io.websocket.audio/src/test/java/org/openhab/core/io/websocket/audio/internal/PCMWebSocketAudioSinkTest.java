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
package org.openhab.core.io.websocket.audio.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;

/**
 * Tests for {@link PCMWebSocketAudioSink}.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class PCMWebSocketAudioSinkTest {
    // 32000 bytes a second
    private static final AudioFormat PCM = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
            false, 16, null, 16000L, 1);

    @Test
    public void processWaitsUntilAStreamOfUnknownLengthHasPlayed() {
        PCMWebSocketConnection connection = mock(PCMWebSocketConnection.class);
        PCMWebSocketAudioSink sink = new PCMWebSocketAudioSink("sink", "Sink", connection, null, null, null);

        long start = System.nanoTime();
        sink.process(new StreamOfUnknownLength(new byte[16000]));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        verify(connection, atLeastOnce()).sendAudio(any(), any());
        assertTrue(elapsed.toMillis() >= 450, "returned after " + elapsed.toMillis() + " ms");
    }

    private static class StreamOfUnknownLength extends AudioStream {
        private final ByteArrayInputStream input;

        StreamOfUnknownLength(byte[] audio) {
            this.input = new ByteArrayInputStream(audio);
        }

        @Override
        public AudioFormat getFormat() {
            return PCM;
        }

        @Override
        public int read() {
            return input.read();
        }

        @Override
        public int read(byte @Nullable [] b, int off, int len) {
            return input.read(Objects.requireNonNull(b), off, len);
        }
    }
}
