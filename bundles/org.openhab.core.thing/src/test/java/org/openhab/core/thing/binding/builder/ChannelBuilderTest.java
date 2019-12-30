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
package org.openhab.core.thing.binding.builder;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.openhab.core.thing.DefaultSystemChannelTypeProvider.SYSTEM_OUTDOOR_TEMPERATURE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * Tests the {@link ChannelBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelBuilderTest {

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
    private ChannelBuilder builder;
    private Channel channel;

    @Before
    public void setup() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId", "thingTypeId"), "thingLabel")
                .build();
        ChannelUID channelUID = new ChannelUID(new ThingUID(thingType.getUID(), "thingId"), "temperature");
        builder = ChannelBuilder.create(channelUID, SYSTEM_OUTDOOR_TEMPERATURE.getItemType()).withLabel("Test")
                .withDescription("My test channel").withType(SYSTEM_OUTDOOR_TEMPERATURE.getUID())
                .withProperties(properties);
        channel = builder.build();
    }

    @Test
    public void testChannelBuilder() {
        assertThat(channel.getAcceptedItemType(), is(SYSTEM_OUTDOOR_TEMPERATURE.getItemType()));
        assertThat(channel.getChannelTypeUID(), is(SYSTEM_OUTDOOR_TEMPERATURE.getUID()));
        assertThat(channel.getDefaultTags().size(), is(0));
        assertThat(channel.getDescription(), is("My test channel"));
        assertThat(channel.getKind(), is(ChannelKind.STATE));
        assertThat(channel.getLabel(), is("Test"));
        assertThat(channel.getProperties().size(), is(2));
        assertThat(channel.getProperties().get(KEY1), is(VALUE1));
        assertThat(channel.getProperties().get(KEY2), is(VALUE2));
    }

    @Test
    public void testChannelBuilderFromChannel() {
        Channel otherChannel = ChannelBuilder.create(channel).build();

        assertThat(otherChannel.getAcceptedItemType(), is(channel.getAcceptedItemType()));
        assertThat(otherChannel.getChannelTypeUID(), is(channel.getChannelTypeUID()));
        assertThat(otherChannel.getConfiguration(), is(channel.getConfiguration()));
        assertThat(otherChannel.getDefaultTags().size(), is(channel.getDefaultTags().size()));
        assertThat(otherChannel.getDescription(), is(channel.getDescription()));
        assertThat(otherChannel.getKind(), is(channel.getKind()));
        assertThat(otherChannel.getLabel(), is(channel.getLabel()));
        assertThat(otherChannel.getProperties().size(), is(channel.getProperties().size()));
        assertThat(otherChannel.getProperties().get(KEY1), is(channel.getProperties().get(KEY1)));
        assertThat(otherChannel.getProperties().get(KEY2), is(channel.getProperties().get(KEY2)));
        assertThat(otherChannel.getUID(), is(channel.getUID()));
    }

    @Test
    public void subsequentBuildsCreateIndependentChannels() {
        Channel otherChannel = builder.withLabel("Second Test").withDescription("My second test channel")
                .withProperties(Collections.emptyMap()).build();

        assertThat(otherChannel.getDescription(), is(not(channel.getDescription())));
        assertThat(otherChannel.getLabel(), is(not(channel.getLabel())));
        assertThat(otherChannel.getProperties().size(), is(not(channel.getProperties().size())));
    }
}
