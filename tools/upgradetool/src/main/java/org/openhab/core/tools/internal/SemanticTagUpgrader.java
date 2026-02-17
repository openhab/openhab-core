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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 * The {@link SemanticTagUpgrader} renames custom semantic tags that are now part of standard semantic tags.
 * The custom tags will get an index appended and managed item tags will be updated accordingly.
 * This upgrader only considers managed tags and items.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SemanticTagUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(SemanticTagUpgrader.class);

    private Map<String, SemanticTag> defaultTags = Map.of();
    private Set<String> defaultTagNames = Set.of();
    private Set<String> defaultTagSynonyms = Set.of();

    private Map<String, SemanticTag> customTags = new HashMap<>();
    private Set<String> customTagNames = Set.of();
    private Set<String> customTagSynonyms = Set.of();

    private Set<String> addedTagNames = new HashSet<>();
    private Map<String, String> replacedParentUIDs = new HashMap<>();

    private Set<String> itemTags = Set.of();

    private Map<String, String> updateMap = new HashMap<>();

    @Override
    public String getName() {
        return "deduplicateSemanticTags";
    }

    @Override
    public String getDescription() {
        return "Rename custom semantic tags that have been superseded by newly provided semantic tags with the same name";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path semanticsJsonDatabasePath = userdataPath
                .resolve(Path.of("jsondb", "org.openhab.core.semantics.SemanticTag.json"));
        Path itemJsonDatabasePath = userdataPath.resolve(Path.of("jsondb", "org.openhab.core.items.Item.json"));
        logger.info("Deduplicating custom semantic tags '{}'", semanticsJsonDatabasePath);

        if (!Files.isReadable(semanticsJsonDatabasePath)) {
            logger.warn("Cannot access semantic tags database '{}', no tags to update, check path and access rights.",
                    semanticsJsonDatabasePath);
            return false;
        }

        defaultTags = (new DefaultSemanticTagProvider()).getAll().stream()
                .collect(Collectors.toMap(tag -> tag.getUID(), tag -> tag));
        defaultTagNames = tagNames(defaultTags);
        defaultTagSynonyms = tagSynonyms(defaultTags);

        JsonStorage<SemanticTagDTO> semanticTagStorage = new JsonStorage<>(semanticsJsonDatabasePath.toFile(), null, 5,
                0, 0, List.of());
        customTags = new HashMap<>(semanticTagStorage.getValues().stream().map(tag -> SemanticTagDTOMapper.map(tag))
                .filter(Objects::nonNull).collect(Collectors.toMap(tag -> tag.getUID(), tag -> tag)));
        customTagNames = tagNames(customTags);
        customTagSynonyms = tagSynonyms(customTags);

        boolean itemStorageExists = Files.isReadable(itemJsonDatabasePath);
        if (!itemStorageExists) {
            logger.error("Cannot access item database '{}', update may be incomplete.", itemJsonDatabasePath);
        }
        JsonStorage<ManagedItemProvider.PersistedItem> itemStorage = itemStorageExists
                ? new JsonStorage<>(itemJsonDatabasePath.toFile(), null, 5, 0, 0, List.of())
                : null;
        itemTags = itemStorage != null
                ? itemStorage.getValues().stream().filter(Objects::nonNull).map(item -> item.tags)
                        .filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet())
                : Set.of();

        replacedParentUIDs = new HashMap<>();
        updateMap = new HashMap<>();
        semanticTagStorage.getKeys().forEach(tagKey -> {
            SemanticTag tag = SemanticTagDTOMapper.map(semanticTagStorage.get(tagKey));
            if (tag != null) {
                updateTag(semanticTagStorage, tag);
            }
        });
        semanticTagStorage.flush();

        if (itemStorage != null && !updateMap.isEmpty()) {
            Set<String> oldTags = updateMap.keySet();
            itemStorage.getKeys().forEach(itemKey -> {
                ManagedItemProvider.PersistedItem item = itemStorage.get(itemKey);
                Set<String> tags = item != null ? item.tags : null;
                if (item != null && tags != null && !Collections.disjoint(tags, oldTags)) {
                    Set<String> newItemTags = tags.stream().map(tag -> updateMap.getOrDefault(tag, tag))
                            .collect(Collectors.toSet());
                    item.tags = newItemTags;
                    itemStorage.put(itemKey, item);
                    logger.info("Updated semantic tags for item '{}' with new custom tag name(s)", itemKey);
                }
            });
            itemStorage.flush();
        }

        return true;
    }

    private SemanticTag updateTag(JsonStorage<SemanticTagDTO> tagStorage, SemanticTag tag) {
        SemanticTag newTag = tag;
        String tagUID = tag.getUID();
        String tagName = tag.getName();
        String newTagUID = tagUID;
        String newTagName = tagName;

        // Make sure all parents exist and are in the proper place in the hierarchy.
        // Start from the top level.
        String[] hierarchy = tagUID.split("_");
        if (hierarchy.length > 2) {
            // First element of UID is always Location, Equipment, Point or Property
            for (int i = 2; i <= hierarchy.length; i++) {
                String tagParentUID = String.join("_", Arrays.copyOfRange(hierarchy, 0, i - 1));
                if (!(defaultTags.keySet().contains(tagParentUID) || customTags.keySet().contains(tagParentUID))) {
                    String newTagParentUID = replacedParentUIDs.getOrDefault(tagParentUID, tagParentUID);
                    SemanticTag tagParent = new SemanticTagImpl(newTagParentUID, tagName(newTagParentUID), null,
                            List.of());
                    if (newTagParentUID.equals(tagParentUID)) {
                        tagParent = updateTag(tagStorage, tagParent);
                        newTagParentUID = tagParent.getUID();
                        if (!tagParentUID.equals(newTagParentUID)) {
                            replacedParentUIDs.put(tagParentUID, newTagParentUID);
                        }
                    }
                    String newTagParentName = tagParent.getName();
                    hierarchy[i - 2] = newTagParentName;
                    addedTagNames.add(newTagParentName);
                    logger.info("Added custom semantic tag parent '{}' for '{}'", newTagParentUID, newTagParentName);
                }
            }
            newTag = new SemanticTagImpl(String.join("_", hierarchy), tag.getLabel(), tag.getDescription(),
                    tag.getSynonyms());
            newTagUID = newTag.getUID();
        }

        // Check if the tag itself does not exist in the default tags, and if so, rename it
        if (defaultTagNames.contains(tagName)) {
            for (int i = 1;; i++) {
                newTagName = tagName + String.valueOf(i);
                if (!tagNameExists(newTagName)) {
                    updateMap.put(tagName, newTagName);
                    newTagUID = tagUID + String.valueOf(i);
                    newTag = new SemanticTagImpl(newTagUID, tag.getLabel(), tag.getDescription(), tag.getSynonyms());
                    newTagName = newTag.getName();
                    addedTagNames.add(newTagName);
                    logger.info("Changed custom semantic tag '{}' to '{}'", tagName, newTagName);
                    break;
                }
            }
        }

        tagStorage.put(newTagUID, SemanticTagDTOMapper.map(newTag));
        if (!newTagUID.equals(tagUID)) {
            tagStorage.remove(tagUID);
        }
        return newTag;
    }

    private Set<String> tagNames(Map<String, SemanticTag> tags) {
        return tags.values().stream().map(tag -> tag.getName()).collect(Collectors.toSet());
    }

    private Set<String> tagSynonyms(Map<String, SemanticTag> tags) {
        return tags.values().stream().flatMap(tag -> tag.getSynonyms().stream()).collect(Collectors.toSet());
    }

    private String tagName(String tagUID) {
        int idx = tagUID.lastIndexOf("_");
        return (idx >= 0 ? tagUID.substring(idx + 1) : tagUID).trim();
    }

    private boolean tagNameExists(String tagName) {
        return defaultTagNames.contains(tagName) || defaultTagSynonyms.contains(tagName)
                || customTagNames.contains(tagName) || customTagSynonyms.contains(tagName) || itemTags.contains(tagName)
                || addedTagNames.contains(tagName);
    }
}
