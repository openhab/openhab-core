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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.eclipse.smarthome.core.voice.internal.SinkStub;
import org.eclipse.smarthome.core.voice.internal.TTSServiceStub;
import org.eclipse.smarthome.core.voice.internal.VoiceStub;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "voices".
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - migrated tests from groovy to java
 *
 */
public class VoicesCommandTest extends VoiceConsoleCommandExtensionTest {
    private final String SUBCMD_VOICES = "voices";
    private TTSServiceStub ttsService;
    private SinkStub sink;
    private VoiceStub voice;

    @Before
    public void setUp() {
        BundleContext context = bundleContext;

        ttsService = new TTSServiceStub(context);
        sink = new SinkStub();
        voice = new VoiceStub();

        registerService(ttsService);
        registerService(sink);
        registerService(voice);
    }

    @Test
    public void testVoicesCommand() {
        String[] command = new String[] { SUBCMD_VOICES };
        Locale locale = Locale.getDefault();
        String expectedText = String.format("* %s - %s - %s (%s)", ttsService.getLabel(locale),
                voice.getLocale().getDisplayName(locale), voice.getLabel(), voice.getUID());
        extensionService.execute(command, console);

        assertThat(console.getPrintedText(), is(expectedText));
    }
}
