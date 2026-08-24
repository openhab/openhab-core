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
package org.openhab.core.io.rest.core.config.dto;

import java.util.List;

import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.io.rest.core.config.EnrichedConfigDescriptionParameterDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An enriched version of {@link ConfigDescriptionDTO} using {@link EnrichedConfigDescriptionParameterDTO} instead of
 * {@link ConfigDescriptionParameterDTO}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Schema(name = "EnrichedConfigDescription")
public class EnrichedConfigDescriptionDTO {

    public String uri;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<EnrichedConfigDescriptionParameterDTO> parameters;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public List<ConfigDescriptionParameterGroupDTO> parameterGroups;

    public EnrichedConfigDescriptionDTO() {
    }

    public EnrichedConfigDescriptionDTO(String uri, List<EnrichedConfigDescriptionParameterDTO> parameters,
            List<ConfigDescriptionParameterGroupDTO> parameterGroups) {
        this.uri = uri;
        this.parameters = parameters;
        this.parameterGroups = parameterGroups;
    }
}
