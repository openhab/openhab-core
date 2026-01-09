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
package org.openhab.core.io.rest.ui;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object for a UI tile.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Schema(name = "Tile")
@NonNullByDefault
public class TileDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String url;
    public @Nullable String overlay;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String imageUrl;

    public TileDTO(String name, String url, @Nullable String overlay, String imageUrl) {
        this.name = name;
        this.url = url;
        this.overlay = overlay;
        this.imageUrl = imageUrl;
    }
}
