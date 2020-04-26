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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.firmware.dto.FirmwareStatusDTO;

/**
 * The {@link EnrichedThingDTOMapper} is an utility class to map things into enriched thing data transfer objects
 * (DTOs).
 *
 * @author Dennis Nobel - Initial contribution
 */
public class EnrichedThingDTOMapper extends ThingDTOMapper {

    /**
     * Maps thing into enriched thing data transfer object.
     *
     * @param thing the thing
     * @param thingStatusInfo the thing status information to be used for the enriched object
     * @param firmwareStatus the firmwareStatus to be used for the enriched object
     * @param linkedItemsMap the map of linked items to be injected into the enriched object
     * @param editable true if this thing can be edited
     * @return the enriched thing DTO object
     */
    public static EnrichedThingDTO map(Thing thing, ThingStatusInfo thingStatusInfo, FirmwareStatusDTO firmwareStatus,
            Map<String, Set<String>> linkedItemsMap, boolean editable) {
        ThingDTO thingDTO = ThingDTOMapper.map(thing);

        List<ChannelDTO> channels = new ArrayList<>();
        for (ChannelDTO channel : thingDTO.channels) {
            Set<String> linkedItems = linkedItemsMap != null ? linkedItemsMap.get(channel.id) : Collections.emptySet();
            channels.add(new EnrichedChannelDTO(channel, linkedItems));
        }

        return new EnrichedThingDTO(thingDTO, channels, thingStatusInfo, firmwareStatus, editable);
    }
}
