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
package org.openhab.core.voice.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.openhab.core.voice.tts.Voice;

/**
 * Unit tests for {@link VoiceServiceResolver}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class VoiceServiceResolverTest {

    private static final URI VOICE_CONFIG_URI = URI.create(VoiceConfigurationConstants.CONFIG_URI);

    private final TTSServiceRegistry ttsRegistry = mock(TTSServiceRegistry.class);
    private final STTServiceRegistry sttRegistry = mock(STTServiceRegistry.class);
    private final KSServiceRegistry ksRegistry = mock(KSServiceRegistry.class);
    private final HumanLanguageInterpreterRegistry hliRegistry = mock(HumanLanguageInterpreterRegistry.class);
    private final LocaleProvider localeProvider = mock(LocaleProvider.class);
    private final ConfigDescriptionRegistry configDescriptionRegistry = mock(ConfigDescriptionRegistry.class);

    private @NonNullByDefault({}) VoiceServiceResolver resolver;

    @BeforeEach
    public void setUp() {
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
        resolver = new VoiceServiceResolver(ttsRegistry, sttRegistry, ksRegistry, hliRegistry, localeProvider,
                configDescriptionRegistry);
    }

    @Test
    public void getTTSReturnsDefaultWhenConfigured() {
        TTSService defaultTTS = mock(TTSService.class);
        when(ttsRegistry.get("myTTS")).thenReturn(defaultTTS);

        resolver.modified(Map.of("defaultTTS", "myTTS"));

        assertEquals(defaultTTS, resolver.getTTS());
    }

    @Test
    public void getTTSFallsBackToFirstAvailableWhenNoDefaultConfigured() {
        TTSService firstTTS = mock(TTSService.class);
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.of(firstTTS));

        assertEquals(firstTTS, resolver.getTTS());
    }

    @Test
    public void getTTSReturnsNullWhenEmpty() {
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.empty());

        assertNull(resolver.getTTS());
    }

    @Test
    public void getTTSByIdReturnsService() {
        TTSService tts = mock(TTSService.class);
        when(ttsRegistry.get("tts-1")).thenReturn(tts);

        assertEquals(tts, resolver.getTTS("tts-1"));
        assertNull(resolver.getTTS("non-existent"));
        assertNull(resolver.getTTS((String) null));
    }

    @Test
    public void getTTSByVoiceParsesUID() {
        Voice voice = mock(Voice.class);
        when(voice.getUID()).thenReturn("tts-1:voice-en");
        TTSService tts = mock(TTSService.class);
        when(ttsRegistry.get("tts-1")).thenReturn(tts);

        assertEquals(tts, resolver.getTTS(voice));
    }

    @Test
    public void getSTTReturnsDefaultWhenConfigured() {
        STTService defaultSTT = mock(STTService.class);
        when(sttRegistry.get("mySTT")).thenReturn(defaultSTT);

        resolver.modified(Map.of("defaultSTT", "mySTT"));

        assertEquals(defaultSTT, resolver.getSTT());
    }

    @Test
    public void getSTTFallsBackToFirstAvailableWhenNoDefaultConfigured() {
        STTService firstSTT = mock(STTService.class);
        when(sttRegistry.stream()).thenAnswer(inv -> Stream.of(firstSTT));

        assertEquals(firstSTT, resolver.getSTT());
    }

    @Test
    public void getSTTByIdReturnsService() {
        STTService stt = mock(STTService.class);
        when(sttRegistry.get("stt-1")).thenReturn(stt);

        assertEquals(stt, resolver.getSTT("stt-1"));
        assertNull(resolver.getSTT("non-existent"));
        assertNull(resolver.getSTT(null));
    }

    @Test
    public void getKSReturnsDefaultWhenConfigured() {
        KSService defaultKS = mock(KSService.class);
        when(ksRegistry.get("myKS")).thenReturn(defaultKS);

        resolver.modified(Map.of("defaultKS", "myKS"));

        assertEquals(defaultKS, resolver.getKS());
    }

    @Test
    public void getKSFallsBackToFirstAvailableWhenNoDefaultConfigured() {
        KSService firstKS = mock(KSService.class);
        when(ksRegistry.stream()).thenAnswer(inv -> Stream.of(firstKS));

        assertEquals(firstKS, resolver.getKS());
    }

    @Test
    public void getKSByIdReturnsService() {
        KSService ks = mock(KSService.class);
        when(ksRegistry.get("ks-1")).thenReturn(ks);

        assertEquals(ks, resolver.getKS("ks-1"));
        assertNull(resolver.getKS("non-existent"));
        assertNull(resolver.getKS(null));
    }

    @Test
    public void getHLIReturnsDefaultWhenConfigured() {
        HumanLanguageInterpreter defaultHLI = mock(HumanLanguageInterpreter.class);
        when(hliRegistry.get("myHLI")).thenReturn(defaultHLI);

        resolver.modified(Map.of("defaultHLI", "myHLI"));

        assertEquals(defaultHLI, resolver.getHLI());
    }

    @Test
    public void getHLIFallsBackToFirstAvailableWhenNoDefaultConfigured() {
        HumanLanguageInterpreter firstHLI = mock(HumanLanguageInterpreter.class);
        when(hliRegistry.stream()).thenAnswer(inv -> Stream.of(firstHLI));

        assertEquals(firstHLI, resolver.getHLI());
    }

    @Test
    public void getHLIByIdReturnsService() {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hliRegistry.get("hli-1")).thenReturn(hli);

        assertEquals(hli, resolver.getHLI("hli-1"));
        assertNull(resolver.getHLI("non-existent"));
        assertNull(resolver.getHLI(null));
    }

    @Test
    public void getHLIsByIdsDelegatesToRegistry() {
        HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
        HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);
        when(hliRegistry.getByIds("hli1,hli2")).thenReturn(List.of(hli1, hli2));
        when(hliRegistry.getByIds(List.of("hli1", "hli2"))).thenReturn(List.of(hli1, hli2));

        assertEquals(List.of(hli1, hli2), resolver.getHLIsByIds("hli1,hli2"));
        assertEquals(List.of(hli1, hli2), resolver.getHLIsByIds(List.of("hli1", "hli2")));
    }

    @Test
    public void getDefaultVoiceReturnsConfiguredVoice() {
        Voice voice = mock(Voice.class);
        when(voice.getUID()).thenReturn("tts-1:voice-1");
        TTSService tts = mock(TTSService.class);
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));
        when(ttsRegistry.get("tts-1")).thenReturn(tts);

        resolver.modified(Map.of("defaultVoice", "tts-1:voice-1"));

        assertEquals(voice, resolver.getDefaultVoice());
    }

    @Test
    public void getVoiceResolvesQualifiedAndUnqualifiedVoiceIds() {
        Voice voice1 = mock(Voice.class);
        when(voice1.getUID()).thenReturn("tts-1:voice-en");
        TTSService tts1 = mock(TTSService.class);
        when(tts1.getAvailableVoices()).thenReturn(Set.of(voice1));
        when(ttsRegistry.get("tts-1")).thenReturn(tts1);
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.of(tts1));

        // Fully qualified
        assertEquals(voice1, resolver.getVoice("tts-1:voice-en"));

        // Unqualified (uses default/first TTS)
        assertEquals(voice1, resolver.getVoice("voice-en"));

        // Null and non-matching
        assertNull(resolver.getVoice(null));
        assertNull(resolver.getVoice("unknown"));
    }

    @Test
    public void getAllVoicesReturnsSortedSet() {
        TTSService tts = mock(TTSService.class);
        when(tts.getLabel(Locale.ENGLISH)).thenReturn("TTS Label");

        Voice voiceA = mock(Voice.class);
        when(voiceA.getUID()).thenReturn("tts:vA");
        when(voiceA.getLocale()).thenReturn(Locale.ENGLISH);
        when(voiceA.getLabel()).thenReturn("Alice");

        Voice voiceB = mock(Voice.class);
        when(voiceB.getUID()).thenReturn("tts:vB");
        when(voiceB.getLocale()).thenReturn(Locale.ENGLISH);
        when(voiceB.getLabel()).thenReturn("Bob");

        when(tts.getAvailableVoices()).thenReturn(Set.of(voiceB, voiceA));
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.of(tts));
        when(ttsRegistry.get("tts")).thenReturn(tts);

        Set<Voice> sorted = resolver.getAllVoices();
        List<Voice> list = sorted.stream().toList();
        assertEquals(2, list.size());
        assertEquals(voiceA, list.get(0));
        assertEquals(voiceB, list.get(1));
    }

    @Test
    public void getPreferredVoiceSelectsMatchingLocale() {
        Voice voiceEn = mock(Voice.class);
        when(voiceEn.getLocale()).thenReturn(Locale.ENGLISH);
        Voice voiceDe = mock(Voice.class);
        when(voiceDe.getLocale()).thenReturn(Locale.GERMAN);

        Voice preferred = resolver.getPreferredVoice(Set.of(voiceDe, voiceEn));
        assertEquals(voiceEn, preferred);

        assertNull(resolver.getPreferredVoice(Set.of()));
    }

    @Test
    public void getParameterOptionsReturnsNullForUnknownUriOrParam() {
        assertNull(resolver.getParameterOptions(URI.create("unknown:uri"),
                VoiceConfigurationConstants.CONFIG_DEFAULT_HLI, null, null));
        assertNull(resolver.getParameterOptions(VOICE_CONFIG_URI, "unknownParam", null, null));
    }

    @Test
    public void getParameterOptionsForTheDefaultHli() {
        HumanLanguageInterpreter hli = mock(HumanLanguageInterpreter.class);
        when(hli.getId()).thenReturn("hliStub");
        when(hli.getLabel(null)).thenReturn("HLI Label");
        when(hliRegistry.stream()).thenAnswer(inv -> Stream.of(hli));

        boolean isHliStubInTheOptions = false;

        Collection<ParameterOption> options = resolver.getParameterOptions(VOICE_CONFIG_URI,
                VoiceConfigurationConstants.CONFIG_DEFAULT_HLI, null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String hliStubId = hli.getId();
            String hliLabel = hli.getLabel(null);
            if (optionValue.equals(hliStubId) && optionLabel.equals(hliLabel)) {
                isHliStubInTheOptions = true;
            }
        }

        assertTrue(isHliStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultKs() {
        KSService ks = mock(KSService.class);
        when(ks.getId()).thenReturn("ksStub");
        when(ks.getLabel(null)).thenReturn("KS Label");
        when(ksRegistry.stream()).thenAnswer(inv -> Stream.of(ks));

        boolean isKSStubInTheOptions = false;

        Collection<ParameterOption> options = resolver.getParameterOptions(VOICE_CONFIG_URI,
                VoiceConfigurationConstants.CONFIG_DEFAULT_KS, null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String ksStubId = ks.getId();
            String ksLabel = ks.getLabel(null);
            if (optionValue.equals(ksStubId) && optionLabel.equals(ksLabel)) {
                isKSStubInTheOptions = true;
            }
        }

        assertTrue(isKSStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultSTT() {
        STTService stt = mock(STTService.class);
        when(stt.getId()).thenReturn("sttStub");
        when(stt.getLabel(null)).thenReturn("STT Label");
        when(sttRegistry.stream()).thenAnswer(inv -> Stream.of(stt));

        boolean isSTTStubInTheOptions = false;

        Collection<ParameterOption> options = resolver.getParameterOptions(VOICE_CONFIG_URI,
                VoiceConfigurationConstants.CONFIG_DEFAULT_STT, null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String sttStubId = stt.getId();
            String sttLabel = stt.getLabel(null);
            if (optionValue.equals(sttStubId) && optionLabel.equals(sttLabel)) {
                isSTTStubInTheOptions = true;
            }
        }

        assertTrue(isSTTStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultTTS() {
        TTSService tts = mock(TTSService.class);
        when(tts.getId()).thenReturn("ttsStub");
        when(tts.getLabel(null)).thenReturn("TTS Label");
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.of(tts));

        boolean isTTSStubInTheOptions = false;

        Collection<ParameterOption> options = resolver.getParameterOptions(VOICE_CONFIG_URI,
                VoiceConfigurationConstants.CONFIG_DEFAULT_TTS, null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String ttsStubId = tts.getId();
            String ttsLabel = tts.getLabel(null);
            if (optionValue.equals(ttsStubId) && optionLabel.equals(ttsLabel)) {
                isTTSStubInTheOptions = true;
            }
        }

        assertTrue(isTTSStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultVoice() {
        Voice voice = mock(Voice.class);
        when(voice.getUID()).thenReturn("ttsStub:voiceStub");
        when(voice.getLocale()).thenReturn(Locale.ENGLISH);
        when(voice.getLabel()).thenReturn("Voice Label");

        TTSService tts = mock(TTSService.class);
        when(tts.getId()).thenReturn("ttsStub");
        when(tts.getLabel(Locale.ENGLISH)).thenReturn("TTS Label");
        when(tts.getAvailableVoices()).thenReturn(Set.of(voice));

        when(ttsRegistry.get("ttsStub")).thenReturn(tts);
        when(ttsRegistry.stream()).thenAnswer(inv -> Stream.of(tts));

        Locale locale = localeProvider.getLocale();

        boolean isVoiceStubInTheOptions = false;

        Collection<ParameterOption> options = resolver.getParameterOptions(VOICE_CONFIG_URI,
                VoiceConfigurationConstants.CONFIG_DEFAULT_VOICE, null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String voiceStubId = voice.getUID();
            String expectedLabel = String.format("%s - %s - %s", tts.getLabel(locale),
                    voice.getLocale().getDisplayName(locale), voice.getLabel());

            if (optionValue.equals(voiceStubId) && optionLabel.equals(expectedLabel)) {
                isVoiceStubInTheOptions = true;
            }
        }

        assertTrue(isVoiceStubInTheOptions);
    }
}
