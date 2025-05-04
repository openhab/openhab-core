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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.items.dto.MetadataDTO;

/**
 * The {@link FileFormatItemDTOMapper} is a utility class to map items into file format item data transfer objects
 * (DTOs).
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class FileFormatItemDTOMapper {

    /**
     * Maps item into file format item DTO object.
     *
     * @param item the item
     * @param metadata some metadata (including channel links)
     * @return file format item DTO object
     */
    public static FileFormatItemDTO map(Item item, List<Metadata> metadata) {
        ItemDTO itemDto = ItemDTOMapper.map(item);
        FileFormatItemDTO dto = new FileFormatItemDTO(itemDto, itemDto instanceof GroupItemDTO);

        List<FileFormatChannelLinkDTO> hannelLinks = new ArrayList<>();
        Map<String, MetadataDTO> metadataDTO = new LinkedHashMap<>();
        metadata.forEach(md -> {
            if (item.getName().equals(md.getUID().getItemName())) {
                if ("channel".equals(md.getUID().getNamespace())) {
                    hannelLinks.add(new FileFormatChannelLinkDTO(md.getValue(),
                            md.getConfiguration().isEmpty() ? null : md.getConfiguration()));
                } else {
                    MetadataDTO mdDTO = new MetadataDTO();
                    mdDTO.value = md.getValue();
                    mdDTO.config = md.getConfiguration().isEmpty() ? null : md.getConfiguration();
                    metadataDTO.put(md.getUID().getNamespace(), mdDTO);
                }
            }
        });
        if (!hannelLinks.isEmpty()) {
            dto.channelLinks = hannelLinks;
        }
        if (!metadataDTO.isEmpty()) {
            dto.metadata = metadataDTO;
        }

        return dto;
    }

    /**
     * Maps file format item DTO object into item.
     *
     * @param dto the file format item DTO object
     * @param itemBuilderFactory the item builder factory
     * @return item
     */
    public static @Nullable Item map(FileFormatItemDTO dto, ItemBuilderFactory itemBuilderFactory) {
        if (GroupItem.TYPE.equals(dto.type)) {
            GroupItemDTO groupDto = new GroupItemDTO();
            groupDto.type = dto.type;
            groupDto.name = dto.name;
            groupDto.label = dto.label;
            groupDto.category = dto.category;
            groupDto.tags = dto.tags;
            groupDto.groupNames = dto.groupNames;
            groupDto.groupType = dto.groupType;
            groupDto.function = dto.function;
            return ItemDTOMapper.map(groupDto, itemBuilderFactory);
        }
        return ItemDTOMapper.map(dto, itemBuilderFactory);
    }

    /**
     * Maps file format item DTO object into a collection of metadata including channels links
     * provided through the "channel" namespace.
     *
     * @param dto the file format item DTO object
     * @return the collection of metadata
     */
    public static Collection<Metadata> mapMetadata(FileFormatItemDTO dto) {
        String name = dto.name;
        Collection<Metadata> metadata = new ArrayList<>();
        if (dto.channelLinks != null) {
            for (FileFormatChannelLinkDTO link : dto.channelLinks) {
                MetadataKey key = new MetadataKey("channel", name);
                metadata.add(new Metadata(key, link.channelUID, link.configuration));
            }
        }
        if (dto.metadata != null) {
            for (Map.Entry<String, MetadataDTO> md : dto.metadata.entrySet()) {
                MetadataKey key = new MetadataKey(md.getKey(), name);
                metadata.add(new Metadata(key, Objects.requireNonNull(md.getValue().value), md.getValue().config));
            }
        }
        return metadata;
    }
}
