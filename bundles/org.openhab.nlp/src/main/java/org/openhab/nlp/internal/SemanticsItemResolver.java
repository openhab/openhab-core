/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.nlp.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemPredicates;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.semantics.SemanticTags;
import org.eclipse.smarthome.core.semantics.SemanticsPredicates;
import org.eclipse.smarthome.core.semantics.SemanticsService;
import org.eclipse.smarthome.core.semantics.model.Location;
import org.eclipse.smarthome.core.semantics.model.Property;
import org.eclipse.smarthome.core.semantics.model.Tag;
import org.openhab.nlp.ItemNamedAttribute;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.UnsupportedLanguageException;
import org.openhab.nlp.ItemNamedAttribute.AttributeSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class resolves items and provide name samples using Eclipse SmartHome's semantics feature.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = ItemResolver.class, immediate = true)
public class SemanticsItemResolver implements ItemResolver {

    private static final String SYNONYMS_NAMESPACE = "synonyms";

    private ItemRegistry itemRegistry;
    private MetadataRegistry metadataRegistry;
    private SemanticsService semanticsService;
    private Locale currentLocale = null;

    @Override
    public void setLocale(Locale locale) {
        if (locale == null || locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            this.currentLocale = Locale.ROOT;
        } else {
            this.currentLocale = locale;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Item> getMatchingItems(String object, String location) {
        Stream<Item> items;
        if (location != null) {
            items = semanticsService.getItemsInLocation(location, currentLocale).stream();
        } else {
            items = new HashSet<Item>(itemRegistry.getAll()).stream();
        }

        if (object != null) {
            List<Class<? extends Tag>> semanticTagTypes = SemanticTags.getByLabelOrSynonym(object, currentLocale);
            if (!semanticTagTypes.isEmpty()
                    && semanticTagTypes.stream().noneMatch(t -> Location.class.isAssignableFrom(t))) {
                Predicate<Item> tagsPredicate = null;
                for (Class<? extends Tag> tag : semanticTagTypes) {
                    Predicate<Item> tagPredicate = Property.class.isAssignableFrom(tag)
                            ? SemanticsPredicates.relatesTo((Class<? extends Property>) tag)
                            : SemanticsPredicates.isA(tag);

                    tagsPredicate = (tagsPredicate == null) ? tagPredicate : tagsPredicate.or(tagPredicate);
                }
                items = items.filter(ItemPredicates.hasLabel(object).or(hasSynonym(object)).or(tagsPredicate));
            } else {
                return items.filter(ItemPredicates.hasLabel(object).or(hasSynonym(object))
                        .and(SemanticsPredicates.isLocation().negate()));
            }
        }

        return items;
    }

    @Override
    public Map<Item, Set<ItemNamedAttribute>> getAllItemNamedAttributes() throws UnsupportedLanguageException {
        if (currentLocale == null) {
            throw new UnsupportedLanguageException(currentLocale);
        }

        Map<Item, Set<ItemNamedAttribute>> attributes = new HashMap<Item, Set<ItemNamedAttribute>>();

        for (Item item : itemRegistry.getAll()) {
            Class<? extends Tag> semanticType = SemanticTags.getSemanticType(item);
            if (semanticType != null) {
                Set<ItemNamedAttribute> itemAttributes = new HashSet<ItemNamedAttribute>();

                attributes.put(item, new HashSet<ItemNamedAttribute>());
                String attributeType = (Location.class.isAssignableFrom(semanticType)) ? "location" : "object";

                // Add the item's label
                itemAttributes.add(new ItemNamedAttribute(attributeType, item.getLabel(), AttributeSource.LABEL));

                // Add the primary type's label and synonyms
                for (String tagLabel : SemanticTags.getLabelAndSynonyms(semanticType, currentLocale)) {
                    itemAttributes.add(new ItemNamedAttribute(attributeType, tagLabel, AttributeSource.TAG));
                }

                // Add the related property's label and synonyms
                Class<? extends Property> relatedProperty = SemanticTags.getProperty(item);
                if (relatedProperty != null) {
                    for (String propertyLabel : SemanticTags.getLabelAndSynonyms(relatedProperty, currentLocale)) {
                        itemAttributes.add(new ItemNamedAttribute("object", propertyLabel, AttributeSource.TAG));
                    }
                }

                // Add the user-specified synonyms (in the "synonyms" item metadata namespace)
                MetadataKey key = new MetadataKey(SYNONYMS_NAMESPACE, item.getName());
                Metadata md = metadataRegistry.get(key);
                if (md != null) {
                    String[] synonyms = md.getValue().split(",");
                    for (String synonym : synonyms) {
                        itemAttributes
                                .add(new ItemNamedAttribute(attributeType, synonym.trim(), AttributeSource.METADATA));
                    }
                }

                attributes.put(item, itemAttributes);
            }
        }

        return attributes;
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

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        if (this.itemRegistry == null) {
            this.itemRegistry = itemRegistry;
        }
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        if (itemRegistry == this.itemRegistry) {
            this.itemRegistry = null;
        }
    }

    @Reference
    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        if (this.metadataRegistry == null) {
            this.metadataRegistry = metadataRegistry;
        }
    }

    protected void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        if (metadataRegistry == this.metadataRegistry) {
            this.metadataRegistry = null;
        }
    }

    @Reference
    public void setSemanticsService(SemanticsService semanticsService) {
        this.semanticsService = semanticsService;
    }

    public void unsetSemanticsService(SemanticsService semanticsService) {
        this.semanticsService = null;
    }

}
