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
package org.openhab.core.io.rest.core.thing;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.firmware.dto.FirmwareStatusDTO;

/**
 * @author Henning Treu - Initial contribution
 */
public class EnrichedThingDTOMapperTest {

    private static final String ITEM_TYPE = "itemType";
    private static final String THING_TYPE_UID = "thing:type:uid";
    private static final String UID = "thing:uid:1";
    private static final String THING_LABEL = "label";
    private static final String LOCATION = "location";

    @Mock
    private Thing thing;

    @Mock
    private ThingStatusInfo thingStatusInfo;

    @Mock
    private FirmwareStatusDTO firmwareStatus;

    @Mock
    private Map<String, Set<String>> linkedItemsMap;

    @Mock
    private Configuration configuration;

    @Mock
    private Map<String, String> properties;

    @Before
    public void setup() {
        initMocks(this);
        when(thing.getThingTypeUID()).thenReturn(new ThingTypeUID(THING_TYPE_UID));
        when(thing.getUID()).thenReturn(new ThingUID(UID));
        when(thing.getLabel()).thenReturn(THING_LABEL);
        when(thing.getChannels()).thenReturn(mockChannels());
        when(thing.getConfiguration()).thenReturn(configuration);
        when(thing.getProperties()).thenReturn(properties);
        when(thing.getLocation()).thenReturn(LOCATION);
    }

    @Test
    public void shouldMapEnrichedThingDTO() {
        when(linkedItemsMap.get("1")).thenReturn(Stream.of("linkedItem1", "linkedItem2").collect(Collectors.toSet()));

        EnrichedThingDTO enrichedThingDTO = EnrichedThingDTOMapper.map(thing, thingStatusInfo, firmwareStatus,
                linkedItemsMap, true);

        assertThat(enrichedThingDTO.editable, is(true));
        assertThat(enrichedThingDTO.firmwareStatus, is(equalTo(firmwareStatus)));
        assertThat(enrichedThingDTO.statusInfo, is(equalTo(thingStatusInfo)));
        assertThat(enrichedThingDTO.thingTypeUID, is(equalTo(thing.getThingTypeUID().getAsString())));
        assertThat(enrichedThingDTO.label, is(equalTo(THING_LABEL)));
        assertThat(enrichedThingDTO.bridgeUID, is(CoreMatchers.nullValue()));

        assertChannels(enrichedThingDTO);

        assertThat(enrichedThingDTO.configuration.values(), is(empty()));
        assertThat(enrichedThingDTO.properties, is(equalTo(properties)));
        assertThat(enrichedThingDTO.location, is(equalTo(LOCATION)));
    }

    private void assertChannels(EnrichedThingDTO enrichedThingDTO) {
        assertThat(enrichedThingDTO.channels, hasSize(2));
        assertThat(enrichedThingDTO.channels.get(0), is(instanceOf(EnrichedChannelDTO.class)));

        EnrichedChannelDTO channel1 = (EnrichedChannelDTO) enrichedThingDTO.channels.get(0);
        assertThat(channel1.linkedItems, hasSize(2));
    }

    private List<Channel> mockChannels() {
        List<Channel> channels = new ArrayList<>();

        channels.add(ChannelBuilder.create(new ChannelUID(THING_TYPE_UID + ":" + UID + ":1"), ITEM_TYPE).build());
        channels.add(ChannelBuilder.create(new ChannelUID(THING_TYPE_UID + ":" + UID + ":2"), ITEM_TYPE).build());

        return channels;
    }

}
