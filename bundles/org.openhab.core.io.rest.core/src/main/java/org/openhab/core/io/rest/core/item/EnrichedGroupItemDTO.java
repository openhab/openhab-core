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
package org.openhab.core.io.rest.core.item;

import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.types.StateDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is an enriched data transfer object that is used to serialize group items.
 *
 * @author Dennis Nobel - Initial contribution
 */
@Schema(name = "EnrichedGroupItem")
public class EnrichedGroupItemDTO extends EnrichedItemDTO {

    public EnrichedGroupItemDTO(ItemDTO itemDTO, EnrichedItemDTO[] members, String link, String state, String lastState,
            Long lastStateUpdate, Long lastStateChange, String transformedState, StateDescription stateDescription,
            String unitSymbol) {
        super(itemDTO, link, state, lastState, lastStateUpdate, lastStateChange, transformedState, stateDescription,
                null, unitSymbol);
        this.members = members;
        this.groupType = ((GroupItemDTO) itemDTO).groupType;
        this.function = ((GroupItemDTO) itemDTO).function;
    }

    public EnrichedItemDTO[] members;
    public String groupType;
    public GroupFunctionDTO function;
}
