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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemPredicates;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.SemanticsPredicates;
import org.openhab.core.semantics.SemanticsService;
import org.openhab.core.semantics.model.Equipment;
import org.openhab.core.semantics.model.Location;
import org.openhab.core.semantics.model.Point;
import org.openhab.core.semantics.model.Tag;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The internal implementation of the {@link SemanticsService} interface, which is registered as an OSGi service.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component
public class SemanticsServiceImpl implements SemanticsService {

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;

    @Activate
    public SemanticsServiceImpl(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    public Set<Item> getItemsInLocation(Class<? extends Location> locationType) {
        Set<Item> items = new HashSet<>();
        Set<Item> locationItems = itemRegistry.stream().filter(SemanticsPredicates.isA(locationType))
                .collect(Collectors.toSet());
        for (Item locationItem : locationItems) {
            if (locationItem instanceof GroupItem) {
                GroupItem gItem = (GroupItem) locationItem;
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
        List<Class<? extends Tag>> tagList = SemanticTags.getByLabelOrSynonym(labelOrSynonym, locale);
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
                if (locationItem instanceof GroupItem) {
                    GroupItem gItem = (GroupItem) locationItem;
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
}
