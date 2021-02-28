/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.model.script.internal.engine.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.script.actions.Semantics;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.semantics.SemanticsPredicates;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the Semantics action.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
@Component
public class SemanticsActionService implements ActionService {

    private static @Nullable ItemRegistry itemRegistry;

    @Activate
    public SemanticsActionService(final @Reference ItemRegistry itemRegistry) {
        SemanticsActionService.itemRegistry = itemRegistry;
    }

    @Override
    public Class<?> getActionClass() {
        return Semantics.class;
    }

    public static boolean isLocation(Item item) {
        return SemanticsPredicates.isLocation().test(item);
    }

    public static boolean isEquipment(Item item) {
        return SemanticsPredicates.isEquipment().test(item);
    }

    public static boolean isPoint(Item item) {
        return SemanticsPredicates.isPoint().test(item);
    }

    public static @Nullable Item getLocationItemFromGroupNames(List<String> groupNames) {
        ItemRegistry ir = itemRegistry;
        if (ir != null) {
            List<Item> groupItems = new ArrayList<>();
            for (String groupName : groupNames) {
                try {
                    Item group = ir.getItem(groupName);
                    // if group is a location, return it (first location found)
                    if (isLocation(group)) {
                        return group;
                    }
                    groupItems.add(group);
                } catch (ItemNotFoundException e) {
                    // should not happen
                }
            }
            // if no location is found, iterate the groups of each group
            for (Item group : groupItems) {
                Item locationItem = getLocationItemFromGroupNames(group.getGroupNames());
                if (locationItem != null) {
                    return locationItem;
                }
            }
        }
        return null;
    }

    public static @Nullable Item getEquipmentItemFromGroupNames(List<String> groupNames) {
        ItemRegistry ir = itemRegistry;
        if (ir != null) {
            List<Item> groupItems = new ArrayList<>();
            for (String groupName : groupNames) {
                try {
                    Item group = ir.getItem(groupName);
                    // if group is an equipment, return it (first equipment found)
                    if (isEquipment(group)) {
                        return group;
                    }
                    groupItems.add(group);
                } catch (ItemNotFoundException e) {
                    // should not happen
                }
            }
            // if no equipment is found, iterate the groups of each group
            for (Item group : groupItems) {
                Item equipmentItem = getEquipmentItemFromGroupNames(group.getGroupNames());
                if (equipmentItem != null) {
                    return equipmentItem;
                }
            }
        }
        return null;
    }
}