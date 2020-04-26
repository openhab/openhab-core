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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;

/**
 * {@link ConfigDescriptionDTOMapper} maps {@link ConfigDescription}s to the data transfer object
 * {@link ConfigDescriptionDTO}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Ana Dimova - converting ConfigDescriptionParameterDTO to ConfigDescriptionParameter
 */
public class ConfigDescriptionDTOMapper {

    /**
     * Maps configuration description into configuration description DTO object.
     *
     * @param configDescription the configuration description (not null)
     * @return the configuration description DTO object
     */
    public static ConfigDescriptionDTO map(ConfigDescription configDescription) {
        List<ConfigDescriptionParameterGroupDTO> parameterGroups = mapParameterGroups(
                configDescription.getParameterGroups());
        List<ConfigDescriptionParameterDTO> parameters = mapParameters(configDescription.getParameters());
        return new ConfigDescriptionDTO(toDecodedString(configDescription.getUID()), parameters, parameterGroups);
    }

    protected static String toDecodedString(URI uri) {
        // combine these partials because URI.toString() does not decode
        return uri.getScheme() + ":" + uri.getSchemeSpecificPart();
    }

    public static List<ConfigDescriptionParameter> map(List<ConfigDescriptionParameterDTO> parameters) {
        if (parameters == null) {
            return null;
        }
        final List<ConfigDescriptionParameter> result = new ArrayList<>(parameters.size());
        for (ConfigDescriptionParameterDTO parameter : parameters) {
            result.add(ConfigDescriptionParameterBuilder.create(parameter.name, parameter.type)
                    .withContext(parameter.context).withDefault(parameter.defaultValue)
                    .withDescription(parameter.description).withLabel(parameter.label).withRequired(parameter.required)
                    .withMinimum(parameter.min).withMaximum(parameter.max).withStepSize(parameter.stepsize)
                    .withPattern(parameter.pattern).withReadOnly(parameter.readOnly).withMultiple(parameter.multiple)
                    .withMultipleLimit(parameter.multipleLimit).withGroupName(parameter.groupName)
                    .withAdvanced(parameter.advanced).withVerify(parameter.verify)
                    .withLimitToOptions(parameter.limitToOptions).withUnit(parameter.unitLabel)
                    .withUnitLabel(parameter.unitLabel).withOptions(mapOptionsDTO(parameter.options))
                    .withFilterCriteria(mapFilterCriteriaDTO(parameter.filterCriteria)).build());
        }
        return result;
    }

    private static List<FilterCriteria> mapFilterCriteriaDTO(List<FilterCriteriaDTO> filterCriteria) {
        if (filterCriteria == null) {
            return null;
        }
        List<FilterCriteria> result = new LinkedList<>();
        for (FilterCriteriaDTO criteria : filterCriteria) {
            result.add(new FilterCriteria(criteria.name, criteria.value));
        }
        return result;
    }

    private static List<ParameterOption> mapOptionsDTO(List<ParameterOptionDTO> options) {
        if (options == null) {
            return null;
        }
        List<ParameterOption> result = new LinkedList<>();
        for (ParameterOptionDTO option : options) {
            result.add(new ParameterOption(option.value, option.label));
        }
        return result;
    }

    /**
     * Maps configuration description parameters into DTO objects.
     *
     * @param parameters the configuration description parameters (not null)
     * @return the parameter DTO objects (not null)
     */
    public static List<ConfigDescriptionParameterDTO> mapParameters(List<ConfigDescriptionParameter> parameters) {
        List<ConfigDescriptionParameterDTO> configDescriptionParameterBeans = new ArrayList<>(parameters.size());
        for (ConfigDescriptionParameter configDescriptionParameter : parameters) {
            ConfigDescriptionParameterDTO configDescriptionParameterBean = new ConfigDescriptionParameterDTO(
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

    /**
     * Maps configuration description parameter groups into DTO objects.
     *
     * @param parameterGroups the configuration description parameter groups (not null)
     * @return the parameter group DTO objects (not null)
     */
    public static List<ConfigDescriptionParameterGroupDTO> mapParameterGroups(
            List<ConfigDescriptionParameterGroup> parameterGroups) {
        List<ConfigDescriptionParameterGroupDTO> parameterGroupBeans = new ArrayList<>(parameterGroups.size());

        for (ConfigDescriptionParameterGroup parameterGroup : parameterGroups) {
            parameterGroupBeans
                    .add(new ConfigDescriptionParameterGroupDTO(parameterGroup.getName(), parameterGroup.getContext(),
                            parameterGroup.isAdvanced(), parameterGroup.getLabel(), parameterGroup.getDescription()));
        }

        return parameterGroupBeans;
    }

    protected static List<FilterCriteriaDTO> mapFilterCriteria(List<FilterCriteria> filterCriteria) {
        if (filterCriteria == null) {
            return null;
        }
        List<FilterCriteriaDTO> result = new LinkedList<>();
        for (FilterCriteria criteria : filterCriteria) {
            result.add(new FilterCriteriaDTO(criteria.getName(), criteria.getValue()));
        }
        return result;
    }

    protected static List<ParameterOptionDTO> mapOptions(List<ParameterOption> options) {
        if (options == null) {
            return null;
        }
        List<ParameterOptionDTO> result = new LinkedList<>();
        for (ParameterOption option : options) {
            result.add(new ParameterOptionDTO(option.getValue(), option.getLabel()));
        }
        return result;
    }
}
