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
package org.openhab.core.items.dto;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object that is used to serialize metadata for a certain namespace and item.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Schema(name = "Metadata")
@NonNullByDefault
public class MetadataDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public @Nullable String value;
    public @Nullable Map<String, Object> config;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public @Nullable Boolean editable;
}
