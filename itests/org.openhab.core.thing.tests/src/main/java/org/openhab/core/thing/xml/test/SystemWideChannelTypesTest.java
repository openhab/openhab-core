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
package org.openhab.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.xml.test.LoadedTestBundle.StuffAddition;

/**
 * @author Ivan Iliev - Initial contribution
 */
@NonNullByDefault
public class SystemWideChannelTypesTest extends JavaOSGiTest {

    private LoadedTestBundle loadedSystemChannelsBundle() throws Exception {
        return new LoadedTestBundle("SystemChannels.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(1).channelTypes(1));
    }

    private LoadedTestBundle loadedSystemChannelsUserBundle() throws Exception {
        return new LoadedTestBundle("SystemChannelsUser.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(1).channelTypes(0));
    }

    private LoadedTestBundle loadedSystemChannelsWithoutThingTypesBundle() throws Exception {
        return new LoadedTestBundle("SystemChannelsNoThingTypes.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(0).channelTypes(1));
    }

    private @NonNullByDefault({}) ThingTypeProvider thingTypeProvider;
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    private @NonNullByDefault({}) ChannelTypeProvider systemChannelTypeProvider;

    @BeforeEach
    public void setUp() {
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));

        channelTypeRegistry = getService(ChannelTypeRegistry.class);
        assertThat(channelTypeRegistry, is(notNullValue()));

        ChannelTypeProvider provider = getService(ChannelTypeProvider.class, DefaultSystemChannelTypeProvider.class);
        assertTrue(provider instanceof DefaultSystemChannelTypeProvider);
        systemChannelTypeProvider = provider;
    }

    @Test
    public void systemChannelsShouldLoadAndUnload() throws Exception {
        try (final AutoCloseable unused = loadedSystemChannelsBundle()) {
        }
    }

    @Test
    public void systemChannelsShouldBeusedByOtherBinding() throws Exception {
        try (final AutoCloseable unused1 = loadedSystemChannelsBundle()) {
            try (final AutoCloseable unused2 = loadedSystemChannelsUserBundle()) {
            }
        }
    }

    @Test
    public void thingTyoesShouldHaveProperChannelDefinitions() throws Exception {
        try (final AutoCloseable unused = loadedSystemChannelsBundle()) {
            ThingType wirelessRouterType = thingTypeProvider.getThingTypes(null).stream()
                    .filter(it -> "SystemChannels:wireless-router".equals(it.getUID().getAsString())).findFirst().get();
            assertThat(wirelessRouterType, is(notNullValue()));

            Collection<ChannelDefinition> channelDefs = wirelessRouterType.getChannelDefinitions();
            assertThat(channelDefs.size(), is(3));

            ChannelDefinition myChannel = channelDefs.stream().filter(
                    it -> "test".equals(it.getId()) && "system:my-channel".equals(it.getChannelTypeUID().getAsString()))
                    .findFirst().get();
            assertThat(myChannel, is(notNullValue()));

            ChannelDefinition sigStr = channelDefs.stream().filter(it -> "sigstr".equals(it.getId())
                    && "system:signal-strength".equals(it.getChannelTypeUID().getAsString())).findFirst().get();
            assertThat(sigStr, is(notNullValue()));

            ChannelDefinition lowBat = channelDefs.stream().filter(it -> "lowbat".equals(it.getId())
                    && "system:low-battery".equals(it.getChannelTypeUID().getAsString())).findFirst().get();
            assertThat(lowBat, is(notNullValue()));
        }
    }

    @Test
    public void systemChannelsShouldBeAddedWithoutThingTypes() throws Exception {
        try (final AutoCloseable unused = loadedSystemChannelsWithoutThingTypesBundle()) {
        }
    }

    @Test
    public void systemChannelsShouldTranslateProperly() throws Exception {
        try (final AutoCloseable unused = loadedSystemChannelsBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(Locale.GERMAN);

            ThingType wirelessRouterType = thingTypes.stream()
                    .filter(it -> "SystemChannels:wireless-router".equals(it.getUID().getAsString())).findFirst().get();
            assertNotNull(wirelessRouterType);

            List<ChannelDefinition> channelDefs = wirelessRouterType.getChannelDefinitions();
            assertEquals(3, channelDefs.size());

            ChannelDefinition myChannel = channelDefs.stream().filter(
                    it -> "test".equals(it.getId()) && "system:my-channel".equals(it.getChannelTypeUID().getAsString()))
                    .findFirst().get();
            assertNotNull(myChannel);

            ChannelDefinition sigStr = channelDefs.stream().filter(it -> "sigstr".equals(it.getId())
                    && "system:signal-strength".equals(it.getChannelTypeUID().getAsString())).findFirst().get();
            assertNotNull(sigStr);

            ChannelDefinition lowBat = channelDefs.stream().filter(it -> "lowbat".equals(it.getId())
                    && "system:low-battery".equals(it.getChannelTypeUID().getAsString())).findFirst().get();
            assertNotNull(lowBat);

            ChannelType lowBatType = systemChannelTypeProvider.getChannelType(lowBat.getChannelTypeUID(),
                    Locale.GERMAN);

            ChannelType myChannelChannelType = channelTypeRegistry.getChannelType(myChannel.getChannelTypeUID(),
                    Locale.GERMAN);
            assertNotNull(myChannelChannelType);
            assertEquals("Mein String My Channel", myChannelChannelType.getLabel());
            assertEquals("Wetterinformation mit My Channel Type Beschreibung", myChannelChannelType.getDescription());

            assertEquals("Mein String My Channel", myChannel.getLabel());
            assertEquals("Wetterinformation mit My Channel Type Beschreibung", myChannel.getDescription());

            assertEquals("Meine spezial Signalstärke", sigStr.getLabel());
            assertEquals("Meine spezial Beschreibung für Signalstärke", sigStr.getDescription());

            assertEquals("Niedriger Batteriestatus", lowBatType.getLabel());
            assertNotNull(lowBatType.getDescription());
        }
    }
}
