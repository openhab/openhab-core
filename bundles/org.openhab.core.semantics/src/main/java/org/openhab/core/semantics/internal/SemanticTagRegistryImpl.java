/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.semantics.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagProvider;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main implementing class of the {@link SemanticTagRegistry} interface. It
 * keeps track of all declared semantic tags of all semantic tags providers and keeps
 * their current state in memory.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class SemanticTagRegistryImpl extends AbstractRegistry<SemanticTag, String, SemanticTagProvider>
        implements SemanticTagRegistry {

    private static final SemanticClassLoader CLASS_LOADER = new SemanticClassLoader();

    private final Logger logger = LoggerFactory.getLogger(SemanticTagRegistryImpl.class);

    private final DefaultSemanticTagProvider defaultSemanticTagProvider;
    private final ManagedSemanticTagProvider managedProvider;

    @Activate
    public SemanticTagRegistryImpl(@Reference DefaultSemanticTagProvider defaultSemanticTagProvider,
            @Reference ManagedSemanticTagProvider managedProvider) {
        super(SemanticTagProvider.class);
        this.defaultSemanticTagProvider = defaultSemanticTagProvider;
        this.managedProvider = managedProvider;
        // Add the default semantic tags provider first, before all others
        super.addProvider(defaultSemanticTagProvider);
        super.addProvider(managedProvider);
        setManagedProvider(managedProvider);
    }

    @Override
    protected void addProvider(Provider<SemanticTag> provider) {
        // Ignore the default semantic tags provider and the managed provider (they are added in the constructor)
        if (!provider.equals(defaultSemanticTagProvider) && !provider.equals(managedProvider)) {
            logger.trace("addProvider {} => calling super.addProvider", provider.getClass().getSimpleName());
            super.addProvider(provider);
        } else {
            logger.trace("addProvider {} => ignoring it", provider.getClass().getSimpleName());
        }
    }

    @Override
    public @Nullable Class<? extends Tag> getTagClassById(String tagId) {
        return SemanticTags.getById(tagId);
    }

    /**
     * Builds the fully qualified id for a semantic tag class.
     *
     * @param tag the semantic tag class
     * @return the fully qualified id
     */
    public static String buildId(Class<? extends Tag> tag) {
        return buildId("", tag);
    }

    private static String buildId(String relativeId, Class<?> tag) {
        if (!Location.class.isAssignableFrom(tag) && !Equipment.class.isAssignableFrom(tag)
                && !Point.class.isAssignableFrom(tag) && !Property.class.isAssignableFrom(tag)) {
            return relativeId;
        }
        String id = tag.getSimpleName();
        if (!relativeId.isEmpty()) {
            id += "_" + relativeId;
        }
        return buildId(id, tag.getInterfaces()[0]);
    }

    @Override
    public boolean canBeAdded(SemanticTag tag) {
        String id = tag.getUID();
        // check that a tag with this id does not already exist in the registry
        if (get(id) != null) {
            return false;
        }
        // Extract the tag name and the parent tag
        int lastSeparator = id.lastIndexOf("_");
        if (lastSeparator <= 0) {
            return false;
        }
        String name = id.substring(lastSeparator + 1);
        String parentId = id.substring(0, lastSeparator);
        SemanticTag parent = get(parentId);
        // Check that the tag name has a valid syntax and the parent tag already exists
        // and is either a default tag or a managed tag
        // Check also that a semantic tag class with the same name does not already exist
        return name.matches("[A-Z][a-zA-Z0-9]+") && parent != null
                && (managedProvider.get(parentId) != null || defaultSemanticTagProvider.getAll().contains(parent))
                && getTagClassById(name) == null;
    }

    @Override
    public List<SemanticTag> getSubTree(SemanticTag tag) {
        List<String> ids = getAll().stream().map(Identifiable::getUID).filter(uid -> uid.startsWith(tag.getUID() + "_"))
                .toList();
        List<SemanticTag> tags = new ArrayList<>();
        tags.add(tag);
        ids.forEach(id -> {
            SemanticTag t = get(id);
            if (t != null) {
                tags.add(t);
            }
        });
        return tags;
    }

    @Override
    public boolean isEditable(SemanticTag tag) {
        return managedProvider.get(tag.getUID()) != null;
    }

    @Override
    public void removeSubTree(SemanticTag tag) {
        // Get tags id in reverse order
        List<String> ids = getSubTree(tag).stream().filter(this::isEditable).map(SemanticTag::getUID)
                .sorted(Comparator.reverseOrder()).toList();
        ids.forEach(managedProvider::remove);
    }

    @Override
    protected void onAddElement(SemanticTag tag) throws IllegalArgumentException {
        logger.trace("onAddElement {}", tag.getUID());
        super.onAddElement(tag);

        String uid = tag.getUID();
        Class<? extends Tag> tagClass = getTagClassById(uid);
        if (tagClass != null) {
            // Class already exists
            return;
        }

        String type;
        String className;
        Class<? extends Tag> newTag;
        int lastSeparator = uid.lastIndexOf("_");
        if (lastSeparator < 0) {
            switch (uid) {
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
                    throw new IllegalArgumentException("Failed to create semantic tag '" + uid
                            + "': only Equipment, Location, Point and Property are allowed as root tags.");
            }
            type = uid;
            className = newTag.getName();
        } else {
            String name = uid.substring(lastSeparator + 1);
            String parentId = uid.substring(0, lastSeparator);
            Class<? extends Tag> parentTagClass = getTagClassById(parentId);
            if (parentTagClass == null) {
                throw new IllegalArgumentException(
                        "Failed to create semantic tag '" + uid + "': no existing parent tag with id " + parentId);
            }
            if (!name.matches("[A-Z][a-zA-Z0-9]+")) {
                throw new IllegalArgumentException("Failed to create semantic tag '" + uid
                        + "': tag name must start with a capital letter and contain only alphanumerics.");
            }
            tagClass = getTagClassById(name);
            if (tagClass != null) {
                throw new IllegalArgumentException(
                        "Failed to create semantic tag '" + uid + "': tag '" + buildId(tagClass) + "' already exists.");
            }

            type = parentId.split("_")[0];
            className = "org.openhab.core.semantics.model." + type.toLowerCase() + "." + name;
            try {
                newTag = (Class<? extends Tag>) Class.forName(className, false, CLASS_LOADER);
                logger.debug("'{}' semantic {} tag already exists.", className, type);
            } catch (ClassNotFoundException e) {
                // Create the tag interface
                ClassWriter classWriter = new ClassWriter(0);
                classWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                        className.replace('.', '/'), null, "java/lang/Object",
                        new String[] { parentTagClass.getName().replace('.', '/') });
                classWriter.visitSource("Status.java", null);
                classWriter.visitEnd();
                byte[] byteCode = classWriter.toByteArray();
                try {
                    newTag = (Class<? extends Tag>) CLASS_LOADER.defineClass(className, byteCode);
                    logger.debug("'{}' semantic {} tag created.", className, type);
                } catch (Exception ex) {
                    logger.warn("Failed to create semantic tag '{}': {}", className, ex.getMessage());
                    throw new IllegalArgumentException("Failed to create semantic tag '" + className + "'", ex);
                }
            }
        }

        addTagSet(uid, newTag);
        logger.debug("'{}' semantic {} tag added.", className, type);
    }

    @Override
    protected void onRemoveElement(SemanticTag tag) {
        logger.trace("onRemoveElement {}", tag.getUID());
        super.onRemoveElement(tag);
        removeTagSet(tag.getUID());
    }

    private void addTagSet(String tagId, Class<? extends Tag> tagSet) {
        logger.trace("addTagSet {}", tagId);
        String id = tagId;
        while (id.contains("_")) {
            SemanticTags.addTagSet(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        SemanticTags.addTagSet(id, tagSet);
    }

    private void removeTagSet(String tagId) {
        logger.trace("removeTagSet {}", tagId);
        Class<? extends Tag> tagSet = getTagClassById(tagId);
        if (tagSet == null) {
            return;
        }
        String id = tagId;
        while (id.contains("_")) {
            SemanticTags.removeTagSet(id, tagSet);
            id = id.substring(id.indexOf("_") + 1);
        }
        SemanticTags.removeTagSet(id, tagSet);
    }

    private static class SemanticClassLoader extends ClassLoader {
        public SemanticClassLoader() {
            super(SemanticTagRegistryImpl.class.getClassLoader());
        }

        public Class<?> defineClass(String className, byte[] byteCode) {
            // defineClass is protected in the normal ClassLoader
            return defineClass(className, byteCode, 0, byteCode.length);
        }
    }
}
