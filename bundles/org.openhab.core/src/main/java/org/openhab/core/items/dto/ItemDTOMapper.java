/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.items.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilder;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemUtil;

/**
 * The {@link ItemDTOMapper} is a utility class to map items into item data transfer objects (DTOs).
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Bußweiler - Moved to core and renamed class to DTO mapper
 * @author Dennis Nobel - Removed dynamic data
 */
@NonNullByDefault
public class ItemDTOMapper {
    /**
     * Maps item DTO into item object.
     *
     * @param itemDTO the DTO
     * @param itemBuilderFactory the item registry
     * @return the item object
     */
    public static @Nullable Item map(ItemDTO itemDTO, ItemBuilderFactory itemBuilderFactory) {
        if (!ItemUtil.isValidItemName(itemDTO.name)) {
            throw new IllegalArgumentException("The item name '" + itemDTO.name + "' is invalid.");
        }

        if (itemDTO.type != null) {
            ItemBuilder builder = itemBuilderFactory.newItemBuilder(itemDTO.type, itemDTO.name);

            if (itemDTO instanceof GroupItemDTO groupItemDTO && GroupItem.TYPE.equals(itemDTO.type)) {
                Item baseItem = null;
                if (groupItemDTO.groupType != null && !groupItemDTO.groupType.isEmpty()) {
                    baseItem = itemBuilderFactory.newItemBuilder(groupItemDTO.groupType, itemDTO.name).build();
                    builder.withBaseItem(baseItem);
                }
                if (groupItemDTO.function != null) {
                    builder.withGroupFunction(groupItemDTO.function.name, groupItemDTO.function.params);
                }
            }

            builder.withLabel(itemDTO.label);
            builder.withCategory(itemDTO.category);
            builder.withGroups(itemDTO.groupNames);
            builder.withTags(itemDTO.tags);
            try {
                return builder.build();
            } catch (IllegalStateException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Maps item into item DTO object.
     *
     * @param item the item
     * @return item DTO object
     */
    public static ItemDTO map(Item item) {
        ItemDTO itemDTO = item instanceof GroupItem ? new GroupItemDTO() : new ItemDTO();
        fillProperties(itemDTO, item);
        return itemDTO;
    }

    private static void fillProperties(ItemDTO itemDTO, Item item) {
        if (item instanceof GroupItem groupItem) {
            GroupItemDTO groupItemDTO = (GroupItemDTO) itemDTO;
            Item baseItem = groupItem.getBaseItem();
            if (baseItem != null) {
                groupItemDTO.groupType = baseItem.getType();
                GroupFunctionDTO groupFunctionDTO = new GroupFunctionDTO();
                groupFunctionDTO.name = groupItem.getFunction();
                groupFunctionDTO.params = groupItem.getFunctionParams();
                groupItemDTO.function = groupFunctionDTO;
            }
        }

        itemDTO.name = item.getName();
        itemDTO.type = item.getType();
        itemDTO.label = item.getLabel();
        itemDTO.tags = item.getTags();
        itemDTO.category = item.getCategory();
        itemDTO.groupNames = item.getGroupNames();
    }
}
