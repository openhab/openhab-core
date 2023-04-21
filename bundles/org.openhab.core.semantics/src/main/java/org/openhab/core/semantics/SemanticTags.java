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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 */
@NonNullByDefault
public class SemanticTags {

    private static final String TAGS_BUNDLE_NAME = "tags";

    private static final Map<String, Class<? extends Tag>> TAGS = new TreeMap<>();

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

    public static @Nullable Class<? extends Tag> getByLabel(String tagLabel, Locale locale) {
        Optional<Class<? extends Tag>> tag = TAGS.values().stream().distinct()
                .filter(t -> getLabel(t, locale).equalsIgnoreCase(tagLabel)).findFirst();
        return tag.isPresent() ? tag.get() : null;
    }

    public static List<Class<? extends Tag>> getByLabelOrSynonym(String tagLabelOrSynonym, Locale locale) {
        return TAGS.values().stream().distinct()
                .filter(t -> getLabelAndSynonyms(t, locale).contains(tagLabelOrSynonym.toLowerCase(locale)))
                .collect(Collectors.toList());
    }

    public static List<String> getLabelAndSynonyms(Class<? extends Tag> tag, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale,
                Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
        TagInfo tagInfo = tag.getAnnotation(TagInfo.class);
        try {
            String entry = rb.getString(tagInfo.id());
            return List.of(entry.toLowerCase(locale).split(","));
        } catch (MissingResourceException e) {
            Stream<String> label = Stream.of(tagInfo.label());
            Stream<String> synonyms = Stream.of(tagInfo.synonyms().split(",")).map(String::trim);
            return Stream.concat(label, synonyms).map(s -> s.toLowerCase(locale)).distinct().toList();
        }
    }

    public static String getLabel(Class<? extends Tag> tag, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale,
                Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
        TagInfo tagInfo = tag.getAnnotation(TagInfo.class);
        try {
            String entry = rb.getString(tagInfo.id());
            if (entry.contains(",")) {
                return entry.substring(0, entry.indexOf(","));
            } else {
                return entry;
            }
        } catch (MissingResourceException e) {
            return tagInfo.label();
        }
    }

    public static List<String> getSynonyms(Class<? extends Tag> tag, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale,
                Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
        String synonyms = "";
        TagInfo tagInfo = tag.getAnnotation(TagInfo.class);
        try {
            String entry = rb.getString(tagInfo.id());
            int start = entry.indexOf(",") + 1;
            if (start > 0 && entry.length() > start) {
                synonyms = entry.substring(start);
            }
        } catch (MissingResourceException e) {
            synonyms = tagInfo.synonyms();
        }
        return Stream.of(synonyms.split(",")).map(String::trim).toList();
    }

    public static String getDescription(Class<? extends Tag> tag, Locale locale) {
        return tag.getAnnotation(TagInfo.class).description();
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
        return add(name, parent, null, null, null);
    }

    /**
     * Adds a new semantic tag.
     * 
     * @param name the tag name to add
     * @param parent the parent tag that the new tag should belong to
     * @param label an optional label. When null, the label will be inferred from the tag name,
     *            splitting the CamelCase with a space.
     * @param synonyms a comma separated list of synonyms
     * @param description the tag description
     * @return the created semantic tag class, or null if it was already added.
     */
    public static @Nullable Class<? extends Tag> add(String name, String parent, @Nullable String label,
            @Nullable String synonyms, @Nullable String description) {
        Class<? extends Tag> parentClass = getById(parent);
        if (parentClass == null) {
            LOGGER.warn("Adding semantic tag '{}' failed because parent tag '{}' is not found.", name, parent);
            return null;
        }
        return add(name, parentClass, label, synonyms, description);
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
    public static @Nullable Class<? extends Tag> add(String name, Class<? extends Tag> parent) {
        return add(name, parent, null, null, null);
    }

    /**
     * Adds a new semantic tag.
     * 
     * @param name the tag name to add
     * @param parent the parent tag that the new tag should belong to
     * @param label an optional label. When null, the label will be inferred from the tag name,
     *            splitting the CamelCase with a space.
     * @param synonyms a comma separated list of synonyms
     * @param description the tag description
     * @return the created semantic tag class, or null if it was already added.
     */
    public static @Nullable Class<? extends Tag> add(String name, Class<? extends Tag> parent, @Nullable String label,
            @Nullable String synonyms, @Nullable String description) {
        if (getById(name) != null) {
            return null;
        }

        if (!name.matches("[A-Z][a-zA-Z0-9]+")) {
            throw new IllegalArgumentException(
                    "The tag name '" + name + "' must start with a capital letter and contain only alphanumerics.");
        }

        String parentId = parent.getAnnotation(TagInfo.class).id();
        String type = parentId.split("_")[0];
        String className = "org.openhab.core.semantics.model." + type.toLowerCase() + "." + name;

        // Infer label from name, splitting up CamelCaseALL99 -> Camel Case ALL 99
        label = Optional.ofNullable(label).orElseGet(() -> name.replaceAll("([A-Z][a-z]+|[A-Z][A-Z]+|[0-9]+)", " $1"))
                .trim();
        synonyms = Optional.ofNullable(synonyms).orElse("").replaceAll("\\s*,\\s*", ",").trim();

        // Create the tag interface
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                className.replace('.', '/'), null, "java/lang/Object",
                new String[] { parent.getName().replace('.', '/') });

        // Add TagInfo Annotation
        classWriter.visitSource("Status.java", null);

        AnnotationVisitor annotation = classWriter.visitAnnotation("Lorg/openhab/core/semantics/TagInfo;", true);
        annotation.visit("id", parentId + "_" + name);
        annotation.visit("label", label);
        annotation.visit("synonyms", synonyms);
        annotation.visit("description", Optional.ofNullable(description).orElse("").trim());
        annotation.visitEnd();

        classWriter.visitEnd();
        byte[] byteCode = classWriter.toByteArray();
        Class newTag = null;
        try {
            newTag = CLASS_LOADER.defineClass(className, byteCode);
        } catch (Exception e) {
            LOGGER.warn("Failed creating a new semantic tag '{}': {}", className, e.getMessage());
            return null;
        }
        addToModel(newTag);
        addTagSet(newTag);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("'{}' semantic {} tag added.", className, type);
        }
        return newTag;
    }

    private static void addTagSet(Class<? extends Tag> tagSet) {
        String id = tagSet.getAnnotation(TagInfo.class).id();
        while (id.indexOf("_") != -1) {
            TAGS.put(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        TAGS.put(id, tagSet);
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
