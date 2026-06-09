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
package org.openhab.core.voice.text.interpreter.llm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * {@link LLMItemSerializer} is a utility class to serialize a collection of items (both semantic and non-semantic) into
 * a structured,
 * hierarchical string representation for passing into the context of a Large Language Model (LLM) based
 * {@link org.openhab.core.voice.text.HumanLanguageInterpreter}.
 * The output format is YAML, as this is hierarchical and token-efficient, as well as easy to generate using Jackson.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMItemSerializer {

    private static final Comparator<Item> itemComparator = Comparator.comparing(Item::getName);
    private static final ObjectMapper mapper;

    static {
        YAMLFactory yamlFactory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        mapper = new ObjectMapper(yamlFactory);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record LocationNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<LocationNode> locationItems, List<EquipmentNode> equipmentItems, List<PointNode> pointItems) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record EquipmentNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<EquipmentNode> equipmentItems, List<PointNode> pointItems) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record PointNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<String> properties) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record NonSemanticItemNode(String name, @Nullable String label, String type) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record RootNode(List<LocationNode> locationItems, List<EquipmentNode> equipmentItems,
            List<PointNode> pointItems, List<NonSemanticItemNode> items) {
    }

    /**
     * Serializes a collection of items.
     *
     * @param items the items to serialize
     * @return the serialized representation (YAML) of the items
     */
    public static String serialize(Collection<Item> items) {
        if (items.isEmpty()) {
            return "";
        }

        Map<String, Item> itemMap = new HashMap<>();
        for (Item item : items) {
            if (item != null) {
                itemMap.put(item.getName(), item);
            }
        }

        Map<String, List<Item>> parentToChildren = new HashMap<>();
        Set<String> childNames = new HashSet<>();

        for (Item child : items) {
            if (child == null) {
                continue;
            }

            boolean isLocation = SemanticTags.getLocation(child) != null;
            boolean isEquipment = SemanticTags.getEquipment(child) != null;
            boolean isPoint = SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null;

            if (!isLocation && !isEquipment && !isPoint) {
                continue;
            }

            String parentName = null;
            if (isLocation) {
                for (String groupName : child.getGroupNames()) {
                    Item parent = itemMap.get(groupName);
                    if (parent != null && SemanticTags.getLocation(parent) != null) {
                        parentName = groupName;
                        break;
                    }
                }
            } else if (isEquipment) {
                String fallbackParent = null;
                for (String groupName : child.getGroupNames()) {
                    Item parent = itemMap.get(groupName);
                    if (parent != null) {
                        if (SemanticTags.getEquipment(parent) != null) {
                            parentName = groupName;
                            break;
                        } else if (SemanticTags.getLocation(parent) != null && fallbackParent == null) {
                            fallbackParent = groupName;
                        }
                    }
                }
                if (parentName == null) {
                    parentName = fallbackParent;
                }
            } else {
                String fallbackParent = null;
                for (String groupName : child.getGroupNames()) {
                    Item parent = itemMap.get(groupName);
                    if (parent != null) {
                        if (SemanticTags.getEquipment(parent) != null) {
                            parentName = groupName;
                            break;
                        } else if (SemanticTags.getLocation(parent) != null && fallbackParent == null) {
                            fallbackParent = groupName;
                        }
                    }
                }
                if (parentName == null) {
                    parentName = fallbackParent;
                }
            }

            if (parentName != null) {
                parentToChildren.computeIfAbsent(parentName, k -> new ArrayList<>()).add(child);
                childNames.add(child.getName());
            }
        }

        List<Item> rootLocations = new ArrayList<>();
        List<Item> rootEquipments = new ArrayList<>();
        List<Item> rootPoints = new ArrayList<>();
        List<Item> nonSemanticItems = new ArrayList<>();

        for (Item item : items) {
            if (item == null) {
                continue;
            }

            boolean isLocation = SemanticTags.getLocation(item) != null;
            boolean isEquipment = SemanticTags.getEquipment(item) != null;
            boolean isPoint = SemanticTags.getPoint(item) != null || SemanticTags.getProperty(item) != null;

            if (isLocation) {
                if (!childNames.contains(item.getName())) {
                    rootLocations.add(item);
                }
            } else if (isEquipment) {
                if (!childNames.contains(item.getName())) {
                    rootEquipments.add(item);
                }
            } else if (isPoint) {
                if (!childNames.contains(item.getName())) {
                    rootPoints.add(item);
                }
            } else {
                nonSemanticItems.add(item);
            }
        }

        rootLocations.sort(itemComparator);
        rootEquipments.sort(itemComparator);
        rootPoints.sort(itemComparator);
        nonSemanticItems.sort(itemComparator);

        List<LocationNode> rootLocNodes = new ArrayList<>();
        List<EquipmentNode> rootEqNodes = new ArrayList<>();
        List<PointNode> rootPtNodes = new ArrayList<>();
        List<NonSemanticItemNode> nonSemanticNodes = new ArrayList<>();

        for (Item loc : rootLocations) {
            rootLocNodes.add(buildLocationNode(loc, parentToChildren));
        }
        for (Item eq : rootEquipments) {
            rootEqNodes.add(buildEquipmentNode(eq, parentToChildren));
        }
        for (Item pt : rootPoints) {
            rootPtNodes.add(buildPointNode(pt));
        }
        for (Item item : nonSemanticItems) {
            nonSemanticNodes.add(new NonSemanticItemNode(item.getName(), getOrNullLabel(item), item.getType()));
        }

        RootNode root = new RootNode(rootLocNodes, rootEqNodes, rootPtNodes, nonSemanticNodes);

        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items to YAML", e);
        }
    }

    private static LocationNode buildLocationNode(Item loc, Map<String, List<Item>> parentToChildren) {
        List<Item> children = parentToChildren.getOrDefault(loc.getName(), List.of());
        List<LocationNode> subLocations = new ArrayList<>();
        List<EquipmentNode> equipments = new ArrayList<>();
        List<PointNode> points = new ArrayList<>();

        for (Item child : children) {
            if (SemanticTags.getLocation(child) != null) {
                subLocations.add(buildLocationNode(child, parentToChildren));
            } else if (SemanticTags.getEquipment(child) != null) {
                equipments.add(buildEquipmentNode(child, parentToChildren));
            } else if (SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null) {
                points.add(buildPointNode(child));
            }
        }

        subLocations.sort(Comparator.comparing(LocationNode::name));
        equipments.sort(Comparator.comparing(EquipmentNode::name));
        points.sort(Comparator.comparing(PointNode::name));

        Class<? extends Location> locType = SemanticTags.getLocation(loc);
        String semType = locType != null ? locType.getSimpleName() : null;

        return new LocationNode(loc.getName(), getOrNullLabel(loc), loc.getType(), semType, subLocations, equipments,
                points);
    }

    private static EquipmentNode buildEquipmentNode(Item eq, Map<String, List<Item>> parentToChildren) {
        List<Item> children = parentToChildren.getOrDefault(eq.getName(), List.of());
        List<EquipmentNode> subEquipments = new ArrayList<>();
        List<PointNode> points = new ArrayList<>();

        for (Item child : children) {
            if (SemanticTags.getEquipment(child) != null) {
                subEquipments.add(buildEquipmentNode(child, parentToChildren));
            } else if (SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null) {
                points.add(buildPointNode(child));
            }
        }

        subEquipments.sort(Comparator.comparing(EquipmentNode::name));
        points.sort(Comparator.comparing(PointNode::name));

        Class<? extends Equipment> eqType = SemanticTags.getEquipment(eq);
        String semType = eqType != null ? eqType.getSimpleName() : null;

        return new EquipmentNode(eq.getName(), getOrNullLabel(eq), eq.getType(), semType, subEquipments, points);
    }

    private static PointNode buildPointNode(Item pt) {
        Class<? extends Point> ptType = SemanticTags.getPoint(pt);
        String semType = ptType != null ? ptType.getSimpleName() : null;

        List<String> properties = new ArrayList<>();
        for (String tagId : pt.getTags()) {
            Class<? extends Tag> type = SemanticTags.getById(tagId);
            if (type != null && Property.class.isAssignableFrom(type)) {
                properties.add(type.getSimpleName());
            }
        }
        if (!properties.isEmpty()) {
            Collections.sort(properties);
        }

        return new PointNode(pt.getName(), getOrNullLabel(pt), pt.getType(), semType, properties);
    }

    private static @Nullable String getOrNullLabel(Item item) {
        String label = item.getLabel();
        return (label == null || label.isBlank()) ? null : label;
    }
}
