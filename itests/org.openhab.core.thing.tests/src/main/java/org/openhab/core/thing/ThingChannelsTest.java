/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.dto.ThingDTOMapper;

/**
 * Test for thing channel creation and display order.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ThingChannelsTest extends JavaOSGiTest {

    private static final List<String> CHANNEL_IDS = List.of("polarBear", "alligator", "hippopotamus", "aardvark",
            "whiteRabbit", "redHerring", "orangutan", "kangaroo", "rubberDuck", "timorousBeastie");

    @Test
    public void testThingChannelOrder() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingTypeId");
        ThingUID thingUID = new ThingUID(thingTypeUID, "thingLabel");

        // create and fill the list of origin channels
        List<Channel> originChannels = new ArrayList<>();
        CHANNEL_IDS.forEach(channelId -> originChannels
                .add(ChannelBuilder.create(new ChannelUID(thingUID, channelId), null).build()));
        assertEquals(CHANNEL_IDS.size(), originChannels.size());

        // build a thing with the origin channels
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID).withChannels(originChannels).build();

        List<Channel> resultChannels;

        // test #1: read the channels from the thing, and compare the resulting channel order
        resultChannels = thing.getChannels();
        assertEquals(CHANNEL_IDS.size(), resultChannels.size());
        for (int i = 0; i < CHANNEL_IDS.size(); i++) {
            assertTrue(CHANNEL_IDS.get(i).equals(resultChannels.get(i).getUID().getId()));
        }

        // test #2: serialize/deserialize the thing via a DTO, and compare the resulting channel order
        resultChannels = ThingDTOMapper.map(ThingDTOMapper.map(thing), false).getChannels();
        assertEquals(CHANNEL_IDS.size(), resultChannels.size());
        for (int i = 0; i < CHANNEL_IDS.size(); i++) {
            assertTrue(CHANNEL_IDS.get(i).equals(resultChannels.get(i).getUID().getId()));
        }
    }
}
