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
package org.openhab.core.voice.voiceconsolecommandextension;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.voice.internal.HumanLanguageInterpreterStub;
import org.openhab.core.voice.internal.TTSServiceStub;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.openhab.core.voice.internal.VoiceStub;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "interpret".
 *
 * @author Mihaela Memova - Initial contribution
 *
 * @author Velin Yordanov - Migrated tests from groovy to java
 */
public class InterpretCommandTest extends VoiceConsoleCommandExtensionTest {
    private static final String CONFIG_DEFAULT_HLI = "defaultHLI";
    private static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private static final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    private static final String SUBCMD_INTERPRET = "interpret";
    private static final String INTERPRETED_TEXT = "Interpreted text";
    private static final String EXCEPTION_MESSAGE = "Exception message";

    private HumanLanguageInterpreterStub hliStub;
    private VoiceStub voice;
    private TTSServiceStub ttsService;

    @Before
    public void setUp() throws IOException, InterruptedException {
        ttsService = new TTSServiceStub();
        hliStub = new HumanLanguageInterpreterStub();
        voice = new VoiceStub();

        registerService(voice);
        registerService(hliStub);
        registerService(ttsService);

        Dictionary<String, Object> config = new Hashtable<>();
        config.put(CONFIG_DEFAULT_TTS, ttsService.getId());
        config.put(CONFIG_DEFAULT_HLI, hliStub.getId());
        config.put(CONFIG_DEFAULT_VOICE, voice.getUID());
        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
        configuration.update(config);
    }

    @Test
    public void interpretSomeText() {
        waitForAssert(() -> {
            String[] params = new String[] { SUBCMD_INTERPRET, "text", "to", "be", "interpreted" };

            extensionService.execute(params, console);
            assertThat(console.getPrintedText(), is(INTERPRETED_TEXT));
        });
    }

    @Test
    public void verifyThatAnInterpretationExceptionIsHandledProperly() {
        waitForAssert(() -> {
            hliStub.setIsInterpretationExceptionExpected(true);
            String[] params = new String[] { SUBCMD_INTERPRET, "text", "to", "be", "interpreted" };
            extensionService.execute(params, console);
            assertThat(console.getPrintedText(), is(EXCEPTION_MESSAGE));
        });
    }

    @Test
    public void verifyThatNoGivenArgumentToInterpretIsHandledProperly() {
        waitForAssert(() -> {
            String[] params = new String[] { SUBCMD_INTERPRET };
            extensionService.execute(params, console);
            assertThat(console.getPrintedText(), is(not(INTERPRETED_TEXT)));
        });
    }
}
