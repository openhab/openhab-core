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
package org.openhab.core.config.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * The {@link ConfigDescriptionParameterBuilder} class provides a builder for the {@link ConfigDescriptionParameter}
 * class.
 *
 * @author Chris Jackson - Initial contribution
 * @author Thomas Höfer - Added unit
 */
public class ConfigDescriptionParameterBuilder {
    private String name;
    private Type type;

    private String groupName;

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal step;
    private String pattern;
    private Boolean required;
    private Boolean readOnly;
    private Boolean multiple;
    private Integer multipleLimit;
    private String unit;
    private String unitLabel;

    private String context;
    private String defaultValue;
    private String label;
    private String description;

    private Boolean limitToOptions;
    private Boolean advanced;
    private Boolean verify;

    private List<ParameterOption> options = new ArrayList<>();
    private List<FilterCriteria> filterCriteria = new ArrayList<>();

    private ConfigDescriptionParameterBuilder(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Creates a parameter builder
     *
     * @param name configuration parameter name
     * @param type configuration parameter type
     * @return parameter builder
     */
    public static ConfigDescriptionParameterBuilder create(String name, Type type) {
        return new ConfigDescriptionParameterBuilder(name, type);
    }

    /**
     * Set the minimum value of the configuration parameter
     *
     * @param min the min value of the {@link ConfigDescriptionParameter}
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withMinimum(BigDecimal min) {
        this.min = min;
        return this;
    }

    /**
     * Set the maximum value of the configuration parameter
     *
     * @param max the max value of the {@link ConfigDescriptionParameter}
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withMaximum(BigDecimal max) {
        this.max = max;
        return this;
    }

    /**
     * Set the step size of the configuration parameter
     *
     * @param step the step of the {@link ConfigDescriptionParameter}
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withStepSize(BigDecimal step) {
        this.step = step;
        return this;
    }

    /**
     * Set the pattern of the configuration parameter
     *
     * @param pattern the pattern for the {@link ConfigDescriptionParameter}
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * Set the configuration parameter as read only
     *
     * @param readOnly <code>true</code> to make the parameter read only
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * Set the configuration parameter to allow multiple selection
     *
     * @param multiple <code>true</code> for multiple selection
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withMultiple(Boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    /**
     * Set the configuration parameter to allow multiple selection
     *
     * @param multipleLimit the parameters limit
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withMultipleLimit(Integer multipleLimit) {
        this.multipleLimit = multipleLimit;
        return this;
    }

    /**
     * Set the context of the configuration parameter
     *
     * @param context the context for this parameter
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withContext(String context) {
        this.context = context;
        return this;
    }

    /**
     * Set the configuration parameter to be required
     *
     * @param required <code>true</code> if the parameter is required
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withRequired(Boolean required) {
        this.required = required;
        return this;
    }

    /**
     * Set the default value of the configuration parameter
     *
     * @param defaultValue the default value of the configuration parameter
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withDefault(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Set the label of the configuration parameter
     *
     * @param label a short user friendly description
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Set the description of the configuration parameter
     *
     * @param description a detailed user friendly description
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the options of the configuration parameter
     *
     * @param options the options for this parameter
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withOptions(List<ParameterOption> options) {
        this.options = options;
        return this;
    }

    /**
     * Set the configuration parameter as an advanced parameter
     *
     * @param advanced <code>true</code> to make the parameter advanced
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withAdvanced(Boolean advanced) {
        this.advanced = advanced;
        return this;
    }

    /**
     * Set the configuration parameter as a verifyable parameter
     *
     * @param verify flag
     */
    public ConfigDescriptionParameterBuilder withVerify(Boolean verify) {
        this.verify = verify;
        return this;
    }

    /**
     * Set the configuration parameter to be limited to the values in the options list
     *
     * @param limitToOptions <code>true</code> if only the declared options are acceptable
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withLimitToOptions(Boolean limitToOptions) {
        this.limitToOptions = limitToOptions;
        return this;
    }

    /**
     * Set the configuration parameter to be limited to the values in the options list
     *
     * @param groupName the group name of this config description parameter
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    /**
     * Set the filter criteria of the configuration parameter
     *
     * @param filterCriteria the filter criteria
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withFilterCriteria(List<FilterCriteria> filterCriteria) {
        this.filterCriteria = filterCriteria;
        return this;
    }

    /**
     * Sets the unit of the configuration parameter.
     *
     * @param unit the unit to be set
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withUnit(String unit) {
        this.unit = unit;
        return this;
    }

    /**
     * Sets the unit label of the configuration parameter.
     *
     * @param unitLabel the unit label to be set
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterBuilder withUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
        return this;
    }

    /**
     * Builds a result with the settings of this builder.
     *
     * @return the desired result
     */
    public ConfigDescriptionParameter build() throws IllegalArgumentException {
        return new ConfigDescriptionParameter(name, type, min, max, step, pattern, required, readOnly, multiple,
                context, defaultValue, label, description, options, filterCriteria, groupName, advanced, limitToOptions,
                multipleLimit, unit, unitLabel, verify);
    }
}
