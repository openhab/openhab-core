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
import java.util.Collection;
import java.util.List;

import org.eclipse.smarthome.automation.dto.CompositeTriggerTypeDTO;
import org.eclipse.smarthome.automation.dto.TriggerTypeDTO;
import org.eclipse.smarthome.automation.type.CompositeTriggerType;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTOMapper;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution and API
 * @author Ana Dimova - extends Trigger Module type DTOs with composites
 */
public class TriggerTypeDTOMapper extends ModuleTypeDTOMapper {

    public static TriggerTypeDTO map(final TriggerType triggerType) {
        return map(triggerType, new TriggerTypeDTO());
    }

    public static CompositeTriggerTypeDTO map(final CompositeTriggerType triggerType) {
        final CompositeTriggerTypeDTO triggerTypeDto = map(triggerType, new CompositeTriggerTypeDTO());
        triggerTypeDto.children = TriggerDTOMapper.map(triggerType.getChildren());
        return triggerTypeDto;
    }

    public static TriggerType map(final CompositeTriggerTypeDTO triggerTypeDto) {
        if (triggerTypeDto.children == null || triggerTypeDto.children.isEmpty()) {
            return new TriggerType(triggerTypeDto.uid,
                    ConfigDescriptionDTOMapper.map(triggerTypeDto.configDescriptions), triggerTypeDto.label,
                    triggerTypeDto.description, triggerTypeDto.tags, triggerTypeDto.visibility, triggerTypeDto.outputs);
        } else {
            return new CompositeTriggerType(triggerTypeDto.uid,
                    ConfigDescriptionDTOMapper.map(triggerTypeDto.configDescriptions), triggerTypeDto.label,
                    triggerTypeDto.description, triggerTypeDto.tags, triggerTypeDto.visibility, triggerTypeDto.outputs,
                    TriggerDTOMapper.mapDto(triggerTypeDto.children));
        }
    }

    public static List<TriggerTypeDTO> map(final Collection<TriggerType> types) {
        if (types == null) {
            return null;
        }
        final List<TriggerTypeDTO> dtos = new ArrayList<TriggerTypeDTO>(types.size());
        for (final TriggerType type : types) {
            if (type instanceof CompositeTriggerType) {
                dtos.add(map((CompositeTriggerType) type));
            } else {
                dtos.add(map(type));
            }
        }
        return dtos;
    }

    private static <T extends TriggerTypeDTO> T map(final TriggerType triggerType, final T triggerTypeDto) {
        fillProperties(triggerType, triggerTypeDto);
        triggerTypeDto.outputs = triggerType.getOutputs();
        return triggerTypeDto;
    }

}
