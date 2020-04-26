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
package org.openhab.core.config.core.dto;

import java.util.List;

import org.openhab.core.config.core.ConfigDescription;

/**
 * {@link ConfigDescriptionDTO} is a data transfer object for {@link ConfigDescription}.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ConfigDescriptionDTO {

    public String uri;

    public List<ConfigDescriptionParameterDTO> parameters;

    public List<ConfigDescriptionParameterGroupDTO> parameterGroups;

    public ConfigDescriptionDTO(String uri, List<ConfigDescriptionParameterDTO> parameters,
            List<ConfigDescriptionParameterGroupDTO> parameterGroups) {
        this.uri = uri;
        this.parameters = parameters;
        this.parameterGroups = parameterGroups;
    }
}
