/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.core.dto;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.dto.ConditionDTO;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution and API
 * @author Kai Kreuzer - Changed to using ModuleBuilder
 */
public class ConditionDTOMapper extends ModuleDTOMapper {

    public static ConditionDTO map(final Condition condition) {
        final ConditionDTO conditionDto = new ConditionDTO();
        fillProperties(condition, conditionDto);
        conditionDto.inputs = condition.getInputs();
        return conditionDto;
    }

    public static Condition mapDto(final ConditionDTO conditionDto) {
        return ModuleBuilder.createCondition().withId(conditionDto.id).withTypeUID(conditionDto.type)
                .withConfiguration(new Configuration(conditionDto.configuration)).withInputs(conditionDto.inputs)
                .withLabel(conditionDto.label).withDescription(conditionDto.description).build();
    }

    public static List<ConditionDTO> map(final List<? extends Condition> conditions) {
        if (conditions == null) {
            return null;
        }
        final List<ConditionDTO> dtos = new ArrayList<ConditionDTO>(conditions.size());
        for (final Condition action : conditions) {
            dtos.add(map(action));
        }
        return dtos;
    }

    public static List<Condition> mapDto(final List<ConditionDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        final List<Condition> conditions = new ArrayList<Condition>(dtos.size());
        for (final ConditionDTO dto : dtos) {
            conditions.add(mapDto(dto));
        }
        return conditions;
    }

}
