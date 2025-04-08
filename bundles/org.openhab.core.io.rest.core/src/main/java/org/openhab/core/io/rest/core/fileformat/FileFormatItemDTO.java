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
package org.openhab.core.io.rest.core.fileformat;

import java.util.List;
import java.util.Map;

import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.items.dto.MetadataDTO;

/**
 * This is a data transfer object to serialize an item contained in a file format.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class FileFormatItemDTO extends ItemDTO {

    public String groupType;
    public GroupFunctionDTO function;
    public Map<String, MetadataDTO> metadata;
    public List<FileFormatChannelLinkDTO> channelLinks;

    public FileFormatItemDTO(ItemDTO itemDTO, boolean isGroup) {
        this.type = itemDTO.type;
        this.name = itemDTO.name;
        this.label = itemDTO.label;
        this.category = itemDTO.category;
        this.tags = itemDTO.tags;
        this.groupNames = itemDTO.groupNames;
        if (isGroup) {
            this.groupType = ((GroupItemDTO) itemDTO).groupType;
            this.function = ((GroupItemDTO) itemDTO).function;
        }
    }
}
