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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ThingBuilderTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
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
    private ThingBuilder thingBuilder;

    @Before
    public void setup() {
        thingBuilder = ThingBuilder.create(THING_TYPE_UID, THING_UID);
    }

    @Test
    public void testInstance() {
        assertThat(thingBuilder, is(instanceOf(ThingBuilder.class)));
        assertThat(thingBuilder.withLabel("TEST"), is(instanceOf(ThingBuilder.class)));
        assertThat(thingBuilder.build(), is(instanceOf(ThingImpl.class)));

        final BridgeBuilder bridgeBuilder = BridgeBuilder.create(THING_TYPE_UID, THING_UID);
        assertThat(bridgeBuilder, is(instanceOf(BridgeBuilder.class)));
        assertThat(bridgeBuilder.withLabel("TEST"), is(instanceOf(BridgeBuilder.class)));
        assertThat(bridgeBuilder.build(), is(instanceOf(BridgeImpl.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannelDuplicates() {
        thingBuilder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
        thingBuilder.withChannel(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannelsDuplicatesCollections() {
        thingBuilder.withChannels(Arrays.asList( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannelsDuplicatesVararg() {
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build());
    }

    @Test
    public void testWithoutChannel() {
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        thingBuilder.withoutChannel(new ChannelUID(THING_UID, "channel1"));
        Thing thing = thingBuilder.build();
        assertThat(thing.getChannels(), hasSize(1));
        assertThat(thing.getChannels().get(0).getUID().getId(), is(equalTo("channel2")));
    }

    @Test
    public void testWithoutChannelMissing() {
        thingBuilder.withChannels( //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(), //
                ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build());
        thingBuilder.withoutChannel(new ChannelUID(THING_UID, "channel3"));
        assertThat(thingBuilder.build().getChannels(), hasSize(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithChannelWrongThing() {
        thingBuilder.withChannel(
                ChannelBuilder.create(new ChannelUID(new ThingUID(THING_TYPE_UID, "wrong"), "channel1"), "").build());
    }

    @Test
    public void subsequentBuildsCreateIndependentThings() {
        Thing thing = thingBuilder.withLabel("Test").withLocation("Some Place").withProperties(properties).build();
        Thing otherThing = thingBuilder.withLabel("Second Test").withLocation("Other Place")
                .withProperties(Collections.emptyMap()).build();

        assertThat(otherThing.getLabel(), is(not(thing.getLabel())));
        assertThat(otherThing.getLocation(), is(not(thing.getLocation())));
        assertThat(otherThing.getProperties().size(), is(not(thing.getProperties().size())));
    }
}
