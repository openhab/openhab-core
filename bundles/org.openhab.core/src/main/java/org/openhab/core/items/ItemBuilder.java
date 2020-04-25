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
package org.openhab.core.items;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class allows the easy construction of an {@link Item} using the builder pattern.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ItemBuilder {

    /**
     * Creates an item with the currently configured values.
     *
     * @return an item
     * @throws IllegalStateException in case no item factory can create the given item type
     */
    Item build();

    /**
     * Set the label of the item.
     *
     * @param label the label
     * @return the builder itself
     */
    ItemBuilder withLabel(@Nullable String label);

    /**
     * Set the group membership of the item.
     *
     * @param groups the group names the item belongs to
     * @return the builder itself
     */
    ItemBuilder withGroups(@Nullable Collection<String> groups);

    /**
     * Set the category of the item.
     *
     * @param category the category
     * @return the builder itself
     */
    ItemBuilder withCategory(@Nullable String category);

    /**
     * Set the base item..
     *
     * @param baseItem the base item
     * @return the builder itself
     * @throws IllegalArgumentException in case this is not a group item
     */
    ItemBuilder withBaseItem(@Nullable Item baseItem);

    /**
     * Set the group function
     *
     * @param function the group function
     * @return the builder itself
     * @throws IllegalArgumentException in case this is not a group item
     */
    ItemBuilder withGroupFunction(@Nullable GroupFunction function);

    /**
     * Set the tags
     * 
     * @param tags the tags
     * @return the builder itself
     */
    ItemBuilder withTags(@Nullable Set<String> tags);
}
