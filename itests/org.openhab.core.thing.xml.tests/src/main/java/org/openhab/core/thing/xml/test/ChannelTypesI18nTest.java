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
package org.openhab.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.BundleCloseable;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ThingType;

/***
 *
 * This test checks if channel types are loaded properly.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ChannelTypesI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "ChannelTypesI18nTest.bundle";

    private ChannelTypeProvider channelTypeProvider;
    private ChannelGroupTypeProvider channelGroupTypeProvider;
    private ThingTypeProvider thingTypeProvider;

    @Before
    public void setUp() {
        // get ONLY the XMLChannelTypeProvider
        channelTypeProvider = getService(ChannelTypeProvider.class,
                serviceReference -> "core.xml.channels".equals(serviceReference.getProperty("esh.scope")));

        assertThat(channelTypeProvider, is(notNullValue()));
        channelGroupTypeProvider = getService(ChannelGroupTypeProvider.class,
                serviceReference -> "core.xml.channelGroups".equals(serviceReference.getProperty("esh.scope")));

        assertThat(channelGroupTypeProvider, is(notNullValue()));
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));
    }

    @Test
    public void channelTypesShouldTranslateCorrectly() throws Exception {
        try (BundleCloseable bundle = new BundleCloseable(
                SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME))) {
            assertThat(bundle, is(notNullValue()));

            ChannelType channelType1 = waitForAssert(() -> {
                final Optional<ChannelType> opt = channelTypeProvider.getChannelTypes(null).stream()
                        .filter(c -> c.getUID().toString().equals("somebinding:channel-with-i18n")).findFirst();
                assertTrue(opt.isPresent());
                return opt.get();
            });
            assertThat(channelType1, is(not(nullValue())));
            assertThat(channelType1.getLabel(), is(equalTo("Channel Label")));
            assertThat(channelType1.getDescription(), is(equalTo("Channel Description")));
            assertThat(channelType1.getCommandDescription().getCommandOptions().get(0).getLabel(),
                    is(equalTo("Short Alarm")));
            assertThat(channelType1.getCommandDescription().getCommandOptions().get(1).getLabel(),
                    is(equalTo("Long Alarm")));

            Collection<ChannelGroupType> channelGroupTypes = channelGroupTypeProvider.getChannelGroupTypes(null);
            ChannelGroupType channelGroupType = channelGroupTypes.stream()
                    .filter(c -> c.getUID().toString().equals("somebinding:channelgroup-with-i18n")).findFirst().get();
            assertThat(channelGroupType, is(not(nullValue())));
            assertThat(channelGroupType.getLabel(), is(equalTo("Channel Group Label")));
            assertThat(channelGroupType.getDescription(), is(equalTo("Channel Group Description")));
        }
    }

    @Test
    public void channelDefinitionsShouldBeTranslatedCorrectly() throws Exception {
        try (BundleCloseable bundle = new BundleCloseable(
                SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME))) {
            assertThat(bundle, is(notNullValue()));

            ThingType thingType = waitForAssert(() -> {
                Optional<ThingType> thingTypeOpt = thingTypeProvider.getThingTypes(null).stream()
                        .filter(it -> it.getUID().toString().equals("somebinding:something")).findFirst();
                Assert.assertTrue(thingTypeOpt.isPresent());
                final ThingType thingTypeTmp = thingTypeOpt.get();
                // assertThat(thingTypeTmp, is(notNullValue()));
                assertThat(thingTypeTmp.getChannelDefinitions().size(), is(2));
                return thingTypeTmp;
            });

            ChannelDefinition channelDefinition1 = thingType.getChannelDefinitions().stream()
                    .filter(it -> it.getId().equals("channelPlain")).findFirst().get();
            assertThat(channelDefinition1.getLabel(), is(equalTo("Channel Plain Label")));
            assertThat(channelDefinition1.getDescription(), is(equalTo("Channel Plain Description")));

            ChannelDefinition channelDefinition2 = thingType.getChannelDefinitions().stream()
                    .filter(it -> it.getId().equals("channelInplace")).findFirst().get();
            assertThat(channelDefinition2.getLabel(), is(equalTo("Channel Inplace Label")));
            assertThat(channelDefinition2.getDescription(), is(equalTo("Channel Inplace Description")));
        }
    }

}
