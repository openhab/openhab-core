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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
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
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
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
    private final ConversationManager conversationManager = mock(ConversationManager.class);
    private final InterpretationService interpretationService = mock(InterpretationService.class);
    private final SpeechSynthesizer speechSynthesizer = mock(SpeechSynthesizer.class);
    private final VoiceI18nProvider i18nProvider = mock(VoiceI18nProvider.class);
    private final ConfigDescriptionRegistry configDescriptionRegistry = mock(ConfigDescriptionRegistry.class);

    private final InMemoryStorage<DialogRegistration> storage = new InMemoryStorage<>();
    private @NonNullByDefault({}) DialogManagerImpl dialogManager;

    @BeforeEach
    public void setUp() {
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
        doReturn(storage).when(storageService).getStorage(any(), any());

        dialogManager = new DialogManagerImpl(localeProvider, audioManager, eventPublisher, storageService,
                llmToolRegistry, ttsRegistry, sttRegistry, ksRegistry, hliRegistry, conversationManager,
                interpretationService, speechSynthesizer, i18nProvider, configDescriptionRegistry);
    }

    private DialogContext createValidContext(String sourceId, String sinkId) throws Exception {
        return createValidContextBuilder(sourceId, sinkId).build();
    }

    private DialogContext.Builder createValidContextBuilder(String sourceId, String sinkId) throws Exception {
        AudioSource source = mock(AudioSource.class);
        when(source.getId()).thenReturn(sourceId);
        when(source.getLabel(null)).thenReturn(sourceId);
        when(source.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(source.getInputStream(any())).thenReturn(mock(AudioStream.class));

        AudioSink sink = mock(AudioSink.class);
        when(sink.getId()).thenReturn(sinkId);
        when(sink.getLabel(null)).thenReturn(sinkId);
        when(sink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        KSService ks = mock(KSService.class);
        when(ks.getId()).thenReturn("test-ks");
        when(ks.getUID()).thenReturn("test-ks");
        when(ks.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(ks.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(ks.spot(any(), any(), any(), any())).thenReturn(mock(KSServiceHandle.class));

        STTService stt = mock(STTService.class);
        when(stt.getId()).thenReturn("test-stt");
        when(stt.getUID()).thenReturn("test-stt");
        when(stt.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        TTSService tts = mock(TTSService.class);
        when(tts.getId()).thenReturn("test-tts");
        when(tts.getUID()).thenReturn("test-tts");

        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("test-hli");
        when(hli.getUID()).thenReturn("test-hli");
        when(hli.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));

        return new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink).withKS(ks)
                .withSTT(stt).withTTS(tts).withHLIs(List.of(hli));
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
    public void unregisterDialogStopsActiveDialogIfRunning() throws Exception {
        DialogRegistration reg = new DialogRegistration("source-1", "sink-1");
        dialogManager.registerDialog(reg);

        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.unregisterDialog("source-1");
        assertFalse(storage.containsKey("source-1"));
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    public void onBeforeDialogInterpretationUpdatesLastContext() {
        AudioSource source = mock(AudioSource.class);
        AudioSink sink = mock(AudioSink.class);
        STTService stt = mock(STTService.class);
        TTSService tts = mock(TTSService.class);
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink)
                .withSTT(stt).withTTS(tts).withHLI(hli).build();
        assertNull(dialogManager.getLastDialogContext());

        dialogManager.onBeforeDialogInterpretation(context);
        assertEquals(context, dialogManager.getLastDialogContext());
    }

    @Test
    public void listenAndAnswerPreventsDuplicateExecutionOnSameDialogGroup() throws Exception {
        AudioSource source1 = mock(AudioSource.class);
        when(source1.getId()).thenReturn("source-1");
        when(source1.getLabel(null)).thenReturn("Source 1");
        when(source1.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(source1.getInputStream(any())).thenReturn(mock(AudioStream.class));

        AudioSource source2 = mock(AudioSource.class);
        when(source2.getId()).thenReturn("source-2");
        when(source2.getLabel(null)).thenReturn("Source 2");
        when(source2.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(source2.getInputStream(any())).thenReturn(mock(AudioStream.class));

        AudioSink sink = mock(AudioSink.class);
        when(sink.getId()).thenReturn("sink-1");
        when(sink.getLabel(null)).thenReturn("Sink 1");

        TTSService tts = mock(TTSService.class);

        STTService stt = mock(STTService.class);
        when(stt.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));

        DialogContext context1 = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source1).withSink(sink)
                .withTTS(tts).withSTT(stt).withHLIs(List.of(hli)).withDialogGroup("group-A").build();

        DialogContext context2 = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source2).withSink(sink)
                .withTTS(tts).withSTT(stt).withHLIs(List.of(hli)).withDialogGroup("group-A").build();

        dialogManager.listenAndAnswer(context1);
        verify(source1).getInputStream(any());

        // Second dialog on same group should be rejected and not start audio stream
        dialogManager.listenAndAnswer(context2);
        verify(source2, never()).getInputStream(any());

        // Releasing context1 releases dialog group
        dialogManager.onDialogStopped(context1);

        // Now context2 can run
        dialogManager.listenAndAnswer(context2);
        verify(source2).getInputStream(any());
    }

    @Test
    public void startDialogSuccessfullyStartsAndTracksProcessor() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        DTServiceHandle handle = dialogManager.startDialog(context);

        assertNotNull(handle);
        assertEquals(1, dialogManager.getDialogsContexts().size());
        assertEquals(context, dialogManager.getDialogsContexts().getFirst());
    }

    @Test
    public void startDialogPreventsDuplicateForSameAudioSource() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);

        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenMissingDialogTrigger() {
        AudioSource source = mock(AudioSource.class);
        AudioSink sink = mock(AudioSink.class);
        STTService stt = mock(STTService.class);
        TTSService tts = mock(TTSService.class);
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);

        DialogContext context = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source).withSink(sink)
                .withSTT(stt).withTTS(tts).withHLIs(List.of(hli)).build();

        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenKSKeywordMissingOrEmpty() {
        AudioSource source = mock(AudioSource.class);
        AudioSink sink = mock(AudioSink.class);
        KSService ks = mock(KSService.class);
        STTService stt = mock(STTService.class);
        TTSService tts = mock(TTSService.class);
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);

        DialogContext context = new DialogContext.Builder("", Locale.ENGLISH).withSource(source).withSink(sink)
                .withKS(ks).withSTT(stt).withTTS(tts).withHLIs(List.of(hli)).build();

        assertThrows(IllegalStateException.class, () -> dialogManager.startDialog(context));
    }

    @Test
    public void startDialogThrowsWhenKSLocaleIncompatible() throws Exception {
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
    public void startDialogThrowsWhenSTTLocaleIncompatible() throws Exception {
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
    public void startDialogThrowsWhenHLILocaleIncompatible() throws Exception {
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
    public void startDialogThrowsWhenVoiceLocaleIncompatible() throws Exception {
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
    public void stopDialogStopsAndRemovesRunningProcessor() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.stopDialog(context);
        assertEquals(0, dialogManager.getDialogsContexts().size());

        assertThrows(IllegalStateException.class, () -> dialogManager.stopDialog(context.source()));
    }

    @Test
    public void stopDialogWithNullSourceUsesDefaultAudioSource() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);

        when(audioManager.getSource()).thenReturn(context.source());
        dialogManager.stopDialog((AudioSource) null);
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    public void stopDialogWithNullSourceThrowsWhenDefaultSourceMissing() {
        when(audioManager.getSource()).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> dialogManager.stopDialog((AudioSource) null));
    }

    @Test
    public void getDialogRegistrationsMarksRunningStateCorrectly() throws Exception {
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
    public void getDialogContextBuilderPopulatesDefaults() {
        AudioSource defaultSource = mock(AudioSource.class);
        AudioSink defaultSink = mock(AudioSink.class);
        when(audioManager.getSource()).thenReturn(defaultSource);
        when(audioManager.getSink()).thenReturn(defaultSink);

        DialogContext.Builder builder = dialogManager.getDialogContextBuilder();
        assertNotNull(builder);
    }

    @Test
    public void removingAudioSinkStopsDependentDialog() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.removeAudioSink(context.sink());
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    public void removingAudioSourceStopsDependentDialog() throws Exception {
        DialogContext context = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        dialogManager.removeAudioSource(context.source());
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removingServiceStopsDependentDialog() throws Exception {
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

        // Test TTS service removal
        DialogContext context1 = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(context1);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        ttsCaptor.getValue().removed(context1.tts());
        assertEquals(0, dialogManager.getDialogsContexts().size());

        // Test STT service removal
        DialogContext context2 = createValidContext("source-2", "sink-2");
        dialogManager.startDialog(context2);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        sttCaptor.getValue().removed(context2.stt());
        assertEquals(0, dialogManager.getDialogsContexts().size());

        // Test KS service removal
        DialogContext context3 = createValidContext("source-3", "sink-3");
        dialogManager.startDialog(context3);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        ksCaptor.getValue().removed((KSService) context3.dt());
        assertEquals(0, dialogManager.getDialogsContexts().size());

        // Test HLI service removal
        DialogContext context4 = createValidContext("source-4", "sink-4");
        dialogManager.startDialog(context4);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        hliCaptor.getValue().removed(context4.hlis().get(0));
        assertEquals(0, dialogManager.getDialogsContexts().size());

        // Test LLMTool removal
        LLMTool tool = mock(LLMTool.class);
        when(tool.getUID()).thenReturn("tool-1");
        DialogContext context5 = createValidContextBuilder("source-5", "sink-5").withLLMTools(Set.of(tool)).build();
        dialogManager.startDialog(context5);
        assertEquals(1, dialogManager.getDialogsContexts().size());

        llmCaptor.getValue().removed(tool);
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    public void deactivateStopsAllRunningDialogs() throws Exception {
        DialogContext context1 = createValidContext("source-1", "sink-1");
        DialogContext context2 = createValidContext("source-2", "sink-2");

        dialogManager.startDialog(context1);
        dialogManager.startDialog(context2);
        assertEquals(2, dialogManager.getDialogsContexts().size());

        dialogManager.deactivate();
        assertEquals(0, dialogManager.getDialogsContexts().size());
    }

    @Test
    public void listenAndAnswerWithCompatibleActiveDialogDelegatesToActiveProcessor() throws Exception {
        AudioSource source = mock(AudioSource.class);
        when(source.getId()).thenReturn("source-1");
        when(source.getLabel(null)).thenReturn("source-1");
        when(source.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(source.getInputStream(any())).thenReturn(mock(AudioStream.class));

        AudioSink sink = mock(AudioSink.class);
        when(sink.getId()).thenReturn("sink-1");
        when(sink.getLabel(null)).thenReturn("sink-1");
        when(sink.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        KSService ks = mock(KSService.class);
        when(ks.getId()).thenReturn("test-ks");
        when(ks.getUID()).thenReturn("test-ks");
        when(ks.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(ks.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));
        when(ks.spot(any(), any(), any(), any())).thenReturn(mock(KSServiceHandle.class));

        STTService stt = mock(STTService.class);
        when(stt.getId()).thenReturn("test-stt");
        when(stt.getUID()).thenReturn("test-stt");
        when(stt.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));
        when(stt.getSupportedFormats()).thenReturn(Set.of(AudioFormat.WAV));

        TTSService tts = mock(TTSService.class);
        when(tts.getId()).thenReturn("test-tts");
        when(tts.getUID()).thenReturn("test-tts");

        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("test-hli");
        when(hli.getUID()).thenReturn("test-hli");
        when(hli.getSupportedLocales()).thenReturn(Set.of(Locale.ENGLISH));

        DialogContext persistentContext = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source)
                .withSink(sink).withKS(ks).withSTT(stt).withTTS(tts).withHLIs(List.of(hli)).withDialogGroup("group-1")
                .build();

        dialogManager.startDialog(persistentContext);

        DialogContext simpleContext = new DialogContext.Builder("keyword", Locale.ENGLISH).withSource(source)
                .withSink(sink).withKS(ks).withSTT(stt).withTTS(tts).withHLIs(List.of(hli)).withDialogGroup("group-2")
                .build();

        assertDoesNotThrow(() -> dialogManager.listenAndAnswer(simpleContext));
    }

    @Test
    public void listenAndAnswerWithIncompatibleActiveDialogThrows() throws Exception {
        DialogContext persistentContext = createValidContext("source-1", "sink-1");
        dialogManager.startDialog(persistentContext);

        AudioSink differentSink = mock(AudioSink.class);
        when(differentSink.getId()).thenReturn("sink-different");
        when(differentSink.getLabel(null)).thenReturn("sink-different");

        DialogContext incompatibleContext = createValidContextBuilder("source-1", "sink-1").withSink(differentSink)
                .withDialogGroup("group-different").build();

        assertThrows(IllegalStateException.class, () -> dialogManager.listenAndAnswer(incompatibleContext));
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
