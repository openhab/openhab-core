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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTErrorEvent;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSpottedEvent;
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
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);

    @BeforeEach
    public void setUp() {
        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);

        SystemPromptEnricher systemPromptEnricher = mock(SystemPromptEnricher.class);
        when(systemPromptEnricher.enrich(any(), any(), any()))
                .thenAnswer(invocation -> new SystemPromptEnricher.EnrichedPrompt(invocation.getArgument(0),
                        invocation.getArgument(1)));

        interpretationService = new InterpretationService(localeProvider, systemPromptEnricher);
        when(eventListener.onAcquireGroup(any())).thenReturn(true);
        when(eventListener.shouldSpeakRecognitionErrors(any())).thenReturn(true);
        doAnswer(invocation -> mock(ScheduledFuture.class)).when(scheduler).schedule(any(Runnable.class), anyLong(),
                any(TimeUnit.class));
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
        verify(feedbackService, timeout(5000)).playListeningSound();
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
        verify(feedbackService, timeout(5000)).playListeningSound();
        verify(audioDriver).startRecognition(any(STTListener.class));
    }

    @Test
    public void keywordSpottedWithAudioAfterKeywordStartsRecognitionWhereTheKeywordEnded()
            throws STTException, DTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        ksListenerCaptor.getValue().ksEventReceived(new KSpottedEvent(Duration.ofMillis(10)));

        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(audioDriver).startRecognition(any(STTListener.class), eq(Duration.ofMillis(10)));
    }

    @Test
    public void keywordSpottedDoesNotWaitForTheListeningSound() throws Exception {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);
        CountDownLatch soundStarted = new CountDownLatch(1);
        CountDownLatch soundFinished = new CountDownLatch(1);
        doAnswer(invocation -> {
            soundStarted.countDown();
            soundFinished.await(10, TimeUnit.SECONDS);
            return null;
        }).when(feedbackService).playListeningSound();

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());

        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> ksListenerCaptor.getValue().ksEventReceived(new KSpottedEvent(Duration.ofMillis(10))));
            assertTrue(soundStarted.await(5, TimeUnit.SECONDS));
        } finally {
            soundFinished.countDown();
        }
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
        verify(audioDriver, never()).notifyTriggerIdle();
    }

    @Test
    public void finishDialogReleasesGroupForPersistentDialog() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.start();
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        InOrder inOrder = inOrder(eventListener, audioDriver);
        inOrder.verify(eventListener).onReleaseGroup(context);
        inOrder.verify(audioDriver).notifyTriggerIdle();
        verify(eventListener, never()).onDialogStopped(context);
    }

    @Test
    public void finishDialogNotifiesTriggerIdleWhenNothingWasRecognized() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.start();
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("nothing heard"));

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver).notifyTriggerIdle();
    }

    @Test
    public void finishDialogNotifiesTriggerIdleWhenRecognitionFailsToStart() throws STTException {
        when(audioDriver.startRecognition(any())).thenThrow(STTException.class);
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        processor.start();
        processor.startSimpleDialog();

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver).notifyTriggerIdle();
    }

    @Test
    public void ignoredTriggerNotifiesTriggerIdle() throws DTException, STTException {
        when(eventListener.onAcquireGroup(any())).thenReturn(false);
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);
        processor.start();

        processor.startSimpleDialog();

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver, never()).startRecognition(any());
        verify(audioDriver).notifyTriggerIdle();
    }

    @Test
    public void finishDialogCommitsStateBeforeReleasingGroupAndDoesNotOverwriteNewSession() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.start();
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
        verify(audioDriver, never()).notifyTriggerIdle();
    }

    @Test
    public void recognitionErrorIsSpoken() throws STTException {
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.start();
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("no results"));

        verify(speechSynthesizer).say(eq("no results"), eq(context.tts()), eq(context.voice()), eq(context.sink()),
                eq(null));
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);
    }

    @Test
    public void recognitionErrorEndsInteractionQuietlyWhenNotSpoken() throws STTException {
        when(eventListener.shouldSpeakRecognitionErrors(any())).thenReturn(false);
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.start();
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("no results"));

        verifyNoInteractions(speechSynthesizer);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver).stopRecognition(sttListenerCaptor.getValue());
        verify(feedbackService).setListening(false);
        verify(eventListener).onReleaseGroup(context);
    }

    @Test
    public void recognitionErrorStopsOneShotDialogQuietlyWhenNotSpoken() throws STTException {
        when(eventListener.shouldSpeakRecognitionErrors(any())).thenReturn(false);
        DialogContext context = createTestContext(List.of(), false);
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        processor.startSimpleDialog();
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionErrorEvent("no results"));

        verifyNoInteractions(speechSynthesizer);
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(eventListener).onReleaseGroup(context);
        verify(eventListener).onDialogStopped(context);
    }

    @Test
    public void recognitionStartFailureIsSpokenWhenRecognitionErrorsAreNot() throws STTException {
        when(eventListener.shouldSpeakRecognitionErrors(any())).thenReturn(false);
        when(i18nProvider.getFormattedText(eq("error.stt-exception"), any(), any()))
                .thenReturn("Error during recognition");
        when(audioDriver.startRecognition(any())).thenThrow(new STTException("broken"));
        DialogContext context = createTestContext(List.of());
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener);

        processor.start();
        processor.startSimpleDialog();

        verify(speechSynthesizer).say(eq("Error during recognition"), eq(context.tts()), eq(context.voice()),
                eq(context.sink()), eq(null));
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
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
        verify(audioDriver, never()).notifyTriggerIdle();
    }

    @Test
    public void followUpListensAgainOnceTheAnswerHasPlayedAndEndsQuietlyOnSilence()
            throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        CompletableFuture<@Nullable Void> playback = new CompletableFuture<>();
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any())).thenReturn(playback);
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        assertEquals(DialogProcessor.DialogState.SPEAKING, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());

        playback.complete(null);

        STTListener followUpListener = followUpListener();
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(eventListener, never()).onReleaseGroup(context);

        followUpListener.sttEventReceived(new SpeechRecognitionErrorEvent("no results"));

        verify(speechSynthesizer, never()).say(eq("no results"), any(), any(), any(), any());
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);
    }

    @Test
    public void followUpStopsAtTheLimitUntilTheNextDialog() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        when(eventListener.getFollowUpLimit(any())).thenReturn(1);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        followUpListener().sttEventReceived(new SpeechRecognitionEvent("and the hallway too", 1.0f));

        verify(audioDriver, times(1)).startRecognition(any(), isNull(), any());
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);

        processor.startSimpleDialog();
        ArgumentCaptor<STTListener> nextDialog = ArgumentCaptor.forClass(STTListener.class);
        verify(audioDriver, times(2)).startRecognition(nextDialog.capture());
        nextDialog.getValue().sttEventReceived(new SpeechRecognitionEvent("turn off the light", 1.0f));

        verify(audioDriver, times(2)).startRecognition(any(), isNull(), any());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
    }

    @Test
    public void followUpKeepsListeningWithoutALimit() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        when(eventListener.getFollowUpLimit(any())).thenReturn(0);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        for (int i = 0; i < 3; i++) {
            followUpListener().sttEventReceived(new SpeechRecognitionEvent("and the next one", 1.0f));
        }

        verify(audioDriver, times(4)).startRecognition(any(), isNull(), any());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(eventListener, never()).onReleaseGroup(context);
    }

    @Test
    public void triggerDuringFollowUpStartsANewDialog() throws DTException, STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        HumanLanguageInterpreter hli = answeringInterpreter();
        DialogContext context = createTestContext(List.of(hli));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());
        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        STTListener followUpListener = followUpListener();

        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());

        verify(audioDriver).stopRecognition(followUpListener);
        verify(audioDriver, times(2)).startRecognition(any());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());

        followUpListener.sttEventReceived(new SpeechRecognitionEvent("stale", 1.0f));
        verify(hli, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
    }

    @Test
    public void triggerWhileTheFollowUpRecognitionStartsEndsItAndStartsANewDialog()
            throws DTException, STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());
        KSListener ksListener = ksListenerCaptor.getValue();
        doAnswer(invocation -> {
            ksListener.dtEventReceived(new DTTriggeredEvent());
            return null;
        }).when(audioDriver).startRecognition(any(), isNull(), any());

        ksListener.dtEventReceived(new DTTriggeredEvent());
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        ArgumentCaptor<STTListener> followUpListener = ArgumentCaptor.forClass(STTListener.class);
        ArgumentCaptor<BooleanSupplier> followUpCancelled = ArgumentCaptor.forClass(BooleanSupplier.class);
        verify(audioDriver).startRecognition(followUpListener.capture(), isNull(), followUpCancelled.capture());
        assertTrue(followUpCancelled.getValue().getAsBoolean());
        verify(audioDriver).stopRecognition(followUpListener.getValue());
        verify(audioDriver, times(2)).startRecognition(any());
        assertEquals(DialogProcessor.DialogState.LISTENING, processor.getState());
        verify(eventListener).onReleaseGroup(context);
    }

    @Test
    public void followUpIsSkippedWhenTheAnswerFailsToPlay() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("sink gone")));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(eventListener).onReleaseGroup(context);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
    }

    @Test
    public void followUpIsSkippedWhenPlayingTheAnswerThrows() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("sink gone"));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(eventListener).onReleaseGroup(context);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
    }

    @Test
    public void followUpIsSkippedWhenTheCommandIsNotInterpreted() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.interpret(any(Locale.class), any(InterpreterContext.class)))
                .thenThrow(new InterpretationException("not understood"));
        DialogContext context = createTestContext(List.of(hli));
        when(speechSynthesizer.say(any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("something on the TV", 1.0f));

        verify(speechSynthesizer).say(eq("not understood"), any(), any(), any(), any());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);
    }

    @Test
    public void followUpIsNotUsedForOneShotDialogs() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()), false);
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(speechSynthesizer).say(eq("answer"), any(), any(), any(), any());
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
        verify(eventListener).onDialogStopped(context);
    }

    @Test
    public void followUpIsNotUsedWhenTheDialogWasNotStarted() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);

        // like listenAndAnswer, whose context has a trigger it doesn't wait for
        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(speechSynthesizer).say(eq("answer"), any(), any(), any(), any());
        verify(eventListener).onReleaseGroup(context);
        verify(eventListener).onDialogStopped(context);
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
    }

    @Test
    public void followUpHearsAnotherCommandAndListensAgain() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        HumanLanguageInterpreter hli = answeringInterpreter();
        DialogContext context = createTestContext(List.of(hli));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        followUpListener().sttEventReceived(new SpeechRecognitionEvent("and the fan", 1.0f));

        verify(audioDriver, times(2)).startRecognition(any(), isNull(), any());
        verify(hli, times(2)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(eventListener, never()).onReleaseGroup(context);
    }

    @Test
    public void followUpEndsQuietlyWhenNothingIsRecognizedInTime() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        STTListener followUpListener = followUpListener();
        ArgumentCaptor<Runnable> followUpTimeout = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(followUpTimeout.capture(), eq(DialogProcessor.MAX_FOLLOW_UP_SECONDS),
                eq(TimeUnit.SECONDS));

        followUpTimeout.getValue().run();

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        // the recognition stream closes before the dialog reports it is done
        InOrder inOrder = inOrder(audioDriver, eventListener);
        inOrder.verify(audioDriver).stopRecognition(followUpListener);
        inOrder.verify(eventListener).onReleaseGroup(context);
        verify(feedbackService, times(2)).setListening(false);
        verify(speechSynthesizer, times(1)).say(any(), any(), any(), any(), any());
    }

    @Test
    public void followUpTimeoutEndsTheFollowUpOnTheDialogExecutor() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        List<Runnable> dialogTasks = new ArrayList<>();
        DialogProcessor processor = new DialogProcessor(context, audioDriver, feedbackService, interpretationService,
                speechSynthesizer, i18nProvider, eventListener, scheduler, dialogTasks::add);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        runAll(dialogTasks);
        STTListener followUpListener = followUpListener();
        ArgumentCaptor<Runnable> followUpTimeout = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(followUpTimeout.capture(), eq(DialogProcessor.MAX_FOLLOW_UP_SECONDS),
                eq(TimeUnit.SECONDS));

        followUpTimeout.getValue().run();
        verify(audioDriver, never()).stopRecognition(followUpListener);

        runAll(dialogTasks);
        verify(audioDriver).stopRecognition(followUpListener);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
    }

    private static void runAll(List<Runnable> tasks) {
        while (!tasks.isEmpty()) {
            tasks.remove(0).run();
        }
    }

    @Test
    public void followUpEndsQuietlyWhenItsRecognitionFailsToStart() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(audioDriver.startRecognition(any(), isNull(), any())).thenThrow(new STTException("busy"));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(eventListener).onReleaseGroup(context);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(speechSynthesizer, times(1)).say(any(), any(), any(), any(), any());
    }

    @Test
    public void followUpEndsWhenItsRecognitionFailsUnexpectedly() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        STTListener followUpListener = followUpListener();

        followUpListener.sttEventReceived(new SpeechRecognitionErrorEvent("unexpected failure"));

        verify(audioDriver).stopRecognition(followUpListener);
        verify(eventListener).onReleaseGroup(context);
        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
    }

    @Test
    public void followUpIsSkippedWhenNoVoiceCanSpeakTheAnswer() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(context.tts().getAvailableVoices()).thenReturn(Set.of());
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        verify(eventListener).onReleaseGroup(context);
        verify(audioDriver, never()).startRecognition(any(), any(), any());
        verifyNoInteractions(speechSynthesizer);
    }

    @Test
    public void answerThatNeverFinishesPlayingSkipsFollowUp() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        CompletableFuture<@Nullable Void> playback = new CompletableFuture<>();
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any())).thenReturn(playback);
        ScheduledFuture<?> answerTimeoutFuture = mock(ScheduledFuture.class);
        doReturn(answerTimeoutFuture).when(scheduler).schedule(any(Runnable.class),
                eq(DialogProcessor.MAX_ANSWER_PLAYBACK_SECONDS), eq(TimeUnit.SECONDS));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        ArgumentCaptor<Runnable> answerTimeout = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(answerTimeout.capture(), eq(DialogProcessor.MAX_ANSWER_PLAYBACK_SECONDS),
                eq(TimeUnit.SECONDS));
        assertEquals(DialogProcessor.DialogState.SPEAKING, processor.getState());

        answerTimeout.getValue().run();

        assertEquals(DialogProcessor.DialogState.WAITING_FOR_TRIGGER, processor.getState());
        verify(eventListener).onReleaseGroup(context);

        // the answer finishing late doesn't start a follow-up
        playback.complete(null);
        verify(audioDriver, never()).startRecognition(any(), any(), any());
    }

    @Test
    public void stopWhileTheAnswerPlaysDoesNotListenForAFollowUp() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        CompletableFuture<@Nullable Void> playback = new CompletableFuture<>();
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any())).thenReturn(playback);
        ScheduledFuture<?> answerTimeoutFuture = mock(ScheduledFuture.class);
        doReturn(answerTimeoutFuture).when(scheduler).schedule(any(Runnable.class),
                eq(DialogProcessor.MAX_ANSWER_PLAYBACK_SECONDS), eq(TimeUnit.SECONDS));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        processor.stop();
        verify(answerTimeoutFuture).cancel(false);
        playback.complete(null);

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(audioDriver, never()).startRecognition(any(), any(), any());
        verify(eventListener, times(1)).onReleaseGroup(context);
    }

    @Test
    public void stopDuringFollowUpEndsIt() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        HumanLanguageInterpreter hli = answeringInterpreter();
        DialogContext context = createTestContext(List.of(hli));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        ScheduledFuture<?> followUpTimeoutFuture = mock(ScheduledFuture.class);
        doReturn(followUpTimeoutFuture).when(scheduler).schedule(any(Runnable.class),
                eq(DialogProcessor.MAX_FOLLOW_UP_SECONDS), eq(TimeUnit.SECONDS));
        DialogProcessor processor = createFollowUpProcessor(context);
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        verify(hli, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        STTListener followUpListener = followUpListener();

        processor.stop();

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        verify(audioDriver).stopAll();
        verify(followUpTimeoutFuture).cancel(false);
        followUpListener.sttEventReceived(new SpeechRecognitionEvent("too late", 1.0f));
        verify(hli, times(1)).interpret(any(Locale.class), any(InterpreterContext.class));
        verify(eventListener, times(1)).onReleaseGroup(context);
    }

    @Test
    public void stopWhileTheFollowUpRecognitionStartsCancelsIt() throws STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        ScheduledFuture<?> followUpTimeoutFuture = mock(ScheduledFuture.class);
        doReturn(followUpTimeoutFuture).when(scheduler).schedule(any(Runnable.class),
                eq(DialogProcessor.MAX_FOLLOW_UP_SECONDS), eq(TimeUnit.SECONDS));
        DialogProcessor processor = createFollowUpProcessor(context);
        doAnswer(invocation -> {
            processor.stop();
            return null;
        }).when(audioDriver).startRecognition(any(), isNull(), any());
        processor.start();

        processor.startSimpleDialog();
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));

        ArgumentCaptor<BooleanSupplier> followUpCancelled = ArgumentCaptor.forClass(BooleanSupplier.class);
        verify(audioDriver).startRecognition(any(), isNull(), followUpCancelled.capture());
        assertTrue(followUpCancelled.getValue().getAsBoolean());
        verify(followUpTimeoutFuture).cancel(false);
        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        // only the first interaction played it
        verify(feedbackService, times(1)).playListeningSound();
    }

    @Test
    public void triggerWhileTheAnswerPlaysIsIgnored() throws DTException, STTException, InterpretationException {
        when(eventListener.shouldListenForFollowUp(any())).thenReturn(true);
        DialogContext context = createTestContext(List.of(answeringInterpreter()));
        CompletableFuture<@Nullable Void> playback = new CompletableFuture<>();
        when(speechSynthesizer.say(eq("answer"), any(), any(), any(), any())).thenReturn(playback);
        DialogProcessor processor = createFollowUpProcessor(context);

        ArgumentCaptor<KSListener> ksListenerCaptor = ArgumentCaptor.forClass(KSListener.class);
        processor.start();
        verify(audioDriver).startTrigger(ksListenerCaptor.capture());
        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());
        verify(audioDriver, times(1)).startRecognition(any());
        recognitionListener().sttEventReceived(new SpeechRecognitionEvent("turn on the light", 1.0f));
        assertEquals(DialogProcessor.DialogState.SPEAKING, processor.getState());

        ksListenerCaptor.getValue().dtEventReceived(new DTTriggeredEvent());

        assertEquals(DialogProcessor.DialogState.SPEAKING, processor.getState());
        verify(audioDriver, times(1)).startRecognition(any());
        playback.complete(null);
        verify(audioDriver).startRecognition(any(), isNull(), any());
    }

    private HumanLanguageInterpreter answeringInterpreter() throws InterpretationException {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.interpret(any(Locale.class), any(InterpreterContext.class))).thenReturn("answer");
        return hli;
    }

    /**
     * Creates a processor that starts the follow-up on the calling thread and leaves the timeouts to the test.
     */
    private DialogProcessor createFollowUpProcessor(DialogContext context) {
        return new DialogProcessor(context, audioDriver, feedbackService, interpretationService, speechSynthesizer,
                i18nProvider, eventListener, scheduler, Runnable::run);
    }

    private STTListener recognitionListener() throws STTException {
        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        verify(audioDriver).startRecognition(sttListenerCaptor.capture());
        return sttListenerCaptor.getValue();
    }

    private STTListener followUpListener() throws STTException {
        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);
        verify(audioDriver, atLeastOnce()).startRecognition(sttListenerCaptor.capture(), isNull(), any());
        return sttListenerCaptor.getValue();
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
        }).when(audioDriver).stopRecognition(any());

        sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("hello", 1.0f));

        assertEquals(DialogProcessor.DialogState.IDLE, processor.getState());
        assertFalse(processor.isProcessing());
        verify(hli, never()).interpret(any(Locale.class), any(InterpreterContext.class));
        verifyNoInteractions(speechSynthesizer);
        verify(audioDriver, never()).notifyTriggerIdle();
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
        verify(audioDriver, never()).notifyTriggerIdle();
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
