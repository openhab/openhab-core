/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.binding.builder;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ThingBuilderTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
    private ThingBuilder builder;

    @Before
    public void setup() {
        builder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannel_duplicates() {
        builder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
        builder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannels_duplicatesCollections() {
        builder.withChannels(Arrays.asList( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannels_duplicatesVararg() {
        builder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test
    public void testWithoutChannel() {
        builder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        builder.withoutChannel(new ChannelUID(THING_UID, "channel1"));
        Thing thing = builder.build();
        assertThat(thing.getChannels().size(), is(equalTo(1)));
        assertThat(thing.getChannels().get(0).getUID().getId(), is(equalTo("channel2")));
    }

    @Test
    public void testWithoutChannel_missing() {
        builder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        builder.withoutChannel(new ChannelUID(THING_UID, "channel3"));
        assertThat(builder.build().getChannels().size(), is(equalTo(2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannel_wrongThing() {
        builder.withChannel(
                ChannelBuilder.create(new ChannelUID(new ThingUID(THING_TYPE_UID, "wrong"), "channel1"), "").build());
    }

    @Test
    @Ignore
    public void subsequentBuildsCreateIndependentThings() {
        Thing thing = builder.withLabel("Test").build();
        Thing otherThing = builder.withLabel("Second Test").build();

        assertThat(otherThing.getLabel(), is(not(thing.getLabel())));
    }
}
