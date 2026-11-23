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
package org.openhab.core.voice.internal.tts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.tts.TTSException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.Voice;

/**
 * Unit tests for {@link SpeechSynthesizer}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class SpeechSynthesizerTest {

    private static final AudioFormat WAV_FORMAT = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, true, 16, 705600, 44100L, 1);

    private @NonNullByDefault({}) AudioManager audioManager;
    private @NonNullByDefault({}) TTSService ttsService;
    private @NonNullByDefault({}) Voice voice;
    private @NonNullByDefault({}) AudioSink sink;

    private @NonNullByDefault({}) SpeechSynthesizer speechSynthesizer;

    @BeforeEach
    public void setUp() {
        audioManager = mock(AudioManager.class);
        ttsService = mock(TTSService.class);
        voice = mock(Voice.class);
        sink = mock(AudioSink.class);
        speechSynthesizer = new SpeechSynthesizer(audioManager);

        when(ttsService.getId()).thenReturn("test-tts");
        when(sink.getId()).thenReturn("test-sink");
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void sayDelegatesPlaybackToAudioManager() throws Exception {
        when(ttsService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(sink.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));

        AudioStream audioStream = mock(AudioStream.class);
        when(ttsService.synthesize("hello", voice, WAV_FORMAT)).thenReturn(audioStream);

        PercentType volume = new PercentType(80);
        speechSynthesizer.say("hello", ttsService, voice, sink, volume);

        verify(audioManager).play(audioStream, sink, volume);
    }

    @Test
    public void synthesizeAndPlayThrowsWhenNoCompatibleFormat() {
        when(ttsService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(sink.getSupportedFormats()).thenReturn(Set.of());

        assertThrows(TTSException.class,
                () -> speechSynthesizer.synthesizeAndPlay("hello", ttsService, voice, sink, null));
        verifyNoInteractions(audioManager);
    }

    @Test
    public void sayDoesNotPropagateTTSException() {
        when(ttsService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(sink.getSupportedFormats()).thenReturn(Set.of());

        assertDoesNotThrow(() -> speechSynthesizer.say("hello", ttsService, voice, sink, null));
    }
}
