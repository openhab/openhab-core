/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.StateDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize channel definitions.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Added properties
 */
@Schema(name = "ChannelDefinition")
@NonNullByDefault
public class ChannelDefinitionDTO {

    public @Nullable String description;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String label;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public Set<String> tags;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public Map<String, String> properties;
    public @Nullable String category;
    public @Nullable StateDescription stateDescription;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean advanced;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String typeUID;

    // do not remove - needed by GSON
    public ChannelDefinitionDTO() {
        this("", "", "", null, Set.of(), null, null, false, Map.of());
    }

    public ChannelDefinitionDTO(String id, String typeUID, String label, @Nullable String description, Set<String> tags,
            @Nullable String category, @Nullable StateDescription stateDescription, boolean advanced,
            Map<String, String> properties) {
        this.description = description;
        this.label = label;
        this.id = id;
        this.typeUID = typeUID;
        this.tags = tags;
        this.category = category;
        this.stateDescription = stateDescription;
        this.advanced = advanced;
        this.properties = properties;
    }
}
