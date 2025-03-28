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
package org.openhab.core.io.rest.core.config;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.FilterCriteriaDTO;
import org.openhab.core.config.core.dto.ParameterOptionDTO;

/**
 * This is an enriched data transfer object that is used to serialize config descriptions parameters with a list of
 * default values if a configuration description defines <code>multiple="true"</code>.
 * 
 * The default values are split by a comma. Individual values that contain a comma
 * must be escaped with a backslash character. The backslash character will be
 * removed from the value.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class EnrichedConfigDescriptionParameterDTO extends ConfigDescriptionParameterDTO {

    public Collection<String> defaultValues;

    @SuppressWarnings("unchecked")
    public EnrichedConfigDescriptionParameterDTO(String name, Type type, BigDecimal minimum, BigDecimal maximum,
            BigDecimal stepsize, String pattern, Boolean required, Boolean readOnly, Boolean multiple, String context,
            String defaultValue, String label, String description, List<ParameterOptionDTO> options,
            List<FilterCriteriaDTO> filterCriteria, String groupName, Boolean advanced, Boolean limitToOptions,
            Integer multipleLimit, String unit, String unitLabel, Boolean verify) {
        super(name, type, minimum, maximum, stepsize, pattern, required, readOnly, multiple, context, defaultValue,
                label, description, options, filterCriteria, groupName, advanced, limitToOptions, multipleLimit, unit,
                unitLabel, verify);

        if (multiple && defaultValue != null) {
            ConfigDescriptionParameter parameter = ConfigDescriptionParameterBuilder.create(name, type)
                    .withMultiple(multiple).withDefault(defaultValue).withMultipleLimit(multipleLimit).build();
            if (ConfigUtil.getDefaultValueAsCorrectType(parameter) instanceof List defaultValues) {
                this.defaultValues = (Collection<String>) defaultValues;
            }
        }
    }
}
