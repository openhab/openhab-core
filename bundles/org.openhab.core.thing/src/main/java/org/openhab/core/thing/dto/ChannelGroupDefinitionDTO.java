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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize channel group definitions.
 *
 * @author Dennis Nobel - Initial contribution
 */
@Schema(name = "ChannelGroupDefinition")
@NonNullByDefault
public class ChannelGroupDefinitionDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String id;
    public @Nullable String description;
    public @Nullable String label;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<ChannelDefinitionDTO> channels;

    // do not remove - needed by GSON
    public ChannelGroupDefinitionDTO() {
        this("", null, null, List.of());
    }

    public ChannelGroupDefinitionDTO(String id, @Nullable String label, @Nullable String description,
            List<ChannelDefinitionDTO> channels) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.channels = channels;
    }
}
