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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;
import org.eclipse.smarthome.config.core.dto.FilterCriteriaDTO;
import org.eclipse.smarthome.config.core.dto.ParameterOptionDTO;

/**
 * This is a data transfer object that is used to serialize parameter of an enriched configuration description.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class EnrichedConfigDescriptionParameterDTO extends ConfigDescriptionParameterDTO {

    private static final String DEFAULT_LIST_DELIMITER = ",";

    public @Nullable Collection<String> defaultValues;

    public EnrichedConfigDescriptionParameterDTO(String name, Type type, BigDecimal minimum, BigDecimal maximum,
            BigDecimal stepsize, String pattern, Boolean required, Boolean readOnly, Boolean multiple, String context,
            String defaultValue, String label, String description, List<ParameterOptionDTO> options,
            List<FilterCriteriaDTO> filterCriteria, String groupName, Boolean advanced, Boolean limitToOptions,
            Integer multipleLimit, String unit, String unitLabel, Boolean verify) {
        super(name, type, minimum, maximum, stepsize, pattern, required, readOnly, multiple, context, defaultValue,
                label, description, options, filterCriteria, groupName, advanced, limitToOptions, multipleLimit, unit,
                unitLabel, verify);

        if (multiple) {
            if (defaultValue.contains(DEFAULT_LIST_DELIMITER)) {
                defaultValues = Arrays.asList(defaultValue.split(DEFAULT_LIST_DELIMITER)).stream().map(v -> v.trim())
                        .filter(v -> !v.isEmpty()).collect(Collectors.toList());
            } else {
                defaultValues = Collections.singleton(defaultValue);
            }
        }
    }

}
