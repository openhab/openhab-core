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

import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.config.core.Configuration;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - Changed to using ModuleBuilder
 */
public class TriggerDTOMapper extends ModuleDTOMapper {

    public static TriggerDTO map(final Trigger trigger) {
        final TriggerDTO triggerDto = new TriggerDTO();
        fillProperties(trigger, triggerDto);
        return triggerDto;
    }

    public static Trigger mapDto(final TriggerDTO triggerDto) {
        return ModuleBuilder.createTrigger().withId(triggerDto.id).withTypeUID(triggerDto.type)
                .withConfiguration(new Configuration(triggerDto.configuration)).withLabel(triggerDto.label)
                .withDescription(triggerDto.description).build();
    }

    public static List<TriggerDTO> map(final Collection<? extends Trigger> triggers) {
        if (triggers == null) {
            return null;
        }
        final List<TriggerDTO> dtos = new ArrayList<>(triggers.size());
        for (final Trigger trigger : triggers) {
            dtos.add(map(trigger));
        }
        return dtos;
    }

    public static List<Trigger> mapDto(final Collection<TriggerDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        final List<Trigger> triggers = new ArrayList<>(dtos.size());
        for (final TriggerDTO dto : dtos) {
            triggers.add(mapDto(dto));
        }
        return triggers;
    }

}
