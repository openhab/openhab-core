/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests for {@link org.openhab.core.voice.internal.VoiceManagerImpl}
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
public class VoiceManagerImplTest extends JavaOSGiTest {
    private static final String CONFIG_DEFAULT_SINK = "defaultSink";
    private static final String CONFIG_DEFAULT_SOURCE = "defaultSource";
    private static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    private static final String CONFIG_DEFAULT_KS = "defaultKS";
    private static final String CONFIG_DEFAULT_STT = "defaultSTT";
    private static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    private static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private static final String CONFIG_KEYWORD = "keyword";
    private static final String CONFIG_LANGUAGE = "language";
    private VoiceManagerImpl voiceManager;
    private AudioManager audioManager;
    private LocaleProvider localeProvider;
    private TranslationProvider i18nProvider;
    private SinkStub sink;
    private TTSServiceStub ttsService;
    private VoiceStub voice;
    private HumanLanguageInterpreterStub hliStub;
    private KSServiceStub ksService;
    private STTServiceStub sttService;
    private AudioSourceStub source;

    @BeforeEach
    public void setUp() throws IOException {
        BundleContext context = bundleContext;
        ttsService = new TTSServiceStub(context);
        sink = new SinkStub();
        voice = new VoiceStub();
        source = new AudioSourceStub();

        registerService(sink);
        registerService(voice);
        registerService(source);

        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);

        audioManager = getService(AudioManager.class);
        assertNotNull(audioManager);

        Dictionary<String, Object> audioConfig = new Hashtable<>();
        audioConfig.put(CONFIG_DEFAULT_SINK, sink.getId());
        audioConfig.put(CONFIG_DEFAULT_SOURCE, source.getId());
        Configuration configuration = configAdmin.getConfiguration("org.openhab.audio", null);
        configuration.update(audioConfig);
        configuration.update();

        localeProvider = getService(LocaleProvider.class);
        assertNotNull(localeProvider);

        Dictionary<String, Object> localeConfig = new Hashtable<>();
        localeConfig.put(CONFIG_LANGUAGE, Locale.ENGLISH.getLanguage());
        configuration = configAdmin.getConfiguration("org.openhab.i18n", null);
        configuration.update(localeConfig);
        configuration.update();

        i18nProvider = getService(TranslationProvider.class);
        assertNotNull(i18nProvider);

        voiceManager = getService(VoiceManager.class, VoiceManagerImpl.class);
        assertNotNull(voiceManager);

