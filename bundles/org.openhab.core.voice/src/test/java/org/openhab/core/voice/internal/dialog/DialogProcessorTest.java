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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.tts.TTSException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.Voice;
import org.osgi.framework.Bundle;

/**
 * Tests for {@link DialogProcessor}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogProcessorTest {

    @Test
    public void interpreterLoopReturnsFastAfterUnrecoverableInterpretationException()
            throws InterpretationException, TTSException {
        HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
        HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getId()).thenReturn("hli2");

        when(hli1.interpret(any(Locale.class), any(InterpreterContext.class)))
                .thenThrow(new UnrecoverableInterpretationException("unrecoverable error"));

        AudioSource source = mock(AudioSource.class);
        when(source.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        AudioSink sink = mock(AudioSink.class);
        when(sink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sink.getSupportedStreams()).thenReturn(Set.of(AudioStream.class));

        STTService stt = mock(STTService.class);
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        TTSService tts = mock(TTSService.class);
        when(tts.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        Voice voice = mock(Voice.class);
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));
        AudioStream audioStream = mock(AudioStream.class);
        when(tts.synthesize(anyString(), any(Voice.class), any(AudioFormat.class))).thenReturn(audioStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink)
                .withSTT(stt).withTTS(tts).withVoice(voice).withHLIs(List.of(hli1, hli2)).build();

        DialogProcessor.DialogEventListener listener = mock(DialogProcessor.DialogEventListener.class);
        EventPublisher publisher = mock(EventPublisher.class);
        TranslationProvider i18n = mock(TranslationProvider.class);
        Bundle bundle = mock(Bundle.class);

        DialogProcessor processor = new DialogProcessor(context, listener, publisher, new WeakHashMap<>(), i18n,
                bundle);

        processor.sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        verify(hli1, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(hli2, never()).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(tts, times(1)).synthesize(eq("unrecoverable error"), any(Voice.class), any(AudioFormat.class));
    }

    @Test
    public void interpreterLoopContinuesAfterRecoverableInterpretationException()
            throws InterpretationException, TTSException {
        HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
        HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getId()).thenReturn("hli2");

        when(hli1.interpret(any(Locale.class), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("recoverable error"));
        when(hli2.interpret(any(Locale.class), any(InterpreterContext.class))).thenReturn("success answer");

        AudioSource source = mock(AudioSource.class);
        when(source.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        AudioSink sink = mock(AudioSink.class);
        when(sink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sink.getSupportedStreams()).thenReturn(Set.of(AudioStream.class));

        STTService stt = mock(STTService.class);
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        TTSService tts = mock(TTSService.class);
        when(tts.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        Voice voice = mock(Voice.class);
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));
        AudioStream audioStream = mock(AudioStream.class);
        when(tts.synthesize(anyString(), any(Voice.class), any(AudioFormat.class))).thenReturn(audioStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink)
                .withSTT(stt).withTTS(tts).withVoice(voice).withHLIs(List.of(hli1, hli2)).build();

        DialogProcessor.DialogEventListener listener = mock(DialogProcessor.DialogEventListener.class);
        EventPublisher publisher = mock(EventPublisher.class);
        TranslationProvider i18n = mock(TranslationProvider.class);
        Bundle bundle = mock(Bundle.class);

        DialogProcessor processor = new DialogProcessor(context, listener, publisher, new WeakHashMap<>(), i18n,
                bundle);

        processor.sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        verify(hli1, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(hli2, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(tts, times(1)).synthesize(eq("success answer"), any(Voice.class), any(AudioFormat.class));
    }
}
