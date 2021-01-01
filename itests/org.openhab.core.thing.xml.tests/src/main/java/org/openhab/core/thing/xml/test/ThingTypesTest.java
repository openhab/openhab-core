/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.xml.test.LoadedTestBundle.StuffAddition;
import org.openhab.core.types.StateDescription;

/**
 * @author Henning Treu - Initial contribution
 */
public class ThingTypesTest extends JavaOSGiTest {

    private LoadedTestBundle loadedTestBundle() throws Exception {
        return new LoadedTestBundle("ThingTypesTest.bundle", bundleContext, this::getService,
                new StuffAddition().thingTypes(4));
    }

    private ThingTypeProvider thingTypeProvider;
    private ChannelTypeRegistry channelTypeRegistry;
    private ChannelGroupTypeRegistry channelGroupTypeRegistry;

    @BeforeEach
    public void setUp() {
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));

        channelTypeRegistry = getService(ChannelTypeRegistry.class);
        assertThat(channelTypeRegistry, is(notNullValue()));

        channelGroupTypeRegistry = getService(ChannelGroupTypeRegistry.class);
        assertThat(channelGroupTypeRegistry, is(notNullValue()));
    }

    @Test
    public void thingTypesShouldLoad() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(null);

            // HUE Bridge
            BridgeType bridgeType = (BridgeType) thingTypes.stream().filter(it -> "hue:bridge".equals(it.toString()))
                    .findFirst().get();
            assertThat(bridgeType, is(notNullValue()));
            assertThat(bridgeType.getCategory(), is("NetworkAppliance"));
            assertThat(bridgeType.isListed(), is(false));
            assertThat(bridgeType.getLabel(), is("HUE Bridge"));
            assertThat(bridgeType.getDescription(), is("The hue Bridge represents the Philips hue bridge."));
            assertThat(bridgeType.getProperties().size(), is(1));
            assertThat(bridgeType.getProperties().get("vendor"), is("Philips"));
            assertThat(bridgeType.getRepresentationProperty(), is("serialNumber"));

            // HUE Lamp
            ThingType thingType = thingTypes.stream().filter(it -> "hue:lamp".equals(it.toString())).findFirst().get();

            assertThat(thingType, is(notNullValue()));
            assertThat(thingType.getCategory(), is("Lightbulb"));
            assertThat(thingType.isListed(), is(false));
            assertThat(thingType.getLabel(), is("HUE Lamp"));
            assertThat(thingType.getDescription(), is("My own great HUE Lamp."));
            assertThat(thingType.getSupportedBridgeTypeUIDs().size(), is(1));
            assertThat(thingType.getSupportedBridgeTypeUIDs().get(0), is("hue:bridge"));
            assertThat(thingType.getExtensibleChannelTypeIds(), containsInAnyOrder("alarm", "brightness"));
            assertThat(thingType.getProperties().size(), is(2));
            assertThat(thingType.getProperties().get("key1"), is("value1"));
            assertThat(thingType.getProperties().get("key2"), is("value2"));
            assertThat(thingType.getRepresentationProperty(), is("uniqueId"));

            List<ChannelDefinition> channelDefinitions = thingType.getChannelDefinitions();

            assertThat(channelDefinitions.size(), is(3));
            ChannelDefinition colorChannel = channelDefinitions.stream().filter(it -> "color".equals(it.getId()))
                    .findFirst().get();
            assertThat(colorChannel, is(notNullValue()));

            assertThat(colorChannel.getProperties().size(), is(2));
            assertThat(colorChannel.getProperties().get("chan.key1"), is("value1"));
            assertThat(colorChannel.getProperties().get("chan.key2"), is("value2"));

            ChannelType colorChannelType = channelTypeRegistry.getChannelType(colorChannel.getChannelTypeUID());
            assertThat(colorChannelType, is(notNullValue()));
            assertThat(colorChannelType.toString(), is("hue:color"));
            assertThat(colorChannelType.getItemType(), is("ColorItem"));
            assertThat(colorChannelType.getLabel(), is("HUE Lamp Color"));
            assertThat(colorChannelType.getDescription(), is(
                    "The color channel allows to control the color of the hue lamp. It is also possible to dim values and switch the lamp on and off."));

            Set<String> tags = colorChannelType.getTags();
            assertThat(tags, is(notNullValue()));
            assertThat(tags.contains("Hue"), is(true));
            assertThat(tags.contains("ColorLamp"), is(true));
            assertThat(tags.contains("AmbientLamp"), is(false));
            assertThat(tags.contains("AlarmSystem"), is(false));

            ChannelDefinition colorTemperatureChannel = channelDefinitions.stream()
                    .filter(it -> "color_temperature".equals(it.getId())).findFirst().get();

            assertThat(colorTemperatureChannel, is(notNullValue()));
            assertThat(colorTemperatureChannel.getProperties().size(), is(0));
            ChannelType colorTemperatureChannelType = channelTypeRegistry
                    .getChannelType(colorTemperatureChannel.getChannelTypeUID());
            assertThat(colorTemperatureChannelType, is(notNullValue()));

            assertThat(colorTemperatureChannelType.toString(), is("hue:color_temperature"));
            assertThat(colorTemperatureChannelType.getItemType(), is("DimmerItem"));
            assertThat(colorTemperatureChannelType.getLabel(), is("HUE Lamp Color Temperature"));
            assertThat(colorTemperatureChannelType.getDescription(), is(
                    "The color temperature channel allows to set the color temperature from 0 (cold) to 100 (warm)."));

            tags = colorTemperatureChannelType.getTags();
            assertThat(tags, is(notNullValue()));
            assertThat(tags.contains("Hue"), is(true));
            assertThat(tags.contains("AmbientLamp"), is(true));
            assertThat(tags.contains("ColorLamp"), is(false));
            assertThat(tags.contains("AlarmSystem"), is(false));

            ChannelDefinition alarmChannel = channelDefinitions.stream().filter(it -> "alarm".equals(it.getId()))
                    .findFirst().get();
            assertThat(alarmChannel, is(notNullValue()));
            ChannelType alarmChannelType = channelTypeRegistry.getChannelType(alarmChannel.getChannelTypeUID());
            assertThat(alarmChannelType, is(notNullValue()));

            assertThat(alarmChannelType.toString(), is("hue:alarm"));
            assertThat(alarmChannelType.getItemType(), is("Number"));
            assertThat(alarmChannelType.getLabel(), is("Alarm System"));
            assertThat(alarmChannelType.getDescription(), is("The light blinks if alarm is set."));

            tags = alarmChannelType.getTags();
            assertThat(tags, is(notNullValue()));
            assertThat(tags.contains("Hue"), is(true));
            assertThat(tags.contains("AlarmSystem"), is(true));
            assertThat(tags.contains("AmbientLamp"), is(false));
            assertThat(tags.contains("ColorLamp"), is(false));
            assertThat(alarmChannelType.getCategory(), is(equalTo("ALARM")));

            StateDescription state = alarmChannelType.getState();
            assertThat(state.getMinimum(), is(BigDecimal.ZERO));
            assertThat(state.getMaximum(), is(BigDecimal.valueOf(100.0)));
            assertThat(state.getStep(), is(BigDecimal.valueOf(10.0)));
            assertThat(state.getPattern(), is(equalTo("%d Peek")));
            assertThat(state.isReadOnly(), is(true));
            assertThat(state.getOptions().size(), is(2));
            assertThat(state.getOptions().get(0).getValue(), is(equalTo("SOUND")));
            assertThat(state.getOptions().get(0).getLabel(), is(equalTo("My great sound.")));

            // HUE Lamp with group
            thingType = thingTypes.stream().filter(it -> "hue:lamp-with-group".equals(it.toString())).findFirst().get();
            assertThat(thingType, is(notNullValue()));
            assertThat(thingType.getProperties().size(), is(0));
            assertThat(thingType.getCategory(), is(nullValue()));
            assertThat(thingType.isListed(), is(true));
            assertThat(thingType.getExtensibleChannelTypeIds(), containsInAnyOrder("brightness", "alarm"));

            List<ChannelGroupDefinition> channelGroupDefinitions = thingType.getChannelGroupDefinitions();
            assertThat(channelGroupDefinitions.size(), is(2));

            // Channel Group
            ChannelGroupDefinition channelGroupDefinition = channelGroupDefinitions.stream()
                    .filter(it -> "lampgroup".equals(it.getId())).findFirst().get();
            assertThat(channelGroupDefinition, is(notNullValue()));
            ChannelGroupType channelGroupType = channelGroupTypeRegistry
                    .getChannelGroupType(channelGroupDefinition.getTypeUID());
            assertThat(channelGroupType, is(notNullValue()));
            channelDefinitions = channelGroupType.getChannelDefinitions();
            assertThat(channelDefinitions.size(), is(3));

            // Channel Group without channels
            channelGroupDefinition = channelGroupDefinitions.stream()
                    .filter(it -> "lampgroup-without-channels".equals(it.getId())).findFirst().get();
            assertThat(channelGroupDefinition, is(notNullValue()));
            channelGroupType = channelGroupTypeRegistry.getChannelGroupType(channelGroupDefinition.getTypeUID());
            assertThat(channelGroupType, is(notNullValue()));
            channelDefinitions = channelGroupType.getChannelDefinitions();
            assertThat(channelDefinitions.size(), is(0));

            // HUE Lamp without channels
            thingType = thingTypes.stream().filter(it -> "hue:lamp-without-channels".equals(it.toString())).findFirst()
                    .get();
            assertThat(thingType, is(notNullValue()));
            channelDefinitions = thingType.getChannelDefinitions();
            assertThat(channelDefinitions.size(), is(0));
        }
    }

    @Test
    public void thingTypesShouldBeRemovedWhenBundleIsUninstalled() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
        }
    }
}
