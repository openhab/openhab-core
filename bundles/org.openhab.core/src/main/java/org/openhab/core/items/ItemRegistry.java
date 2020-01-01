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
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.internal.items.ItemBuilderImpl;
import org.openhab.core.library.CoreItemFactory;
import org.slf4j.LoggerFactory;

/**
 * The ItemRegistry is the central place, where items are kept in memory and their state
 * is permanently tracked. So any code that requires the current state of items should use
 * this service (instead of trying to keep their own local copy of the items).
 *
 * Items are registered by {@link ItemProvider}s, which can provision them from any source
 * they like and also dynamically remove or add items.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface ItemRegistry extends Registry<Item, String> {

    /**
     * This method retrieves a single item from the registry.
     *
     * @param name the item name
     * @return the uniquely identified item
     * @throws ItemNotFoundException if no item matches the input
     */
    public Item getItem(String name) throws ItemNotFoundException;

    /**
     * This method retrieves a single item from the registry.
     * Search patterns and shortened versions are supported, if they uniquely identify an item
     *
     * @param name the item name, a part of the item name or a search pattern
     * @return the uniquely identified item
     * @throws ItemNotFoundException if no item matches the input
     * @throws ItemNotUniqueException if multiply items match the input
     */
    public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException;

    /**
     * This method retrieves all items that are currently available in the registry
     *
     * @return a collection of all available items
     */
    public Collection<Item> getItems();

    /**
     * This method retrieves all items with the given type
     *
     * @param type item type as defined by {@link ItemFactory}s
     * @return a collection of all items of the given type
     */
    public Collection<Item> getItemsOfType(String type);

    /**
     * This method retrieves all items that match a given search pattern
     *
     * @return a collection of all items matching the search pattern
     */
    public Collection<Item> getItems(String pattern);

    /**
     * Returns list of items which contains all of the given tags.
     *
     * @param tags array of tags to be present on the returned items.
     * @return list of items which contains all of the given tags.
     */
    public Collection<Item> getItemsByTag(String... tags);

    /**
     * Returns list of items with a certain type containing all of the given tags.
     *
     * @param type item type as defined by {@link ItemFactory}s
     * @param tags array of tags to be present on the returned items.
     * @return list of items which contains all of the given tags.
     */
    public Collection<Item> getItemsByTagAndType(String type, String... tags);

    /**
     * Returns list of items which contains all of the given tags.
     *
     * @param typeFilter subclass of {@link GenericItem} to filter the resulting list for.
     * @param tags array of tags to be present on the returned items.
     * @return list of items which contains all of the given tags, which is
     *         filtered by the given type filter.
     */
    public <T extends Item> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags);

    /**
     * @see ManagedItemProvider#remove(String, boolean)
     */
    public @Nullable Item remove(String itemName, boolean recursive);

    /**
     * Add a hook to be informed before adding/after removing items.
     *
     * @param hook
     */
    void addRegistryHook(RegistryHook<Item> hook);

    /**
     * Remove the hook again.
     *
     * @param hook
     */
    void removeRegistryHook(RegistryHook<Item> hook);

    /**
     * Create a new {@link ItemBuilder}, which is initialized by the given item.
     *
     * @param item the template to initialize the builder with
     * @return an ItemBuilder instance
     *
     * @deprecated Use the {@link ItemBuilderFactory} service instead.
     */
    @Deprecated
    default ItemBuilder newItemBuilder(Item item) {
        LoggerFactory.getLogger(ItemRegistry.class)
                .warn("Deprecation: You are using a deprecated API. Please use the ItemBuilder OSGi service instead.");
        return new ItemBuilderImpl(Collections.singleton(new CoreItemFactory()), item);
    }

    /**
     * Create a new {@link ItemBuilder}, which is initialized by the given item.
     *
     * @param itemType the item type to create
     * @param itemName the name of the item to create
     * @return an ItemBuilder instance
     *
     * @deprecated Use the {@link ItemBuilderFactory} service instead.
     */
    @Deprecated
    default ItemBuilder newItemBuilder(String itemType, String itemName) {
        LoggerFactory.getLogger(ItemRegistry.class)
                .warn("Deprecation: You are using a deprecated API. Please use the ItemBuilder OSGi service instead.");
        return new ItemBuilderImpl(Collections.singleton(new CoreItemFactory()), itemType, itemName);
    }

}
