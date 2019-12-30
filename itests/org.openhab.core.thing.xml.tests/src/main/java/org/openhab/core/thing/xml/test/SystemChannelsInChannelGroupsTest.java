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
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.xml.test.LoadedTestBundle.StuffAddition;

/**
 * @author Simon Kaufmann - Initial contribution and API
 */
public class SystemChannelsInChannelGroupsTest extends JavaOSGiTest {

    private LoadedTestBundle loadedTestBundle() throws Exception {
        return new LoadedTestBundle("SystemChannelsInChannelGroups.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(1).channelTypes(1).channelGroupTypes(1));
    }

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

    @Test
    public void systemChannelsInChannelGroupsShouldLoadAndUnload() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
        }
    }

    @Test
    public void thingTypesWithSystemChannelsInChannelsGoupsShouldHavePorperChannelDefinitions() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            List<ThingType> thingTypes = thingTypeProvider.getThingTypes(null).stream()
                    .filter(it -> it.getUID().getId().equals("wireless-router")).collect(Collectors.toList());
            assertThat(thingTypes.size(), is(1));

            List<ChannelGroupType> channelGroupTypes = channelGroupTypeRegistry.getChannelGroupTypes();

            ChannelGroupType channelGroup = channelGroupTypes.stream().filter(
                    it -> it.getUID().equals(new ChannelGroupTypeUID("SystemChannelsInChannelGroups:channelGroup")))
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

            List<ChannelDefinition> lowBat = channelDefs.stream()
                    .filter(it -> it.getId().equals("lowbat")
                            && it.getChannelTypeUID().getAsString().equals("system:low-battery"))
                    .collect(Collectors.toList());

            assertThat(myChannel.size(), is(1));
            assertThat(sigStr.size(), is(1));
            assertThat(lowBat.size(), is(1));
        }
    }

}
