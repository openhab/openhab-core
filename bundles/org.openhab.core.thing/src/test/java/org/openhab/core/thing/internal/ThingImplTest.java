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
package org.openhab.core.thing.internal;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ThingImplTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
    private static final String FIRST_CHANNEL_ID = "firstgroup#channel1";
    private static final String SECOND_CHANNEL_ID = "secondgroup#channel1";
    private static final ChannelUID FIRST_CHANNEL_UID = new ChannelUID(THING_UID, FIRST_CHANNEL_ID);
    private static final ChannelUID SECOND_CHANNEL_UID = new ChannelUID(THING_UID, SECOND_CHANNEL_ID);

    @Test
    public void testGetChannelMethods() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(FIRST_CHANNEL_UID, CoreItemFactory.STRING).build()).build();
        assertEquals(1, thing.getChannels().size());

        assertNull(thing.getChannel("channel1"));

        assertNotNull(thing.getChannel(FIRST_CHANNEL_UID));
        assertEquals(FIRST_CHANNEL_UID, thing.getChannel(FIRST_CHANNEL_UID).getUID());

        assertNotNull(thing.getChannel(FIRST_CHANNEL_ID));
        assertEquals(FIRST_CHANNEL_UID, thing.getChannel(FIRST_CHANNEL_ID).getUID());
    }

    @Test
    public void testGetGroupChannels() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(FIRST_CHANNEL_UID, CoreItemFactory.STRING).build())
                .withChannel(ChannelBuilder.create(SECOND_CHANNEL_UID, CoreItemFactory.STRING).build()).build();
        assertEquals(2, thing.getChannels().size());

        assertNull(thing.getChannel("channel1"));

        assertNotNull(thing.getChannel(FIRST_CHANNEL_UID));
        assertEquals(FIRST_CHANNEL_UID, thing.getChannel(FIRST_CHANNEL_UID).getUID());

        assertNotNull(thing.getChannel(FIRST_CHANNEL_ID));
        assertEquals(FIRST_CHANNEL_UID, thing.getChannel(FIRST_CHANNEL_ID).getUID());

        assertNotNull(thing.getChannel(SECOND_CHANNEL_UID));
        assertEquals(SECOND_CHANNEL_UID, thing.getChannel(SECOND_CHANNEL_UID).getUID());

        assertNotNull(thing.getChannel(SECOND_CHANNEL_ID));
        assertEquals(SECOND_CHANNEL_UID, thing.getChannel(SECOND_CHANNEL_ID).getUID());
    }

}
