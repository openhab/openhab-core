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
package org.openhab.core.io.rest.core.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionDTOMapper;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.io.rest.core.config.dto.EnrichedConfigDescriptionDTO;

/**
 * The {@link EnrichedConfigDescriptionDTOMapper} is a utility class to map {@link ConfigDescription}s to config
 * descriptions data transform objects {@link ConfigDescriptionDTO} containing
 * {@link EnrichedConfigDescriptionParameterDTO}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EnrichedConfigDescriptionDTOMapper {

    /**
     * Maps configuration description into configuration description DTO object.
     *
     * @param configDescription the configuration description (not null)
     * @return enriched configuration description DTO object
     */
    public static EnrichedConfigDescriptionDTO map(ConfigDescription configDescription) {
        List<ConfigDescriptionParameterGroupDTO> parameterGroups = ConfigDescriptionDTOMapper
                .mapParameterGroups(configDescription.getParameterGroups());
        List<EnrichedConfigDescriptionParameterDTO> parameters = mapEnrichedParameters(
                configDescription.getParameters());
        return new EnrichedConfigDescriptionDTO(ConfigDescriptionDTOMapper.toDecodedString(configDescription.getUID()),
                parameters, parameterGroups);
    }

    /**
     * Maps configuration description parameters into enriched DTO objects.
     *
     * @param parameters the configuration description parameters (not null)
     * @return the parameter enriched DTO objects
     */
    public static List<EnrichedConfigDescriptionParameterDTO> mapEnrichedParameters(
            List<ConfigDescriptionParameter> parameters) {
        List<EnrichedConfigDescriptionParameterDTO> configDescriptionParameterBeans = new ArrayList<>(
                parameters.size());
        for (ConfigDescriptionParameter configDescriptionParameter : parameters) {
            EnrichedConfigDescriptionParameterDTO configDescriptionParameterBean = new EnrichedConfigDescriptionParameterDTO(
                    configDescriptionParameter.getName(), configDescriptionParameter.getType(),
                    configDescriptionParameter.getMinimum(), configDescriptionParameter.getMaximum(),
                    configDescriptionParameter.getStepSize(), configDescriptionParameter.getPattern(),
                    configDescriptionParameter.isRequired(), configDescriptionParameter.isReadOnly(),
                    configDescriptionParameter.isMultiple(), configDescriptionParameter.getContext(),
                    configDescriptionParameter.getDefault(), configDescriptionParameter.getLabel(),
                    configDescriptionParameter.getDescription(),
                    ConfigDescriptionDTOMapper.mapOptions(configDescriptionParameter.getOptions()),
                    ConfigDescriptionDTOMapper.mapFilterCriteria(configDescriptionParameter.getFilterCriteria()),
                    configDescriptionParameter.getGroupName(), configDescriptionParameter.isAdvanced(),
                    configDescriptionParameter.getLimitToOptions(), configDescriptionParameter.getMultipleLimit(),
                    configDescriptionParameter.getUnit(), configDescriptionParameter.getUnitLabel(),
                    configDescriptionParameter.isVerifyable());
            configDescriptionParameterBeans.add(configDescriptionParameterBean);
        }
        return configDescriptionParameterBeans;
    }
}
