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

import static org.openhab.core.semantics.SemanticTags.METADATA_NAMESPACE;
import static org.openhab.core.semantics.SemanticTags.REL_HAS_LOCATION;
import static org.openhab.core.semantics.SemanticTags.REL_IS_PART_OF;
import static org.openhab.core.semantics.SemanticTags.REL_IS_POINT_OF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;

/**
 * {@link LLMItemSerializer} is a utility class to serialize a collection of items (both semantic and non-semantic) into
 * a structured, hierarchical string representation for passing into the context of a Large Language Model (LLM) based
 * {@link org.openhab.core.voice.text.HumanLanguageInterpreter}.
 * <p>
 * Evaluates semantic relations via {@link MetadataRegistry} (using keys like {@code hasLocation}, {@code isPartOf}, and
 * {@code isPointOf}) with fallback to standard Group membership hierarchy.
 * Output is formatted in a custom, token-efficient syntax.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMItemSerializer {

    private static final Comparator<Item> ITEM_COMPARATOR = Comparator.comparing(Item::getName);

    private record LocationNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<LocationNode> locationItems, List<EquipmentNode> equipmentItems, List<PointNode> pointItems) {
    }

    private record CommandOptionNode(String command, @Nullable String label) {
    }

    private record EquipmentNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<CommandOptionNode> commandOptions, List<EquipmentNode> equipmentItems, List<PointNode> pointItems) {
    }

    private record PointNode(String name, @Nullable String label, String type, @Nullable String semanticType,
            List<String> properties, List<CommandOptionNode> commandOptions) {
    }

    private record NonSemanticItemNode(String name, @Nullable String label, String type,
            List<CommandOptionNode> commandOptions, List<NonSemanticItemNode> children) {
    }

    private record RootNode(List<LocationNode> locationItems, List<EquipmentNode> equipmentItems,
            List<PointNode> pointItems, List<NonSemanticItemNode> items) {
    }

    /**
     * Serializes a collection of items, evaluating openHAB semantic relations and group membership,
     * localizing command options with the given locale if available.
     *
     * @param items the items to serialize
     * @param metadataRegistry the metadata registry used to query semantic relation metadata, or null
     * @param locale the locale to use for command options localization
     * @return the serialized representation of the items
     */
    public static String serialize(Collection<Item> items, MetadataRegistry metadataRegistry, @Nullable Locale locale) {
        if (items.isEmpty()) {
            return "";
        }

        Map<String, Item> itemMap = new HashMap<>();
        for (Item item : items) {
            itemMap.put(item.getName(), item);
        }

        Map<String, List<Item>> parentToChildren = new HashMap<>();
        Set<String> semanticChildNames = new HashSet<>();
        Set<String> nonSemanticChildNames = new HashSet<>();

        List<Item> allLocations = new ArrayList<>();
        List<Item> allEquipments = new ArrayList<>();
        List<Item> allPoints = new ArrayList<>();
        List<Item> allNonSemanticItems = new ArrayList<>();

        for (Item child : items) {
            boolean isLocation = SemanticTags.getLocation(child) != null;
            boolean isEquipment = SemanticTags.getEquipment(child) != null;
            boolean isPoint = SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null;

            if (isLocation) {
                allLocations.add(child);
            } else if (isEquipment) {
                allEquipments.add(child);
            } else if (isPoint) {
                allPoints.add(child);
            } else {
                allNonSemanticItems.add(child);
            }

            resolveParents(metadataRegistry, child, itemMap, isLocation, isEquipment, isPoint, parentToChildren,
                    semanticChildNames, nonSemanticChildNames);
        }

        List<Item> rootLocations = filterAndSortRoots(allLocations, semanticChildNames);
        List<Item> rootEquipments = filterAndSortRoots(allEquipments, semanticChildNames);
        List<Item> rootPoints = filterAndSortRoots(allPoints, semanticChildNames);
        List<Item> nonSemanticRoots = filterAndSortRoots(allNonSemanticItems, nonSemanticChildNames);
        allNonSemanticItems.sort(ITEM_COMPARATOR);

        List<LocationNode> rootLocNodes = new ArrayList<>();
        List<EquipmentNode> rootEqNodes = new ArrayList<>();
        List<PointNode> rootPtNodes = new ArrayList<>();
        List<NonSemanticItemNode> nonSemanticNodes = new ArrayList<>();

        for (Item loc : rootLocations) {
            rootLocNodes.add(buildLocationNode(loc, parentToChildren, locale));
        }
        for (Item eq : rootEquipments) {
            rootEqNodes.add(buildEquipmentNode(eq, parentToChildren, locale));
        }
        for (Item pt : rootPoints) {
            rootPtNodes.add(buildPointNode(pt, locale));
        }

        Set<String> ancestors = new HashSet<>();
        for (Item item : nonSemanticRoots) {
            nonSemanticNodes
                    .add(buildNonSemanticNode(item, parentToChildren, ancestors, nonSemanticChildNames, locale));
        }
        for (Item item : allNonSemanticItems) {
            if (nonSemanticChildNames.contains(item.getName())) {
                nonSemanticNodes
                        .add(buildNonSemanticNode(item, parentToChildren, ancestors, nonSemanticChildNames, locale));
            }
        }

        RootNode root = new RootNode(rootLocNodes, rootEqNodes, rootPtNodes, nonSemanticNodes);
        return formatRoot(root);
    }

    private static List<Item> filterAndSortRoots(List<Item> items, Set<String> childNames) {
        List<Item> roots = new ArrayList<>();
        for (Item item : items) {
            if (!childNames.contains(item.getName())) {
                roots.add(item);
            }
        }
        roots.sort(ITEM_COMPARATOR);
        return roots;
    }

    /**
     * Resolves the parent item names for a child item using semantic relation metadata for semantic parents
     * and group membership for non-semantic parent groups.
     */
    private static void resolveParents(MetadataRegistry metadataRegistry, Item child, Map<String, Item> itemMap,
            boolean isLocation, boolean isEquipment, boolean isPoint, Map<String, List<Item>> parentToChildren,
            Set<String> semanticChildNames, Set<String> nonSemanticChildNames) {

        String semanticParent = null;

        // Resolve semantic parent via SemanticsMetadataProvider configuration keys
        Metadata md = metadataRegistry.get(new MetadataKey(METADATA_NAMESPACE, child.getName()));
        if (md != null) {
            Map<String, Object> config = md.getConfiguration();

            if (isLocation) {
                // Sub-location -> Parent Location
                semanticParent = getValidTarget(config.get(REL_IS_PART_OF), itemMap);
            } else if (isEquipment) {
                // Sub-equipment -> Parent Equipment
                semanticParent = getValidTarget(config.get(REL_IS_PART_OF), itemMap);
                if (semanticParent == null) {
                    // Equipment -> Parent Location
                    semanticParent = getValidTarget(config.get(REL_HAS_LOCATION), itemMap);
                }
            } else if (isPoint) {
                // Point -> Parent Equipment
                semanticParent = getValidTarget(config.get(REL_IS_POINT_OF), itemMap);
                if (semanticParent == null) {
                    // Loose Point -> Parent Location
                    semanticParent = getValidTarget(config.get(REL_HAS_LOCATION), itemMap);
                }
            }
        }

        if (semanticParent != null) {
            parentToChildren.computeIfAbsent(semanticParent, k -> new ArrayList<>()).add(child);
            semanticChildNames.add(child.getName());
        }

        // Add non-semantic parent groups from group membership
        for (String groupName : child.getGroupNames()) {
            Item parent = itemMap.get(groupName);
            if (parent == null) {
                continue;
            }

            boolean parentIsLocation = SemanticTags.getLocation(parent) != null;
            boolean parentIsEquipment = SemanticTags.getEquipment(parent) != null;

            if (!parentIsLocation && !parentIsEquipment && !groupName.equals(semanticParent)) {
                parentToChildren.computeIfAbsent(groupName, k -> new ArrayList<>()).add(child);
                nonSemanticChildNames.add(child.getName());
            }
        }
    }

    private static @Nullable String getValidTarget(@Nullable Object targetName, Map<String, Item> itemMap) {
        if (targetName instanceof String name && itemMap.containsKey(name)) {
            return name;
        }
        return null;
    }

    private static LocationNode buildLocationNode(Item loc, Map<String, List<Item>> parentToChildren,
            @Nullable Locale locale) {
        List<Item> children = parentToChildren.getOrDefault(loc.getName(), List.of());
        List<LocationNode> subLocations = new ArrayList<>();
        List<EquipmentNode> equipments = new ArrayList<>();
        List<PointNode> points = new ArrayList<>();

        for (Item child : children) {
            if (SemanticTags.getLocation(child) != null) {
                subLocations.add(buildLocationNode(child, parentToChildren, locale));
            } else if (SemanticTags.getEquipment(child) != null) {
                equipments.add(buildEquipmentNode(child, parentToChildren, locale));
            } else if (SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null) {
                points.add(buildPointNode(child, locale));
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

    private static EquipmentNode buildEquipmentNode(Item eq, Map<String, List<Item>> parentToChildren,
            @Nullable Locale locale) {
        List<Item> children = parentToChildren.getOrDefault(eq.getName(), List.of());
        List<EquipmentNode> subEquipments = new ArrayList<>();
        List<PointNode> points = new ArrayList<>();

        for (Item child : children) {
            if (SemanticTags.getEquipment(child) != null) {
                subEquipments.add(buildEquipmentNode(child, parentToChildren, locale));
            } else if (SemanticTags.getPoint(child) != null || SemanticTags.getProperty(child) != null) {
                points.add(buildPointNode(child, locale));
            }
        }

        subEquipments.sort(Comparator.comparing(EquipmentNode::name));
        points.sort(Comparator.comparing(PointNode::name));

        Class<? extends Equipment> eqType = SemanticTags.getEquipment(eq);
        String semType = eqType != null ? eqType.getSimpleName() : null;

        return new EquipmentNode(eq.getName(), getOrNullLabel(eq), eq.getType(), semType, getCommandOptions(eq, locale),
                subEquipments, points);
    }

    private static PointNode buildPointNode(Item pt, @Nullable Locale locale) {
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

        return new PointNode(pt.getName(), getOrNullLabel(pt), pt.getType(), semType, properties,
                getCommandOptions(pt, locale));
    }

    private static List<CommandOptionNode> getCommandOptions(Item item, @Nullable Locale locale) {
        @Nullable
        CommandDescription commandDesc = item.getCommandDescription(locale);
        if (commandDesc == null) {
            return List.of();
        }
        List<CommandOption> options = commandDesc.getCommandOptions();
        if (options.isEmpty()) {
            return List.of();
        }
        List<CommandOptionNode> commandOptions = new ArrayList<>();
        for (CommandOption option : options) {
            String label = option.getLabel();
            if (label != null && label.isBlank()) {
                label = null;
            }
            commandOptions.add(new CommandOptionNode(option.getCommand(), label));
        }
        return commandOptions;
    }

    private static @Nullable String getOrNullLabel(Item item) {
        String label = item.getLabel();
        return (label == null || label.isBlank()) ? null : label;
    }

    private static String formatRoot(RootNode root) {
        if (root.locationItems().isEmpty() && root.equipmentItems().isEmpty() && root.pointItems().isEmpty()
                && root.items().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(
                "# Format: [..]name [type] [\"label\"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]\n\n");

        boolean hasSemanticItems = !root.locationItems().isEmpty() || !root.equipmentItems().isEmpty()
                || !root.pointItems().isEmpty();

        if (hasSemanticItems) {
            sb.append("# Semantic Items\n");
            for (LocationNode loc : root.locationItems()) {
                formatLocationNode(loc, 0, sb);
            }
            for (EquipmentNode eq : root.equipmentItems()) {
                formatEquipmentNode(eq, 0, sb);
            }
            for (PointNode pt : root.pointItems()) {
                formatPointNode(pt, 0, sb);
            }
        }
        if (!root.items().isEmpty()) {
            if (hasSemanticItems) {
                sb.append("\n");
            }
            sb.append("# Non-semantic Items\n");
            for (NonSemanticItemNode item : root.items()) {
                formatNonSemanticItemNode(item, sb);
            }
        }
        return sb.toString();
    }

    private static void formatLocationNode(LocationNode loc, int depth, StringBuilder sb) {
        sb.repeat("..", depth).append(loc.name());

        if (!"Group".equals(loc.type())) {
            sb.append(" ").append(loc.type());
        }
        if (shouldPrintLabel(loc.name(), loc.label())) {
            sb.append(" \"").append(loc.label().replace("\"", "\\\"")).append("\"");
        }
        if (loc.semanticType() != null) {
            sb.append(" :").append(loc.semanticType());
        }
        sb.append("\n");

        for (LocationNode subLoc : loc.locationItems()) {
            formatLocationNode(subLoc, depth + 1, sb);
        }
        for (EquipmentNode eq : loc.equipmentItems()) {
            formatEquipmentNode(eq, depth + 1, sb);
        }
        for (PointNode pt : loc.pointItems()) {
            formatPointNode(pt, depth + 1, sb);
        }
    }

    private static void formatEquipmentNode(EquipmentNode eq, int depth, StringBuilder sb) {
        sb.repeat("..", depth).append(eq.name());

        if (!"Group".equals(eq.type())) {
            sb.append(" ").append(eq.type());
        }
        if (shouldPrintLabel(eq.name(), eq.label())) {
            sb.append(" \"").append(eq.label().replace("\"", "\\\"")).append("\"");
        }
        if (eq.semanticType() != null) {
            sb.append(" :").append(eq.semanticType());
        }
        if (!eq.commandOptions().isEmpty()) {
            sb.append(" (");
            formatCommandOptions(eq.commandOptions(), sb);
            sb.append(")");
        }
        sb.append("\n");

        for (EquipmentNode subEq : eq.equipmentItems()) {
            formatEquipmentNode(subEq, depth + 1, sb);
        }
        for (PointNode pt : eq.pointItems()) {
            formatPointNode(pt, depth + 1, sb);
        }
    }

    private static void formatPointNode(PointNode pt, int depth, StringBuilder sb) {
        sb.repeat("..", depth).append(pt.name());

        if (!"Group".equals(pt.type())) {
            sb.append(" ").append(pt.type());
        }
        if (shouldPrintLabel(pt.name(), pt.label())) {
            sb.append(" \"").append(pt.label().replace("\"", "\\\"")).append("\"");
        }
        if (pt.semanticType() != null) {
            sb.append(" :").append(pt.semanticType());
        }
        if (!pt.properties().isEmpty()) {
            sb.append(" [").append(String.join(",", pt.properties())).append("]");
        }
        if (!pt.commandOptions().isEmpty()) {
            sb.append(" (");
            formatCommandOptions(pt.commandOptions(), sb);
            sb.append(")");
        }
        sb.append("\n");
    }

    private static NonSemanticItemNode buildNonSemanticNode(Item item, Map<String, List<Item>> parentToChildren,
            Set<String> ancestors, Set<String> nonSemanticChildNames, @Nullable Locale locale) {
        nonSemanticChildNames.remove(item.getName());
        ancestors.add(item.getName());

        List<Item> children = parentToChildren.getOrDefault(item.getName(), List.of());
        List<NonSemanticItemNode> childNodes = new ArrayList<>();

        for (Item child : children) {
            if (!ancestors.contains(child.getName())) {
                childNodes.add(buildNonSemanticNode(child, parentToChildren, ancestors, nonSemanticChildNames, locale));
            }
        }

        ancestors.remove(item.getName());
        childNodes.sort(Comparator.comparing(NonSemanticItemNode::name));

        return new NonSemanticItemNode(item.getName(), getOrNullLabel(item), item.getType(),
                getCommandOptions(item, locale), childNodes);
    }

    private static void formatNonSemanticItemNode(NonSemanticItemNode item, StringBuilder sb) {
        formatNonSemanticItemNode(item, 0, sb);
    }

    private static void formatNonSemanticItemNode(NonSemanticItemNode item, int depth, StringBuilder sb) {
        sb.repeat("..", depth).append(item.name());

        if (!"Group".equals(item.type())) {
            sb.append(" ").append(item.type());
        }
        if (shouldPrintLabel(item.name(), item.label())) {
            sb.append(" \"").append(item.label().replace("\"", "\\\"")).append("\"");
        }
        if (!item.commandOptions().isEmpty()) {
            sb.append(" (");
            formatCommandOptions(item.commandOptions(), sb);
            sb.append(")");
        }
        sb.append("\n");

        for (NonSemanticItemNode child : item.children()) {
            formatNonSemanticItemNode(child, depth + 1, sb);
        }
    }

    private static boolean shouldPrintLabel(String name, @Nullable String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String cleanLabel = label.replace(" ", "").replace("_", "");
        String cleanName = name.replace("_", "");
        return !cleanLabel.equalsIgnoreCase(cleanName);
    }

    private static void formatCommandOptions(List<CommandOptionNode> commandOptions, StringBuilder sb) {
        for (int i = 0; i < commandOptions.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            CommandOptionNode option = commandOptions.get(i);
            sb.append(option.command());
            if (option.label() != null && !option.label().isBlank()) {
                sb.append("=").append(option.label());
            }
        }
    }
}
