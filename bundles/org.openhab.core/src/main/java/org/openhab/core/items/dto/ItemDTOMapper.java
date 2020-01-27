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
package org.openhab.core.items.dto;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Quantity;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.items.GroupFunctionHelper;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilder;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.slf4j.LoggerFactory;

/**
 * The {@link ItemDTOMapper} is an utility class to map items into item data transfer objects (DTOs).
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Bußweiler - Moved to core and renamed class to DTO mapper
 * @author Dennis Nobel - Removed dynamic data
 */
@NonNullByDefault
public class ItemDTOMapper {

    private static final GroupFunctionHelper GROUP_FUNCTION_HELPER = new GroupFunctionHelper();

    /**
     * Maps item DTO into item object.
     *
     * @param itemDTO the DTO
     * @param itemBuilderFactory the item registry
     * @return the item object
     */
    public static @Nullable Item map(ItemDTO itemDTO, ItemBuilderFactory itemBuilderFactory) {
        if (itemDTO == null) {
            throw new IllegalArgumentException("The argument 'itemDTO' must no be null.");
        }
        if (itemBuilderFactory == null) {
            throw new IllegalArgumentException("The argument 'itemBuilderFactory' must no be null.");
        }

        if (itemDTO.type != null) {
            ItemBuilder builder = itemBuilderFactory.newItemBuilder(itemDTO.type, itemDTO.name);

            if (itemDTO instanceof GroupItemDTO && GroupItem.TYPE.equals(itemDTO.type)) {
                GroupItemDTO groupItemDTO = (GroupItemDTO) itemDTO;
                Item baseItem = null;
                if (!StringUtils.isEmpty(groupItemDTO.groupType)) {
                    baseItem = itemBuilderFactory.newItemBuilder(groupItemDTO.groupType, itemDTO.name).build();
                    builder.withBaseItem(baseItem);
                }
                GroupFunction function = new GroupFunction.Equality();
                if (groupItemDTO.function != null) {
                    function = mapFunction(baseItem, groupItemDTO.function);
                }
                builder.withGroupFunction(function);
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

    public static GroupFunction mapFunction(@Nullable Item baseItem, GroupFunctionDTO function) {
        List<State> args = parseStates(baseItem, function.params);

        return GROUP_FUNCTION_HELPER.createGroupFunction(function, args, getDimension(baseItem));
    }

    private static @Nullable Class<? extends Quantity<?>> getDimension(@Nullable Item baseItem) {
        if (baseItem instanceof NumberItem) {
            return ((NumberItem) baseItem).getDimension();
        }

        return null;
    }

    private static List<State> parseStates(@Nullable Item baseItem, String @Nullable [] params) {
        List<State> states = new ArrayList<>();

        if (params == null || baseItem == null) {
            return states;
        }

        for (String param : params) {
            State state = TypeParser.parseState(baseItem.getAcceptedDataTypes(), param);
            if (state == null) {
                LoggerFactory.getLogger(ItemDTOMapper.class).warn(
                        "State '{}' is not valid for a group item with base type '{}'", param, baseItem.getType());
                states.clear();
                break;
            } else {
                states.add(state);
            }
        }
        return states;
    }

    /**
     * Maps item into item DTO object.
     *
     * @param item the item
     * @param drillDown the drill down
     * @param uri the uri
     * @return item DTO object
     */
    public static ItemDTO map(Item item) {
        ItemDTO itemDTO = item instanceof GroupItem ? new GroupItemDTO() : new ItemDTO();
        fillProperties(itemDTO, item);
        return itemDTO;
    }

    private static void fillProperties(ItemDTO itemDTO, Item item) {
        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            GroupItemDTO groupItemDTO = (GroupItemDTO) itemDTO;
            if (groupItem.getBaseItem() != null) {
                groupItemDTO.groupType = groupItem.getBaseItem().getType();
                groupItemDTO.function = mapFunction(groupItem.getFunction());
            }
        }

        itemDTO.name = item.getName();
        itemDTO.type = item.getType();
        itemDTO.label = item.getLabel();
        itemDTO.tags = item.getTags();
        itemDTO.category = item.getCategory();
        itemDTO.groupNames = item.getGroupNames();
    }

    public static @Nullable GroupFunctionDTO mapFunction(@Nullable GroupFunction function) {
        if (function == null) {
            return null;
        }

        GroupFunctionDTO dto = new GroupFunctionDTO();
        dto.name = function.getClass().getSimpleName().toUpperCase();
        List<String> params = new ArrayList<>();
        for (State param : function.getParameters()) {
            params.add(param.toString());
        }
        if (!params.isEmpty()) {
            dto.params = params.toArray(new String[params.size()]);
        }

        return dto;
    }

}
