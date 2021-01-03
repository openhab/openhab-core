/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link LegacyThingDTOMapper} is an utility class to map things in the old structure to things in the new
 * structure.
 *
 * @author Simon Lamon - Initial contribution
 */
@NonNullByDefault
@Deprecated
public class LegacyThingDTOMapper {

    /**
     * Maps channel into channel DTO object.
     *
     * @param channel the channel
     * @return the channel DTO object
     */
    public static ChannelDTO map(LegacyChannelDTO channel) {
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        String channelTypeUIDValue = channelTypeUID != null ? channelTypeUID.getAsString() : null;
        return new ChannelDTO(channel.getUid(), channelTypeUIDValue, channel.getAcceptedItemType(), channel.getKind(),
                channel.getLabel(), channel.getDescription(), channel.getProperties(), channel.getConfiguration(),
                channel.getDefaultTags(), channel.getAutoUpdatePolicy());
    }

    /**
     * Maps thing into thing data transfer object (DTO).
     *
     * @param thing the thing
     * @return the thing DTO object
     */
    public static ThingDTO map(LegacyThingDTO thing, boolean isBridge) {
        List<ChannelDTO> channelDTOs = new ArrayList<>();
        for (LegacyChannelDTO channel : thing.getChannels()) {
            ChannelDTO channelDTO = map(channel);
            channelDTOs.add(channelDTO);
        }

        String thingTypeUID = thing.getThingTypeUID().getAsString();
        String thingUID = thing.getUID().toString();
        final ThingUID bridgeUID = thing.getBridgeUID();

        return new ThingDTO(thingTypeUID, thingUID, thing.getLabel(), bridgeUID != null ? bridgeUID.toString() : null,
                channelDTOs, toMap(thing.getConfiguration()), thing.getProperties(), thing.getLocation(), isBridge);
    }

    private static Map<String, Object> toMap(Configuration configuration) {
        Map<String, Object> configurationMap = new HashMap<>(configuration.keySet().size());
        for (String key : configuration.keySet()) {
            configurationMap.put(key, configuration.get(key));
        }
        return configurationMap;
    }
}
