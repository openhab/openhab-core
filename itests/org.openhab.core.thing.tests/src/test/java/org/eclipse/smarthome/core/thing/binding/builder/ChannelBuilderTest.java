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
package org.eclipse.smarthome.core.thing.binding.builder;

import static org.eclipse.smarthome.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_OUTDOOR_TEMPERATURE;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ChannelBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelBuilderTest extends JavaOSGiTest {

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private final Map<String, String> properties = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put(KEY1, VALUE1);
            put(KEY2, VALUE2);
        }
    };
    private Channel channel;

    @Before
    public void setup() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId", "thingTypeId"), "thingLabel")
                .build();
        ChannelUID channelUID = new ChannelUID(new ThingUID(thingType.getUID(), "thingId"), "temperature");
        channel = ChannelBuilder.create(channelUID, SYSTEM_OUTDOOR_TEMPERATURE.getItemType()).withLabel("Test")
                .withDescription("My test channel").withType(SYSTEM_OUTDOOR_TEMPERATURE.getUID())
                .withProperties(properties).build();
    }

    @Test
    public void testChannelBuilder() {
        assertEquals(SYSTEM_OUTDOOR_TEMPERATURE.getItemType(), channel.getAcceptedItemType());
        assertEquals(SYSTEM_OUTDOOR_TEMPERATURE.getUID(), channel.getChannelTypeUID());
        assertEquals(0, channel.getDefaultTags().size());
        assertEquals("My test channel", channel.getDescription());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals("Test", channel.getLabel());
        assertEquals(2, channel.getProperties().size());
        assertEquals(VALUE1, channel.getProperties().get(KEY1));
        assertEquals(VALUE2, channel.getProperties().get(KEY2));
    }

    @Test
    public void testChannelBuilderFromChannel() {
        Channel otherChannel = ChannelBuilder.create(channel).build();

        assertEquals(channel.getAcceptedItemType(), otherChannel.getAcceptedItemType());
        assertEquals(channel.getChannelTypeUID(), otherChannel.getChannelTypeUID());
        assertEquals(channel.getConfiguration(), otherChannel.getConfiguration());
        assertEquals(channel.getDefaultTags().size(), otherChannel.getDefaultTags().size());
        assertEquals(channel.getDescription(), otherChannel.getDescription());
        assertEquals(channel.getKind(), otherChannel.getKind());
        assertEquals(channel.getLabel(), otherChannel.getLabel());
        assertEquals(channel.getProperties().size(), otherChannel.getProperties().size());
        assertEquals(channel.getProperties().get(KEY1), otherChannel.getProperties().get(KEY1));
        assertEquals(channel.getProperties().get(KEY2), otherChannel.getProperties().get(KEY2));
        assertEquals(channel.getUID(), otherChannel.getUID());
    }
}
