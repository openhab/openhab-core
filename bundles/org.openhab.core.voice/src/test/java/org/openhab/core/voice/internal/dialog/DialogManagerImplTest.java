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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSinkRegistry;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioSourceRegistry;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSException;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.VoiceManagerConfiguration;
import org.openhab.core.voice.internal.VoiceServiceResolver;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceHandle;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.openhab.core.voice.tts.Voice;

/**
 * Unit tests for {@link DialogManagerImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogManagerImplTest {

    private final LocaleProvider localeProvider = mock(LocaleProvider.class);
    private final AudioManager audioManager = mock(AudioManager.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final StorageService storageService = mock(StorageService.class);
    private final LLMToolRegistry llmToolRegistry = mock(LLMToolRegistry.class);
    private final TTSServiceRegistry ttsRegistry = mock(TTSServiceRegistry.class);
    private final STTServiceRegistry sttRegistry = mock(STTServiceRegistry.class);
    private final KSServiceRegistry ksRegistry = mock(KSServiceRegistry.class);
    private final HumanLanguageInterpreterRegistry hliRegistry = mock(HumanLanguageInterpreterRegistry.class);
    private final AudioSinkRegistry audioSinkRegistry = mock(AudioSinkRegistry.class);
    private final AudioSourceRegistry audioSourceRegistry = mock(AudioSourceRegistry.class);
    private final ConversationManager conversationManager = mock(ConversationManager.class);
    private final InterpretationService interpretationService = mock(InterpretationService.class);
    private final SpeechSynthesizer speechSynthesizer = mock(SpeechSynthesizer.class);
    private final VoiceI18nProvider i18nProvider = mock(VoiceI18nProvider.class);
    private final VoiceServiceResolver voiceServiceResolver = mock(VoiceServiceResolver.class);

    private final InMemoryStorage<DialogRegistration> storage = new InMemoryStorage<>();
    private final Map<String, AudioStream> sourceStreams = new HashMap<>();
    private final Map<String, KSServiceHandle> ksHandles = new HashMap<>();
    private final Map<String, STTServiceHandle> sttHandles = new HashMap<>();
    private @NonNullByDefault({}) DialogManagerImpl dialogManager;

    @BeforeEach
    public void setUp() {
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
        when(voiceServiceResolver.getConfiguration())
                .thenReturn(new VoiceManagerConfiguration(mock(ConfigDescriptionRegistry.class)));
        doReturn(storage).when(storageService).getStorage(any(), any());
        sourceStreams.clear();
        ksHandles.clear();
        sttHandles.clear();

        dialogManager = new DialogManagerImpl(localeProvider, audioManager, eventPublisher, storageService,
                llmToolRegistry, ttsRegistry, sttRegistry, ksRegistry, hliRegistry, audioSinkRegistry,
                audioSourceRegistry, conversationManager, interpretationService, speechSynthesizer, i18nProvider,
                voiceServiceResolver);
    }

    private DialogContext createValidContext(String sourceId, String sinkId)
            throws AudioException, STTException, KSException {
        return createValidContextBuilder(sourceId, sinkId).build();
    }

    private DialogContext.Builder createValidContextBuilder(String sourceId, String sinkId)
            throws AudioException, STTException, KSException {
        return createValidContextBuilder(sourceId, sinkId, true, "keyword");
    }

    private DialogContext.Builder createValidContextBuilder(String sourceId, String sinkId, boolean withDT,
            String keyword) throws AudioException, STTException, KSException {
        AudioSource source = mock(AudioSource.class);
        when(source.getId()).thenReturn(sourceId);
        when(source.getUID()).thenReturn(sourceId);
        when(source.getLabel(null)).thenReturn(sourceId);
        when(source.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        AudioStream audioStream = mock(AudioStream.class);
        sourceStreams.put(sourceId, audioStream);
        when(source.getInputStream(any())).thenReturn(audioStream);

        AudioSink sink = mock(AudioSink.class);
        when(sink.getId()).thenReturn(sinkId);
        when(sink.getUID()).thenReturn(sinkId);
        when(sink.getLabel(null)).thenReturn(sinkId);
        when(sink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        STTService stt = mock(STTService.class);
        when(stt.getId()).thenReturn("test-stt");
        when(stt.getUID()).thenReturn("test-stt");
        when(stt.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        STTServiceHandle sttHandle = mock(STTServiceHandle.class);
        sttHandles.put(sourceId, sttHandle);
        when(stt.recognize(any(), any(), any())).thenReturn(sttHandle);

        TTSService tts = mock(TTSService.class);
        when(tts.getId()).thenReturn("test-tts");
        when(tts.getUID()).thenReturn("test-tts");
        Voice voice = mock(Voice.class);
        when(voice.getUID()).thenReturn("test-tts:voice-en");
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));

        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("test-hli");
        when(hli.getUID()).thenReturn("test-hli");
        when(hli.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));

        var builder = new DialogContext.Builder(keyword, Locale.ENGLISH).withSource(source).withSink(sink).withSTT(stt)
                .withTTS(tts).withHLIs(List.of(hli));

        if (withDT) {
            KSService ks = mock(KSService.class);
            when(ks.getId()).thenReturn("test-ks");
            when(ks.getUID()).thenReturn("test-ks");
            when(ks.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
            when(ks.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
            KSServiceHandle ksHandle = mock(KSServiceHandle.class);
            ksHandles.put(sourceId, ksHandle);
            when(ks.spot(any(), any(), any(), any())).thenReturn(ksHandle);
            builder.withKS(ks);
        }

        return builder;
    }

    private KSServiceHandle getKSHandle(String sourceId) {
        return Objects.requireNonNull(ksHandles.get(sourceId));
    }

    private STTServiceHandle getSTTHandle(String sourceId) {
        return Objects.requireNonNull(sttHandles.get(sourceId));
    }

    private AudioStream getSourceStream(String sourceId) {
        return Objects.requireNonNull(sourceStreams.get(sourceId));
    }

    @Test
    public void registerDialogPersistsAndPreventsDuplicates() {
        DialogRegistration reg = new DialogRegistration("source-1", "sink-1");

        dialogManager.registerDialog(reg);
        assertTrue(storage.containsKey("source-1"));
        assertEquals(1, dialogManager.getDialogRegistrations().size());

        assertThrows(IllegalStateException.class, () -> dialogManager.registerDialog(reg));
    }

    @Test
    public void unregisterDialogRemovesFromStorage() {
        DialogRegistration reg = new DialogRegistration("source-1", "sink-1");

        dialogManager.registerDialog(reg);
        dialogManager.unregisterDialog("source-1");

        assertFalse(storage.containsKey("source-1"));
        assertEquals(0, dialogManager.getDialogRegistrations().size());
    }

    @Test
    public void unregisterDialogStopsActiveDialogIfRunning()
            throws AudioException, STTException, KSException, IOException {
        DialogRegistration reg = new DialogRegistration("source-1", "sink-1");
        dialogManager.registerDialog(reg);

        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.unregisterDialog("source-1");

        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();
    }

    @Test
    public void onBeforeDialogInterpretationUpdatesLastContext() throws AudioException, STTException, KSException {
        DialogContext context = createValidContext("source-1", "sink-1");
        assertNull(dialogManager.getLastDialogContext());

        dialogManager.onBeforeDialogInterpretation(context);
        assertEquals(context, dialogManager.getLastDialogContext());
    }

    @Test
    public void listenAndAnswerPreventsDuplicateExecutionOnSameDialogGroup()
            throws AudioException, STTException, KSException {
        DialogContext context1 = createValidContextBuilder("source-1", "sink-1").withDialogGroup("group-A").build();
        DialogContext context2 = createValidContextBuilder("source-2", "sink-1").withDialogGroup("group-A").build();

        dialogManager.listenAndAnswer(context1);
        verify(context1.source()).getInputStream(any());

        // Second dialog on same group should be rejected and not start audio stream
        dialogManager.listenAndAnswer(context2);
        verify(context2.source(), never()).getInputStream(any());

        // Releasing context1 releases dialog group
        dialogManager.onReleaseGroup(context1);

        // Now context2 can run
        dialogManager.listenAndAnswer(context2);
        verify(context2.source()).getInputStream(any());
    }

    @Test
    public void listenAndAnswerWithCompatibleActiveDialogDelegatesToActiveProcessor()
            throws AudioException, STTException, KSException {
        DialogContext.Builder builder = createValidContextBuilder("source-1", "sink-1");
        DialogContext persistentContext = builder.withDialogGroup("group-1").build();
        dialogManager.startDialog(persistentContext);

        DialogContext simpleContext = builder.withDialogGroup("group-2").build();
        assertDoesNotThrow(() -> dialogManager.listenAndAnswer(simpleContext));
    }

    @Test
    public void listenAndAnswerWithIncompatibleActiveDialogThrows() throws AudioException, STTException, KSException {
        DialogContext persistentContext = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(persistentContext);

        AudioSink differentSink = mock(AudioSink.class);
        when(differentSink.getId()).thenReturn("sink-different");
        when(differentSink.getLabel(null)).thenReturn("sink-different");

        DialogContext incompatibleContext = createValidContextBuilder("source-1", "sink-1").withSink(differentSink)
                .withDialogGroup("group-different").build();

        assertThrows(IllegalStateException.class, () -> dialogManager.listenAndAnswer(incompatibleContext));
    }

    @Test
    public void startDialogSuccessfullyStartsAndTracksProcessor() throws AudioException, STTException, KSException {
        DialogContext context = createValidContext("source-1", "sink-1");
        DTServiceHandle handle = dialogManager.startDialog(context);

        assertNotNull(handle);
        assertEquals(1, dialogManager.getDialogsContexts().size());
        assertEquals(context, dialogManager.getDialogsContexts().getFirst());
    }

    @Test
    public void startDialogPreventsDuplicateForSameAudioSource() throws AudioException, STTException, KSException {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);

        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenMissingDialogTrigger() throws AudioException, STTException, KSException {
        DialogContext context = createValidContextBuilder("source-1", "sink-1", false, "keyword").build();
        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenKSKeywordMissingOrEmpty() throws AudioException, STTException, KSException {
        DialogContext context = createValidContextBuilder("source-1", "sink-1", true, "").build();
        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenKSLocaleIncompatible() throws AudioException, STTException, KSException {
        DialogContext.Builder builder = createValidContextBuilder("source-1", "sink-1");
        KSService ks = mock(KSService.class);
        when(ks.getId()).thenReturn("ks-de");
        when(ks.getUID()).thenReturn("ks-de");
        when(ks.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN));
        builder.withKS(ks);

        DialogContext context = builder.build();
        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenSTTLocaleIncompatible() throws AudioException, STTException, KSException {
        DialogContext.Builder builder = createValidContextBuilder("source-1", "sink-1");
        STTService stt = mock(STTService.class);
        when(stt.getId()).thenReturn("stt-de");
        when(stt.getUID()).thenReturn("stt-de");
        when(stt.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN));
        builder.withSTT(stt);

        DialogContext context = builder.build();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
        assertTrue(ex.getMessage().contains("Cannot start dialog as provided locale is not supported by STT service"));
    }

    @Test
    public void startDialogThrowsWhenHLILocaleIncompatible() throws AudioException, STTException, KSException {
        DialogContext.Builder builder = createValidContextBuilder("source-1", "sink-1");
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("hli-de");
        when(hli.getUID()).thenReturn("hli-de");
        when(hli.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN));
        builder.withHLIs(List.of(hli));

        DialogContext context = builder.build();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
        assertEquals("Cannot start dialog as provided locale is not supported by all HLI services.", ex.getMessage());
    }

    @Test
    public void startDialogThrowsWhenVoiceLocaleIncompatible() throws AudioException, STTException, KSException {
        DialogContext.Builder builder = createValidContextBuilder("source-1", "sink-1");
        Voice voice = mock(Voice.class);
        when(voice.getUID()).thenReturn("tts:voice-de");
        when(voice.getLocale()).thenReturn(Locale.GERMAN);
        builder.withVoice(voice);

        DialogContext context = builder.build();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
        assertTrue(ex.getMessage().contains("Cannot start dialog as provided locale is not supported by TTS voice"));
    }

    @Test
    public void stopDialogStopsAndRemovesRunningProcessor()
            throws AudioException, STTException, KSException, IOException {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.stopDialog(context);
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();

        assertThrows(IllegalStateException.class, () -> dialogManager.stopDialog(context.source()));
    }

    @Test
    public void stopDialogStopsSingleDialogSuccessfully()
            throws AudioException, STTException, KSException, IOException {
        DialogContext context = createValidContext("source-single", "sink-single");

        dialogManager.listenAndAnswer(context);
        verify(context.source()).getInputStream(any());
        verify(context.stt()).recognize(any(), any(), any());

        assertDoesNotThrow(() -> dialogManager.stopDialog(context.source()));
        verify(getSTTHandle("source-single")).abort();
        verify(getSourceStream("source-single")).close();
        assertThrows(IllegalStateException.class, () -> dialogManager.stopDialog(context.source()));
    }

    @Test
    public void stopDialogWithNullSourceUsesDefaultAudioSource()
            throws AudioException, STTException, KSException, IOException {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);

        when(audioManager.getSource()).thenReturn(context.source());
        dialogManager.stopDialog((AudioSource) null);
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();
    }

    @Test
    public void stopDialogWithNullSourceThrowsWhenDefaultSourceMissing() {
        when(audioManager.getSource()).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> dialogManager.stopDialog((AudioSource) null));
    }

    @Test
    public void stopDialogDoesNotBlockWhileStartDialogIsInFlightAndAbortsHandle()
            throws AudioException, STTException, KSException, InterruptedException, IOException {
        DialogContext context = createValidContext("source-inflight", "sink-inflight");
        KSService ks = (KSService) Objects.requireNonNull(context.dt());
        KSServiceHandle handle = getKSHandle("source-inflight");

        CountDownLatch enterSpotLatch = new CountDownLatch(1);
        CountDownLatch releaseSpotLatch = new CountDownLatch(1);
        when(ks.spot(any(), any(), any(), any())).thenAnswer(inv -> {
            enterSpotLatch.countDown();
            assertTrue(releaseSpotLatch.await(5, TimeUnit.SECONDS));
            return handle;
        });

        @Nullable
        DTServiceHandle[] returnedHandle = new DTServiceHandle[1];
        Thread startThread = new Thread(() -> {
            returnedHandle[0] = dialogManager.startDialog(context);
        });
        startThread.start();

        assertTrue(enterSpotLatch.await(5, TimeUnit.SECONDS));

        // stopDialog must complete without blocking on startDialog
        assertDoesNotThrow(() -> dialogManager.stopDialog(context.source()));
        assertEquals(0, dialogManager.getDialogsContexts().size());

        releaseSpotLatch.countDown();
        startThread.join(5000);

        assertNull(returnedHandle[0]);
        verify(handle).abort();
        verify(getSourceStream("source-inflight")).close();
    }

    @Test
    public void getDialogRegistrationsMarksRunningStateCorrectly() throws AudioException, STTException, KSException {
        DialogRegistration reg1 = new DialogRegistration("source-1", "sink-1");
        DialogRegistration reg2 = new DialogRegistration("source-2", "sink-2");
        dialogManager.registerDialog(reg1);
        dialogManager.registerDialog(reg2);

        DialogContext context1 = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context1);

        List<DialogRegistration> regs = dialogManager.getDialogRegistrations();
        assertEquals(2, regs.size());

        DialogRegistration found1 = regs.stream().filter(r -> "source-1".equals(r.sourceId)).findFirst().orElseThrow();
        DialogRegistration found2 = regs.stream().filter(r -> "source-2".equals(r.sourceId)).findFirst().orElseThrow();

        assertTrue(found1.running);
        assertFalse(found2.running);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingAudioSinkStopsDependentDialog() throws AudioException, STTException, KSException, IOException {
        ArgumentCaptor<RegistryChangeListener<AudioSink>> sinkCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        dialogManager.activate(Map.of());
        verify(audioSinkRegistry).addRegistryChangeListener(sinkCaptor.capture());

        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        sinkCaptor.getValue().removed(context.sink());
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingAudioSourceStopsDependentDialog()
            throws AudioException, STTException, KSException, IOException {
        ArgumentCaptor<RegistryChangeListener<AudioSource>> sourceCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        dialogManager.activate(Map.of());
        verify(audioSourceRegistry).addRegistryChangeListener(sourceCaptor.capture());

        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        sourceCaptor.getValue().removed(context.source());
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingServiceStopsDependentDialog() throws AudioException, STTException, KSException, IOException {
        ArgumentCaptor<RegistryChangeListener<TTSService>> ttsCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        ArgumentCaptor<RegistryChangeListener<STTService>> sttCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        ArgumentCaptor<RegistryChangeListener<KSService>> ksCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        ArgumentCaptor<RegistryChangeListener<HumanLanguageInterpreter>> hliCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        ArgumentCaptor<RegistryChangeListener<LLMTool>> llmCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);

        dialogManager.activate(Map.of());

        verify(ttsRegistry).addRegistryChangeListener(ttsCaptor.capture());
        verify(sttRegistry).addRegistryChangeListener(sttCaptor.capture());
        verify(ksRegistry).addRegistryChangeListener(ksCaptor.capture());
        verify(hliRegistry).addRegistryChangeListener(hliCaptor.capture());
        verify(llmToolRegistry).addRegistryChangeListener(llmCaptor.capture());
        verify(audioSinkRegistry).addRegistryChangeListener(any());
        verify(audioSourceRegistry).addRegistryChangeListener(any());

        // Test TTS service removal
        DialogContext context1 = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context1);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        ttsCaptor.getValue().removed(context1.tts());
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();

        // Test STT service removal
        DialogContext context2 = createValidContext("source-2", "sink-2");
        dialogManager.startDialog(context2);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        sttCaptor.getValue().removed(context2.stt());
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-2")).abort();
        verify(getSourceStream("source-2")).close();

        // Test KS service removal
        DialogContext context3 = createValidContext("source-3", "sink-3");
        dialogManager.startDialog(context3);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        ksCaptor.getValue().removed((KSService) Objects.requireNonNull(context3.dt()));
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-3")).abort();
        verify(getSourceStream("source-3")).close();

        // Test HLI service removal
        DialogContext context4 = createValidContext("source-4", "sink-4");
        dialogManager.startDialog(context4);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        hliCaptor.getValue().removed(context4.hlis().getFirst());
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-4")).abort();
        verify(getSourceStream("source-4")).close();

        // Test LLMTool removal
        LLMTool tool = mock(LLMTool.class);
        when(tool.getUID()).thenReturn("tool-1");
        DialogContext context5 = createValidContextBuilder("source-5", "sink-5").withLLMTools(Set.of(tool)).build();
        dialogManager.startDialog(context5);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        llmCaptor.getValue().removed(tool);
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(getKSHandle("source-5")).abort();
        verify(getSourceStream("source-5")).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingAudioSinkOrSourceStopsSingleDialog()
            throws AudioException, STTException, KSException, IOException {
        ArgumentCaptor<RegistryChangeListener<AudioSink>> sinkCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        ArgumentCaptor<RegistryChangeListener<AudioSource>> sourceCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        dialogManager.activate(Map.of());
        verify(audioSinkRegistry).addRegistryChangeListener(sinkCaptor.capture());
        verify(audioSourceRegistry).addRegistryChangeListener(sourceCaptor.capture());

        DialogContext context1 = createValidContext("source-single-1", "sink-single-1");
        dialogManager.listenAndAnswer(context1);

        sinkCaptor.getValue().removed(context1.sink());
        verify(getSTTHandle("source-single-1")).abort();
        verify(getSourceStream("source-single-1")).close();

        DialogContext context2 = createValidContext("source-single-2", "sink-single-2");
        dialogManager.listenAndAnswer(context2);

        sourceCaptor.getValue().removed(context2.source());
        verify(getSTTHandle("source-single-2")).abort();
        verify(getSourceStream("source-single-2")).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingServiceStopsSingleDialog() throws AudioException, STTException, KSException, IOException {
        ArgumentCaptor<RegistryChangeListener<STTService>> sttCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        dialogManager.activate(Map.of());
        verify(sttRegistry).addRegistryChangeListener(sttCaptor.capture());

        DialogContext context = createValidContext("source-single-stt", "sink-single-stt");
        dialogManager.listenAndAnswer(context);

        sttCaptor.getValue().removed(context.stt());
        verify(getSTTHandle("source-single-stt")).abort();
        verify(getSourceStream("source-single-stt")).close();
    }

    @Test
    public void stopDialogStopsSingleDialogDuringInterpretation()
            throws AudioException, STTException, KSException, InterpretationException, InterruptedException {
        CountDownLatch enterInterpretLatch = new CountDownLatch(1);
        CountDownLatch finishInterpretLatch = new CountDownLatch(1);
        when(interpretationService.interpret(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            enterInterpretLatch.countDown();
            assertTrue(finishInterpretLatch.await(5, TimeUnit.SECONDS));
            return "ok";
        });

        DialogContext context = createValidContext("source-interpret", "sink-interpret");
        ArgumentCaptor<STTListener> sttListenerCaptor = ArgumentCaptor.forClass(STTListener.class);

        dialogManager.listenAndAnswer(context);
        verify(context.stt()).recognize(sttListenerCaptor.capture(), any(), any());

        Thread thread = new Thread(() -> {
            sttListenerCaptor.getValue().sttEventReceived(new SpeechRecognitionEvent("text", 1.0f));
        });
        thread.start();

        assertTrue(enterInterpretLatch.await(5, TimeUnit.SECONDS));

        // In INTERPRETING state, stopDialog should find and stop the singleDialogProcessor rather than throwing
        assertDoesNotThrow(() -> dialogManager.stopDialog(context.source()));

        finishInterpretLatch.countDown();
        thread.join(5000);

        verify(speechSynthesizer, never()).say(any(), any(), any(), any(), any());
    }

    @Test
    public void deactivateStopsAllRunningDialogs() throws AudioException, STTException, KSException, IOException {
        DialogContext context1 = createValidContext("source-1", "sink-1");
        DialogContext context2 = createValidContext("source-2", "sink-2");

        dialogManager.startDialog(context1);
        dialogManager.startDialog(context2);
        assertEquals(2, dialogManager.getDialogsContexts().size());

        DialogContext contextSingle = createValidContext("source-single", "sink-single");
        dialogManager.listenAndAnswer(contextSingle);

        dialogManager.deactivate();
        assertEquals(0, dialogManager.getDialogsContexts().size());
        verify(audioSinkRegistry).removeRegistryChangeListener(any());
        verify(audioSourceRegistry).removeRegistryChangeListener(any());
        verify(getKSHandle("source-1")).abort();
        verify(getSourceStream("source-1")).close();
        verify(getKSHandle("source-2")).abort();
        verify(getSourceStream("source-2")).close();
        verify(getSTTHandle("source-single")).abort();
        verify(getSourceStream("source-single")).close();
    }

    @Test
    public void persistentDialogTriggerPreventsConcurrentExecutionInSameGroup()
            throws AudioException, STTException, KSException {
        DialogContext context1 = createValidContextBuilder("source-1", "sink-1").withDialogGroup("group-shared")
                .build();
        AudioSource source1 = context1.source();
        KSService ks1 = (KSService) Objects.requireNonNull(context1.dt());

        DialogContext context2 = createValidContextBuilder("source-2", "sink-2").withDialogGroup("group-shared")
                .build();
        AudioSource source2 = context2.source();
        KSService ks2 = (KSService) Objects.requireNonNull(context2.dt());

        dialogManager.startDialog(context1);
        dialogManager.startDialog(context2);

        ArgumentCaptor<KSListener> ksListenerCaptor1 = ArgumentCaptor.forClass(KSListener.class);
        verify(ks1).spot(ksListenerCaptor1.capture(), any(), any(), any());
        ArgumentCaptor<KSListener> ksListenerCaptor2 = ArgumentCaptor.forClass(KSListener.class);
        verify(ks2).spot(ksListenerCaptor2.capture(), any(), any(), any());

        STTService stt1 = context1.stt();
        STTService stt2 = context2.stt();

        // Persistent dialogs start listening for keyword trigger
        verify(source1, times(1)).getInputStream(any());
        verify(source2, times(1)).getInputStream(any());
        verify(stt1, never()).recognize(any(), any(), any());
        verify(stt2, never()).recognize(any(), any(), any());

        // First trigger succeeds and acquires group-shared, starting STT recognition
        ksListenerCaptor1.getValue().dtEventReceived(new DTTriggeredEvent());
        verify(source1, times(2)).getInputStream(any());
        verify(stt1).recognize(any(), any(), any());

        // Second trigger on same group is ignored and does not start STT recognition
        ksListenerCaptor2.getValue().dtEventReceived(new DTTriggeredEvent());
        verify(source2, times(1)).getInputStream(any());
        verify(stt2, never()).recognize(any(), any(), any());

        // Releasing dialog 1 frees group-shared
        dialogManager.onReleaseGroup(context1);

        // Now dialog 2 can trigger and start STT recognition
        ksListenerCaptor2.getValue().dtEventReceived(new DTTriggeredEvent());
        verify(source2, times(2)).getInputStream(any());
        verify(stt2).recognize(any(), any(), any());
    }

    private static class InMemoryStorage<T> implements Storage<T> {
        private final Map<String, T> map = new HashMap<>();

        @Override
        @SuppressWarnings("null")
        public @Nullable T put(String key, @Nullable T value) {
            return map.put(key, value);
        }

        @Override
        public @Nullable T get(String key) {
            return map.get(key);
        }

        @Override
        public @Nullable T remove(String key) {
            return map.remove(key);
        }

        @Override
        public boolean containsKey(String key) {
            return map.containsKey(key);
        }

        @Override
        public Collection<String> getKeys() {
            return map.keySet();
        }

        @Override
        public Collection<T> getValues() {
            return map.values();
        }
    }
}
