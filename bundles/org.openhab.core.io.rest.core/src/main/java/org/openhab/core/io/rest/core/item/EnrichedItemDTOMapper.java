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
package org.openhab.core.io.rest.core.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnrichedItemDTOMapper} is a utility class to map items into enriched item data transform objects (DTOs).
 *
 * @author Dennis Nobel - Initial contribution
 * @author Jochen Hiller - Fix #473630 - handle optional dependency to TransformationHelper
 */
@NonNullByDefault
public class EnrichedItemDTOMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedItemDTOMapper.class);

    /**
     * Maps item into enriched item DTO object.
     *
     * @param item the item
     * @param drillDown defines whether the whole tree should be traversed or only direct members are considered
     * @param itemFilter a predicate that filters items while traversing the tree (true means that an item is
     *            considered, can be null)
     * @param uriBuilder if present the URI builder contains one template that will be replaced by the specific item
     *            name
     * @param locale locale (can be null)
     * @return item DTO object
     */
    public static EnrichedItemDTO map(Item item, boolean drillDown, @Nullable Predicate<Item> itemFilter,
            @Nullable UriBuilder uriBuilder, @Nullable Locale locale) {
        ItemDTO itemDTO = ItemDTOMapper.map(item);
        return map(item, itemDTO, drillDown, itemFilter, uriBuilder, locale, new ArrayList<>());
    }

    private static EnrichedItemDTO mapRecursive(Item item, @Nullable Predicate<Item> itemFilter,
            @Nullable UriBuilder uriBuilder, @Nullable Locale locale, List<Item> parents) {
        ItemDTO itemDTO = ItemDTOMapper.map(item);
        return map(item, itemDTO, true, itemFilter, uriBuilder, locale, parents);
    }

    private static EnrichedItemDTO map(Item item, ItemDTO itemDTO, boolean drillDown,
            @Nullable Predicate<Item> itemFilter, @Nullable UriBuilder uriBuilder, @Nullable Locale locale,
            List<Item> parents) {
        if (item instanceof GroupItem) {
            // only add as parent item if it is a group, otherwise duplicate memberships trigger false warnings
            parents.add(item);
        }
        String state = item.getState().toFullString();
        String transformedState = considerTransformation(state, item, locale);
        if (transformedState != null && transformedState.equals(state)) {
            transformedState = null;
        }
        StateDescription stateDescription = considerTransformation(item.getStateDescription(locale));

        final String link;
        if (uriBuilder != null) {
            link = uriBuilder.build(itemDTO.name).toASCIIString();
        } else {
            link = null;
        }

        EnrichedItemDTO enrichedItemDTO;

        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            EnrichedItemDTO[] memberDTOs;
            if (drillDown) {
                Collection<EnrichedItemDTO> members = new LinkedHashSet<>();
                for (Item member : groupItem.getMembers()) {
                    if (parents.contains(member)) {
                        LOGGER.error(
                                "Recursive group membership found: {} is both, a direct or indirect parent and a child of {}.",
                                member.getName(), groupItem.getName());
                    } else if (itemFilter == null || itemFilter.test(member)) {
                        members.add(mapRecursive(member, itemFilter, uriBuilder, locale, parents));
                    }
                }
                memberDTOs = members.toArray(new EnrichedItemDTO[0]);
            } else {
                memberDTOs = new EnrichedItemDTO[0];
            }
            enrichedItemDTO = new EnrichedGroupItemDTO(itemDTO, memberDTOs, link, state, transformedState,
                    stateDescription);
        } else {
            enrichedItemDTO = new EnrichedItemDTO(itemDTO, link, state, transformedState, stateDescription,
                    item.getCommandDescription(locale));
        }

        return enrichedItemDTO;
    }

    private static @Nullable StateDescription considerTransformation(@Nullable StateDescription stateDescription) {
        if (stateDescription != null) {
            String pattern = stateDescription.getPattern();
            if (pattern != null) {
                try {
                    return TransformationHelper.isTransform(pattern)
                            ? StateDescriptionFragmentBuilder.create(stateDescription).withPattern(pattern).build()
                                    .toStateDescription()
                            : stateDescription;
                } catch (NoClassDefFoundError ex) {
                    // TransformationHelper is optional dependency, so ignore if class not found
                    // return state description as it is without transformation
                }
            }
        }
        return stateDescription;
    }

    private static @Nullable String considerTransformation(String state, Item item, @Nullable Locale locale) {
        StateDescription stateDescription = item.getStateDescription(locale);
        if (stateDescription != null) {
            String pattern = stateDescription.getPattern();
            if (pattern != null) {
                try {
                    return TransformationHelper.transform(
                            FrameworkUtil.getBundle(EnrichedItemDTOMapper.class).getBundleContext(), pattern, state);
                } catch (NoClassDefFoundError ex) {
                    // TransformationHelper is optional dependency, so ignore if class not found
                    // return state as it is without transformation
                } catch (TransformationException e) {
                    LOGGER.warn("Failed transforming the state '{}' on item '{}' with pattern '{}': {}", state,
                            item.getName(), pattern, e.getMessage());
                }
            }
        }
        return state;
    }
}
