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

import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterGroupDTO;

/**
 * This is a data transfer object that is used with to serialize thing types.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added parameter groups
 * @author Miki Jankov - Introducing StrippedThingTypeDTO
 *
 */
public class ThingTypeDTO extends StrippedThingTypeDTO {

    public List<ChannelDefinitionDTO> channels;
    public List<ChannelGroupDefinitionDTO> channelGroups;
    public List<ConfigDescriptionParameterDTO> configParameters;
    public List<ConfigDescriptionParameterGroupDTO> parameterGroups;
    public Map<String, String> properties;
    public List<String> extensibleChannelTypeIds;

    public ThingTypeDTO() {
    }

    public ThingTypeDTO(String UID, String label, String description, String category, boolean listed,
            List<ConfigDescriptionParameterDTO> configParameters, List<ChannelDefinitionDTO> channels,
            List<ChannelGroupDefinitionDTO> channelGroups, List<String> supportedBridgeTypeUIDs,
            Map<String, String> properties, boolean bridge, List<ConfigDescriptionParameterGroupDTO> parameterGroups,
            List<String> extensibleChannelTypeIds) {
        this.UID = UID;
        this.label = label;
        this.description = description;
        this.category = category;
        this.listed = listed;
        this.configParameters = configParameters;
        this.channels = channels;
        this.channelGroups = channelGroups;
        this.supportedBridgeTypeUIDs = supportedBridgeTypeUIDs;
        this.properties = properties;
        this.bridge = bridge;
        this.parameterGroups = parameterGroups;
        this.extensibleChannelTypeIds = extensibleChannelTypeIds;
    }

}
