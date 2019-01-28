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
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.BridgeType;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ThingTypesTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "ThingTypesTest.bundle";

    private ThingTypeProvider thingTypeProvider;
    private ChannelTypeRegistry channelTypeRegistry;

    @Before
    public void setUp() {
        thingTypeProvider = getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));

        channelTypeRegistry = getService(ChannelTypeRegistry.class);
        assertThat(channelTypeRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws BundleException {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void thingTypesShouldLoad() throws Exception {
        int initialNumberOfThingTypes = thingTypeProvider.getThingTypes(null).size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(null);
        assertThat(thingTypes.size(), is(initialNumberOfThingTypes + 3));

        BridgeType bridgeType = (BridgeType) thingTypes.stream().filter(it -> it.toString().equals("hue:bridge"))
                .findFirst().get();
        assertThat(bridgeType, is(notNullValue()));
        assertThat(bridgeType.getCategory(), is("NetworkAppliance"));
        assertThat(bridgeType.isListed(), is(false));
        assertThat(bridgeType.getLabel(), is("HUE Bridge"));
        assertThat(bridgeType.getDescription(), is("The hue Bridge represents the Philips hue bridge."));
        assertThat(bridgeType.getProperties().size(), is(1));
        assertThat(bridgeType.getProperties().get("vendor"), is("Philips"));
        assertThat(bridgeType.getRepresentationProperty(), is("serialNumber"));

        ThingType thingType = thingTypes.stream().filter(it -> it.toString().equals("hue:lamp")).findFirst().get();

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
        ChannelDefinition colorChannel = channelDefinitions.stream().filter(it -> it.getId().equals("color"))
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
                .filter(it -> it.getId().equals("color_temperature")).findFirst().get();

        assertThat(colorTemperatureChannel, is(notNullValue()));
        assertThat(colorTemperatureChannel.getProperties().size(), is(0));
        ChannelType colorTemperatureChannelType = channelTypeRegistry
                .getChannelType(colorTemperatureChannel.getChannelTypeUID());
        assertThat(colorTemperatureChannelType, is(notNullValue()));

        assertThat(colorTemperatureChannelType.toString(), is("hue:color_temperature"));
        assertThat(colorTemperatureChannelType.getItemType(), is("DimmerItem"));
        assertThat(colorTemperatureChannelType.getLabel(), is("HUE Lamp Color Temperature"));
        assertThat(colorTemperatureChannelType.getDescription(),
                is("The color temperature channel allows to set the color temperature from 0 (cold) to 100 (warm)."));

        tags = colorTemperatureChannelType.getTags();
        assertThat(tags, is(notNullValue()));
        assertThat(tags.contains("Hue"), is(true));
        assertThat(tags.contains("AmbientLamp"), is(true));
        assertThat(tags.contains("ColorLamp"), is(false));
        assertThat(tags.contains("AlarmSystem"), is(false));

        ChannelDefinition alarmChannel = channelDefinitions.stream().filter(it -> it.getId().equals("alarm"))
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

        thingType = thingTypes.stream().filter(it -> it.toString().equals("hue:lamp-with-group")).findFirst().get();
        assertThat(thingType.getProperties().size(), is(0));
        assertThat(thingType.getCategory(), is(nullValue()));
        assertThat(thingType.isListed(), is(true));
        assertThat(thingType.getExtensibleChannelTypeIds(), containsInAnyOrder("brightness", "alarm"));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    @Test
    public void thingTypesShouldBeRemoved_whenBundleIsUninstalled() throws Exception {
        int initialNumberOfThingTypes = thingTypeProvider.getThingTypes(null).size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ThingType> thingTypes = thingTypeProvider.getThingTypes(null);
        assertThat(thingTypes.size(), is(initialNumberOfThingTypes + 3));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));

        thingTypes = thingTypeProvider.getThingTypes(null);
        assertThat(thingTypes.size(), is(initialNumberOfThingTypes));
    }
}
