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

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.function.Predicate;

import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTOMapper;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationHelper;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.io.rest.core.internal.RESTCoreActivator;
import org.eclipse.smarthome.io.rest.core.internal.item.ItemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnrichedItemDTOMapper} is a utility class to map items into enriched item data transform objects (DTOs).
 *
 * @author Dennis Nobel - Initial contribution
 * @author Jochen Hiller - Fix #473630 - handle optional dependency to TransformationHelper
 */
public class EnrichedItemDTOMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedItemDTOMapper.class);

    /**
     * Maps item into enriched item DTO object.
     *
     * @param item the item
     * @param drillDown defines whether the whole tree should be traversed or only direct members are considered
     * @param itemFilter a predicate that filters items while traversing the tree (true means that an item is
     *            considered)
     * @param uri the uri
     * @return item DTO object
     */
    public static EnrichedItemDTO map(Item item, boolean drillDown, Predicate<Item> itemFilter, URI uri,
            Locale locale) {
        ItemDTO itemDTO = ItemDTOMapper.map(item);
        return map(item, itemDTO, uri, drillDown, itemFilter, locale);
    }

    private static EnrichedItemDTO map(Item item, ItemDTO itemDTO, URI uri, boolean drillDown,
            Predicate<Item> itemFilter, Locale locale) {
        String state = item.getState().toFullString();
        String transformedState = considerTransformation(state, item, locale);
        if (transformedState != null && transformedState.equals(state)) {
            transformedState = null;
        }
        StateDescription stateDescription = considerTransformation(item.getStateDescription(locale));
        String link = null != uri ? uri.toASCIIString() + ItemResource.PATH_ITEMS + "/" + itemDTO.name : null;

        EnrichedItemDTO enrichedItemDTO = null;

        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            EnrichedItemDTO[] memberDTOs;
            if (drillDown) {
                Collection<EnrichedItemDTO> members = new LinkedHashSet<>();
                for (Item member : groupItem.getMembers()) {
                    if (itemFilter == null || itemFilter.test(member)) {
                        members.add(map(member, drillDown, itemFilter, uri, locale));
                    }
                }
                memberDTOs = members.toArray(new EnrichedItemDTO[members.size()]);
            } else {
                memberDTOs = new EnrichedItemDTO[0];
            }
            enrichedItemDTO = new EnrichedGroupItemDTO(itemDTO, memberDTOs, link, state, transformedState,
                    stateDescription);
        } else {
            enrichedItemDTO = new EnrichedItemDTO(itemDTO, link, state, transformedState, stateDescription);
        }

        return enrichedItemDTO;
    }

    private static StateDescription considerTransformation(StateDescription desc) {
        if (desc == null || desc.getPattern() == null) {
            return desc;
        } else {
            try {
                return TransformationHelper.isTransform(desc.getPattern()) ? new StateDescription(desc.getMinimum(),
                        desc.getMaximum(), desc.getStep(), "", desc.isReadOnly(), desc.getOptions()) : desc;
            } catch (NoClassDefFoundError ex) {
                // TransformationHelper is optional dependency, so ignore if class not found
                // return state description as it is without transformation
                return desc;
            }
        }
    }

    private static String considerTransformation(String state, Item item, Locale locale) {
        StateDescription stateDescription = item.getStateDescription(locale);
        if (stateDescription != null && stateDescription.getPattern() != null && state != null) {
            try {
                return TransformationHelper.transform(RESTCoreActivator.getBundleContext(),
                        stateDescription.getPattern(), state.toString());
            } catch (NoClassDefFoundError ex) {
                // TransformationHelper is optional dependency, so ignore if class not found
                // return state as it is without transformation
                return state;
            } catch (TransformationException e) {
                LOGGER.warn("Failed transforming the state '{}' on item '{}' with pattern '{}': {}", state,
                        item.getName(), stateDescription.getPattern(), e.getMessage());
                return state;
            }
        } else {
            return state;
        }
    }
}
