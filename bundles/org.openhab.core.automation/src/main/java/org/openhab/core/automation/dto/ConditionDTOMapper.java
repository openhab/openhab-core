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
package org.openhab.core.automation.dto;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.config.core.Configuration;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - Changed to using ModuleBuilder
 */
@NonNullByDefault
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

    public static List<ConditionDTO> map(final @Nullable List<? extends Condition> conditions) {
        if (conditions == null) {
            return List.of();
        }
        final List<ConditionDTO> dtos = new ArrayList<>(conditions.size());
        for (final Condition action : conditions) {
            dtos.add(map(action));
        }
        return dtos;
    }

    public static List<Condition> mapDto(final @Nullable List<ConditionDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        final List<Condition> conditions = new ArrayList<>(dtos.size());
        for (final ConditionDTO dto : dtos) {
            conditions.add(mapDto(dto));
        }
        return conditions;
    }
}
