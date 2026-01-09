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
package org.openhab.core.io.rest.core.fileformat;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a data transfer object to serialize a channel link for an item contained in a file format.
 *
 * @author Laurent Garnier - Initial contribution
 */
@Schema(name = "FileFormatChannelLink")
@NonNullByDefault
public class FileFormatChannelLinkDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String channelUID;
    public @Nullable Map<String, Object> configuration;

    public FileFormatChannelLinkDTO(String channelUID, @Nullable Map<String, Object> configuration) {
        this.channelUID = channelUID;
        this.configuration = configuration;
    }
}
