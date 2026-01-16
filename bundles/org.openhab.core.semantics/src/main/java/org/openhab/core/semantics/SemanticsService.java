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
package org.openhab.core.semantics;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;

/**
 * This interface defines a service, which offers functionality regarding semantic tags.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Few methods moved from class SemanticTags in order to use the semantic tag registry
 */
@NonNullByDefault
public interface SemanticsService {

    /**
     * Retrieves all items that are located in a given location type and which are either classified as Points or
     * Equipments.
     *
     * @param locationType the location type (tag) where items must be located.
     * @return as set of items that are located in a given location type
     */
    Set<Item> getItemsInLocation(Class<? extends Location> locationType);

    /**
     * Retrieves all items that are located in a given location and which are either classified as Points or
     * Equipments. The location is identified by its label or synonym and can reference either a type (e.g. "Bathroom")
     * or a concrete instance (e.g. "Joe's Room").
     *
     * @param labelOrSynonym the label or synonym of the location
     * @param locale the locale used to look up the tag label
     * @return as set of items that are located in the given location(s)
     */
    Set<Item> getItemsInLocation(String labelOrSynonym, Locale locale);

    /**
     * Retrieves the first semantic tag having label matching the given parameter.
     * Case is ignored.
     *
     * @param tagLabel the searched label
     * @param locale the locale to be considered
     * @return the tag class of the first matching semantic tag or null if no matching found
     */
    @Nullable
    Class<? extends Tag> getByLabel(String tagLabel, Locale locale);

    /**
     * Retrieves all semantic tags having label or a synonym matching the given parameter.
     * Case is ignored.
     *
     * @param tagLabelOrSynonym the searched label or synonym
     * @param locale the locale to be considered
     * @return the List of tag classes of all matching semantic tags
     */
    List<Class<? extends Tag>> getByLabelOrSynonym(String tagLabelOrSynonym, Locale locale);

    /**
     * Gets the label and all synonyms of a semantic tag using the given locale.
     *
     * @param tagClass the tag class
     * @param locale the locale to be considered
     * @return the list containing the label and all synonyms of a semantic tag
     */
    List<String> getLabelAndSynonyms(Class<? extends Tag> tagClass, Locale locale);

    /**
     * Verifies the semantics of an item
     *
     * @param item
     * @return list of semantics configuration problems
     */
    default List<ItemSemanticsProblem> getItemSemanticsProblems(Item item) {
        return List.of();
    }
}
