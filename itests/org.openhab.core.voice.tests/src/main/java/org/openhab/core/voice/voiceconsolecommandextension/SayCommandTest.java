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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.internal.SinkStub;
import org.openhab.core.voice.internal.TTSServiceStub;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.openhab.core.voice.internal.VoiceStub;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "say".
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated tests from groovy to java
 */
@NonNullByDefault
public class SayCommandTest extends VoiceConsoleCommandExtensionTest {

    private static final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private static final String SUBCMD_SAY = "say";

    private static @Nullable TTSServiceStub ttsService;

    private @NonNullByDefault({}) SinkStub sink;
    private @NonNullByDefault({}) VoiceStub voice;

    @BeforeEach
    public void setUp() {
        registerVolatileStorageService();
        sink = new SinkStub();
        voice = new VoiceStub();

        BundleContext context = bundleContext;
        ttsService = new TTSServiceStub(context);

        registerService(sink);
        registerService(voice);
    }

    public static Collection<Object[]> data() {
        Object[][] params = new Object[13][5];
        params[0] = new Object[] { false, false, false, null, false, false };
        params[1] = new Object[] { false, false, false, null, true, true };
        params[2] = new Object[] { false, false, false, ttsService, true, true };
        params[3] = new Object[] { false, false, false, ttsService, false, false };
        params[4] = new Object[] { true, true, false, null, false, false };
        params[5] = new Object[] { true, true, false, null, true, true };
        params[6] = new Object[] { true, true, false, ttsService, true, true };
        params[7] = new Object[] { true, true, false, ttsService, false, false };
        params[8] = new Object[] { true, false, false, null, false, false };
        params[9] = new Object[] { true, false, false, null, true, true };
        params[10] = new Object[] { true, false, false, ttsService, true, true };
        params[11] = new Object[] { true, false, false, ttsService, false, false };
        params[12] = new Object[] { true, true, true, ttsService, true, true };

        return List.of(params);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testSayCommand(boolean shouldItemsBePassed, boolean shouldItemsBeRegistered,
            boolean shouldMultipleItemsBeRegistered, @Nullable TTSService defaultTTSService,
            boolean ttsServiceMockShouldBeRegistered, boolean shouldStreamBeExpected) throws IOException {
        String[] methodParameters = new String[2];
        methodParameters[0] = SUBCMD_SAY;

        if (defaultTTSService != null) {
            ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
            Dictionary<String, Object> audioConfig = new Hashtable<>();
            audioConfig.put("defaultSink", sink.getId());
            Configuration configuration = configAdmin.getConfiguration("org.openhab.audio", null);
            configuration.update(audioConfig);
            Dictionary<String, Object> voiceConfig = new Hashtable<>();
            voiceConfig.put(CONFIG_DEFAULT_TTS, defaultTTSService);
            configuration = configAdmin.getConfiguration(VoiceManagerImpl.CONFIGURATION_PID);
            configuration.update(voiceConfig);
        }

        TTSService ttsService = SayCommandTest.ttsService;
        if (ttsServiceMockShouldBeRegistered && ttsService != null) {
            registerService(ttsService);
        }

        if (shouldItemsBePassed) {
            VolatileStorageService volatileStorageService = new VolatileStorageService();
            registerService(volatileStorageService);

            ManagedThingProvider managedThingProvider = getService(ThingProvider.class, ManagedThingProvider.class);
            assertNotNull(managedThingProvider);

            ItemRegistry itemRegistry = getService(ItemRegistry.class);
            assertNotNull(itemRegistry);

            Item item = new StringItem("itemName");

            if (shouldItemsBeRegistered) {
                itemRegistry.add(item);
            }
            methodParameters[1] = "%" + item.getName() + "%";

            if (shouldMultipleItemsBeRegistered) {
                Item item1 = new StringItem("itemName1");
                itemRegistry.add(item1);

                Item item2 = new StringItem("itemName2");
                itemRegistry.add(item2);

                Item item3 = new StringItem("itemName3");
                itemRegistry.add(item3);

                methodParameters[1] = "%itemName.%";
            }
        } else {
            methodParameters[1] = "hello";
        }
        extensionService.execute(methodParameters, console);

        assertThat(sink.isStreamProcessed(), is(shouldStreamBeExpected));
    }
}
