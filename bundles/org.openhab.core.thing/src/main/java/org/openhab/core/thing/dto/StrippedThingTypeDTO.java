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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize stripped thing types.
 * Stripped thing types exclude the parameters, configDescription and channels
 *
 * @author Miki Jankov - Initial contribution
 * @author Andrew Fiddian-Green - Added semanticEquipmentTag
 */
@Schema(name = "StrippedThingType")
@NonNullByDefault
public class StrippedThingTypeDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String UID;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String label;
    public @Nullable String description;
    public @Nullable String category;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean listed;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<String> supportedBridgeTypeUIDs;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean bridge;
    public @Nullable String semanticEquipmentTag;

    // do not remove - needed by GSON
    public StrippedThingTypeDTO() {
        this("", "", null, null, true, List.of(), false, null);
    }

    public StrippedThingTypeDTO(String uid, String label, @Nullable String description, @Nullable String category,
            boolean listed, List<String> supportedBridgeTypeUIDs, boolean bridge,
            @Nullable String semanticEquipmentTag) {
        this.UID = uid;
        this.label = label;
        this.description = description;
        this.category = category;
        this.listed = listed;
        this.supportedBridgeTypeUIDs = supportedBridgeTypeUIDs;
        this.bridge = bridge;
        this.semanticEquipmentTag = semanticEquipmentTag;
    }
}
