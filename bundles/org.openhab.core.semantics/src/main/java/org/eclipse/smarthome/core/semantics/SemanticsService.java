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
package org.eclipse.smarthome.core.semantics;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.semantics.model.Location;

/**
 * This interface defines a service, which offers functionality regarding semantic tags.
 *
 * @author Kai Kreuzer - Initial contribution
 *
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

}
