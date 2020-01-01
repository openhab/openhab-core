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
package org.openhab.core.automation.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.config.core.dto.ConfigDescriptionDTOMapper;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Ana Dimova - extends Condition Module type DTOs with composites
 */
public class ConditionTypeDTOMapper extends ModuleTypeDTOMapper {

    public static ConditionTypeDTO map(final ConditionType conditionType) {
        return map(conditionType, new ConditionTypeDTO());
    }

    public static CompositeConditionTypeDTO map(final CompositeConditionType conditionType) {
        final CompositeConditionTypeDTO conditionTypeDto = map(conditionType, new CompositeConditionTypeDTO());
        conditionTypeDto.children = ConditionDTOMapper.map(conditionType.getChildren());
        return conditionTypeDto;
    }

    public static ConditionType map(CompositeConditionTypeDTO conditionTypeDto) {
        if (conditionTypeDto.children == null || conditionTypeDto.children.isEmpty()) {
            return new ConditionType(conditionTypeDto.uid,
                    ConfigDescriptionDTOMapper.map(conditionTypeDto.configDescriptions), conditionTypeDto.label,
                    conditionTypeDto.description, conditionTypeDto.tags, conditionTypeDto.visibility,
                    conditionTypeDto.inputs);
        } else {
            return new CompositeConditionType(conditionTypeDto.uid,
                    ConfigDescriptionDTOMapper.map(conditionTypeDto.configDescriptions), conditionTypeDto.label,
                    conditionTypeDto.description, conditionTypeDto.tags, conditionTypeDto.visibility,
                    conditionTypeDto.inputs, ConditionDTOMapper.mapDto(conditionTypeDto.children));
        }
    }

    public static List<ConditionTypeDTO> map(final Collection<ConditionType> types) {
        if (types == null) {
            return null;
        }
        final List<ConditionTypeDTO> dtos = new ArrayList<>(types.size());
        for (final ConditionType type : types) {
            if (type instanceof CompositeConditionType) {
                dtos.add(map((CompositeConditionType) type));
            } else {
                dtos.add(map(type));
            }
        }
        return dtos;
    }

    private static <T extends ConditionTypeDTO> T map(final ConditionType conditionType, final T conditionTypeDto) {
        fillProperties(conditionType, conditionTypeDto);
        conditionTypeDto.inputs = conditionType.getInputs();
        return conditionTypeDto;
    }

}
