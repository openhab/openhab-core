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
package org.eclipse.smarthome.io.rest.core.item;

import org.eclipse.smarthome.core.items.dto.GroupFunctionDTO;
import org.eclipse.smarthome.core.items.dto.GroupItemDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * This is an enriched data transfer object that is used to serialize group items.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
public class EnrichedGroupItemDTO extends EnrichedItemDTO {

    public EnrichedGroupItemDTO(ItemDTO itemDTO, EnrichedItemDTO[] members, String link, String state,
            String transformedState, StateDescription stateDescription) {
        super(itemDTO, link, state, transformedState, stateDescription);
        this.members = members;
        this.groupType = ((GroupItemDTO) itemDTO).groupType;
        this.function = ((GroupItemDTO) itemDTO).function;
    }

    public EnrichedItemDTO[] members;
    public String groupType;
    public GroupFunctionDTO function;

}
