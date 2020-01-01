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
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An {@link ActiveItem} can be modified. It provides methods for adding and
 * removing tags, adding and removing group names and setting a label and a
 * category.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @deprecated This class is not meant as a public API - it should only be used internally from within the framework
 */
@NonNullByDefault
@Deprecated
public interface ActiveItem extends Item {

    /**
     * Sets the label of an item
     *
     * @param label label (can be null)
     */
    void setLabel(@Nullable String label);

    /**
     * Sets the category of the item (can be null)
     *
     * @param category category
     */
    void setCategory(@Nullable String category);

    /**
     * Adds a tag to the item.
     *
     * @param tag a tag that is to be added to item's tags.
     */
    public void addTag(String tag);

    /**
     * Adds tags to the item.
     *
     * @param tags tags that are to be added to item's tags.
     */
    public void addTags(String... tags);

    /**
     * Adds tags to the item.
     *
     * @param tags tags that are to be added to item's tags.
     */
    public void addTags(Collection<String> tags);

    /**
     * Removes a tag from the item.
     *
     * @param tag a tag that is to be removed from item's tags.
     */
    public void removeTag(String tag);

    /**
     * Clears all tags of this item.
     */
    public void removeAllTags();

    /**
     * Removes the according item from a group.
     *
     * @param groupItemName name of the group (must not be null)
     */
    public abstract void removeGroupName(String groupItemName);

    /**
     * Assigns the according item to a group.
     *
     * @param groupItemName name of the group (must not be null)
     */
    public abstract void addGroupName(String groupItemName);

    /**
     * Assigns the according item to the given groups.
     *
     * @param groupItemNames names of the groups (must not be null)
     */
    public abstract void addGroupNames(String... groupItemNames);

    /**
     * Assigns the according item to the given groups.
     *
     * @param groupItemNames names of the groups (must not be null)
     */
    public abstract void addGroupNames(List<String> groupItemNames);
}
