/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.voice.voiceconsolecommandextension;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.smarthome.core.voice.internal.HumanLanguageInterpreterStub;
import org.eclipse.smarthome.core.voice.internal.TTSServiceStub;
import org.eclipse.smarthome.core.voice.internal.VoiceStub;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "interpret".
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - Migrated tests from groovy to java
 *
 */
public class InterpretCommandTest extends VoiceConsoleCommandExtensionTest {
    private final String CONFIG_DEFAULT_HLI = "defaultHLI";
    private final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private final String CONFIG_DEFAULT_VOICE = "defaultVoice";
    private final String SUBCMD_INTERPRET = "interpret";
    private final String INTERPRETED_TEXT = "Interpreted text";
    private final String EXCEPTION_MESSAGE = "Exception message";

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

        Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put(CONFIG_DEFAULT_TTS, ttsService.getId());
        config.put(CONFIG_DEFAULT_HLI, hliStub.getId());
        config.put(CONFIG_DEFAULT_VOICE, voice.getUID());
        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        String pid = "org.eclipse.smarthome.voice";
        Configuration configuration = configAdmin.getConfiguration(pid);
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
