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
package org.eclipse.smarthome.core.voice.text;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;

/**
 * This interface is used to find items matching entities extracted from the
 * user natural language query: object - "what" and location - "where".
 *
 * @author Yannick Schaus - Initial contribution
 * @author Laurent Garnier - Moved from HABot + null annotations added
 */
@NonNullByDefault
public interface ItemResolver {

    /**
     * Sets the current locale.
     * The ItemResolver will receive object and location entities in that locale.
     *
     * @param locale
     */
    void setLocale(Locale locale) throws UnsupportedLanguageException;

    /**
     * Resolves items matching the provided object and/or location extracted from user's query using named-entity
     * recognition in the current locale.
     * If a non-null object and a non-null location are provided,
     * items shall match both.
     *
     * @param object the object extracted from the intent (or null)
     * @param location the location extracted from the intent (or null)
     * @return a stream of matching items (groups included)
     */
    Stream<Item> getMatchingItems(@Nullable String object, @Nullable String location);

    /**
     * Gets all named attributes for all items
     *
     * @return a map of the {@link ItemNamedAttribute}s by item
     */
    Map<Item, Set<ItemNamedAttribute>> getAllItemNamedAttributes() throws UnsupportedLanguageException;
}
