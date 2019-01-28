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
package org.eclipse.smarthome.core.thing.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.util.ThingHelper;

/**
 * The {@link ThingDTOMapper} is an utility class to map things into data transfer objects (DTO).
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Kai Kreuzer - Added DTO to Thing mapping
 */
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
        if (configuration == null) {
            return null;
        }

        Map<String, Object> configurationMap = new HashMap<>(configuration.keySet().size());
        for (String key : configuration.keySet()) {
            configurationMap.put(key, configuration.get(key));
        }
        return configurationMap;
    }

}
