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
package org.openhab.core.thing.dto;

import java.util.Map;
import java.util.Set;

import org.openhab.core.types.StateDescription;

/**
 * This is a data transfer object that is used to serialize channel definitions.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Added properties
 */
public class ChannelDefinitionDTO {

    public String description;
    public String id;
    public String label;
    public Set<String> tags;
    public Map<String, String> properties;
    public String category;
    public StateDescription stateDescription;
    public boolean advanced;
    public String typeUID;

    public ChannelDefinitionDTO() {
    }

    public ChannelDefinitionDTO(String id, String typeUID, String label, String description, Set<String> tags,
            String category, StateDescription stateDescription, boolean advanced, Map<String, String> properties) {
        this.description = description;
        this.label = label;
        this.id = id;
        this.typeUID = typeUID;
        this.tags = tags;
        this.category = category;
        this.stateDescription = stateDescription;
        this.advanced = advanced;
        this.properties = properties;
    }

}
