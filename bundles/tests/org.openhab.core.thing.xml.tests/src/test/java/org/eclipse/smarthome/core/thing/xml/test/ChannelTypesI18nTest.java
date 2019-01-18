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
package org.eclipse.smarthome.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

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

    @After
    public void tearDown() throws Exception {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void channelTypesShouldTranslateCorrectly() throws Exception {
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ChannelType> channelTypes = channelTypeProvider.getChannelTypes(null);
        ChannelType channelType1 = channelTypes.stream()
                .filter(c -> c.getUID().toString().equals("somebinding:channel-with-i18n")).findFirst().get();
        assertThat(channelType1, is(not(nullValue())));
        assertThat(channelType1.getLabel(), is(equalTo("Channel Label")));
        assertThat(channelType1.getDescription(), is(equalTo("Channel Description")));

        Collection<ChannelGroupType> channelGroupTypes = channelGroupTypeProvider.getChannelGroupTypes(null);
        ChannelGroupType channelGroupType = channelGroupTypes.stream()
                .filter(c -> c.getUID().toString().equals("somebinding:channelgroup-with-i18n")).findFirst().get();
        assertThat(channelGroupType, is(not(nullValue())));
        assertThat(channelGroupType.getLabel(), is(equalTo("Channel Group Label")));
        assertThat(channelGroupType.getDescription(), is(equalTo("Channel Group Description")));
    }

    @Test
    public void channelDefinitionsShouldBeTranslatedCorrectly() throws Exception {
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        ThingType thingType = thingTypeProvider.getThingTypes(null).stream()
                .filter(it -> it.getUID().toString().equals("somebinding:something")).findFirst().get();
        assertThat(thingType, is(notNullValue()));
        assertThat(thingType.getChannelDefinitions().size(), is(2));

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
