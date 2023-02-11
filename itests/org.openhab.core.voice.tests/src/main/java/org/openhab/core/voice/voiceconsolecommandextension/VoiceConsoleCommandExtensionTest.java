/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.voice.VoiceManager;
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
@NonNullByDefault
public abstract class VoiceConsoleCommandExtensionTest extends JavaOSGiTest {
    protected @NonNullByDefault({}) VoiceManagerImpl voiceManager;
    protected @NonNullByDefault({}) VoiceConsoleCommandExtension extensionService;
    protected @NonNullByDefault({}) AudioManager audioManager;
    protected @NonNullByDefault({}) ConsoleStub console;
    protected @NonNullByDefault({}) SinkStub sink;
    protected @NonNullByDefault({}) AudioSourceStub source;

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();
        voiceManager = getService(VoiceManager.class, VoiceManagerImpl.class);
        assertNotNull(voiceManager);
        audioManager = getService(AudioManager.class, AudioManager.class);
        assertNotNull(audioManager);

        extensionService = getService(ConsoleCommandExtension.class, VoiceConsoleCommandExtension.class);
        assertNotNull(extensionService);

        sink = new SinkStub();
        source = new AudioSourceStub();
        console = new ConsoleStub();
    }
}
