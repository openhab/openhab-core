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
package org.eclipse.smarthome.core.semantics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.semantics.model.Property;
import org.eclipse.smarthome.core.semantics.model.Tag;
import org.eclipse.smarthome.core.semantics.model.TagInfo;
import org.eclipse.smarthome.core.semantics.model.equipment.Equipments;
import org.eclipse.smarthome.core.semantics.model.location.Locations;
import org.eclipse.smarthome.core.semantics.model.point.Control;
import org.eclipse.smarthome.core.semantics.model.point.Measurement;
import org.eclipse.smarthome.core.semantics.model.point.Points;
import org.eclipse.smarthome.core.semantics.model.property.Properties;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * This is a class that gives static access to the semantic tag library.
 * For everything that is not static, the {@link SemanticsService} should be used instead.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
public class SemanticTags {

    private static String TAGS_BUNDLE_NAME = "tags";

    private static final Map<String, Class<? extends Tag>> TAGS = new TreeMap<>();

    static {
        Locations.stream().forEach(location -> addTagSet(location));
        Equipments.stream().forEach(equipment -> addTagSet(equipment));
        Points.stream().forEach(point -> addTagSet(point));
        Properties.stream().forEach(property -> addTagSet(property));
    }

    /**
     * Retrieves the class for a given id.
     *
     * @param tagId the id of the tag. The id can be fully qualified (e.g. "Location_Room_Bedroom") or a segment, if
     *            this uniquely identifies the tag
     *            (e.g. "Bedroom").
     * @return the class for the id or null, if non exists.
     */
    @Nullable
    public static Class<? extends Tag> getById(String tagId) {
        return TAGS.get(tagId);
    }

    @Nullable
    public static Class<? extends Tag> getByLabel(String tagLabel, Locale locale) {
        return TAGS.values().stream().distinct().filter(t -> getLabel(t, locale).equalsIgnoreCase(tagLabel)).findFirst()
                .orElse(null);
    }

    public static List<Class<? extends @NonNull Tag>> getByLabelOrSynonym(String tagLabelOrSynonym, Locale locale) {
        return TAGS.values().stream().distinct()
                .filter(t -> getLabelAndSynonyms(t, locale).contains(tagLabelOrSynonym.toLowerCase(locale)))
                .collect(Collectors.toList());
    }

    public static List<String> getLabelAndSynonyms(Class<? extends Tag> tag, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale);
        try {
            String entry = rb.getString(tag.getAnnotation(TagInfo.class).id());
            return Arrays.asList(entry.toLowerCase(locale).split(","));
        } catch (MissingResourceException e) {
            return Collections.singletonList(tag.getAnnotation(TagInfo.class).label());
        }
    }

    public static String getLabel(Class<? extends Tag> tag, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale);
        try {
            String entry = rb.getString(tag.getAnnotation(TagInfo.class).id());
            if (entry.contains(",")) {
                return entry.substring(0, entry.indexOf(","));
            } else {
                return entry;
            }
        } catch (MissingResourceException e) {
            return tag.getAnnotation(TagInfo.class).label();
        }
    }

    /**
     * Determines the semantic entity type of an item, i.e. a sub-type of Location, Equipment or Point.
     *
     * @param item the item to get the semantic type for
     * @return a sub-type of Location, Equipment or Point
     */
    @Nullable
    public static Class<? extends Tag> getSemanticType(Item item) {
        Set<String> tags = item.getTags();
        for (String tag : tags) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && !Property.class.isAssignableFrom(type)) {
                return type;
            }
        }
        // we haven't found any type as a tag, but if there is a Property tag, we can conclude that it is a Point
        if (getProperty(item) != null) {
            StateDescription stateDescription = item.getStateDescription();
            if (stateDescription != null && stateDescription.isReadOnly()) {
                return Measurement.class;
            } else {
                return Control.class;
            }
        } else {
            return null;
        }
    }

    /**
     * Determines the Property that a Point relates to.
     *
     * @param item the item to get the property for
     * @return a sub-type of Property if the item represents a Point, otherwise null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Class<? extends Property> getProperty(Item item) {
        Set<String> tags = item.getTags();
        for (String tag : tags) {
            Class<? extends Tag> type = getById(tag);
            if (type != null && Property.class.isAssignableFrom(type)) {
                return (Class<? extends Property>) type;
            }
        }
        return null;
    }

    private static void addTagSet(Class<? extends Tag> tagSet) {
        String id = tagSet.getAnnotation(TagInfo.class).id();
        while (id.indexOf("_") != -1) {
            TAGS.put(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        TAGS.put(id, tagSet);
    }
}
