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
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * This is the test class for {@link ChannelDTO}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelDTOTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding-id", "thing-type-id");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing-id");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("binding-id", "channel-type-id");
    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel1");
    private final Map<String, String> properties = Collections.singletonMap("key1", "value1");
    private final Set<String> tags = Collections.singleton("tag1");

    @Test
    public void testChannelDTOMappingIsBidirectional() {
        Channel subject = ChannelBuilder.create(CHANNEL_UID, CoreItemFactory.STRING).withType(CHANNEL_TYPE_UID)
                .withLabel("Test").withDescription("My test channel")
                .withConfiguration(new Configuration(Collections.singletonMap("param1", "value1")))
                .withProperties(properties).withDefaultTags(tags).withAutoUpdatePolicy(AutoUpdatePolicy.VETO).build();
        Channel result = ChannelDTOMapper.map(ChannelDTOMapper.map(subject));
        assertThat(result, is(instanceOf(Channel.class)));
        assertThat(result.getChannelTypeUID(), is(CHANNEL_TYPE_UID));
        assertThat(result.getUID(), is(CHANNEL_UID));
        assertThat(result.getAcceptedItemType(), is(subject.getAcceptedItemType()));
        assertThat(result.getKind(), is(subject.getKind()));
        assertThat(result.getLabel(), is(subject.getLabel()));
        assertThat(result.getDescription(), is(subject.getDescription()));
        assertThat(result.getConfiguration(), is(subject.getConfiguration()));
        assertThat(result.getProperties().values(), hasSize(1));
        assertThat(result.getProperties(), is(subject.getProperties()));
        assertThat(result.getDefaultTags(), hasSize(1));
        assertThat(result.getDefaultTags(), is(subject.getDefaultTags()));
        assertThat(result.getAutoUpdatePolicy(), is(subject.getAutoUpdatePolicy()));
    }

}
