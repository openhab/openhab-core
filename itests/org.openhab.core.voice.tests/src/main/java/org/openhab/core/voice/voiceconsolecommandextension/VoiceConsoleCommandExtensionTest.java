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

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.internal.AudioManagerStub;
import org.openhab.core.voice.internal.AudioSourceStub;
import org.openhab.core.voice.internal.ConsoleStub;
import org.openhab.core.voice.internal.SinkStub;
import org.openhab.core.voice.internal.VoiceConsoleCommandExtension;
import org.openhab.core.voice.internal.VoiceManagerImpl;

/**
 * A base class for the classes testing the different commands of the {@link VoiceConsoleCommandExtension}.
 * It takes care of the mocks, stubs and services that are used in all the extending classes.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
public abstract class VoiceConsoleCommandExtensionTest extends JavaOSGiTest {
    protected VoiceManagerImpl voiceManager;
    protected VoiceConsoleCommandExtension extensionService;
    protected AudioManager audioManager;
    protected ConsoleStub console;
    protected SinkStub sink;
    protected AudioSourceStub source;

    @Before
    public void setup() {
        voiceManager = getService(VoiceManager.class, VoiceManagerImpl.class);
        assertNotNull(voiceManager);

        extensionService = getService(ConsoleCommandExtension.class, VoiceConsoleCommandExtension.class);
        assertNotNull(extensionService);

        sink = new SinkStub();
        source = new AudioSourceStub();
        audioManager = new AudioManagerStub();
        console = new ConsoleStub();

        registerService(audioManager);
        registerService(voiceManager);
    }
}
