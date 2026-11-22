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
package org.openhab.core.voice.internal.dialog;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;

/**
 * Tests for {@link KSAudioStream}.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class KSAudioStreamTest {
    // 32 bytes per millisecond
    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
            false, 16, null, 16000L, 1);
    private static final AudioFormat STEREO = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
            false, 16, null, 16000L, 2);

    @Test
    public void branchStartsBeforeTheAudioReadAndContinuesWithTheAudioReadNext() throws IOException {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            stream.readNBytes(1600);
            AudioStream branch = stream.branch(Duration.ofMillis(10));
            stream.readAllBytes();

            assertArrayEquals(Arrays.copyOfRange(audio, 1280, 3200), branch.readAllBytes());
            assertEquals(FORMAT, branch.getFormat());
        }
    }

    @Test
    public void branchStartsWithAtMostASecondOfAudio() throws IOException {
        byte[] audio = audio(64000);
        KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT));

        stream.readNBytes(64000);
        AudioStream branch = stream.branch(Duration.ofSeconds(5));
        stream.close();

        assertArrayEquals(Arrays.copyOfRange(audio, 32000, 64000), branch.readAllBytes());
    }

    @Test
    public void branchStartsWithNoMoreAudioThanWasRead() throws IOException {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            stream.readNBytes(320);
            AudioStream branch = stream.branch(Duration.ofMillis(50));
            stream.readAllBytes();

            assertArrayEquals(audio, branch.readAllBytes());
        }
    }

    @Test
    public void branchStartsOnAWholeSample() throws IOException {
        byte[] audio = audio(2000);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, STEREO))) {
            stream.readNBytes(1000);
            // 6.4 bytes would split a 4-byte stereo sample
            AudioStream branch = stream.branch(Duration.ofNanos(100_000));
            stream.readAllBytes();

            assertArrayEquals(Arrays.copyOfRange(audio, 996, 2000), branch.readAllBytes());
        }
    }

    @Test
    public void branchStartsOnAWholeSampleWhenTheAudioReadEndsMidSample() throws IOException {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            stream.readNBytes(1001);
            AudioStream branch = stream.branch(Duration.ZERO);
            stream.readAllBytes();

            assertArrayEquals(Arrays.copyOfRange(audio, 1000, 3200), branch.readAllBytes());
        }
    }

    @Test
    public void branchWithoutAudioBeforeStartsWithTheAudioReadNext() throws IOException {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            stream.readNBytes(1600);
            AudioStream branch = stream.branch(Duration.ofMillis(-10));
            stream.readAllBytes();

            assertArrayEquals(Arrays.copyOfRange(audio, 1600, 3200), branch.readAllBytes());
        }
    }

    @Test
    public void branchWaitsForTheAudioReadNext() throws Exception {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            AudioStream branch = stream.branch(Duration.ZERO);

            CompletableFuture<byte[]> read = CompletableFuture.supplyAsync(() -> {
                try {
                    return branch.readNBytes(320);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            stream.readNBytes(320);

            assertArrayEquals(Arrays.copyOfRange(audio, 0, 320), read.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void branchEndsWithTheSpotterStream() throws IOException {
        byte[] audio = audio(3200);
        KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT));

        stream.readNBytes(1600);
        AudioStream branch = stream.branch(Duration.ofMillis(10));
        stream.readNBytes(320);
        stream.close();

        assertArrayEquals(Arrays.copyOfRange(audio, 1280, 1920), branch.readAllBytes());
        // a branch opened after the end only gets the audio read before it
        assertArrayEquals(Arrays.copyOfRange(audio, 1600, 1920), stream.branch(Duration.ofMillis(10)).readAllBytes());
    }

    @Test
    public void readErrorOnlyEndsTheRunningBranches() throws IOException {
        byte[] audio = audio(3200);
        KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT) {
            private boolean failed;

            @Override
            public int read(byte @Nullable [] b, int off, int len) throws IOException {
                if (!failed) {
                    failed = true;
                    throw new IOException("Read error");
                }
                return super.read(b, off, len);
            }
        });
        AudioStream running = stream.branch(Duration.ZERO);

        assertThrows(IOException.class, () -> stream.read(new byte[320], 0, 320));
        assertEquals(-1, running.read());

        stream.readNBytes(320);
        AudioStream branch = stream.branch(Duration.ofMillis(10));
        stream.readNBytes(320);
        stream.close();

        assertArrayEquals(Arrays.copyOfRange(audio, 0, 640), branch.readAllBytes());
    }

    @Test
    public void closedBranchGetsNoMoreAudio() throws IOException {
        byte[] audio = audio(3200);
        try (KSAudioStream stream = new KSAudioStream(new ByteArrayAudioStream(audio, FORMAT))) {
            AudioStream branch = stream.branch(Duration.ZERO);

            stream.readNBytes(320);
            branch.close();
            stream.readNBytes(320);

            assertEquals(-1, branch.read());
            assertEquals(0, branch.available());
        }
    }

    @Test
    public void onlyRawPcmCanBeBranched() {
        assertTrue(KSAudioStream.isSupported(FORMAT));
        assertFalse(KSAudioStream.isSupported(AudioFormat.MP3));
        assertFalse(KSAudioStream.isSupported(
                new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, 16, null, 16000L, 1)));
        assertFalse(KSAudioStream.isSupported(
                new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false, 16, null, null, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new KSAudioStream(new ByteArrayAudioStream(new byte[0], AudioFormat.MP3)));
    }

    private static byte[] audio(int length) {
        byte[] audio = new byte[length];
        for (int i = 0; i < length; i++) {
            audio[i] = (byte) (i * 7);
        }
        return audio;
    }
}
