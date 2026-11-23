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
package org.openhab.core.voice.internal.stt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceHandle;
import org.openhab.core.voice.stt.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;

/**
 * Unit tests for {@link AudioTranscriber}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioTranscriberTest {

    private static final AudioFormat WAV_FORMAT = new AudioFormat(AudioFormat.CONTAINER_WAVE,
            AudioFormat.CODEC_PCM_SIGNED, true, 16, 705600, 44100L, 1);

    private @NonNullByDefault({}) STTService sttService;
    private @NonNullByDefault({}) AudioStream audioStream;
    private @NonNullByDefault({}) AudioSource audioSource;
    private @NonNullByDefault({}) STTServiceHandle sttHandle;

    private @NonNullByDefault({}) AudioTranscriber transcriber;

    @BeforeEach
    public void setUp() {
        sttService = mock(STTService.class);
        audioStream = mock(AudioStream.class);
        audioSource = mock(AudioSource.class);
        sttHandle = mock(STTServiceHandle.class);

        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);

        transcriber = new AudioTranscriber(localeProvider);

        when(sttService.getId()).thenReturn("test-stt");
        when(audioStream.getFormat()).thenReturn(WAV_FORMAT);
    }

    @Test
    public void transcribeStreamSuccess() throws Exception {
        when(sttService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        doAnswer(invocation -> {
            STTListener listener = invocation.getArgument(0);
            listener.sttEventReceived(new SpeechRecognitionEvent("hello world", 0.9f));
            return sttHandle;
        }).when(sttService).recognize(any(STTListener.class), eq(audioStream), eq(Locale.ENGLISH));

        String result = transcriber.transcribe(audioStream, sttService, Locale.ENGLISH);

        assertEquals("hello world", result);
    }

    @Test
    public void transcribeStreamIncompatibleFormatReturnsEmpty() {
        when(sttService.getSupportedFormats()).thenReturn(Set.of());

        String result = transcriber.transcribe(audioStream, sttService, Locale.ENGLISH);

        assertEquals("", result);
    }

    @Test
    public void transcribeStreamErrorEventReturnsEmpty() throws Exception {
        when(sttService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        doAnswer(invocation -> {
            STTListener listener = invocation.getArgument(0);
            listener.sttEventReceived(new SpeechRecognitionErrorEvent("error recognized"));
            return sttHandle;
        }).when(sttService).recognize(any(STTListener.class), eq(audioStream), eq(Locale.ENGLISH));

        String result = transcriber.transcribe(audioStream, sttService, Locale.ENGLISH);

        assertEquals("", result);
    }

    @Test
    public void transcribeSourceSuccess() throws Exception {
        when(audioSource.getId()).thenReturn("source-1");
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(audioSource.getInputStream(any(AudioFormat.class))).thenReturn(audioStream);

        doAnswer(invocation -> {
            STTListener listener = invocation.getArgument(0);
            listener.sttEventReceived(new SpeechRecognitionEvent("source transcription", 0.95f));
            return sttHandle;
        }).when(sttService).recognize(any(STTListener.class), eq(audioStream), eq(Locale.ENGLISH));

        String result = transcriber.transcribe(audioSource, sttService, Locale.ENGLISH);

        assertEquals("source transcription", result);
    }

    @Test
    public void transcribeSourceIncompatibleFormatReturnsEmpty() {
        when(audioSource.getId()).thenReturn("source-1");
        when(audioSource.getSupportedFormats()).thenReturn(Set.of());
        when(sttService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));

        String result = transcriber.transcribe(audioSource, sttService, Locale.ENGLISH);

        assertEquals("", result);
    }

    @Test
    public void transcribeThrowsSTTExceptionReturnsEmpty() throws Exception {
        when(sttService.getSupportedFormats()).thenReturn(Set.of(WAV_FORMAT));
        when(sttService.recognize(any(), any(), any())).thenThrow(new STTException("STT failed"));

        String result = transcriber.transcribe(audioStream, sttService, Locale.ENGLISH);

        assertEquals("", result);
    }
}
