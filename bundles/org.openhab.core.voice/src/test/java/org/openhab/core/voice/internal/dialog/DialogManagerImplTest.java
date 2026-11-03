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
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.DialogRegistration;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.text.conversation.ConversationManager;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;

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
