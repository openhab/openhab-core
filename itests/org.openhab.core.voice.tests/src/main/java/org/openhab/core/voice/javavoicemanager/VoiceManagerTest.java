/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.voice.javavoicemanager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.internal.AudioManagerStub;
import org.openhab.core.voice.internal.AudioSourceStub;
import org.openhab.core.voice.internal.HumanLanguageInterpreterStub;
import org.openhab.core.voice.internal.KSServiceStub;
import org.openhab.core.voice.internal.STTServiceStub;
import org.openhab.core.voice.internal.SinkStub;
import org.openhab.core.voice.internal.TTSServiceStub;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.openhab.core.voice.internal.VoiceStub;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests for {@link VoiceManagerImpl}
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
public class VoiceManagerTest extends JavaOSGiTest {
    private static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    private static final String CONFIG_DEFAULT_KS = "defaultKS";
    private static final String CONFIG_DEFAULT_STT = "defaultSTT";
    private static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    private static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private static final String CONFIG_KEYWORD = "keyword";
    private VoiceManagerImpl voiceManager;
    private SinkStub sink;
    private TTSServiceStub ttsService;
    private VoiceStub voice;
    private HumanLanguageInterpreterStub hliStub;
    private KSServiceStub ksService;
    private STTServiceStub sttService;
    private AudioSourceStub source;

    private AudioManager audioManager;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        voiceManager = getService(VoiceManager.class, VoiceManagerImpl.class);
        assertNotNull(voiceManager);

        BundleContext context = bundleContext;
        ttsService = new TTSServiceStub(context);
        sink = new SinkStub();
        voice = new VoiceStub();
        source = new AudioSourceStub();

        registerService(sink);
        registerService(voice);

        Dictionary<String, Object> voiceConfig = new Hashtable<>();
        voiceConfig.put(CONFIG_DEFAULT_TTS, ttsService.getId());
        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(voiceConfig);

        audioManager = new AudioManagerStub();
        registerService(audioManager);
    }

    @Test
    public void saySomethingWhenTheDefaultTTSIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello", null, sink.getId());
        assertTrue(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWithAGivenVoiceIdWhenTheDefaultTTSIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello", voice.getUID(), sink.getId());
        assertTrue(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWhenTheVoiceIdIsNotFullyQualifiedTheDefaultTtsIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello", "anotherVoiceId", sink.getId());
        assertFalse(sink.getIsStreamProcessed());
    }

    @Test
    public void testTheVoiceManagerWithAGivenVoiceIdAndSinkIdWhenTheDefaultTtsIsSetAndItIsARegisteredService() {
        registerService(ttsService);
        voiceManager.say("hello", voice.getUID(), sink.getId());
        assertTrue(sink.getIsStreamProcessed());
    }

    @Test
    public void saySomethingWhenTheDefaultTtsIsSetButItIsNotARegisteredService() {
        voiceManager.say("hello", null, sink.getId());
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
        String result;
        exception.expect(InterpretationException.class);
        result = voiceManager.interpret("something", hliStub.getId());

        assertNull(result);
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

        String result = voiceManager.interpret("something", hliStub.getId());
        assertThat(result, is("Interpreted text"));
    }

    @Test
    public void verifyThatADialogIsNotStartedWhenAnyOfTheRequiredServiceIsNull() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        ttsService = null;
        hliStub = new HumanLanguageInterpreterStub();
        source = new AudioSourceStub();

        exception.expect(IllegalStateException.class);
        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, Locale.getDefault(), "word",
                null);

        assertFalse(ksService.isWordSpotted());
    }

    @Test
    public void startDialogWhenAllOfTheRequiredServicesAreAvailable() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        ttsService = new TTSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();
        source = new AudioSourceStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);
        registerService(source);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, null, "word", null);

        assertTrue(ksService.isWordSpotted());
    }

    @Test
    public void startDialogAndVerifyThatAKSExceptionIsProperlyHandled() {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        ttsService = new TTSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();
        source = new AudioSourceStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);
        registerService(source);

        ksService.setIsKsExceptionExpected(true);

        voiceManager.startDialog(ksService, sttService, ttsService, hliStub, source, sink, null, "", null);

        assertFalse(ksService.isWordSpotted());
    }

    @Test
    public void startDialogWithoutPassingAnyParameters() throws IOException, InterruptedException {
        sttService = new STTServiceStub();
        ksService = new KSServiceStub();
        ttsService = new TTSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();
        source = new AudioSourceStub();

        registerService(sttService);
        registerService(ksService);
        registerService(ttsService);
        registerService(hliStub);
        registerService(source);

        Dictionary<String, Object> config = new Hashtable<>();
        config.put(CONFIG_KEYWORD, "word");
        config.put(CONFIG_DEFAULT_STT, sttService.getId());
        config.put(CONFIG_DEFAULT_KS, ksService.getId());
        config.put(CONFIG_DEFAULT_HLI, hliStub.getId());
        config.put(CONFIG_DEFAULT_VOICE, voice.getUID());

        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(config);

        waitForAssert(() -> {
            try {
                voiceManager.startDialog();
            } catch (Exception ex) {
                // if the configuration is not updated yet the startDialog method will throw and exception which will
                // break the test
            }

            assertTrue(ksService.isWordSpotted());
        });
    }

    @Test
    public void getParameterOptionsForTheDefaultHli() throws URISyntaxException {
        hliStub = new HumanLanguageInterpreterStub();
        registerService(hliStub);

        boolean isHliStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultHLI",
                null);

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
                null);

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
                null);
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
                null);

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
        Locale locale = Locale.getDefault();

        boolean isVoiceStubInTheOptions = false;

        Collection<ParameterOption> options = voiceManager.getParameterOptions(new URI("system:voice"), "defaultVoice",
                null);

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
}
