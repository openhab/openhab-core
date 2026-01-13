/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used with to serialize channel types.
 *
 * @author Chris Jackson - Initial contribution
 * @author Mark Herwege - added unit hint
 */
@Schema(name = "ChannelType")
@NonNullByDefault
public class ChannelTypeDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<ConfigDescriptionParameterDTO> parameters;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<ConfigDescriptionParameterGroupDTO> parameterGroups;
    public @Nullable String description;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String label;
    public @Nullable String category;
    public @Nullable String itemType;
    public @Nullable String unitHint;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "STATE", "TRIGGER" })
    public String kind;
    public @Nullable StateDescription stateDescription;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public Set<String> tags;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String UID;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean advanced;
    public @Nullable CommandDescription commandDescription;

    // do not remove - needed by GSON
    public ChannelTypeDTO() {
        this("", "", null, null, null, null, ChannelKind.STATE, List.of(), List.of(), null, Set.of(), false, null);
    }

    public ChannelTypeDTO(String uid, String label, @Nullable String description, @Nullable String category,
            @Nullable String itemType, @Nullable String unitHint, ChannelKind kind,
            List<ConfigDescriptionParameterDTO> parameters, List<ConfigDescriptionParameterGroupDTO> parameterGroups,
            @Nullable StateDescription stateDescription, Set<String> tags, boolean advanced,
            @Nullable CommandDescription commandDescription) {
        this.UID = uid;
        this.label = label;
        this.description = description;
        this.category = category;
        this.parameters = parameters;
        this.parameterGroups = parameterGroups;
        this.stateDescription = stateDescription;
        this.tags = tags;
        this.kind = kind.toString();
        this.itemType = itemType;
        this.unitHint = unitHint;
        this.advanced = advanced;
        this.commandDescription = commandDescription;
    }
}
