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
package org.openhab.core.semantics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openhab.core.items.Item;
import org.openhab.core.semantics.model.equipment.Equipments;
import org.openhab.core.semantics.model.location.Locations;
import org.openhab.core.semantics.model.point.Measurement;
import org.openhab.core.semantics.model.point.Points;
import org.openhab.core.semantics.model.property.Properties;
import org.openhab.core.types.StateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class that gives static access to the semantic tag library.
 * For everything that is not static, the {@link SemanticsService} should be used instead.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jimmy Tanagra - Add the ability to add new tags at runtime
 * @author Laurent Garnier - Add the ability to remove tags at runtime
 * @author Laurent Garnier - Several methods moved into class SemanticsService
 */
@NonNullByDefault
public class SemanticTags {

    private static final Map<String, Class<? extends Tag>> TAGS = Collections.synchronizedMap(new TreeMap<>());

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticTags.class);
    private static final SemanticClassLoader CLASS_LOADER = new SemanticClassLoader();

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
                return Measurement.class;
            } else {
                return org.openhab.core.semantics.model.point.Control.class;
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

    /**
     * Adds a new semantic tag with inferred label, empty synonyms and description.
     *
     * The label will be inferred from the tag name by splitting the CamelCase with a space.
     *
     * @param name the tag name to add
     * @param parent the parent tag that the new tag should belong to
     * @return the created semantic tag class, or null if it was already added.
     */
    public static @Nullable Class<? extends Tag> add(String name, String parent) {
        Class<? extends Tag> parentClass = getById(parent);
        if (parentClass == null) {
            LOGGER.warn("Adding semantic tag '{}' failed because parent tag '{}' is not found.", name, parent);
            return null;
        }
        return add(name, parentClass);
    }

    /**
     * Adds a new semantic tag.
     *
     * @param name the tag name to add
     * @param parent the parent tag that the new tag should belong to
     * @return the created semantic tag class, or null if it was already added.
     */
    public static @Nullable Class<? extends Tag> add(String name, @Nullable Class<? extends Tag> parent) {
        String parentId = parent == null ? "" : parent.getAnnotation(TagInfo.class).id();
        LOGGER.trace("Semantics add name \"{}\" parent id {}", name, parentId);
        if (getById(name) != null) {
            return null;
        }

        if (!name.matches("[A-Z][a-zA-Z0-9]+")) {
            throw new IllegalArgumentException(
                    "The tag name '" + name + "' must start with a capital letter and contain only alphanumerics.");
        }

        String type;
        String className;
        Class<? extends Tag> newTag;
        if (parent == null) {
            switch (name) {
                case "Equipment":
                    newTag = Equipment.class;
                    break;
                case "Location":
                    newTag = Location.class;
                    break;
                case "Point":
                    newTag = Point.class;
                    break;
                case "Property":
                    newTag = Property.class;
                    break;
                default:
                    LOGGER.warn(
                            "Failed creating root semantic tag '{}': only Equipment, Location, Point and Property are allowed",
                            name);
                    return null;
            }
            type = name;
            className = newTag.getClass().getName();
        } else {
            type = parentId.split("_")[0];
            className = "org.openhab.core.semantics.model." + type.toLowerCase() + "." + name;
            try {
                newTag = (Class<? extends Tag>) Class.forName(className, false, CLASS_LOADER);
                LOGGER.trace("Class '{}' exists", className);
            } catch (ClassNotFoundException e) {
                newTag = null;
            }

            if (newTag == null) {
                // Create the tag interface
                ClassWriter classWriter = new ClassWriter(0);
                classWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                        className.replace('.', '/'), null, "java/lang/Object",
                        new String[] { parent.getName().replace('.', '/') });

                // Add TagInfo Annotation
                classWriter.visitSource("Status.java", null);

                AnnotationVisitor annotation = classWriter.visitAnnotation("Lorg/openhab/core/semantics/TagInfo;",
                        true);
                annotation.visit("id", parentId + "_" + name);
                annotation.visitEnd();

                classWriter.visitEnd();
                byte[] byteCode = classWriter.toByteArray();
                try {
                    newTag = (Class<? extends Tag>) CLASS_LOADER.defineClass(className, byteCode);
                } catch (Exception e) {
                    LOGGER.warn("Failed creating a new semantic tag '{}': {}", className, e.getMessage());
                    return null;
                }
            }
        }

        addToModel(newTag);
        addTagSet(newTag);
        LOGGER.debug("'{}' semantic {} tag added.", className, type);
        return newTag;
    }

    public static void remove(Class<? extends Tag> tag) {
        removeTagSet(tag);
        removeFromModel(tag);
        LOGGER.debug("'{}' semantic tag removed.", tag.getName());
    }

    private static void addTagSet(Class<? extends Tag> tagSet) {
        LOGGER.trace("addTagSet {}", tagSet.getAnnotation(TagInfo.class).id());
        String id = tagSet.getAnnotation(TagInfo.class).id();
        while (id.indexOf("_") != -1) {
            TAGS.put(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        TAGS.put(id, tagSet);
    }

    private static void removeTagSet(Class<? extends Tag> tagSet) {
        LOGGER.trace("removeTagSet {}", tagSet.getAnnotation(TagInfo.class).id());
        String id = tagSet.getAnnotation(TagInfo.class).id();
        while (id.indexOf("_") != -1) {
            TAGS.remove(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        TAGS.remove(id, tagSet);
    }

    private static boolean addToModel(Class<? extends Tag> tag) {
        if (Location.class.isAssignableFrom(tag)) {
            return Locations.add((Class<? extends Location>) tag);
        } else if (Equipment.class.isAssignableFrom(tag)) {
            return Equipments.add((Class<? extends Equipment>) tag);
        } else if (Point.class.isAssignableFrom(tag)) {
            return Points.add((Class<? extends Point>) tag);
        } else if (Property.class.isAssignableFrom(tag)) {
            return Properties.add((Class<? extends Property>) tag);
        }
        throw new IllegalArgumentException("Unknown type of tag " + tag);
    }

    private static boolean removeFromModel(Class<? extends Tag> tag) {
        if (Location.class.isAssignableFrom(tag)) {
            return Locations.remove((Class<? extends Location>) tag);
        } else if (Equipment.class.isAssignableFrom(tag)) {
            return Equipments.remove((Class<? extends Equipment>) tag);
        } else if (Point.class.isAssignableFrom(tag)) {
            return Points.remove((Class<? extends Point>) tag);
        } else if (Property.class.isAssignableFrom(tag)) {
            return Properties.remove((Class<? extends Property>) tag);
        }
        throw new IllegalArgumentException("Unknown type of tag " + tag);
    }

    private static class SemanticClassLoader extends ClassLoader {
        public SemanticClassLoader() {
            super(SemanticTags.class.getClassLoader());
        }

        public Class<?> defineClass(String className, byte[] byteCode) {
            // defineClass is protected in the normal ClassLoader
            return defineClass(className, byteCode, 0, byteCode.length);
        }
    }
}
