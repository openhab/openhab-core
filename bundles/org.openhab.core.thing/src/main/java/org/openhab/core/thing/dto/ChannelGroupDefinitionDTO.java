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

import java.util.List;

/**
 * This is a data transfer object that is used to serialize channel group definitions.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ChannelGroupDefinitionDTO {

    public String id;
    public String description;
    public String label;
    public List<ChannelDefinitionDTO> channels;

    public ChannelGroupDefinitionDTO() {
    }

    public ChannelGroupDefinitionDTO(String id, String label, String description, List<ChannelDefinitionDTO> channels) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.channels = channels;
    }
}
