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
package org.openhab.core.io.rest.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@link ConfigurableServiceDTO} is a data transfer object for configurable services.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Triller - added multiple field
 */
@Schema(name = "ConfigurableService")
@NonNullByDefault
public class ConfigurableServiceDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String label;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String category;
    public @Nullable String configDescriptionURI;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean multiple;

    public ConfigurableServiceDTO(String id, String label, String category, @Nullable String configDescriptionURI,
            boolean multiple) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.configDescriptionURI = configDescriptionURI;
        this.multiple = multiple;
    }
}