        Dictionary<String, Object> voiceConfig = new Hashtable<>();
        voiceConfig.put(CONFIG_DEFAULT_TTS, ttsService.getId());
        configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(voiceConfig);
    }

    @Test
    public void saySomethingWhenTheDefaultTTSIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello Jack", null, sink.getId());
        assertTrue(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWithAGivenVoiceIdWhenTheDefaultTTSIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello John", voice.getUID(), sink.getId());
        assertTrue(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWhenTheVoiceIdIsNotFullyQualifiedTheDefaultTtsIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello Kate", "anotherVoiceId", sink.getId());
        assertFalse(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWhenTheDefaultTtsIsSetButItIsNotARegisteredService() {
        voiceManager.say("hello Jennifer", null, sink.getId());
        assertFalse(sink.getIsStreamProcessed());
    }

    @Test
    public void interpretSomethingWithGivenHliIdWhenTheHliIsARegisteredService() throws InterpretationException {
        hliStub = new HumanLanguageInterpreterStub();
        registerService(hliStub);

        String result = voiceManager.interpret("something", hliStub.getId());
        assertThat(result, is("Interpreted text"));
    }

    @Test
    public void interpretSomethingWithGivenHliIdEhenTheHliIsNotARegisteredService() throws InterpretationException {
        hliStub = new HumanLanguageInterpreterStub();

        assertThrows(InterpretationException.class, () -> voiceManager.interpret("something", hliStub.getId()));
    }

    @Test
    public void interpretSomethingWhenTheDefaultHliIsSetAndItIsARegisteredService()
            throws IOException, InterpretationException {
        hliStub = new HumanLanguageInterpreterStub();
        registerService(hliStub);

        Dictionary<String, Object> voiceConfig = new Hashtable<>();
        voiceConfig.put("defaultHLI", hliStub.getId());
        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(voiceConfig);

        // Wait some time to be sure that the configuration will be updated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        String result = voiceManager.interpret("something", null);
        assertThat(result, is("Interpreted text"));
    }

    @Test
    public void verifyThatADialogIsNotStartedWhenAnyOfTheRequiredServiceIsNull() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        assertThrows(IllegalStateException.class, () -> voiceManager.startDialog(ksService, sttService, null, hliStub,
                source, sink, Locale.ENGLISH, "word", null));

        assertFalse(ksService.isWordSpotted());
        assertFalse(sink.getIsStreamProcessed());
    }

    @Test
    public void startDialogWhenAllOfTheRequiredServicesAreAvailable() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertTrue(ksService.isWordSpotted());
        assertTrue(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is("Recognized text"));
        assertThat(hliStub.getAnswer(), is("Interpreted text"));
        assertThat(ttsService.getSynthesized(), is("Interpreted text"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);

        assertTrue(ksService.isAborted());
    }

    @Test
    public void startDialogAndVerifyThatAKSExceptionIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        ksService.setExceptionExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "", null);

        assertFalse(ksService.isWordSpotted());
        assertFalse(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is(""));
        assertThat(hliStub.getAnswer(), is(""));
        assertThat(ttsService.getSynthesized(), is(""));
        assertFalse(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);
    }

    @Test
    public void startDialogAndVerifyThatAKSErrorIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        ksService.setErrorExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertFalse(ksService.isWordSpotted());
        assertFalse(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is(""));
        assertThat(hliStub.getAnswer(), is(""));
        assertThat(ttsService.getSynthesized(),
                is("Encountered error while spotting keywords, keyword spotting error"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);
    }

    @Test
    public void startDialogAndVerifyThatASTTExceptionIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        sttService.setExceptionExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertTrue(ksService.isWordSpotted());
        assertFalse(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is(""));
        assertThat(hliStub.getAnswer(), is(""));
        assertThat(ttsService.getSynthesized(), is("Error during recognition, STT exception"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);
    }

    @Test
    public void startDialogAndVerifyThatASpeechRecognitionErrorIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        sttService.setErrorExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertTrue(ksService.isWordSpotted());
        assertFalse(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is(""));
        assertThat(hliStub.getAnswer(), is(""));
        assertThat(ttsService.getSynthesized(), is("Encountered error while recognizing text, STT error"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);
    }

    @Test
    public void startDialogAndVerifyThatAnInterpretationExceptionIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        hliStub.setExceptionExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertTrue(ksService.isWordSpotted());
        assertTrue(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is("Recognized text"));
        assertThat(hliStub.getAnswer(), is(""));
        assertThat(ttsService.getSynthesized(), is("interpretation exception"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(source);
    }

    @Test
    public void startDialogWithoutPassingAnyParameters() throws IOException, InterruptedException {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        Dictionary<String, Object> config = new Hashtable<>();
        config.put(CONFIG_KEYWORD, "word");
        config.put(CONFIG_DEFAULT_STT, sttService.getId());
        config.put(CONFIG_DEFAULT_KS, ksService.getId());
        config.put(CONFIG_DEFAULT_HLI, hliStub.getId());
        config.put(CONFIG_DEFAULT_VOICE, voice.getUID());

        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(config);

        // Wait some time to be sure that the configuration will be updated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        voiceManager.startDialog();

        assertTrue(ksService.isWordSpotted());
        assertTrue(sttService.isRecognized());
        assertThat(hliStub.getQuestion(), is("Recognized text"));
        assertThat(hliStub.getAnswer(), is("Interpreted text"));
        assertThat(ttsService.getSynthesized(), is("Interpreted text"));
        assertTrue(sink.getIsStreamProcessed());

        voiceManager.stopDialog(null);

        assertTrue(ksService.isAborted());
    }

    @Test
    public void verifyThatOnlyOneDialogPerSourceIsPossible() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.ENGLISH, "word",
                null);

        assertTrue(ksService.isWordSpotted());

        assertThrows(IllegalStateException.class, () -> voiceManager.startDialog(ksService, sttService, ttsService,
                hliStub, source, sink, Locale.ENGLISH, "word", null));

        voiceManager.stopDialog(source);

        assertTrue(ksService.isAborted());

        assertThrows(IllegalStateException.class, () -> voiceManager.stopDialog(source));
    }

    @Test
    public void getParameterOptionsForTheDefaultHli() throws URISyntaxException {
        hliStub = new HumanLanguageInterpreterStub();
        registerService(hliStub);

        boolean isHliStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultHLI",
                null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String hliStubId = hliStub.getId();
            String hliLabel = hliStub.getLabel(null);
            if (optionValue.equals(hliStubId) && optionLabel.equals(hliLabel)) {
                isHliStubInTheOptions = true;
            }
        }

        assertTrue(isHliStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultKs() throws URISyntaxException {
        ksService = new KSServiceStub();
        registerService(ksService);

        boolean isKSStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultKS",
                null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String ksStubId = ksService.getId();
            String ksLabel = ksService.getLabel(null);
            if (optionValue.equals(ksStubId) && optionLabel.equals(ksLabel)) {
                isKSStubInTheOptions = true;
            }
        }

        assertTrue(isKSStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultSTT() throws URISyntaxException {
        sttService = new STTServiceStub();
        registerService(sttService);

        boolean isSTTStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultSTT",
                null, null);
        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String sttStubId = sttService.getId();
            String sttLabel = sttService.getLabel(null);
            if (optionValue.equals(sttStubId) && optionLabel.equals(sttLabel)) {
                isSTTStubInTheOptions = true;
            }
        }

        assertTrue(isSTTStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultTts() throws URISyntaxException {
        ttsService = new TTSServiceStub();
        registerService(ttsService);

        boolean isTTSStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultTTS",
                null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String ttsStubId = ttsService.getId();
            String ttsLabel = ttsService.getLabel(null);
            if (optionValue.equals(ttsStubId) && optionLabel.equals(ttsLabel)) {
                isTTSStubInTheOptions = true;
            }
        }

        assertTrue(isTTSStubInTheOptions);
    }

    @Test
    public void getParameterOptionsForTheDefaultVoice() throws URISyntaxException {
        BundleContext context = bundleContext;
        ttsService = new TTSServiceStub(context);
        registerService(ttsService);
        Locale locale = localeProvider.getLocale();

        boolean isVoiceStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultVoice",
                null, null);

        assertNotNull(options);

        for (ParameterOption option : options) {
            String optionValue = option.getValue();
            String optionLabel = option.getLabel();
            String voiceStubId = voice.getUID();
            String expectedLabel = String.format("%s - %s - %s", ttsService.getLabel(locale),
                    voice.getLocale().getDisplayName(locale), voice.getLabel());

            if (optionValue.equals(voiceStubId) && optionLabel.equals(expectedLabel)) {
                isVoiceStubInTheOptions = true;
            }
        }

        assertTrue(isVoiceStubInTheOptions);
    }

    @Test
    public void getPreferredVoiceOfAvailableTTSService() {
        Voice voice = voiceManager.getPreferredVoice(ttsService.getAvailableVoices());
        assertNotNull(voice);
    }

    @Test
    public void getPreferredVoiceOfEmptySet() {
        Voice voice = voiceManager.getPreferredVoice(Set.of());
        assertNull(voice);
    }
}
