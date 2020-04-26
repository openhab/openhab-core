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
package org.openhab.core.internal.items;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ActiveItem;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilder;
import org.openhab.core.items.ItemFactory;

/**
 * The {@link ItemBuilder} implementation.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ItemBuilderImpl implements ItemBuilder {

    private final Set<ItemFactory> itemFactories;
    private final String itemType;
    private final String name;

    private @Nullable String category;
    private List<String> groups = Collections.emptyList();
    private @Nullable String label;
    private @Nullable Item baseItem;
    private @Nullable GroupFunction groupFunction;
    private Set<String> tags = Collections.emptySet();

    public ItemBuilderImpl(Set<ItemFactory> itemFactories, Item item) {
        this.itemFactories = itemFactories;
        this.itemType = item.getType();
        this.name = item.getName();
        this.category = item.getCategory();
        this.groups = item.getGroupNames();
        this.label = item.getLabel();
        this.tags = item.getTags();
        if (item instanceof GroupItem) {
            this.baseItem = ((GroupItem) item).getBaseItem();
            this.groupFunction = ((GroupItem) item).getFunction();
        }
    }

    public ItemBuilderImpl(Set<ItemFactory> itemFactories, String itemType, String itemName) {
        this.itemFactories = itemFactories;
        this.itemType = itemType;
        this.name = itemName;
    }

    @Override
    public ItemBuilder withLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    @Override
    public ItemBuilder withGroups(@Nullable Collection<String> groups) {
        if (groups != null) {
            this.groups = new LinkedList<>(groups);
        } else {
            this.groups = Collections.emptyList();
        }
        return this;
    }

    @Override
    public ItemBuilder withCategory(@Nullable String category) {
        this.category = category;
        return this;
    }

    @Override
    public ItemBuilder withBaseItem(@Nullable Item item) {
        if (item != null && !GroupItem.TYPE.equals(itemType)) {
            throw new IllegalArgumentException("Only group items can have a base item");
        }
        baseItem = item;
        return this;
    }

    @Override
    public ItemBuilder withGroupFunction(@Nullable GroupFunction function) {
        if (function != null && !GroupItem.TYPE.equals(itemType)) {
            throw new IllegalArgumentException("Only group items can have a base item");
        }
        groupFunction = function;
        return this;
    }

    @Override
    public ItemBuilder withTags(@Nullable Set<String> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : Collections.emptySet();
        return this;
    }

    @Override
    public Item build() {
        Item item = null;
        if (GroupItem.TYPE.equals(itemType)) {
            item = new GroupItem(name, baseItem, groupFunction);
        } else {
            for (ItemFactory itemFactory : itemFactories) {
                item = itemFactory.createItem(itemType, name);
                if (item != null) {
                    break;
                }
            }
        }
        if (item == null) {
            throw new IllegalStateException("No item factory could handle type " + itemType);
        }
        if (item instanceof ActiveItem) {
            ActiveItem activeItem = (ActiveItem) item;
            activeItem.setLabel(label);
            activeItem.setCategory(category);
            activeItem.addGroupNames(groups);
            activeItem.addTags(tags);
        }
        return item;
    }
}
