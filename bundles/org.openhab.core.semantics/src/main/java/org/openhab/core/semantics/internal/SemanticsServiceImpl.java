/**
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemPredicates;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.SemanticsPredicates;
import org.openhab.core.semantics.SemanticsService;
import org.openhab.core.semantics.Tag;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The internal implementation of the {@link SemanticsService} interface, which is registered as an OSGi service.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Few methods moved from class SemanticTags in order to use the semantic tag registry
 */
@NonNullByDefault
@Component
public class SemanticsServiceImpl implements SemanticsService {

    private static final String SYNONYMS_NAMESPACE = "synonyms";

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
}
