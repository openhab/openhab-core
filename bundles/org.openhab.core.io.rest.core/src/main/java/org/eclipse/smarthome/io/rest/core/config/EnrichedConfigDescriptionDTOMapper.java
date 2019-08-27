/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.core.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTOMapper;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterGroupDTO;

/**
 * The {@link EnrichedConfigDescriptionDTOMapper} is a utility class to map config descriptions into enriched config
 * descriptions data transform objects (DTOs).
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EnrichedConfigDescriptionDTOMapper extends ConfigDescriptionDTOMapper {

    /**
     * Maps configuration description into enriched configuration description DTO object.
     *
     * @param configDescription the configuration description (not null)
     * @return enriched configuration description DTO object
     */
    public static EnrichedConfigDescriptionDTO map(ConfigDescription configDescription) {
        List<ConfigDescriptionParameterGroupDTO> parameterGroups = mapParameterGroups(
                configDescription.getParameterGroups());
        List<ConfigDescriptionParameterDTO> parameters = mapEnrichedParameters(configDescription.getParameters());
        return new EnrichedConfigDescriptionDTO(toDecodedString(configDescription.getUID()), parameters,
                parameterGroups);
    }

    public static List<ConfigDescriptionParameterDTO> mapEnrichedParameters(
            List<ConfigDescriptionParameter> parameters) {
        List<ConfigDescriptionParameterDTO> configDescriptionParameterBeans = new ArrayList<>(parameters.size());
        for (ConfigDescriptionParameter configDescriptionParameter : parameters) {
            ConfigDescriptionParameterDTO configDescriptionParameterBean = new EnrichedConfigDescriptionParameterDTO(
                    configDescriptionParameter.getName(), configDescriptionParameter.getType(),
                    configDescriptionParameter.getMinimum(), configDescriptionParameter.getMaximum(),
                    configDescriptionParameter.getStepSize(), configDescriptionParameter.getPattern(),
                    configDescriptionParameter.isRequired(), configDescriptionParameter.isReadOnly(),
                    configDescriptionParameter.isMultiple(), configDescriptionParameter.getContext(),
                    configDescriptionParameter.getDefault(), configDescriptionParameter.getLabel(),
                    configDescriptionParameter.getDescription(), mapOptions(configDescriptionParameter.getOptions()),
                    mapFilterCriteria(configDescriptionParameter.getFilterCriteria()),
                    configDescriptionParameter.getGroupName(), configDescriptionParameter.isAdvanced(),
                    configDescriptionParameter.getLimitToOptions(), configDescriptionParameter.getMultipleLimit(),
                    configDescriptionParameter.getUnit(), configDescriptionParameter.getUnitLabel(),
                    configDescriptionParameter.isVerifyable());
            configDescriptionParameterBeans.add(configDescriptionParameterBean);
        }
        return configDescriptionParameterBeans;
    }

}
