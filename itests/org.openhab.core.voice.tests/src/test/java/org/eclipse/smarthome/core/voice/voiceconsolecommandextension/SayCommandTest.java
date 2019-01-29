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
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.ThingProvider;
import org.eclipse.smarthome.core.voice.TTSService;
import org.eclipse.smarthome.core.voice.internal.SinkStub;
import org.eclipse.smarthome.core.voice.internal.TTSServiceStub;
import org.eclipse.smarthome.core.voice.internal.VoiceStub;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link VoiceConsoleCommandExtensionTest} which tests the execution of the command "say".
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - migrated tests from groovy to java
 *
 */
@RunWith(Parameterized.class)
public class SayCommandTest extends VoiceConsoleCommandExtensionTest {
    private final String CONFIG_DEFAULT_TTS = "defaultTTS";
    private final String SUBCMD_SAY = "say";
    private boolean shouldItemsBePassed;
    private boolean shouldItemsBeRegistered;
    private boolean shouldMultipleItemsBeRegistered;
    private TTSService defaultTTSService;
    private boolean TTSServiceMockShouldBeRegistered;
    private boolean shouldStreamBeExpected;

    private static TTSServiceStub ttsService;
    private SinkStub sink;
    private VoiceStub voice;

    @Before
    public void setUp() {
        sink = new SinkStub();
        voice = new VoiceStub();

        BundleContext context = bundleContext;
        ttsService = new TTSServiceStub(context);

        registerService(sink);
        registerService(voice);
    }

    public SayCommandTest(boolean areItemsPassed, boolean areItemsRegistered, boolean areMultipleItemsRegistered,
            TTSService defaultTTSService, boolean isTTSServiceMockRegistered, boolean isStreamProcessedExpected) {
        this.shouldItemsBePassed = areItemsPassed;
        this.shouldItemsBeRegistered = areItemsRegistered;
        this.shouldMultipleItemsBeRegistered = areMultipleItemsRegistered;
        this.defaultTTSService = defaultTTSService;
        this.TTSServiceMockShouldBeRegistered = isStreamProcessedExpected;
        this.shouldStreamBeExpected = isStreamProcessedExpected;
    }

    @Parameters
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
        params[12] = new Object[] { true, true, true, ttsService, true, false };

        return Arrays.asList(params);
    }

    @Test
    public void testSayCommand() throws IOException {
        String[] methodParameters = new String[2];
        methodParameters[0] = SUBCMD_SAY;

        if (defaultTTSService != null) {
            Dictionary<String, Object> config = new Hashtable<String, Object>();
            config.put(CONFIG_DEFAULT_TTS, defaultTTSService);
            ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
            String pid = "org.eclipse.smarthome.voice";
            Configuration configuration = configAdmin.getConfiguration(pid);
            configuration.update(config);
        }

        if (TTSServiceMockShouldBeRegistered) {
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
