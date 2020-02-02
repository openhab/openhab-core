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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.util.ThingHelper;

/**
 * The {@link ThingDTOMapper} is an utility class to map things into data transfer objects (DTO).
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Kai Kreuzer - Added DTO to Thing mapping
 */
@NonNullByDefault
public class ThingDTOMapper {

    /**
     * Maps thing into thing data transfer object (DTO).
     *
     * @param thing the thing
     * @return the thing DTO object
     */
    public static ThingDTO map(Thing thing) {
        List<ChannelDTO> channelDTOs = new ArrayList<>();
        for (Channel channel : thing.getChannels()) {
            ChannelDTO channelDTO = ChannelDTOMapper.map(channel);
            channelDTOs.add(channelDTO);
        }

        String thingTypeUID = thing.getThingTypeUID().getAsString();
        String thingUID = thing.getUID().toString();
        final ThingUID bridgeUID = thing.getBridgeUID();

        return new ThingDTO(thingTypeUID, thingUID, thing.getLabel(), bridgeUID != null ? bridgeUID.toString() : null,
                channelDTOs, toMap(thing.getConfiguration()), thing.getProperties(), thing.getLocation());
    }

    /**
     * Maps thing DTO into thing
     *
     * @param thingDTO the thingDTO
     * @param isBridge flag if the thing DTO identifies a bridge
     * @return the corresponding thing
     */
    public static Thing map(ThingDTO thingDTO, boolean isBridge) {
        ThingUID thingUID = new ThingUID(thingDTO.UID);
        ThingTypeUID thingTypeUID = thingDTO.thingTypeUID == null ? new ThingTypeUID("")
                : new ThingTypeUID(thingDTO.thingTypeUID);
        final Thing thing;
        if (isBridge) {
            thing = BridgeBuilder.create(thingTypeUID, thingUID).build();
        } else {
            thing = ThingBuilder.create(thingTypeUID, thingUID).build();
        }
        return ThingHelper.merge(thing, thingDTO);
    }

    private static Map<String, Object> toMap(Configuration configuration) {
        Map<String, Object> configurationMap = new HashMap<>(configuration.keySet().size());
        for (String key : configuration.keySet()) {
            configurationMap.put(key, configuration.get(key));
        }
        return configurationMap;
    }

}
