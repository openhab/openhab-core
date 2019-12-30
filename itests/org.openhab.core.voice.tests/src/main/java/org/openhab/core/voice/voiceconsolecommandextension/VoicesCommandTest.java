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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.voice.internal.SinkStub;
import org.openhab.core.voice.internal.TTSServiceStub;
import org.openhab.core.voice.internal.VoiceStub;
import org.osgi.framework.BundleContext;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "voices".
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
public class VoicesCommandTest extends VoiceConsoleCommandExtensionTest {
    private static final String SUBCMD_VOICES = "voices";
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
