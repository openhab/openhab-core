/*
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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;

/**
 * The {@link ProviderItemRegistryDelegate} is wrapping a {@link ItemRegistry} to provide a comfortable way to provide
 * items from scripts without worrying about the need to remove items again when the script is unloaded.
 * Nonetheless, using the {@link #addPermanent(Item)} method it is still possible to add items permanently.
 * <p>
 * Use a new instance of this class for each {@link javax.script.ScriptEngine}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ProviderItemRegistryDelegate implements ItemRegistry {
    private final ItemRegistry itemRegistry;

    private final Set<String> items = new HashSet<>();

    private final ScriptedItemProvider itemProvider;

    public ProviderItemRegistryDelegate(ItemRegistry itemRegistry, ScriptedItemProvider itemProvider) {
        this.itemRegistry = itemRegistry;
        this.itemProvider = itemProvider;
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Item> listener) {
        itemRegistry.addRegistryChangeListener(listener);
    }

    @Override
    public Collection<Item> getAll() {
        return itemRegistry.getAll();
    }

    @Override
    public Stream<Item> stream() {
        return itemRegistry.stream();
    }

    @Override
    public @Nullable Item get(String key) {
        return itemRegistry.get(key);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Item> listener) {
        itemRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public Item add(Item element) {
        String itemName = element.getName();
        // Check for item already existing here because the item might exist in a different provider, so we need to
        // check the registry and not only the provider itself
        if (get(itemName) != null) {
            throw new IllegalArgumentException(
                    "Cannot add item, because an item with same name (" + itemName + ") already exists.");
        }

        itemProvider.add(element);
        items.add(itemName);

        return element;
    }

    /**
     * Add an item permanently to the registry.
     * This item will be kept in the registry even if the script is unloaded.
     * 
     * @param element the item to be added (must not be null)
     * @return the added item
     */
    public Item addPermanent(Item element) {
        return itemRegistry.add(element);
    }

    @Override
    public @Nullable Item update(Item element) {
        if (items.contains(element.getName())) {
            return itemProvider.update(element);
        }
        return itemRegistry.update(element);
    }

    @Override
    public @Nullable Item remove(String key) {
        if (items.remove(key)) {
            return itemProvider.remove(key);
        }

        return itemRegistry.remove(key);
    }

    @Override
    public Item getItem(String name) throws ItemNotFoundException {
        return itemRegistry.getItem(name);
    }

    @Override
    public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException {
        return itemRegistry.getItemByPattern(name);
    }

    @Override
    public Collection<Item> getItems() {
        return itemRegistry.getItems();
    }

    @Override
    public Collection<Item> getItemsOfType(String type) {
        return itemRegistry.getItemsOfType(type);
    }

    @Override
    public Collection<Item> getItems(String pattern) {
        return itemRegistry.getItems(pattern);
    }

    @Override
    public Collection<Item> getItemsByTag(String... tags) {
        return itemRegistry.getItemsByTag(tags);
    }

    @Override
    public Collection<Item> getItemsByTagAndType(String type, String... tags) {
        return itemRegistry.getItemsByTagAndType(type, tags);
    }

    @Override
    public <T extends Item> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags) {
        return itemRegistry.getItemsByTag(typeFilter, tags);
    }

    @Override
    public @Nullable Item remove(String itemName, boolean recursive) {
        Item item = get(itemName);
        if (recursive && item instanceof GroupItem groupItem) {
            for (String member : getMemberNamesRecursively(groupItem, getAll())) {
                remove(member);
            }
        }
        if (item != null) {
            remove(item.getName());
            return item;
        } else {
            return null;
        }
    }

    /**
     * Removes all items that are provided by this script.
     * To be called when the script is unloaded or reloaded.
     */
    public void removeAllAddedByScript() {
        for (String item : items) {
            itemProvider.remove(item);
        }
        items.clear();
    }

    private List<String> getMemberNamesRecursively(GroupItem groupItem, Collection<Item> allItems) {
        List<String> memberNames = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getGroupNames().contains(groupItem.getName())) {
                memberNames.add(item.getName());
                if (item instanceof GroupItem groupItem1) {
                    memberNames.addAll(getMemberNamesRecursively(groupItem1, allItems));
                }
            }
        }
        return memberNames;
    }
}
