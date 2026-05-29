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
package org.openhab.core.model.yaml.internal.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.FilterCriteriaDTO;
import org.openhab.core.config.core.dto.ParameterOptionDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is a data transfer object used to (de)serialize a {@link ConfigDescriptionParameter} in a YAML configuration.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlConfigDescriptionParameterDTO {

    public String context;
    @JsonProperty("default")
    @JsonAlias("defaultValue")
    public String defaultValue;
    public String description;
    public String label;
    public boolean required;
    public Type type;
    public BigDecimal min;
    public BigDecimal max;
    @JsonAlias({ "stepsize" })
    public BigDecimal step;
    public String pattern;
    public Boolean readOnly;
    public Boolean multiple;
    public Integer multipleLimit;
    public String groupName;
    public Boolean advanced;
    public Boolean verify;
    public Boolean limitToOptions;
    public String unit;
    public String unitLabel;

    public List<ParameterOptionDTO> options;
    public List<FilterCriteriaDTO> filterCriteria;

    /**
     * Creates a new instance.
     */
    public YamlConfigDescriptionParameterDTO() {
    }

    /**
     * Creates a new instance based on the specified {@link ConfigDescriptionParameter}.
     *
     * @param parameter the {@link ConfigDescriptionParameter}.
     */
    public YamlConfigDescriptionParameterDTO(@NonNull ConfigDescriptionParameter parameter) {
        this.type = parameter.getType();
        this.min = parameter.getMinimum();
        this.max = parameter.getMaximum();
        this.step = parameter.getStepSize();
        this.pattern = parameter.getPattern();
        this.readOnly = parameter.isReadOnly();
        this.multiple = parameter.isMultiple();
        this.context = parameter.getContext();
        this.required = parameter.isRequired();
        this.defaultValue = parameter.getDefault();
        this.label = parameter.getLabel();
        this.description = parameter.getDescription();
        List<@NonNull ParameterOption> options = parameter.getOptions();
        if (!options.isEmpty()) {
            List<ParameterOptionDTO> optionDtos = new ArrayList<>(options.size());
            for (ParameterOption option : options) {
                optionDtos.add(new ParameterOptionDTO(option.getValue(), option.getLabel()));
            }
            this.options = optionDtos;
        }
        List<@NonNull FilterCriteria> filterCriteria = parameter.getFilterCriteria();
        if (!filterCriteria.isEmpty()) {
            List<FilterCriteriaDTO> filterCriteriaDtos = new ArrayList<>(filterCriteria.size());
            for (FilterCriteria filterCriterion : filterCriteria) {
                filterCriteriaDtos.add(new FilterCriteriaDTO(filterCriterion.getName(), filterCriterion.getValue()));
            }
            this.filterCriteria = filterCriteriaDtos;
        }
        this.groupName = parameter.getGroupName();
        this.advanced = parameter.isAdvanced();
        this.limitToOptions = parameter.getLimitToOptions();
        this.multipleLimit = parameter.getMultipleLimit();
        this.unit = parameter.getUnit();
        this.unitLabel = parameter.getUnitLabel();
        this.verify = parameter.isVerifyable();
    }

    @Override
    public int hashCode() {
        return Objects.hash(advanced, context, defaultValue, description, filterCriteria, groupName, label,
                limitToOptions, max, min, multiple, multipleLimit, options, pattern, readOnly, required, step, type,
                unit, unitLabel, verify);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlConfigDescriptionParameterDTO)) {
            return false;
        }
        YamlConfigDescriptionParameterDTO other = (YamlConfigDescriptionParameterDTO) obj;
        return Objects.equals(advanced, other.advanced) && Objects.equals(context, other.context)
                && Objects.equals(defaultValue, other.defaultValue) && Objects.equals(description, other.description)
                && Objects.equals(filterCriteria, other.filterCriteria) && Objects.equals(groupName, other.groupName)
                && Objects.equals(label, other.label) && Objects.equals(limitToOptions, other.limitToOptions)
                && Objects.equals(max, other.max) && Objects.equals(min, other.min)
                && Objects.equals(multiple, other.multiple) && Objects.equals(multipleLimit, other.multipleLimit)
                && Objects.equals(options, other.options) && Objects.equals(pattern, other.pattern)
                && Objects.equals(readOnly, other.readOnly) && required == other.required
                && Objects.equals(step, other.step) && type == other.type && Objects.equals(unit, other.unit)
                && Objects.equals(unitLabel, other.unitLabel) && Objects.equals(verify, other.verify);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (context != null) {
            builder.append("context=").append(context).append(", ");
        }
        if (defaultValue != null) {
            builder.append("defaultValue=").append(defaultValue).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        builder.append("required=").append(required).append(", ");
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (min != null) {
            builder.append("min=").append(min).append(", ");
        }
        if (max != null) {
            builder.append("max=").append(max).append(", ");
        }
        if (step != null) {
            builder.append("step=").append(step).append(", ");
        }
        if (pattern != null) {
            builder.append("pattern=").append(pattern).append(", ");
        }
        if (readOnly != null) {
            builder.append("readOnly=").append(readOnly).append(", ");
        }
        if (multiple != null) {
            builder.append("multiple=").append(multiple).append(", ");
        }
        if (multipleLimit != null) {
            builder.append("multipleLimit=").append(multipleLimit).append(", ");
        }
        if (groupName != null) {
            builder.append("groupName=").append(groupName).append(", ");
        }
        if (advanced != null) {
            builder.append("advanced=").append(advanced).append(", ");
        }
        if (verify != null) {
            builder.append("verify=").append(verify).append(", ");
        }
        if (limitToOptions != null) {
            builder.append("limitToOptions=").append(limitToOptions).append(", ");
        }
        if (unit != null) {
            builder.append("unit=").append(unit).append(", ");
        }
        if (unitLabel != null) {
            builder.append("unitLabel=").append(unitLabel).append(", ");
        }
        if (options != null) {
            builder.append("options=").append(options).append(", ");
        }
        if (filterCriteria != null) {
            builder.append("filterCriteria=").append(filterCriteria);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Create a new {@link ConfigDescriptionParameter} from this {@link YamlConfigDescriptionParameterDTO} using the
     * specified name.
     *
     * @param name the name to use.
     * @return The new {@link ConfigDescriptionParameter}.
     */
    public @NonNull ConfigDescriptionParameter map(@NonNull String name) {
        ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder.create(name, type)
                .withAdvanced(advanced).withContext(context).withDefault(defaultValue).withDescription(description)
                .withGroupName(groupName).withLabel(label).withLimitToOptions(limitToOptions).withMaximum(max)
                .withMinimum(min).withMultiple(multiple).withMultipleLimit(multipleLimit).withPattern(pattern)
                .withReadOnly(readOnly).withRequired(required).withStepSize(step).withUnit(unit)
                .withUnitLabel(unitLabel).withVerify(verify);
        if (filterCriteria != null) {
            List<FilterCriteria> parameterfilterCriteria = new ArrayList<>(filterCriteria.size());
            for (FilterCriteriaDTO filterCriterionDto : filterCriteria) {
                parameterfilterCriteria.add(new FilterCriteria(filterCriterionDto.name, filterCriterionDto.value));
            }
            builder.withFilterCriteria(parameterfilterCriteria);
        }
        if (options != null) {
            List<ParameterOption> parameterOptions = new ArrayList<>(options.size());
            for (ParameterOptionDTO optionDto : options) {
                parameterOptions.add(new ParameterOption(optionDto.value, optionDto.label));
            }
            builder.withOptions(parameterOptions);
        }
        return builder.build();
    }

    /**
     * Create a new {@link ConfigDescriptionParameterDTO} from this {@link YamlConfigDescriptionParameterDTO} using the
     * specified name.
     *
     * @param name the name to use.
     * @return The new {@link ConfigDescriptionParameterDTO}.
     */
    public @NonNull ConfigDescriptionParameterDTO toConfigDescriptionParameterDTO(@NonNull String name) {
        ConfigDescriptionParameterDTO result = new ConfigDescriptionParameterDTO();
        result.name = name;
        result.type = type;
        result.context = context;
        result.defaultValue = defaultValue;
        result.description = description;
        result.label = label;
        result.required = required;
        result.min = min;
        result.max = max;
        result.stepsize = step;
        result.pattern = pattern;
        result.readOnly = readOnly;
        result.multiple = multiple;
        result.multipleLimit = multipleLimit;
        result.groupName = groupName;
        result.advanced = advanced;
        result.verify = verify;
        result.limitToOptions = limitToOptions;
        result.unit = unit;
        result.unitLabel = unitLabel;
        result.options = options;
        result.filterCriteria = filterCriteria;
        return result;
    }

    /**
     * Creates a {@link List} of {@link ConfigDescriptionParameter}s from a {@link Map} of parameter names and
     * {@link YamlConfigDescriptionParameterDTO}s, to be used during deserialization.
     *
     * @param configDescriptionDtos the {@link Map} of {@link String} and {@link YamlConfigDescriptionParameterDTO}
     *            pairs.
     * @return The corresponding {@link List} of {@link ConfigDescriptionParameter}s.
     */
    public static @NonNull List<@NonNull ConfigDescriptionParameter> mapConfigDescriptions(
            @NonNull Map<@NonNull String, @NonNull YamlConfigDescriptionParameterDTO> configDescriptionDtos) {
        List<ConfigDescriptionParameter> result = new ArrayList<>(configDescriptionDtos.size());
        for (@NonNull
        Entry<@NonNull String, @NonNull YamlConfigDescriptionParameterDTO> parameterEntry : configDescriptionDtos
                .entrySet()) {
            result.add(parameterEntry.getValue().map(parameterEntry.getKey()));
        }
        return result;
    }

    /**
     * A variant of {@link YamlConfigDescriptionParameterDTO} where it is specified as a array/list element instead of a
     * map value.
     */
    public static class YamlConfigDescriptionParameterListEntryDTO {

        public String name;
        public String context;
        @JsonProperty("default")
        @JsonAlias("defaultValue")
        public String defaultValue;
        public String description;
        public String label;
        public boolean required;
        public Type type;
        public BigDecimal min;
        public BigDecimal max;
        @JsonAlias({ "stepsize" })
        public BigDecimal step;
        public String pattern;
        public Boolean readOnly;
        public Boolean multiple;
        public Integer multipleLimit;
        public String groupName;
        public Boolean advanced;
        public Boolean verify;
        public Boolean limitToOptions;
        public String unit;
        public String unitLabel;

        public List<ParameterOptionDTO> options;
        public List<FilterCriteriaDTO> filterCriteria;

        /**
         * Convert this {@link YamlConfigDescriptionParameterListEntryDTO} to a corresponding
         * {@link YamlConfigDescriptionParameterDTO), where the name is missing. The name must be handled/kept
         * independently.
         *
         * @return The resulting {@link YamlConfigDescriptionParameterDTO}.
         */
        public YamlConfigDescriptionParameterDTO toYamlConfigDescriptionParameterDTO() {
            YamlConfigDescriptionParameterDTO result = new YamlConfigDescriptionParameterDTO();
            result.context = context;
            result.defaultValue = defaultValue;
            result.description = description;
            result.label = label;
            result.required = required;
            result.type = type;
            result.min = min;
            result.max = max;
            result.step = step;
            result.pattern = pattern;
            result.readOnly = readOnly;
            result.multiple = multiple;
            result.multipleLimit = multipleLimit;
            result.groupName = groupName;
            result.advanced = advanced;
            result.verify = verify;
            result.limitToOptions = limitToOptions;
            result.unit = unit;
            result.unitLabel = unitLabel;
            result.options = options;
            result.filterCriteria = filterCriteria;
            return result;
        }
    }
}
