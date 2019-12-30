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
package org.openhab.core.io.rest.core.item;

import java.util.Map;

import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

/**
 * This is an enriched data transfer object that is used to serialize items with dynamic data like the state, the state
 * description and the link.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - Added metadata
 */
public class EnrichedItemDTO extends ItemDTO {

    public String link;
    public String state;
    public String transformedState;
    public StateDescription stateDescription;
    public CommandDescription commandDescription;
    public Map<String, Object> metadata;
    public Boolean editable;

    public EnrichedItemDTO(ItemDTO itemDTO, String link, String state, String transformedState,
            StateDescription stateDescription, CommandDescription commandDescription) {
        this.type = itemDTO.type;
        this.name = itemDTO.name;
        this.label = itemDTO.label;
        this.category = itemDTO.category;
        this.tags = itemDTO.tags;
        this.groupNames = itemDTO.groupNames;
        this.link = link;
        this.state = state;
        this.transformedState = transformedState;
        this.stateDescription = stateDescription;
        this.commandDescription = commandDescription;
    }

}
