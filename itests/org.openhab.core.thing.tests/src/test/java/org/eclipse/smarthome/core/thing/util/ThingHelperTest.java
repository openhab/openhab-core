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
package org.eclipse.smarthome.core.thing.util;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import java.util.stream.Stream;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.internal.ThingImpl;
import org.junit.Test;

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
        ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
        ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), "").build(),
                        ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build())
                .build();

        ThingHelper
                .addChannelsToThing(thing,
                        Stream.of(ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), "").build(),
                                ChannelBuilder.create(new ChannelUID(THING_UID, "channel3"), "").build())
                                .collect(toList()));
    }
}
