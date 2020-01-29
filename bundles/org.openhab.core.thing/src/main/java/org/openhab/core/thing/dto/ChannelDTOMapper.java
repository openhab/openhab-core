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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link ChannelDTOMapper} is an utility class to map channels into channel data transfer objects (DTOs).
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Kai Kreuzer - added DTO to channel mapping
 */
@NonNullByDefault
public class ChannelDTOMapper {

    /**
     * Maps channel into channel DTO object.
     *
     * @param channel the channel
     * @return the channel DTO object
     */
    public static ChannelDTO map(Channel channel) {
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        String channelTypeUIDValue = channelTypeUID != null ? channelTypeUID.getAsString() : null;
        return new ChannelDTO(channel.getUID(), channelTypeUIDValue, channel.getAcceptedItemType(), channel.getKind(),
                channel.getLabel(), channel.getDescription(), channel.getProperties(), channel.getConfiguration(),
                channel.getDefaultTags(), channel.getAutoUpdatePolicy());
    }

    /**
     * Maps channel DTO into channel object.
     *
     * @param channelDTO the channel DTO
     * @return the channel object
     */
    public static Channel map(ChannelDTO channelDTO) {
        ChannelUID channelUID = new ChannelUID(channelDTO.uid);
        ChannelTypeUID channelTypeUID = channelDTO.channelTypeUID != null
                ? new ChannelTypeUID(channelDTO.channelTypeUID)
                : null;
        return ChannelBuilder.create(channelUID, channelDTO.itemType)
                .withConfiguration(new Configuration(channelDTO.configuration)).withLabel(channelDTO.label)
                .withDescription(channelDTO.description).withProperties(channelDTO.properties).withType(channelTypeUID)
                .withDefaultTags(channelDTO.defaultTags).withKind(ChannelKind.parse(channelDTO.kind))
                .withAutoUpdatePolicy(AutoUpdatePolicy.parse(channelDTO.autoUpdatePolicy)).build();
    }
}
