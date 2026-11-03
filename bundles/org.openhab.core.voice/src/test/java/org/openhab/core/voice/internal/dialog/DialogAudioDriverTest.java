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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSException;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceHandle;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceHandle;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.tts.TTSService;

/**
 * Tests for {@link DialogAudioDriver}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogAudioDriverTest {

    private final AudioSource audioSource = mock(AudioSource.class);
    private final AudioSink audioSink = mock(AudioSink.class);
    private final KSService ksService = mock(KSService.class);
    private final STTService sttService = mock(STTService.class);
    private final TTSService ttsService = mock(TTSService.class);
    private final HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
    private final KSListener ksListener = mock(KSListener.class);
    private final STTListener sttListener = mock(STTListener.class);
    private final AudioStream audioStream = mock(AudioStream.class);
    private final KSServiceHandle ksHandle = mock(KSServiceHandle.class);
    private final STTServiceHandle sttHandle = mock(STTServiceHandle.class);

    @Test
    public void startTriggerAndStopTriggerManagesStreamAndHandle() throws AudioException, KSException, IOException {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(ksService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);
        when(ksService.spot(eq(ksListener), eq(audioStream), eq(Locale.ENGLISH), eq("keyword"))).thenReturn(ksHandle);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        DTServiceHandle handle = driver.startTrigger(ksListener);

        assertNotNull(handle);
        verify(ksService).spot(eq(ksListener), eq(audioStream), eq(Locale.ENGLISH), eq("keyword"));

        driver.stopTrigger();
        verify(ksHandle).abort();
        verify(audioStream).close();
    }

    @Test
    public void startRecognitionAndStopRecognitionManagesStreamAndHandle()
            throws AudioException, STTException, IOException {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);
        when(sttService.recognize(eq(sttListener), eq(audioStream), eq(Locale.ENGLISH))).thenReturn(sttHandle);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        STTServiceHandle handle = driver.startRecognition(sttListener);

        assertNotNull(handle);
        verify(sttService).recognize(eq(sttListener), eq(audioStream), eq(Locale.ENGLISH));

        driver.stopRecognition();
        verify(sttHandle).abort();
        verify(audioStream).close();
    }
}
