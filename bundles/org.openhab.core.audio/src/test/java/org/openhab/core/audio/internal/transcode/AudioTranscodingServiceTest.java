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
package org.openhab.core.audio.internal.transcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;

/**
 * Unit tests for {@link AudioTranscodingServiceImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioTranscodingServiceTest {

    private @NonNullByDefault({}) AudioTranscodingServiceImpl service;

    @BeforeEach
    public void setup() {
        service = new AudioTranscodingServiceImpl();
    }

    private AudioStream createMockStream(AudioFormat format) {
        return new AudioStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });

            @Override
            public AudioFormat getFormat() {
                return format;
            }

            @Override
            public int read() {
                return in.read();
            }
        };
    }

    @Test
    public void testPassThroughWhenAlreadySupported() throws AudioTranscodingException {
        AudioStream source = createMockStream(AudioFormat.WAV);
        AudioStream result = service.transcodeToSupported(source, Set.of(AudioFormat.WAV, AudioFormat.MP3));
        assertSame(source, result);
    }

    @Test
    public void testDirectTranscoding() throws AudioTranscodingException {
        AudioTranscoder transcoder = mock(AudioTranscoder.class);
        when(transcoder.getId()).thenReturn("pcm-to-flac");
        when(transcoder.getSupportedSourceFormats()).thenReturn(Set.of(AudioFormat.PCM_SIGNED));
        when(transcoder.getSupportedTargetFormats()).thenReturn(Set.of(AudioFormat.FLAC));
        when(transcoder.canTranscode(AudioFormat.PCM_SIGNED, AudioFormat.FLAC)).thenReturn(true);
        when(transcoder.getCost()).thenReturn(15);

        AudioStream source = createMockStream(AudioFormat.PCM_SIGNED);
        AudioStream target = createMockStream(AudioFormat.FLAC);
        when(transcoder.transcode(source, AudioFormat.FLAC)).thenReturn(target);

        service.addAudioTranscoder(transcoder);

        assertTrue(service.canTranscode(AudioFormat.PCM_SIGNED, AudioFormat.FLAC));
        AudioStream result = service.transcode(source, AudioFormat.FLAC);
        assertSame(target, result);
    }

    @Test
    public void testMultiHopTranscoding() throws AudioTranscodingException {
        // Step 1: WAV -> PCM
        AudioTranscoder wavToPcm = mock(AudioTranscoder.class);
        when(wavToPcm.getId()).thenReturn("wav-to-pcm");
        when(wavToPcm.getSupportedSourceFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(wavToPcm.getSupportedTargetFormats()).thenReturn(Set.of(AudioFormat.PCM_SIGNED));
        when(wavToPcm.canTranscode(AudioFormat.WAV, AudioFormat.PCM_SIGNED)).thenReturn(true);
        when(wavToPcm.getCost()).thenReturn(1);

        // Step 2: PCM -> FLAC
        AudioTranscoder pcmToFlac = mock(AudioTranscoder.class);
        when(pcmToFlac.getId()).thenReturn("pcm-to-flac");
        when(pcmToFlac.getSupportedSourceFormats()).thenReturn(Set.of(AudioFormat.PCM_SIGNED));
        when(pcmToFlac.getSupportedTargetFormats()).thenReturn(Set.of(AudioFormat.FLAC));
        when(pcmToFlac.canTranscode(AudioFormat.PCM_SIGNED, AudioFormat.FLAC)).thenReturn(true);
        when(pcmToFlac.getCost()).thenReturn(15);

        service.addAudioTranscoder(wavToPcm);
        service.addAudioTranscoder(pcmToFlac);

        AudioStream wavStream = createMockStream(AudioFormat.WAV);
        AudioStream pcmStream = createMockStream(AudioFormat.PCM_SIGNED);
        AudioStream flacStream = createMockStream(AudioFormat.FLAC);

        when(wavToPcm.transcode(wavStream, AudioFormat.PCM_SIGNED)).thenReturn(pcmStream);
        when(pcmToFlac.transcode(pcmStream, AudioFormat.FLAC)).thenReturn(flacStream);

        assertTrue(service.canTranscode(AudioFormat.WAV, AudioFormat.FLAC));

        AudioStream result = service.transcodeToSupported(wavStream, Set.of(AudioFormat.FLAC));
        assertSame(flacStream, result);
    }

    @Test
    public void testNoRouteThrowsException() {
        AudioStream source = createMockStream(AudioFormat.MP3);
        assertThrows(AudioTranscodingException.class, () -> {
            service.transcodeToSupported(source, Set.of(AudioFormat.FLAC));
        });
    }
}
