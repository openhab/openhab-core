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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.BasicDTService;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
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
    private static final AudioFormat PCM = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
            false, 16, null, 16000L, 1);
    private static final AudioFormat WAVE = new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED,
            false, 16, null, 16000L, 1);

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
    public void startTriggerAndStopTriggerManagesStreamAndHandle() throws AudioException, DTException, IOException {
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
    public void startTriggerAbortsHandleIfStoppedDuringSpot() throws AudioException, DTException, IOException {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(ksService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        when(ksService.spot(eq(ksListener), eq(audioStream), eq(Locale.ENGLISH), eq("keyword")))
                .thenAnswer(invocation -> {
                    driver.stopTrigger();
                    return ksHandle;
                });

        DTServiceHandle handle = driver.startTrigger(ksListener);
        assertNull(handle);
        verify(ksHandle).abort();
        verify(audioStream).close();
    }

    @Test
    public void startTriggerWithBasicDTAbortsHandleIfStoppedDuringRegisterListener() throws DTException {
        BasicDTService basicDT = mock(BasicDTService.class);
        DTServiceHandle dtHandle = mock(DTServiceHandle.class);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withDT(basicDT).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        when(basicDT.registerListener(eq(ksListener))).thenAnswer(invocation -> {
            driver.stopTrigger();
            return dtHandle;
        });

        DTServiceHandle handle = driver.startTrigger(ksListener);
        assertNull(handle);
        verify(dtHandle).abort();
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

    @Test
    public void startRecognitionAbortsHandleIfStoppedDuringRecognize()
            throws AudioException, STTException, IOException {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        when(sttService.recognize(eq(sttListener), eq(audioStream), eq(Locale.ENGLISH))).thenAnswer(invocation -> {
            driver.stopRecognition();
            return sttHandle;
        });

        STTServiceHandle handle = driver.startRecognition(sttListener);
        assertNull(handle);
        verify(sttHandle).abort();
        verify(audioStream).close();
    }

    @Test
    public void startRecognitionDoesNotCloseNewerStreamWhenSynchronousCallbackStartsNewSession()
            throws AudioException, STTException, IOException {
        AudioStream firstStream = mock(AudioStream.class);
        AudioStream secondStream = mock(AudioStream.class);
        STTServiceHandle firstHandle = mock(STTServiceHandle.class);
        STTServiceHandle secondHandle = mock(STTServiceHandle.class);
        STTListener secondListener = mock(STTListener.class);

        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(firstStream, secondStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);

        when(sttService.recognize(eq(secondListener), eq(secondStream), eq(Locale.ENGLISH))).thenReturn(secondHandle);
        when(sttService.recognize(eq(sttListener), eq(firstStream), eq(Locale.ENGLISH))).thenAnswer(invocation -> {
            // Simulate terminal callback stopping first recognition and starting a second session before first returns
            driver.stopRecognition();
            STTServiceHandle nestedHandle = driver.startRecognition(secondListener);
            assertNotNull(nestedHandle);
            return firstHandle;
        });

        STTServiceHandle outerResult = driver.startRecognition(sttListener);
        assertNull(outerResult);

        // First session's handle and stream are cleaned up, while second session's stream remains open
        verify(firstHandle).abort();
        verify(firstStream).close();
        verify(secondHandle, never()).abort();
        verify(secondStream, never()).close();

        // Stopping recognition now aborts and closes the second session's handle and stream
        driver.stopRecognition();
        verify(secondHandle).abort();
        verify(secondStream).close();
    }

    @Test
    public void stopRecognitionForAListenerLeavesALaterRecognitionRunning()
            throws AudioException, STTException, IOException {
        AudioStream firstStream = mock(AudioStream.class);
        AudioStream secondStream = mock(AudioStream.class);
        STTServiceHandle secondHandle = mock(STTServiceHandle.class);
        STTListener secondListener = mock(STTListener.class);
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(firstStream, secondStream);
        when(sttService.recognize(eq(sttListener), eq(firstStream), eq(Locale.ENGLISH))).thenReturn(sttHandle);
        when(sttService.recognize(eq(secondListener), eq(secondStream), eq(Locale.ENGLISH))).thenReturn(secondHandle);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        driver.startRecognition(sttListener);
        driver.startRecognition(secondListener);
        verify(sttHandle).abort();
        verify(firstStream).close();

        driver.stopRecognition(sttListener);
        verify(secondHandle, never()).abort();
        verify(secondStream, never()).close();

        driver.stopRecognition(secondListener);
        verify(secondHandle).abort();
        verify(secondStream).close();
    }

    @Test
    public void stopRecognitionForAListenerAbortsItsRecognitionWhileItStarts()
            throws AudioException, STTException, IOException {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        when(sttService.recognize(eq(sttListener), eq(audioStream), eq(Locale.ENGLISH))).thenAnswer(invocation -> {
            driver.stopRecognition(sttListener);
            return sttHandle;
        });

        assertNull(driver.startRecognition(sttListener));
        verify(sttHandle).abort();
        verify(audioStream).close();
    }

    @Test
    public void cancelledStartRecognitionLeavesTheCurrentRecognitionRunning()
            throws AudioException, STTException, IOException {
        STTListener secondListener = mock(STTListener.class);
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(audioStream);
        when(sttService.recognize(eq(sttListener), eq(audioStream), eq(Locale.ENGLISH))).thenReturn(sttHandle);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        driver.startRecognition(sttListener);

        assertNull(driver.startRecognition(secondListener, null, () -> true));
        verify(sttHandle, never()).abort();
        verify(audioStream, never()).close();
        verify(audioSource, times(1)).getInputStream(any());
        verify(sttService, never()).recognize(eq(secondListener), any(), any());
    }

    @Test
    public void stopAllAbortsTriggerAndRecognitionAndClosesStreams() throws Exception {
        AudioStream streamKS = mock(AudioStream.class);
        AudioStream streamSTT = mock(AudioStream.class);
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(ksService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(sttService.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(audioSource.getInputStream(any())).thenReturn(streamKS, streamSTT);
        when(ksService.spot(eq(ksListener), eq(streamKS), eq(Locale.ENGLISH), eq("keyword"))).thenReturn(ksHandle);
        when(sttService.recognize(eq(sttListener), eq(streamSTT), eq(Locale.ENGLISH))).thenReturn(sttHandle);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource)
                .withSink(audioSink).withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();

        DialogAudioDriver driver = new DialogAudioDriver(context);
        driver.startTrigger(ksListener);
        driver.startRecognition(sttListener);

        driver.stopAll();
        verify(ksHandle).abort();
        verify(streamKS).close();
        verify(sttHandle).abort();
        verify(streamSTT).close();
    }

    @Test
    public void startRecognitionWithAudioAfterKeywordStartsWhereTheKeywordEnded() throws Exception {
        byte[] audio = audio(3200);
        DialogAudioDriver driver = new DialogAudioDriver(keywordContext(audio, PCM));

        driver.startTrigger(ksListener);
        AudioStream ksStream = spottedStream();
        ksStream.readNBytes(1600);
        driver.startRecognition(sttListener, Duration.ofMillis(10));
        ksStream.readAllBytes();

        assertArrayEquals(Arrays.copyOfRange(audio, 1280, 3200), recognizedStream().readAllBytes());
        verify(audioSource, times(1)).getInputStream(any());
    }

    @Test
    public void startRecognitionWithoutAudioAfterKeywordListensToTheSource() throws Exception {
        DialogAudioDriver driver = new DialogAudioDriver(keywordContext(audio(3200), PCM));

        driver.startTrigger(ksListener);
        spottedStream().readNBytes(1600);
        driver.startRecognition(sttListener);

        verify(audioSource, times(2)).getInputStream(any());
        assertEquals(3200, recognizedStream().readAllBytes().length);
    }

    @Test
    public void startRecognitionWithAudioInAContainerListensToTheSource() throws Exception {
        DialogAudioDriver driver = new DialogAudioDriver(keywordContext(audio(3200), WAVE));

        driver.startTrigger(ksListener);
        spottedStream().readNBytes(1600);
        driver.startRecognition(sttListener, Duration.ofMillis(10));

        verify(audioSource, times(2)).getInputStream(any());
        assertEquals(3200, recognizedStream().readAllBytes().length);
    }

    private DialogContext keywordContext(byte[] audio, AudioFormat format) throws Exception {
        when(audioSource.getSupportedFormats()).thenReturn(Set.of(format));
        when(audioSource.getInputStream(any())).thenAnswer(invocation -> new ByteArrayAudioStream(audio, format));
        when(ksService.getSupportedFormats()).thenReturn(Set.of(format));
        when(ksService.spot(any(), any(), any(), any())).thenReturn(ksHandle);
        when(sttService.getSupportedFormats()).thenReturn(Set.of(format));
        when(sttService.recognize(any(), any(), any())).thenReturn(sttHandle);
        return new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(audioSource).withSink(audioSink)
                .withKS(ksService).withSTT(sttService).withTTS(ttsService).withHLI(hli).build();
    }

    private AudioStream spottedStream() throws Exception {
        ArgumentCaptor<AudioStream> stream = ArgumentCaptor.forClass(AudioStream.class);
        verify(ksService).spot(any(), stream.capture(), any(), any());
        return stream.getValue();
    }

    private AudioStream recognizedStream() throws Exception {
        ArgumentCaptor<AudioStream> stream = ArgumentCaptor.forClass(AudioStream.class);
        verify(sttService).recognize(any(), stream.capture(), any());
        return stream.getValue();
    }

    private static byte[] audio(int length) {
        byte[] audio = new byte[length];
        for (int i = 0; i < length; i++) {
            audio[i] = (byte) (i * 7);
        }
        return audio;
    }
}
