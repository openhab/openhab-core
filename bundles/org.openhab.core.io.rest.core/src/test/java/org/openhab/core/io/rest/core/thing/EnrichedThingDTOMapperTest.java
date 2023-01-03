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
package org.openhab.core.io.rest.core.thing;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class EnrichedThingDTOMapperTest {

    private static final String ITEM_TYPE = "itemType";
    private static final String THING_TYPE_UID = "thing:type:uid";
    private static final String UID = "thing:uid:1";
    private static final String THING_LABEL = "label";
    private static final String LOCATION = "location";

    private @Mock @NonNullByDefault({}) Thing thingMock;
    private @Mock @NonNullByDefault({}) ThingStatusInfo thingStatusInfoMock;
    private @Mock @NonNullByDefault({}) FirmwareStatusDTO firmwareStatusMock;
    private @Mock @NonNullByDefault({}) Map<String, Set<String>> linkedItemsMapMock;
    private @Mock @NonNullByDefault({}) Configuration configurationMock;
    private @Mock @NonNullByDefault({}) Map<String, String> propertiesMock;

    @BeforeEach
    public void setup() {
        when(thingMock.getThingTypeUID()).thenReturn(new ThingTypeUID(THING_TYPE_UID));
        when(thingMock.getUID()).thenReturn(new ThingUID(UID));
        when(thingMock.getLabel()).thenReturn(THING_LABEL);
        when(thingMock.getChannels()).thenReturn(mockChannels());
        when(thingMock.getConfiguration()).thenReturn(configurationMock);
        when(thingMock.getProperties()).thenReturn(propertiesMock);
        when(thingMock.getLocation()).thenReturn(LOCATION);
    }

    @Test
    public void shouldMapEnrichedThingDTO() {
        when(linkedItemsMapMock.get("1"))
                .thenReturn(Stream.of("linkedItem1", "linkedItem2").collect(Collectors.toSet()));

        EnrichedThingDTO enrichedThingDTO = EnrichedThingDTOMapper.map(thingMock, thingStatusInfoMock,
                firmwareStatusMock, linkedItemsMapMock, true);

        assertThat(enrichedThingDTO.editable, is(true));
        assertThat(enrichedThingDTO.firmwareStatus, is(equalTo(firmwareStatusMock)));
        assertThat(enrichedThingDTO.statusInfo, is(equalTo(thingStatusInfoMock)));
        assertThat(enrichedThingDTO.thingTypeUID, is(equalTo(thingMock.getThingTypeUID().getAsString())));
        assertThat(enrichedThingDTO.label, is(equalTo(THING_LABEL)));
        assertThat(enrichedThingDTO.bridgeUID, is(CoreMatchers.nullValue()));

        assertChannels(enrichedThingDTO);

        assertThat(enrichedThingDTO.configuration.values(), is(empty()));
        assertThat(enrichedThingDTO.properties, is(equalTo(propertiesMock)));
        assertThat(enrichedThingDTO.location, is(equalTo(LOCATION)));
    }

    private void assertChannels(EnrichedThingDTO enrichedThingDTO) {
        assertThat(enrichedThingDTO.channels, hasSize(2));
        assertThat(enrichedThingDTO.channels.get(0), is(instanceOf(EnrichedChannelDTO.class)));

        EnrichedChannelDTO channel1 = enrichedThingDTO.channels.get(0);
        assertThat(channel1.linkedItems, hasSize(2));
    }

    private List<Channel> mockChannels() {
        List<Channel> channels = new ArrayList<>();

        channels.add(ChannelBuilder.create(new ChannelUID(THING_TYPE_UID + ":" + UID + ":1"), ITEM_TYPE).build());
        channels.add(ChannelBuilder.create(new ChannelUID(THING_TYPE_UID + ":" + UID + ":2"), ITEM_TYPE).build());

        return channels;
    }
}
