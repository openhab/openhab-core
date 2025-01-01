/**
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
package org.openhab.core.semantics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.types.StateDescription;

/**
 * This is a class that gives static access to the semantic tag library.
 * For everything that is not static, the {@link SemanticsService} should be used instead.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jimmy Tanagra - Add the ability to add new tags at runtime
 * @author Laurent Garnier - Several methods moved into class SemanticsService or SemanticTagRegistry
 */
@NonNullByDefault
public class SemanticTags {

    private static final Map<String, Class<? extends Tag>> TAGS = Collections.synchronizedMap(new TreeMap<>());

    static {
        addTagSet("Location", Location.class);
        addTagSet("Equipment", Equipment.class);
        addTagSet("Point", Point.class);
        addTagSet("Property", Property.class);
    }

    /**
     * Retrieves the class for a given id.
     *
     * @param tagId the id of the tag. The id can be fully qualified (e.g. "Location_Room_Bedroom") or a segment, if
     *            this uniquely identifies the tag
     *            (e.g. "Bedroom").
     * @return the class for the id or null, if non exists.
     */
    public static @Nullable Class<? extends Tag> getById(String tagId) {
        return TAGS.get(tagId);
    }

    /**
     * Determines the semantic type of an {@link Item} i.e. a sub-type of {@link Location}, {@link Equipment} or
     * {@link Point}.
     *
     * @param item the Item to get the semantic type for
     * @return a sub-type of Location, Equipment or Point
     */
    public static @Nullable Class<? extends Tag> getSemanticType(Item item) {
        for (String tag : item.getTags()) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && !Property.class.isAssignableFrom(type)) {
                return type;
            }
        }
        // we haven't found any type as a tag, but if there is a Property tag, we can conclude that it is a Point
        if (getProperty(item) != null) {
            StateDescription stateDescription = item.getStateDescription();
            if (stateDescription != null && stateDescription.isReadOnly()) {
                return getById("Point_Measurement");
            } else {
                return getById("Point_Control");
            }
        } else {
            return null;
        }
    }

    /**
     * Determines the {@link Property} type that a {@link Point} relates to.
     *
     * @param item the Item to get the property for
     * @return a sub-type of Property if the Item represents a Point, otherwise null
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Class<? extends Property> getProperty(Item item) {
        for (String tag : item.getTags()) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && Property.class.isAssignableFrom(type)) {
                return (Class<? extends Property>) type;
            }
        }
        return null;
    }

    /**
     * Determines the semantic {@link Point} type of an {@link Item}.
     *
     * @param item the Item to get the Point for
     * @return a sub-type of a {@link Point}if the Item represents a Point, otherwise null
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Class<? extends Point> getPoint(Item item) {
        Set<String> tags = item.getTags();
        for (String tag : tags) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && Point.class.isAssignableFrom(type)) {
                return (Class<? extends Point>) type;
            }
        }
        return null;
    }

    /**
     * Determines the semantic {@link Equipment} type of an {@link Item}.
     *
     * @param item the Item to get the Equipment for
     * @return a sub-type of {@link Equipment} if the Item represents an Equipment, otherwise null
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Class<? extends Equipment> getEquipment(Item item) {
        for (String tag : item.getTags()) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && Equipment.class.isAssignableFrom(type)) {
                return (Class<? extends Equipment>) type;
            }
        }
        return null;
    }

    /**
     * Determines the semantic {@link Location} type of an {@link Item}.
     *
     * @param item the item to get the location for
     * @return a sub-type of {@link Location} if the item represents a location, otherwise null
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Class<? extends Location> getLocation(Item item) {
        for (String tag : item.getTags()) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && Location.class.isAssignableFrom(type)) {
                return (Class<? extends Location>) type;
            }
        }
        return null;
    }

    public static void addTagSet(String id, Class<? extends Tag> tagSet) {
        TAGS.put(id, tagSet);
    }

    public static void removeTagSet(String id, Class<? extends Tag> tagSet) {
        TAGS.remove(id, tagSet);
    }
}
