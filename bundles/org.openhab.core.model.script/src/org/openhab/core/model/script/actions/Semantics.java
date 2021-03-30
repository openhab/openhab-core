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
package org.openhab.core.model.script.actions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.model.script.internal.engine.action.SemanticsActionService;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;

/**
 * The static methods of this class are made available as functions in the scripts. This allows a script to use
 * Semantics features.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class Semantics {

    /**
     * Checks if the given {@link Item} is a {@link Location}.
     *
     * @param item the Item to check
     * @return return true, if the given Item is a Location, false otherwise
     */
    @ActionDoc(text = "checks if the given Item is is a Location")
    public static boolean isLocation(Item item) {
        return SemanticsActionService.isLocation(item);
    }

    /**
     * Checks if the given {@link Item} is a {@link Equipment}.
     *
     * @param item the Item to check
     * @return return true, if the given Item is an Equipment, false otherwise
     */
    @ActionDoc(text = "checks if the given Item is is an Equipment")
    public static boolean isEquipment(Item item) {
        return SemanticsActionService.isEquipment(item);
    }

    /**
     * Checks if the given {@link Item} is a {@link Point}.
     *
     * @param item the Item to check
     * @return return true, if the given Item is a Point, false otherwise
     */
    @ActionDoc(text = "checks if the given Item is is a Point")
    public static boolean isPoint(Item item) {
        return SemanticsActionService.isPoint(item);
    }

    /**
     * Gets the related {@link Location} Item of an {@link Item}.
     *
     * @param item the Item to determine the Location for
     * @return the related Location Item of the Item or null
     */
    @ActionDoc(text = "gets the Location Item of the Item")
    public static @Nullable Item getLocation(Item item) {
        if (isLocation(item)) {
            // if item is a location, return itself
            return item;
        } else {
            // if item is not a location, iterate its groups and try to determine a location from them
            return SemanticsActionService.getLocationItemFromGroupNames(item.getGroupNames());
        }
    }

    /**
     * Gets the related {@link Location} type of an {@link Item}.
     *
     * @param item the Item to determine the Location for
     * @return the related Location type of the Item or null
     */
    @ActionDoc(text = "gets the Location type of the Item")
    public static @Nullable Class<? extends Location> getLocationType(Item item) {
        Item locationItem = getLocation(item);
        return locationItem != null ? SemanticTags.getLocation(locationItem) : null;
    }

    /**
     * Gets the related {@link Equipment} Item an {@link Item} belongs to.
     *
     * @param item the Item to retrieve the Equipment Item for
     * @return the related Equipment Item the Item belongs to or null
     */
    @ActionDoc(text = "gets the Equipment Item an Item belongs to")
    public static @Nullable Item getEquipment(Item item) {
        if (isEquipment(item)) {
            // if item is an equipment return its semantics equipment class
            return item;
        } else {
            // if item is not an equipment, iterate its groups and try to determine a equipment there
            return SemanticsActionService.getEquipmentItemFromGroupNames(item.getGroupNames());
        }
    }

    /**
     * Gets the {@link Equipment} type an {@link Item} relates to.
     *
     * @param item the Item to retrieve the Equipment for
     * @return the Equipment the Item relates to or null
     */
    @ActionDoc(text = "gets the Equipment type an Item belongs to")
    public static @Nullable Class<? extends Equipment> getEquipmentType(Item item) {
        Item equipmentItem = getEquipment(item);
        return equipmentItem != null ? SemanticTags.getEquipment(equipmentItem) : null;

    }

    /**
     * Gets the {@link Point} type an {@link Item}.
     *
     * @param item the Item to determine the Point for
     * @return the Point type of the Item or null
     */
    @ActionDoc(text = "gets the Point type of an Item")
    public static @Nullable Class<? extends Point> getPointType(Item item) {
        return isPoint(item) ? SemanticTags.getPoint(item) : null;
    }

    /**
     * Gets the {@link Property} type an {@link Item} relates to.
     *
     * @param item the Item to retrieve the Property for
     * @return the Property type the Item relates to or null
     */
    @ActionDoc(text = "gets the Property type an Item relates to")
    public static @Nullable Class<? extends Property> getPropertyType(Item item) {
        return isPoint(item) ? SemanticTags.getProperty(item) : null;
    }

    /**
     * Determines the semantic type of an {@link Item} (i.e. a sub-type of {@link Location}, {@link Equipment} or
     * {@link Point}).
     *
     * @param item the Item to get the semantic type for
     * @return a sub-type of Location, Equipment or Point
     */
    @ActionDoc(text = "gets the semantic type of an Item")
    public static @Nullable Class<? extends Tag> getSemanticType(Item item) {
        return SemanticTags.getSemanticType(item);
    }
}
