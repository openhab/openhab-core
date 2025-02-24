/*
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
package org.openhab.core.semantics.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemPredicates;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.SemanticsPredicates;
import org.openhab.core.semantics.SemanticsService;
import org.openhab.core.semantics.Tag;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The internal implementation of the {@link SemanticsService} interface, which is registered as an OSGi service.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Few methods moved from class SemanticTags in order to use the semantic tag registry
 * @author Jimmy Tanagra - Add Item semantic tag validation
 */
@NonNullByDefault
@Component(immediate = true)
public class SemanticsServiceImpl implements SemanticsService, RegistryChangeListener<Item> {

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SemanticsServiceImpl.class);

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final SemanticTagRegistry semanticTagRegistry;

    @Activate
    public SemanticsServiceImpl(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference SemanticTagRegistry semanticTagRegistry) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.semanticTagRegistry = semanticTagRegistry;

        this.itemRegistry.stream().forEach(this::checkSemantics);
        this.itemRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    public void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
    }

    @Override
    public Set<Item> getItemsInLocation(Class<? extends Location> locationType) {
        Set<Item> items = new HashSet<>();
        Set<Item> locationItems = itemRegistry.stream().filter(SemanticsPredicates.isA(locationType))
                .collect(Collectors.toSet());
        for (Item locationItem : locationItems) {
            if (locationItem instanceof GroupItem gItem) {
                items.addAll(gItem
                        .getMembers(SemanticsPredicates.isA(Point.class).or(SemanticsPredicates.isA(Equipment.class))));
            }
        }
        return items;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Set<Item> getItemsInLocation(String labelOrSynonym, Locale locale) {
        Set<Item> items = new HashSet<>();
        List<Class<? extends Tag>> tagList = getByLabelOrSynonym(labelOrSynonym, locale);
        if (!tagList.isEmpty()) {
            for (Class<? extends Tag> tag : tagList) {
                if (Location.class.isAssignableFrom(tag)) {
                    items.addAll(getItemsInLocation((Class<? extends Location>) tag));
                }
            }
        } else {
            Set<Item> locationItems = itemRegistry.stream().filter(ItemPredicates.hasLabel(labelOrSynonym)
                    .or(hasSynonym(labelOrSynonym)).and(SemanticsPredicates.isLocation())).collect(Collectors.toSet());
            for (Item locationItem : locationItems) {
                if (locationItem instanceof GroupItem gItem) {
                    items.addAll(gItem.getMembers(
                            SemanticsPredicates.isA(Point.class).or(SemanticsPredicates.isA(Equipment.class))));
                }
            }
        }
        return items;
    }

    private Predicate<? super Item> hasSynonym(String labelOrSynonym) {
        return item -> {
            MetadataKey key = new MetadataKey(SYNONYMS_NAMESPACE, item.getName());
            Metadata md = metadataRegistry.get(key);
            if (md != null) {
                String[] synonyms = md.getValue().split(",");
                for (String synonym : synonyms) {
                    if (synonym.equalsIgnoreCase(labelOrSynonym)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    @Override
    public @Nullable Class<? extends Tag> getByLabel(String tagLabel, Locale locale) {
        Optional<SemanticTag> tag = semanticTagRegistry.getAll().stream()
                .filter(t -> t.localized(locale).getLabel().equalsIgnoreCase(tagLabel))
                .sorted(Comparator.comparing(SemanticTag::getUID)).findFirst();
        return tag.isPresent() ? semanticTagRegistry.getTagClassById(tag.get().getUID()) : null;
    }

    @Override
    public List<Class<? extends Tag>> getByLabelOrSynonym(String tagLabelOrSynonym, Locale locale) {
        List<SemanticTag> tags = semanticTagRegistry.getAll().stream()
                .filter(t -> getLabelAndSynonyms(t, locale).contains(tagLabelOrSynonym.toLowerCase(locale)))
                .sorted(Comparator.comparing(SemanticTag::getUID)).toList();
        List<Class<? extends Tag>> tagList = new ArrayList<>();
        tags.forEach(t -> {
            Class<? extends Tag> tag = semanticTagRegistry.getTagClassById(t.getUID());
            if (tag != null) {
                tagList.add(tag);
            }
        });
        return tagList;
    }

    @Override
    public List<String> getLabelAndSynonyms(Class<? extends Tag> tagClass, Locale locale) {
        SemanticTag tag = semanticTagRegistry.get(SemanticTagRegistryImpl.buildId(tagClass));
        return tag == null ? List.of() : getLabelAndSynonyms(tag, locale);
    }

    private List<String> getLabelAndSynonyms(SemanticTag tag, Locale locale) {
        SemanticTag localizedTag = tag.localized(locale);
        Stream<String> label = Stream.of(localizedTag.getLabel());
        Stream<String> synonyms = localizedTag.getSynonyms().stream();
        return Stream.concat(label, synonyms).map(s -> s.toLowerCase(locale)).distinct().toList();
    }

    /**
     * Validates the semantic tags of an item.
     * 
     * It returns true only if one of the following is true:
     * - No semantic tags at all
     * - Only one Semantic tag of any kind.
     * - Note: having only one Property tag is allowed. It implies that the item is a Point.
     * - One Point tag and one Property tag
     * 
     * It returns false if two Semantic tags are found, but they don't consist of one Point and one Property.
     * It would also return false if more than two Semantic tags are found.
     * 
     * @param item
     * @param semanticTag the determined semantic tag of the item
     * @return true if the item contains no semantic tags, or a valid combination of semantic tags, otherwise false
     */
    boolean validateTags(Item item, @Nullable Class<? extends Tag> semanticTag) {
        if (semanticTag == null) {
            return true;
        }
        String semanticType = SemanticTags.getSemanticRootName(semanticTag);
        // We're using Collectors here instead of Stream.toList() to resolve Java's wildcard capture conversion issue
        List<Class<? extends Tag>> tags = item.getTags().stream().map(SemanticTags::getById).filter(Objects::nonNull)
                .collect(Collectors.toList());
        switch (tags.size()) {
            case 0:
            case 1:
                return true;
            case 2:
                Class<? extends Tag> firstTag = tags.getFirst();
                Class<? extends Tag> lastTag = tags.getLast();
                if ((Point.class.isAssignableFrom(firstTag) && Property.class.isAssignableFrom(lastTag))
                        || (Point.class.isAssignableFrom(lastTag) && Property.class.isAssignableFrom(firstTag))) {
                    return true;
                }
                String firstType = SemanticTags.getSemanticRootName(firstTag);
                String lastType = SemanticTags.getSemanticRootName(lastTag);
                if (firstType.equals(lastType)) {
                    if (Point.class.isAssignableFrom(firstTag) || Property.class.isAssignableFrom(firstTag)) {
                        logger.warn(
                                "Item '{}' ({}) has an invalid combination of semantic tags: {} ({}) and {} ({}). Only one Point and optionally one Property tag may be assigned.",
                                item.getName(), semanticType, firstTag.getSimpleName(), firstType,
                                lastTag.getSimpleName(), lastType);
                    } else {
                        logger.warn(
                                "Item '{}' ({}) has an invalid combination of semantic tags: {} ({}) and {} ({}). Only one {} tag may be assigned.",
                                item.getName(), semanticType, firstTag.getSimpleName(), firstType,
                                lastTag.getSimpleName(), lastType, firstType);
                    }
                } else {
                    logger.warn(
                            "Item '{}' ({}) has an invalid combination of semantic tags: {} ({}) and {} ({}). {} and {} tags cannot be assigned at the same time.",
                            item.getName(), semanticType, firstTag.getSimpleName(), firstType, lastTag.getSimpleName(),
                            lastType, firstType, lastType);
                }
                return false;
            default:
                List<String> allTags = tags.stream().map(tag -> {
                    String tagType = SemanticTags.getSemanticRootName(tag);
                    return String.format("%s (%s)", tag.getSimpleName(), tagType);
                }).toList();
                logger.warn(
                        "Item '{}' ({}) has an invalid combination of semantic tags: {}. An item may only have one tag of Location, Equipment, or Point type. A Property tag may be assigned in conjunction with a Point tag.",
                        item.getName(), semanticType, allTags);
                return false;
        }
    }

    /**
     * Verifies the semantics of an item and logs warnings if the semantics are invalid
     * 
     * @param item
     * @return true if the semantics are valid, false otherwise
     */
    boolean checkSemantics(Item item) {
        String itemName = item.getName();
        Class<? extends Tag> semanticTag = SemanticTags.getSemanticType(item);
        if (semanticTag == null) {
            return true;
        }

        if (!validateTags(item, semanticTag)) {
            return false;
        }

        List<String> warnings = new ArrayList<>();
        List<String> parentLocations = new ArrayList<>();
        List<String> parentEquipments = new ArrayList<>();

        for (String groupName : item.getGroupNames()) {
            try {
                if (itemRegistry.getItem(groupName) instanceof GroupItem groupItem) {
                    Class<? extends Tag> groupSemanticType = SemanticTags.getSemanticType(groupItem);
                    if (groupSemanticType != null) {
                        if (Equipment.class.isAssignableFrom(groupSemanticType)) {
                            parentEquipments.add(groupName);
                        } else if (Location.class.isAssignableFrom(groupSemanticType)) {
                            parentLocations.add(groupName);
                        }
                    }
                }
            } catch (ItemNotFoundException e) {
                // we don't care about invalid parent groups here
            }
        }

        if (Point.class.isAssignableFrom(semanticTag)) {
            if (parentLocations.size() == 1 && parentEquipments.size() == 1) {
                // This case is allowed: a Point can belong to an Equipment and a Location
                //
                // Case 1:
                // When a location contains multiple equipments -> temperature points,
                // the average of the points will be used in the location's UI.
                // However, when there is a point which is the direct member of the location,
                // it will be used in the location's UI instead of the average.
                // So setting one of the equipment's point as a direct member of the location
                // allows to override the average.
                //
                // Case 2:
                // When a central Equipment e.g. a HVAC contains Points located in multiple locations,
                // e.g. room controls and sensors
                String semanticType = SemanticTags.getSemanticRootName(semanticTag);
                logger.info("Item '{}' ({}) belongs to location {} and equipment {}.", itemName, semanticType,
                        parentLocations, parentEquipments);
            } else {
                if (parentLocations.size() > 1) {
                    warnings.add(String.format(
                            "It belongs to multiple locations %s. It should only belong to one Equipment or one location, preferably not both at the same time.",
                            parentLocations.toString()));
                }
                if (parentEquipments.size() > 1) {
                    warnings.add(String.format(
                            "It belongs to multiple equipments %s. A Point can only belong to at most one Equipment.",
                            parentEquipments.toString()));
                }
            }
        } else if (Equipment.class.isAssignableFrom(semanticTag)) {
            if (parentLocations.size() > 0 && parentEquipments.size() > 0) {
                warnings.add(String.format(
                        "It belongs to location(s) %s and equipment(s) %s. An Equipment can only belong to one Location or another Equipment, but not both.",
                        parentLocations.toString(), parentEquipments.toString()));
            }
            if (parentLocations.size() > 1) {
                warnings.add(String.format(
                        "It belongs to multiple locations %s. An Equipment can only belong to one Location or another Equipment.",
                        parentLocations.toString()));
            }
            if (parentEquipments.size() > 1) {
                warnings.add(String.format(
                        "It belongs to multiple equipments %s. An Equipment can only belong to at most one Equipment.",
                        parentEquipments.toString()));
            }
        } else if (Location.class.isAssignableFrom(semanticTag)) {
            if (!(item instanceof GroupItem)) {
                warnings.add(String.format("It is a %s item, not a group. A location should be a Group Item.",
                        item.getType()));
            }
            if (parentEquipments.size() > 0) {
                warnings.add(String.format(
                        "It belongs to equipment(s) %s. A Location can only belong to another Location, not Equipment.",
                        parentEquipments.toString()));
            }
            if (parentLocations.size() > 1) {
                warnings.add(
                        String.format("It belongs to multiple locations %s. It should only belong to one location.",
                                parentLocations.toString()));
            }
        }

        if (!warnings.isEmpty()) {
            String semanticType = SemanticTags.getSemanticRootName(semanticTag);
            logger.warn("Item '{}' ({}) has invalid semantic structure: {}", itemName, semanticType,
                    String.join("\n", warnings));
            return false;
        }
        return true;
    }

    @Override
    public void added(Item item) {
        checkSemantics(item);
    }

    @Override
    public void removed(Item item) {
        // nothing to do
    }

    @Override
    public void updated(Item oldElement, Item element) {
        checkSemantics(element);
    }
}
