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
package org.openhab.core.tools.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.dto.SemanticTagDTO;
import org.openhab.core.semantics.dto.SemanticTagDTOMapper;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SemanticTagUpgrader} removes custom semantic tags that are now part of standard semantic tags.
 * The custom tags will also be checked for their position in the hierarchy. If hierarchy in the default tags has
 * changed, and a custom tag depends on it, the custom tag parent will be adjusted.
 * Semantic tags applied to items will be cleaned. If there is more then one semantic tag by class, only one will be
 * kept and the others will be renamed to be non-semantic. Preference is given to keep the tags that moved class. If
 * there is a Property tag, a Point tag will be added if missing.
 * With these adjustments, the model can be further corrected from the UI without issues of double tags of the same
 * class not being shown in the UI.
 * This upgrader only considers managed tags and items.
 *
 * @Since 5.2.0
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SemanticTagUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(SemanticTagUpgrader.class);

    private static final String TARGET_VERSION = "5.0.0";

    // Tags known to have changed class in 5.0
    private static final Set<String> TAGS_CHANGED_CLASS = Set.of("LowBattery", "OpenLevel", "OpenState", "Tampered");

    private static final String DEFAULT_POINT_TAG = "Status";

    @Override
    public String getName() {
        return "deduplicateSemanticTags";
    }

    @Override
    public String getDescription() {
        return "Correct custom semantic tags and item semantic tags for changes in provided default semantic tags";
    }

    @Override
    public @Nullable String getTargetVersion() {
        return TARGET_VERSION;
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path semanticsJsonDatabasePath = userdataPath
                .resolve(Path.of("jsondb", "org.openhab.core.semantics.SemanticTag.json"));
        logger.info("Deduplicating custom semantic tags '{}'", semanticsJsonDatabasePath);

        boolean dbUpdated = false;
        boolean canUpdateDb = true;
        if (!Files.exists(semanticsJsonDatabasePath)) {
            canUpdateDb = false;
            dbUpdated = true;
            logger.info("Semantics tags database '{}' does not exist, no tags to update.", semanticsJsonDatabasePath);
        } else if (!Files.isReadable(semanticsJsonDatabasePath)) {
            canUpdateDb = false;
            logger.warn(
                    "Cannot access semantic tags database '{}', update may be incomplete, check path and access rights.",
                    semanticsJsonDatabasePath);
        }

        Map<String, SemanticTag> defaultTags = (new DefaultSemanticTagProvider()).getAll().stream()
                .collect(Collectors.toMap(SemanticTag::getName, Function.identity()));
        Map<String, SemanticTag> customTags = Map.of();

        Set<String> changedTags = new HashSet<>(TAGS_CHANGED_CLASS);

        if (canUpdateDb) {
            JsonStorage<SemanticTagDTO> semanticTagStorage = new JsonStorage<>(semanticsJsonDatabasePath.toFile(), null,
                    5, 0, 0, List.of());

            // Remove duplicate tags from custom tag store
            for (String tagKey : semanticTagStorage.getKeys()) {
                SemanticTag tag = SemanticTagDTOMapper.map(semanticTagStorage.get(tagKey));
                if (tag == null) {
                    continue;
                }

                String tagUID = tag.getUID();
                String tagName = tag.getName();
                if (defaultTags.keySet().contains(tagName)) {
                    logger.info("  Removed duplicate tag '{}' with UID '{}' from custom tags", tagName, tagUID);
                    semanticTagStorage.remove(tagUID);
                    changedTags.add(tagName);
                }
            }
            semanticTagStorage.flush();

            // Rewrite parent relationships if position in hierarchy has changed
            for (String tagKey : semanticTagStorage.getKeys()) {
                SemanticTag tag = SemanticTagDTOMapper.map(semanticTagStorage.get(tagKey));
                if (tag == null) {
                    continue;
                }

                String tagUID = tag.getUID();
                String[] hierarchy = tagUID.split("_");
                if (hierarchy.length <= 2) {
                    // No need to check for update if only semantic class and tag
                    continue;
                }

                String tagName = tag.getName();
                for (int i = hierarchy.length - 2; i > 0; i--) {
                    String parentName = hierarchy[i];
                    SemanticTag newParent = defaultTags.get(parentName);
                    if (newParent != null) {
                        String newParentUID = newParent.getUID();
                        String childUID = String.join("_", Arrays.copyOfRange(hierarchy, i + 1, hierarchy.length));
                        String newUID = newParentUID + "_" + childUID;
                        if (!tagUID.equals(newUID)) {
                            SemanticTag newTag = new SemanticTagImpl(newUID, tag.getLabel(), tag.getDescription(),
                                    tag.getSynonyms());
                            semanticTagStorage.put(newUID, SemanticTagDTOMapper.map(newTag));
                            semanticTagStorage.remove(tagUID);
                            changedTags.add(tagName);
                            logger.info("  Updated custom tag '{}' parent from '{}' to '{}'", tagName,
                                    tag.getParentUID(), newTag.getParentUID());
                            break;
                        }
                    }
                }
            }
            semanticTagStorage.flush();
            dbUpdated = true;

            customTags = semanticTagStorage.getValues().stream().map(tag -> SemanticTagDTOMapper.map(tag))
                    .filter(Objects::nonNull).collect(Collectors.toMap(SemanticTag::getName, Function.identity()));
        }

        Path itemJsonDatabasePath = userdataPath.resolve(Path.of("jsondb", "org.openhab.core.items.Item.json"));
        logger.info("Cleaning item semantic tags '{}'", itemJsonDatabasePath);

        if (!Files.exists(itemJsonDatabasePath)) {
            logger.info("No item database '{}', no managed items to update.", itemJsonDatabasePath);
            return dbUpdated;
        } else if (!Files.isReadable(itemJsonDatabasePath)) {
            logger.warn("Cannot access item database '{}', update may be incomplete, check path and access rights.",
                    itemJsonDatabasePath);
            return false;
        }
        JsonStorage<ManagedItemProvider.PersistedItem> itemStorage = new JsonStorage<>(itemJsonDatabasePath.toFile(),
                null, 5, 0, 0, List.of());

        // Adjust item tags, make sure only 1 tag of every class exists, rename other tags
        Map<String, SemanticTag> allSemanticTags = Stream.of(defaultTags, customTags)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<String> itemTagNames = itemStorage.getValues().stream().filter(Objects::nonNull).map(item -> item.tags)
                .filter(Objects::nonNull).flatMap(tags -> tags.stream()).collect(Collectors.toSet());
        Set<String> allTagNames = Stream.of(allSemanticTags.keySet(), itemTagNames).flatMap(tags -> tags.stream())
                .collect(Collectors.toSet());

        for (String itemKey : itemStorage.getKeys()) {
            ManagedItemProvider.PersistedItem item = itemStorage.get(itemKey);
            Set<String> tags = item != null ? item.tags : null;
            if (item == null || tags == null || tags.isEmpty()) {
                continue;
            }

            Set<String> newTags = new HashSet<>(tags);
            Set<String> locationTags = new HashSet<>(tags.stream()
                    .filter(tag -> "Location".equals(tagClass(allSemanticTags.get(tag)))).collect(Collectors.toSet()));
            Set<String> equipmentTags = new HashSet<>(tags.stream()
                    .filter(tag -> "Equipment".equals(tagClass(allSemanticTags.get(tag)))).collect(Collectors.toSet()));
            Set<String> pointTags = new HashSet<>(tags.stream()
                    .filter(tag -> "Point".equals(tagClass(allSemanticTags.get(tag)))).collect(Collectors.toSet()));
            Set<String> propertyTags = new HashSet<>(tags.stream()
                    .filter(tag -> "Property".equals(tagClass(allSemanticTags.get(tag)))).collect(Collectors.toSet()));

            Map<String, Set<String>> tagSets = Map.of("Location", locationTags, "Equipment", equipmentTags, "Point",
                    pointTags, "Property", propertyTags);
            for (String tagClass : tagSets.keySet()) {
                Set<String> tagSet = tagSets.get(tagClass);
                while (tagSet != null && tagSet.size() > 1) {
                    String tagToRemove = tagSet.stream().filter(tag -> !changedTags.contains(tag)).findAny()
                            .orElse(tagSet.iterator().next());
                    if (tagToRemove != null) {
                        String tagToAdd = newTagName(tagToRemove, allTagNames);
                        newTags.remove(tagToRemove);
                        newTags.add(tagToAdd);
                        tagSet.remove(tagToRemove);
                        logger.info("  Multiple {} tags on item '{}', renamed '{}' to '{}'", tagClass, itemKey,
                                tagToRemove, tagToAdd);
                    }
                }
                if (tagSet != null && tagSet.size() == 1 && "Property".equals(tagClass) && pointTags.isEmpty()) {
                    // When there is a Property tag, there should also be a Point tag, add it if missing.
                    // Status is a neutral one, easy to reconfigure if necessary.
                    newTags.add(DEFAULT_POINT_TAG);
                    logger.info("  Item '{}' has Property tag '{}' without Point tag, added Point tag '{}'", itemKey,
                            tagSet.iterator().next(), DEFAULT_POINT_TAG);
                }
            }

            if (!newTags.equals(tags)) {
                item.tags = newTags;
                itemStorage.put(itemKey, item);
            }
        }
        itemStorage.flush();

        return dbUpdated;
    }

    private String newTagName(String oldTagName, Set<String> allTagNames) {
        String tagName = oldTagName;
        int i = 1;
        while (allTagNames.contains(tagName)) {
            tagName = oldTagName + String.valueOf(i);
            i++;
        }
        return tagName;
    }

    private String tagClass(String tagUID) {
        int idx = tagUID.indexOf("_");
        return (idx >= 0 ? tagUID.substring(0, idx) : "").trim();
    }

    private @Nullable String tagClass(@Nullable SemanticTag tag) {
        if (tag == null) {
            return null;
        }
        return tagClass(tag.getUID());
    }
}
