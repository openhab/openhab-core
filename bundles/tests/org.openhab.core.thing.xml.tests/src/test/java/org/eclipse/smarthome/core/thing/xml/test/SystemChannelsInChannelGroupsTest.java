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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author Simon Kaufmann - Initial contribution and API
 *
 */
public class SystemChannelsInChannelGroupsTest extends JavaOSGiTest {

    private static final String SYSTEM_CHANNELS_IN_CHANNEL_GROUPS_BUNDLE_NAME = "SystemChannelsInChannelGroups.bundle";

    private ThingTypeProvider thingTypeProvider;
    private ChannelTypeRegistry channelTypeRegistry;
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;

    @Before
    public void setUp() {
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));

        channelTypeRegistry = getService(ChannelTypeRegistry.class);
        assertThat(channelTypeRegistry, is(notNullValue()));

        channelGroupTypeRegistry = getService(ChannelGroupTypeRegistry.class);
        assertThat(channelGroupTypeRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        SyntheticBundleInstaller.uninstall(bundleContext, SYSTEM_CHANNELS_IN_CHANNEL_GROUPS_BUNDLE_NAME);
    }

    @Test
    public void systemChannelsInChannelGroupsShouldLoadAndUnload() throws Exception {
        int initialNumberOfThingTypes = thingTypeProvider.getThingTypes(null).size();
        int initialNumberOfChannelTypes = channelTypeRegistry.getChannelTypes().size();
        int initialNumberOfChannelGroupTypes = channelGroupTypeRegistry.getChannelGroupTypes().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, SYSTEM_CHANNELS_IN_CHANNEL_GROUPS_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(null);
        assertThat(thingTypes.size(), is(initialNumberOfThingTypes + 1));
        assertThat(channelTypeRegistry.getChannelTypes().size(), is(initialNumberOfChannelTypes + 1));
        assertThat(channelGroupTypeRegistry.getChannelGroupTypes().size(), is(initialNumberOfChannelGroupTypes + 1));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));

        assertThat(thingTypeProvider.getThingTypes(null).size(), is(initialNumberOfThingTypes));
        assertThat(channelTypeRegistry.getChannelTypes().size(), is(initialNumberOfChannelTypes));
        assertThat(channelGroupTypeRegistry.getChannelGroupTypes().size(), is(initialNumberOfChannelGroupTypes));

    }

    @Test
    public void thingTypesWithSystemChannelsInChannelsGoupsShouldHavePorperChannelDefinitions() throws Exception {
        // install test bundle
        Bundle sysBundle = SyntheticBundleInstaller.install(bundleContext,
                SYSTEM_CHANNELS_IN_CHANNEL_GROUPS_BUNDLE_NAME);
        assertThat(sysBundle, is(notNullValue()));

        List<ThingType> thingTypes = thingTypeProvider.getThingTypes(null).stream()
                .filter(it -> it.getUID().getId().equals("wireless-router")).collect(Collectors.toList());

        assertThat(thingTypes.size(), is(1));

        List<ChannelGroupType> channelGroupTypes = channelGroupTypeRegistry.getChannelGroupTypes();

        ChannelGroupType channelGroup = channelGroupTypes.stream()
                .filter(it -> it.getUID().equals(new ChannelGroupTypeUID("SystemChannelsInChannelGroups:channelGroup")))
                .findFirst().get();
        assertThat(channelGroup, is(notNullValue()));

        List<ChannelDefinition> channelDefs = channelGroup.getChannelDefinitions();

        List<ChannelDefinition> myChannel = channelDefs.stream().filter(
                it -> it.getId().equals("test") && it.getChannelTypeUID().getAsString().equals("system:my-channel"))
                .collect(Collectors.toList());

        List<ChannelDefinition> sigStr = channelDefs.stream()
                .filter(it -> it.getId().equals("sigstr")
                        && it.getChannelTypeUID().getAsString().equals("system:signal-strength"))
                .collect(Collectors.toList());

        List<ChannelDefinition> lowBat = channelDefs.stream().filter(
                it -> it.getId().equals("lowbat") && it.getChannelTypeUID().getAsString().equals("system:low-battery"))
                .collect(Collectors.toList());

        assertThat(myChannel.size(), is(1));
        assertThat(sigStr.size(), is(1));
        assertThat(lowBat.size(), is(1));
    }

}
