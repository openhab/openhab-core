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
package org.openhab.core.semantics.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.model.Equipment;
import org.openhab.core.semantics.model.Location;
import org.openhab.core.semantics.model.Point;
import org.openhab.core.semantics.model.Property;
import org.openhab.core.semantics.model.Tag;
import org.openhab.core.semantics.model.TagInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This {@link MetadataProvider} collects semantic information about items and provides them as metadata under the
 * "semantics" namespace.
 *
 * The main value of the metadata holds the semantic type of the item, i.e. a sub-class of Location, Equipment or Point.
 * The metadata configuration contains the information about the relations with the key being the name of the relation
 * (e.g. "hasLocation") and the value being the id of the referenced entity (e.g. its item name).
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = MetadataProvider.class)
@NonNullByDefault
public class SemanticsMetadataProvider extends AbstractProvider<Metadata>
        implements ItemRegistryChangeListener, MetadataProvider {

    // the namespace to use for the metadata
    public static final String NAMESPACE = "semantics";

    // holds the static definition of the relations between entities
    private final Map<List<Class<? extends Tag>>, String> parentRelations = new HashMap<>();
    private final Map<List<Class<? extends Tag>>, String> memberRelations = new HashMap<>();
    private final Map<List<Class<? extends Tag>>, String> propertyRelations = new HashMap<>();

    // local cache of the created metadata as a map from itemName->Metadata
    private final Map<String, Metadata> semantics = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo(s2);
        }
    });

    private final ItemRegistry itemRegistry;

    @Activate
    public SemanticsMetadataProvider(final @Reference ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Activate
    protected void activate() {
        initRelations();
        for (Item item : itemRegistry.getAll()) {
            processItem(item);
        }
        itemRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        semantics.clear();
    }

    @Override
    public Collection<Metadata> getAll() {
        return semantics.values();
    }

    /**
     * Updates the semantic metadata for an item and notifies all listeners about changes
     *
     * @param item the item to update the metadata for
     */
    private void processItem(Item item) {
        MetadataKey key = new MetadataKey(NAMESPACE, item.getName());
        Map<String, Object> configuration = new HashMap<>();
        Class<? extends Tag> type = SemanticTags.getSemanticType(item);
        if (type != null) {
            processProperties(item, configuration);
            processHierarchy(item, configuration);
            Metadata md = new Metadata(key, type.getAnnotation(TagInfo.class).id(), configuration);
            Metadata oldMd = semantics.put(item.getName(), md);
            if (oldMd == null) {
                notifyListenersAboutAddedElement(md);
            } else {
                notifyListenersAboutUpdatedElement(oldMd, md);
            }
        } else {
            Metadata removedMd = semantics.remove(item.getName());
            if (removedMd != null) {
                notifyListenersAboutRemovedElement(removedMd);
            }
        }
    }

    /**
     * Processes Property tags on items and if found, adds it to the metadata configuration.
     *
     * @param item the item to process
     * @param configuration the metadata configuration that should be amended
     */
    private void processProperties(Item item, Map<String, Object> configuration) {
        Class<? extends Tag> type = SemanticTags.getSemanticType(item);
        if (type == null) {
            return;
        }
        for (Entry<List<Class<? extends Tag>>, String> relation : propertyRelations.entrySet()) {
            Class<? extends Tag> entityClass = relation.getKey().get(0);
            if (entityClass.isAssignableFrom(type)) {
                Class<? extends Property> p = SemanticTags.getProperty(item);
                if (p != null) {
                    configuration.put(relation.getValue(), p.getAnnotation(TagInfo.class).id());
                }
            }
        }
    }

    /**
     * Retrieves semantic information from parent or member items.
     *
     * @param item the item to gather the semantic metadata for
     * @param configuration the metadata configuration that should be amended
     */
    private void processHierarchy(Item item, Map<String, Object> configuration) {
        Class<? extends Tag> type = SemanticTags.getSemanticType(item);
        if (type != null) {
            for (String parent : item.getGroupNames()) {
                Item parentItem = itemRegistry.get(parent);
                if (parentItem != null) {
                    processParent(type, parentItem, configuration);
                }
            }
            if (item instanceof GroupItem) {
                GroupItem gItem = (GroupItem) item;
                for (Item memberItem : gItem.getMembers()) {
                    processMember(type, memberItem, configuration);
                }
            }
        }
    }

    /**
     * Retrieves semantic information from a parent items.
     *
     * @param type the semantic type of the item for which the semantic information is gathered
     * @param parentItem the parent item to process
     * @param configuration the metadata configuration that should be amended
     */
    private void processParent(Class<? extends Tag> type, Item parentItem, Map<String, Object> configuration) {
        Class<? extends Tag> typeParent = SemanticTags.getSemanticType(parentItem);
        if (typeParent == null) {
            return;
        }
        for (Entry<List<Class<? extends Tag>>, String> relation : parentRelations.entrySet()) {
            List<Class<? extends Tag>> relClasses = relation.getKey();
            Class<? extends Tag> entityClass = relClasses.get(0);
            Class<? extends Tag> parentClass = relClasses.get(1);
            // process relations of locations
            if (entityClass.isAssignableFrom(type)) {
                if (parentClass.isAssignableFrom(typeParent)) {
                    configuration.put(relation.getValue(), parentItem.getName());
                }
            }
        }
    }

    /**
     * Retrieves semantic information from a member items.
     *
     * @param type the semantic type of the item for which the semantic information is gathered
     * @param memberItem the member item to process
     * @param configuration the metadata configuration that should be amended
     */
    private void processMember(Class<? extends Tag> type, Item memberItem, Map<String, Object> configuration) {
        Class<? extends Tag> typeMember = SemanticTags.getSemanticType(memberItem);
        if (typeMember == null) {
            return;
        }
        for (Entry<List<Class<? extends Tag>>, String> relation : memberRelations.entrySet()) {
            List<Class<? extends Tag>> relClasses = relation.getKey();
            Class<? extends Tag> entityClass = relClasses.get(0);
            Class<? extends Tag> parentClass = relClasses.get(1);
            // process relations of locations
            if (entityClass.isAssignableFrom(type)) {
                if (parentClass.isAssignableFrom(typeMember)) {
                    configuration.put(relation.getValue(), memberItem.getName());
                }
            }
        }
    }

    private void initRelations() {
        parentRelations.put(List.of(Equipment.class, Location.class), "hasLocation");
        parentRelations.put(List.of(Point.class, Location.class), "hasLocation");
        parentRelations.put(List.of(Location.class, Location.class), "isPartOf");
        parentRelations.put(List.of(Equipment.class, Equipment.class), "isPartOf");
        parentRelations.put(List.of(Point.class, Equipment.class), "isPointOf");

        memberRelations.put(List.of(Equipment.class, Point.class), "hasPoint");

        propertyRelations.put(List.of(Point.class), "relatesTo");
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        for (Item item : itemRegistry.getItems()) {
            added(item);
        }
    }

    @Override
    public void added(Item item) {
        processItem(item);
    }

    @Override
    public void removed(Item item) {
        Metadata removedMd = semantics.remove(item.getName());
        if (removedMd != null) {
            notifyListenersAboutRemovedElement(removedMd);
        }
    }

    @Override
    public void updated(Item oldItem, Item item) {
        processItem(item);
    }
}
