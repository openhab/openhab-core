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

import java.util.List;
import java.util.Set;

import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

/**
 * This is a data transfer object that is used with to serialize channel types.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ChannelTypeDTO {

    public List<ConfigDescriptionParameterDTO> parameters;
    public List<ConfigDescriptionParameterGroupDTO> parameterGroups;
    public String description;
    public String label;
    public String category;
    public String itemType;
    public String kind;
    public StateDescription stateDescription;
    public Set<String> tags;
    public String UID;
    public boolean advanced;
    public CommandDescription commandDescription;

    public ChannelTypeDTO() {
    }

    public ChannelTypeDTO(String UID, String label, String description, String category, String itemType,
            ChannelKind kind, List<ConfigDescriptionParameterDTO> parameters,
            List<ConfigDescriptionParameterGroupDTO> parameterGroups, StateDescription stateDescription,
            Set<String> tags, boolean advanced, CommandDescription commandDescription) {
        this.UID = UID;
        this.label = label;
        this.description = description;
        this.category = category;
        this.parameters = parameters;
        this.parameterGroups = parameterGroups;
        this.stateDescription = stateDescription;
        this.tags = tags;
        this.kind = kind.toString();
        this.itemType = itemType;
        this.advanced = advanced;
        this.commandDescription = commandDescription;
    }
}
