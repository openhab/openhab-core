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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTErrorEvent;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.text.SystemPromptEnricher;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.RecognitionStartEvent;
import org.openhab.core.voice.stt.RecognitionStopEvent;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.UnrecoverableInterpretationException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.Voice;

/**
 * Tests for {@link DialogProcessor}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogProcessorTest {

    private @NonNullByDefault({}) InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer = mock(SpeechSynthesizer.class);
    private final DialogAudioDriver audioDriver = mock(DialogAudioDriver.class);
    private final DialogFeedbackService feedbackService = mock(DialogFeedbackService.class);
    private final VoiceI18nProvider i18nProvider = mock(VoiceI18nProvider.class);
    private final DialogProcessor.DialogEventListener eventListener = mock(DialogProcessor.DialogEventListener.class);

    @BeforeEach
    public void setUp() {
        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);

        SystemPromptEnricher systemPromptEnricher = mock(SystemPromptEnricher.class);
        when(systemPromptEnricher.enrichSystemPrompt(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        interpretationService = new InterpretationService(localeProvider, systemPromptEnricher);
        when(eventListener.onAcquireGroup(any())).thenReturn(true);
    }

    private DialogContext createTestContext(List<HumanLanguageInterpreter> hlis) {
        AudioSource source = mock(AudioSource.class);
        AudioSink sink = mock(AudioSink.class);
        KSService ks = mock(KSService.class);
        STTService stt = mock(STTService.class);
        TTSService tts = mock(TTSService.class);
        Voice voice = mock(Voice.class);
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));
        List<HumanLanguageInterpreter> interpreters = hlis.isEmpty() ? List.of(mock(HumanLanguageInterpreter.class))
                : hlis;

        return new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink).withKS(ks)
                .withSTT(stt).withTTS(tts).withVoice(voice).withHLIs(interpreters).build();
    }

    private DialogContext createTestContext(List<HumanLanguageInterpreter> hlis, boolean withDt) {
        AudioSource source = mock(AudioSource.class);
        AudioSink sink = mock(AudioSink.class);
        KSService ks = mock(KSService.class);
        STTService stt = mock(STTService.class);
        TTSService tts = mock(TTSService.class);
        Voice voice = mock(Voice.class);
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));
        List<HumanLanguageInterpreter> interpreters = hlis.isEmpty() ? List.of(mock(HumanLanguageInterpreter.class))
                : hlis;

        DialogContext.Builder builder = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source)
                .withSink(sink).withSTT(stt).withTTS(tts).withVoice(voice).withHLIs(interpreters);

        if (withDt) {
            builder.withKS(ks);
        }

        return builder.build();
    }

    @Test
    public void interpreterLoopReturnsFastAfterUnrecoverableInterpretationException()
            throws STTException, InterpretationException {
        HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
        HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getId()).thenReturn("hli2");

        when(hli1.interpret(any(Locale.class), any(InterpreterContext.class)))
                .thenThrow(new UnrecoverableInterpretationException("unrecoverable error"));

        DialogContext context = createTestContext(List.of(hli1, hli2));

        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        verify(hli1, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(hli2, never()).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(speechSynthesizer, times(1)).say(eq("unrecoverable error"), eq(context.tts()),
                eq(Objects.requireNonNull(context.voice())), eq(context.sink()), eq(null));
    }

    @Test
    public void interpreterLoopContinuesAfterRecoverableInterpretationException()
            throws STTException, InterpretationException {
        HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
        HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getId()).thenReturn("hli2");

        when(hli1.interpret(any(Locale.class), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("recoverable error"));
        when(hli2.interpret(any(Locale.class), any(InterpreterContext.class))).thenReturn("success answer");

        DialogContext context = createTestContext(List.of(hli1, hli2));

        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        verify(hli1, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(hli2, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(speechSynthesizer, times(1)).say(eq("success answer"), eq(context.tts()), eq(context.voice()),
                eq(context.sink()), eq(null));
    }

    @Test
    public void startTransitionsToWaitingForTriggerAndPlaysStartSound() throws DTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        processor.start();

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver).startTrigger(any(KSListener.class));
        verify(feedbackService).playStartSound();
    }

    @Test
    public void startSimpleDialogTransitionsToListeningAndPlaysListeningSound() throws STTException {
        DialogContext context = createTestContext(List.of(), false);
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        processor.startSimpleDialog();

        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        assertTrue(processor.isProcessing());
        verify(feedbackService).playListeningSound();
        verify(audioDriver).startRecognition(any(STTListener.class));
    }

    @Test
    public void dtEventReceivedTriggeredStartsSimpleDialog() throws STTException, DTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());

        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(feedbackService).playListeningSound();
        verify(audioDriver).startRecognition(any(STTListener.class));
    }

    @Test
    public void recognitionEventsToggleFeedbackServiceListening() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());
        STTListener sttListener = sttListenerCaptor.getValue();

        sttListener.sttEventReceived(new RecognitionStartEvent());
        verify(feedbackService).setListening(true);

        sttListener.sttEventReceived(new RecognitionStopEvent());
        verify(feedbackService).setListening(false);
    }

    @Test
    public void stopTransitionsToIdleAndReleasesResources() {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        processor.stop();

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(audioDriver).stopAll();
        verify(feedbackService).setListening(false);
        verify(feedbackService).playStopSound();
        verify(eventListener).onReleaseGroup(context);
        verify(eventListener).onDialogStopped(context);
    }

    @Test
    public void startSimpleDialogDoesNotStartWhenGroupAcquisitionFails() {
        DialogContext context = createTestContext(List.of(), false);
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        when(eventListener.onAcquireGroup(any())).thenReturn(false);

        processor.startSimpleDialog();

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(audioDriver);
        verifyNoInteractions(feedbackService);
    }

    @Test
    public void startSimpleDialogFinishesDialogImmediatelyWhenRecognitionFails() throws STTException {
        DialogContext context = createTestContext(List.of(), false);
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        when(audioDriver.startRecognition(any())).thenThrow(STTException.class);
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());

        processor.startSimpleDialog();

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(feedbackService);
        verify(eventListener).onReleaseGroup(context);
        verify(eventListener).onDialogStopped(context);
    }

    @Test
    public void finishDialogReleasesGroupForPersistentDialog() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);
        verify(eventListener, never()).onDialogStopped(context);
    }

    @Test
    public void finishDialogCommitsStateBeforeReleasingGroupAndDoesNotOverwriteNewSession() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        doAnswer(invocation -> {
            // Resting state must already be committed when group is released
            assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
            assertFalse(processor.isProcessing());
            // Starting a new interaction immediately upon group release must stay in LISTENING
            processor.startSimpleDialog();
            return null;
        }).when(eventListener).onReleaseGroup(context);

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        assertTrue(processor.isProcessing());
    }

    @Test
    public void stopDuringListeningRejectsSpeechRecognitionEvents() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        processor.stop();
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));
        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("error"));

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void stopDuringListeningRejectsSpeechRecognitionErrorEvent() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        processor.stop();
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("recognition failed"));

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void stopDuringListeningRejectsRecognitionStartAndStopEvents() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        processor.stop();
        reset(feedbackService);

        sttListenerCaptor.getValue().sttEventReceived(new RecognitionStartEvent());
        sttListenerCaptor.getValue().sttEventReceived(new RecognitionStopEvent());

        verifyNoInteractions(feedbackService);
    }

    @Test
    public void stopDuringInterpretationDiscardsResultAndDoesNotSpeakOrRestoreWaitingForTrigger()
            throws STTException, InterruptedException, InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("hli");

        CountDownLatch enterInterpretLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);

        when(hli.interpret(any(Locale.class), any(InterpreterContext.class))).thenAnswer(invocation -> {
            enterInterpretLatch.countDown();
            assertTrue(stopLatch.await(5, TimeUnit.SECONDS));
            return "answer";
        });

        DialogContext context = createTestContext(List.of(hli));
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        Thread thread = new Thread(
                () -> sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f)));
        thread.start();

        assertTrue(enterInterpretLatch.await(5, TimeUnit.SECONDS));
        processor.stop();
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        stopLatch.countDown();

        thread.join(5000);

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void stopDuringInterpretationDiscardsInterpretationException()
            throws STTException, InterruptedException, InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("hli");

        CountDownLatch enterInterpretLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);

        when(hli.interpret(any(Locale.class), any(InterpreterContext.class))).thenAnswer(invocation -> {
            enterInterpretLatch.countDown();
            assertTrue(stopLatch.await(5, TimeUnit.SECONDS));
            throw new InterpretationException("some error");
        });

        DialogContext context = createTestContext(List.of(hli));
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        Thread thread = new Thread(() -> {
            sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));
        });
        thread.start();

        assertTrue(enterInterpretLatch.await(5, TimeUnit.SECONDS));
        processor.stop();
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        stopLatch.countDown();

        thread.join(5000);

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void stopDuringStopRecognitionInCallbackDoesNotInterpretOrRestoreState()
            throws STTException, InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("hli");

        DialogContext context = createTestContext(List.of(hli));
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        doAnswer(invocation -> {
            processor.stop();
            return null;
        }).when(audioDriver).stopRecognition();

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        assertFalse(processor.isProcessing());
        verify(hli, never()).interpret(any(Locale.class), any(InterpreterContext.class));
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void triggerIgnoredWhenProcessorNotWaitingForTrigger() throws DTException, STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        // First trigger transitions to LISTENING
        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());

        // Second trigger while in LISTENING is ignored
        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(audioDriver, times(1)).startRecognition(any(STTListener.class));
    }

    @Test
    public void dtErrorEventSpeaksWhenWaitingForTrigger() throws DTException, STTException {
        DialogContext context = createTestContext(List.of());
        when(i18nProvider.getFormattedText(eq("error.ks-error"), any(), eq("something went wrong")))
                .thenReturn("Error: something went wrong");

        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        ksListenerCaptor.getValue().dtEventReceived(new DTErrorEvent("something went wrong"));

        verify(audioDriver, never()).startRecognition(any(STTListener.class));
        verify(speechSynthesizer).say(eq("Error: something went wrong"), eq(context.tts()), eq(context.voice()),
                eq(context.sink()), eq(null));
    }

    @Test
    public void dtErrorEventIgnoredWhenStoppedOrNotWaitingForTrigger() throws DTException, STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        processor.stop();
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());

        ksListenerCaptor.getValue().dtEventReceived(new DTErrorEvent("something went wrong"));

        verify(audioDriver, never()).startRecognition(any(STTListener.class));
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void isProcessingReturnsTrueDuringListeningInterpretingAndSpeaking()
            throws STTException, InterruptedException, InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        CountDownLatch enterInterpretLatch = new CountDownLatch(1);
        CountDownLatch finishInterpretLatch = new CountDownLatch(1);
        when(hli.interpret(any(Locale.class), any(InterpreterContext.class))).thenAnswer(invocation -> {
            enterInterpretLatch.countDown();
            finishInterpretLatch.await(5, TimeUnit.SECONDS);
            return "Answer";
        });

        CountDownLatch enterSpeakLatch = new CountDownLatch(1);
        CountDownLatch finishSpeakLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            enterSpeakLatch.countDown();
            finishSpeakLatch.await(5, TimeUnit.SECONDS);
            return null;
        }).when(speechSynthesizer).say(any(), any(), any(), any(), any());

        DialogContext context = createTestContext(List.of(hli));
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        // IDLE
        assertFalse(processor.isProcessing());

        // WAITING_FOR_TRIGGER
        processor.start();
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        assertFalse(processor.isProcessing());

        // LISTENING
        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        assertTrue(processor.isProcessing());

        // INTERPRETING
        Thread thread = new Thread(() -> {
            sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));
        });
        thread.start();

        assertTrue(enterInterpretLatch.await(5, TimeUnit.SECONDS));
        assertEquals(DialogProcessor.DialogState.INTERPRETING, processor.getState());
        assertTrue(processor.isProcessing());

        // SPEAKING
        finishInterpretLatch.countDown();
        assertTrue(enterSpeakLatch.await(5, TimeUnit.SECONDS));
        assertEquals(DialogProcessor.DialogState.SPEAKING, processor.getState());
        assertTrue(processor.isProcessing());

        // Finish back to IDLE / WAITING_FOR_TRIGGER
        finishSpeakLatch.countDown();
        thread.join(5000);
        assertFalse(processor.isProcessing());
    }
}
