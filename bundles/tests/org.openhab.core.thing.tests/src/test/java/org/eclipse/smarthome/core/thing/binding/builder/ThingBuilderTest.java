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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class ThingBuilderTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannel_duplicates() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
        thingBuilder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannels_duplicatesCollections() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannels(Arrays.asList( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannels_duplicatesVararg() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test
    public void testWithoutChannel() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        thingBuilder.withoutChannel(new ChannelUID(THING_UID, "channel1"));
        assertThat(thingBuilder.build().getChannels().size(), is(equalTo(1)));
        assertThat(thingBuilder.build().getChannels().get(0).getUID().getId(), is(equalTo("channel2")));
    }

    @Test
    public void testWithoutChannel_missing() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        thingBuilder.withoutChannel(new ChannelUID(THING_UID, "channel3"));
        assertThat(thingBuilder.build().getChannels().size(), is(equalTo(2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannel_wrongThing() {
        ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
        thingBuilder.withChannel(
                ChannelBuilder.create(new ChannelUID(new ThingUID(THING_TYPE_UID, "wrong"), "channel1"), "").build());
    }
}
