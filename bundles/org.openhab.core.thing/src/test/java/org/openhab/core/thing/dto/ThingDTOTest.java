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
package org.openhab.core.thing.dto;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;

/**
 * This is the test class for {@link ThingDTO}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ThingDTOTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding-id", "thing-type-id");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing-id");
    private final Map<String, String> properties = Collections.singletonMap("key1", "value1");

    @Test
    public void testThingDTOMappingIsBidirectional() {
        Thing subject = ThingBuilder.create(THING_TYPE_UID, THING_UID).withLabel("Test")
                .withBridge(new ThingUID(new ThingTypeUID("binding-id", "bridge-type-id"), "bridge-id"))
                .withChannels(
                        ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), CoreItemFactory.STRING).build(),
                        ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), CoreItemFactory.STRING).build())
                .withConfiguration(new Configuration(Collections.singletonMap("param1", "value1")))
                .withProperties(properties).withLocation("Somewhere over the rainbow").build();
        Thing result = ThingDTOMapper.map(ThingDTOMapper.map(subject), false);
        assertThat(result, is(instanceOf(ThingImpl.class)));
        assertThat(result.getThingTypeUID(), is(THING_TYPE_UID));
        assertThat(result.getUID(), is(THING_UID));
        assertThat(result.getLabel(), is(subject.getLabel()));
        assertThat(result.getBridgeUID(), is(subject.getBridgeUID()));
        assertThatChannelsArePresent(result.getChannels(), subject.getChannels());
        assertThat(result.getConfiguration(), is(subject.getConfiguration()));
        assertThat(result.getProperties().values(), hasSize(1));
        assertThat(result.getProperties(), is(subject.getProperties()));
        assertThat(result.getLocation(), is(subject.getLocation()));
    }

    @Test
    public void testBridgeDTOMappingIsBidirectional() {
        Bridge subject = BridgeBuilder.create(THING_TYPE_UID, THING_UID).build();
        Thing result = ThingDTOMapper.map(ThingDTOMapper.map(subject), true);
        assertThat(result, is(instanceOf(BridgeImpl.class)));
    }

    private void assertThatChannelsArePresent(List<Channel> actual, List<Channel> expected) {
        assertThat(actual, hasSize(expected.size()));
        actual.stream().map(channel -> channel.getUID()).forEach(uid -> {
            assertThat(expected.stream().filter(channel -> uid.equals(channel.getUID())).findFirst().orElse(null),
                    is(notNullValue()));
        });
    }

}
