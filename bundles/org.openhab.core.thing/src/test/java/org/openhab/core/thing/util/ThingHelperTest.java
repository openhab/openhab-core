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
package org.openhab.core.thing.util;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.stream.Stream;

import org.junit.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.ThingImpl;

/**
 * @author Alex Tugarev - Initial contribution
 */
public class ThingHelperTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thingId");

    @Test
    public void twoTechnicalEqualThingInstancesAreDetectedAsEqual() {
        Thing thingA = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel1"), "itemType").build(),
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel2"), "itemType").build())
                .withConfiguration(new Configuration()).build();

        thingA.getConfiguration().put("prop1", "value1");
        thingA.getConfiguration().put("prop2", "value2");

        assertTrue(ThingHelper.equals(thingA, thingA));

        Thing thingB = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel2"), "itemType").build(),
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel1"), "itemType").build())
                .withConfiguration(new Configuration()).build();
        thingB.getConfiguration().put("prop2", "value2");
        thingB.getConfiguration().put("prop1", "value1");

        assertTrue(ThingHelper.equals(thingA, thingB));
    }

    @Test
    public void twoThingsAreDifferentAfterPropertiesWereModified() {
        Thing thingA = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel1"), "itemType").build(),
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel2"), "itemType").build())
                .withConfiguration(new Configuration()).build();
        thingA.getConfiguration().put("prop1", "value1");

        Thing thingB = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel2"), "itemType").build(),
                        ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel1"), "itemType").build())
                .withConfiguration(new Configuration()).build();
        thingB.getConfiguration().put("prop1", "value1");

        assertTrue(ThingHelper.equals(thingA, thingB));

        thingB.getConfiguration().put("prop3", "value3");

        assertFalse(ThingHelper.equals(thingA, thingB));
    }

    @Test
    public void twoThingsAreDifferentAfterChannelsWereModified() {
        Thing thingA = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration()).build();
        Thing thingB = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration()).build();

        assertTrue(ThingHelper.equals(thingA, thingB));

        ((ThingImpl) thingB).setChannels(singletonList(
                ChannelBuilder.create(new ChannelUID("binding:type:thingId:channel3"), "itemType3").build()));

        assertFalse(ThingHelper.equals(thingA, thingB));
    }

    @Test
    public void twoThingsAreDifferentAfterLabelWasModified() {
        Thing thingA = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration())
                .withLabel("foo").build();
        Thing thingB = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration())
                .withLabel("foo").build();

        assertTrue(ThingHelper.equals(thingA, thingB));

        thingB.setLabel("bar");

        assertFalse(ThingHelper.equals(thingA, thingB));
    }

    @Test
    public void twoThingsAreDifferentAfterLocationWasModified() {
        Thing thingA = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration())
                .withLocation("foo").build();
        Thing thingB = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration())
                .withLocation("foo").build();

        assertTrue(ThingHelper.equals(thingA, thingB));

        thingB.setLocation("bar");

        assertFalse(ThingHelper.equals(thingA, thingB));
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertThatNoDuplicateChannelsCanBeAdded() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");

        Thing thing = ThingBuilder.create(thingTypeUID, thingUID)
                .withChannels(ChannelBuilder.create(new ChannelUID(thingUID, "channel1"), "").build(),
                        ChannelBuilder.create(new ChannelUID(thingUID, "channel2"), "").build())
                .build();

        ThingHelper
                .addChannelsToThing(thing,
                        Stream.of(ChannelBuilder.create(new ChannelUID(thingUID, "channel2"), "").build(),
                                ChannelBuilder.create(new ChannelUID(thingUID, "channel3"), "").build())
                                .collect(toList()));
    }
}
