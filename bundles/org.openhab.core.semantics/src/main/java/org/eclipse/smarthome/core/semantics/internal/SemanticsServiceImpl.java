/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.semantics.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemPredicates;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.semantics.SemanticTags;
import org.eclipse.smarthome.core.semantics.SemanticsPredicates;
import org.eclipse.smarthome.core.semantics.SemanticsService;
import org.eclipse.smarthome.core.semantics.model.Equipment;
import org.eclipse.smarthome.core.semantics.model.Location;
import org.eclipse.smarthome.core.semantics.model.Point;
import org.eclipse.smarthome.core.semantics.model.Tag;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The internal implementation of the {@link SemanticsService} interface, which is registered as an OSGi service.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
@Component
public class SemanticsServiceImpl implements SemanticsService {

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    @NonNullByDefault({})
    private ItemRegistry itemRegistry;

    @NonNullByDefault({})
    private MetadataRegistry metadataRegistry;

    void activate(BundleContext context) {
    }

    @Reference
    void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = null;
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
    public @NonNull Set<Item> getItemsInLocation(@NonNull String labelOrSynonym, Locale locale) {
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
