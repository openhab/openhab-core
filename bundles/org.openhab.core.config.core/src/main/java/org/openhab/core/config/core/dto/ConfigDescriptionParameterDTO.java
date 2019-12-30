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

import java.math.BigDecimal;
import java.util.List;

import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * This is a data transfer object that is used to serialize parameter of a
 * configuration description.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Alex Tugarev - Extended for options and filter criteria
 * @author Chris Jackson - Added group, advanced, limitToOptions, multipleLimit, verify attributes
 * @author Thomas Höfer - Added unit
 */
public class ConfigDescriptionParameterDTO {

    public String context;
    public String defaultValue;
    public String description;
    public String label;
    public String name;
    public boolean required;
    public Type type;
    public BigDecimal min;
    public BigDecimal max;
    public BigDecimal stepsize;
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

    public ConfigDescriptionParameterDTO() {
    }

    public ConfigDescriptionParameterDTO(String name, Type type, BigDecimal minimum, BigDecimal maximum,
            BigDecimal stepsize, String pattern, Boolean required, Boolean readOnly, Boolean multiple, String context,
            String defaultValue, String label, String description, List<ParameterOptionDTO> options,
            List<FilterCriteriaDTO> filterCriteria, String groupName, Boolean advanced, Boolean limitToOptions,
            Integer multipleLimit, String unit, String unitLabel, Boolean verify) {
        this.name = name;
        this.type = type;
        this.min = minimum;
        this.max = maximum;
        this.stepsize = stepsize;
        this.pattern = pattern;
        this.readOnly = readOnly;
        this.multiple = multiple;
        this.context = context;
        this.required = required;
        this.defaultValue = defaultValue;
        this.label = label;
        this.description = description;
        this.options = options;
        this.filterCriteria = filterCriteria;
        this.groupName = groupName;
        this.advanced = advanced;
        this.limitToOptions = limitToOptions;
        this.multipleLimit = multipleLimit;
        this.unit = unit;
        this.unitLabel = unitLabel;
        this.verify = verify;
    }

}
