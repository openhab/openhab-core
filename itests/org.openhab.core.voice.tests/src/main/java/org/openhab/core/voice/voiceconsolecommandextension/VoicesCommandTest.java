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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.internal.SinkStub;
import org.openhab.core.voice.internal.TTSServiceStub;
import org.openhab.core.voice.internal.VoiceStub;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "voices".
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
@NonNullByDefault
public class VoicesCommandTest extends VoiceConsoleCommandExtensionTest {
    private static final String CONFIG_LANGUAGE = "language";
    private static final String SUBCMD_VOICES = "voices";

    private @NonNullByDefault({}) LocaleProvider localeProvider;
    private @NonNullByDefault({}) TTSServiceStub ttsService;
    private @NonNullByDefault({}) SinkStub sink;
    private @NonNullByDefault({}) VoiceStub voice;

    @BeforeEach
    public void setUp() throws IOException {
        registerVolatileStorageService();
        localeProvider = getService(LocaleProvider.class);
        assertNotNull(localeProvider);

        Dictionary<String, Object> localeConfig = new Hashtable<>();
        localeConfig.put(CONFIG_LANGUAGE, Locale.ENGLISH.getLanguage());
        ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getFactoryConfiguration("org.openhab.i18n", null);
        configuration.update(localeConfig);
        configuration.update();

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
        Locale locale = localeProvider.getLocale();
        String expectedText = String.format("* %s - %s - %s (%s)", ttsService.getLabel(locale),
                voice.getLocale().getDisplayName(locale), voice.getLabel(), voice.getUID());
        extensionService.execute(command, console);

        assertThat(console.getPrintedText(), is(expectedText));
    }
}
