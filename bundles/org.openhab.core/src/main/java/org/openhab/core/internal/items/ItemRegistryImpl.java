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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.RegistryHook;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.service.CommandDescriptionService;
import org.openhab.core.service.StateDescriptionService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main implementing class of the {@link ItemRegistry} interface. It
 * keeps track of all declared items of all item providers and keeps their
 * current state in memory. This is the central point where states are kept and
 * thus it is a core part for all stateful services.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Bußweiler - Migration to new event mechanism
 */
@NonNullByDefault
@Component(immediate = true)
public class ItemRegistryImpl extends AbstractRegistry<Item, String, ItemProvider> implements ItemRegistry {

    private final Logger logger = LoggerFactory.getLogger(ItemRegistryImpl.class);

    private final List<RegistryHook<Item>> registryHooks = new CopyOnWriteArrayList<>();
    private @Nullable StateDescriptionService stateDescriptionService;
    private @Nullable CommandDescriptionService commandDescriptionService;
    private final MetadataRegistry metadataRegistry;
    private @Nullable UnitProvider unitProvider;
    private @Nullable ItemStateConverter itemStateConverter;

    @Activate
    public ItemRegistryImpl(final @Reference MetadataRegistry metadataRegistry) {
        super(ItemProvider.class);
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    public Item getItem(String name) throws ItemNotFoundException {
        final Item item = get(name);
        if (item == null) {
            throw new ItemNotFoundException(name);
        } else {
            return item;
        }
    }

    @Override
    public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException {
        Collection<Item> items = getItems(name);

        if (items.isEmpty()) {
            throw new ItemNotFoundException(name);
        }

        if (items.size() > 1) {
            throw new ItemNotUniqueException(name, items);
        }

        Item item = items.iterator().next();

        if (item == null) {
            throw new ItemNotFoundException(name);
        } else {
            return item;
        }
    }

    @Override
    public Collection<Item> getItems() {
        return getAll();
    }

    @Override
    public Collection<Item> getItemsOfType(String type) {
        Collection<Item> matchedItems = new ArrayList<>();

        for (Item item : getItems()) {
            if (item.getType().equals(type)) {
                matchedItems.add(item);
            }
        }

        return matchedItems;
    }

    @Override
    public Collection<Item> getItems(String pattern) {
        String regex = pattern.replace("?", ".?").replace("*", ".*?");
        Collection<Item> matchedItems = new ArrayList<>();

        for (Item item : getItems()) {
            if (item.getName().matches(regex)) {
                matchedItems.add(item);
            }
        }

        return matchedItems;
    }

    private void addToGroupItems(Item item, List<String> groupItemNames) {
        for (String groupName : groupItemNames) {
            if (groupName != null) {
                try {
                    Item groupItem = getItem(groupName);
                    if (groupItem instanceof GroupItem) {
                        ((GroupItem) groupItem).addMember(item);
                    }
                } catch (ItemNotFoundException e) {
                    // the group might not yet be registered, let's ignore this
                }
            }
        }
    }

    private void replaceInGroupItems(Item oldItem, Item newItem, List<String> groupItemNames) {
        for (String groupName : groupItemNames) {
            if (groupName != null) {
                try {
                    Item groupItem = getItem(groupName);
                    if (groupItem instanceof GroupItem) {
                        ((GroupItem) groupItem).replaceMember(oldItem, newItem);
                    }
                } catch (ItemNotFoundException e) {
                    // the group might not yet be registered, let's ignore this
                }
            }
        }
    }

    /**
     * An item should be initialized, which means that the event publisher is
     * injected and its implementation is notified that it has just been
     * created, so it can perform any task it needs to do after its creation.
     *
     * @param item the item to initialize
     * @throws IllegalArgumentException if the item has no valid name
     */
    private void initializeItem(Item item) throws IllegalArgumentException {
        ItemUtil.assertValidItemName(item.getName());

        injectServices(item);

        if (item instanceof GroupItem) {
            // fill group with its members
            addMembersToGroupItem((GroupItem) item);
        }

        // add the item to all relevant groups
        addToGroupItems(item, item.getGroupNames());
    }

    private void injectServices(Item item) {
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.setEventPublisher(getEventPublisher());
            genericItem.setStateDescriptionService(stateDescriptionService);
            genericItem.setCommandDescriptionService(commandDescriptionService);
            genericItem.setUnitProvider(unitProvider);
            genericItem.setItemStateConverter(itemStateConverter);
        }
    }

    private void addMembersToGroupItem(GroupItem groupItem) {
        for (Item i : getItems()) {
            if (i.getGroupNames().contains(groupItem.getName())) {
                groupItem.addMember(i);
            }
        }
    }

    private void removeFromGroupItems(Item item, List<String> groupItemNames) {
        for (String groupName : groupItemNames) {
            if (groupName != null) {
                try {
                    Item groupItem = getItem(groupName);
                    if (groupItem instanceof GroupItem) {
                        ((GroupItem) groupItem).removeMember(item);
                    }
                } catch (ItemNotFoundException e) {
                    // the group might not yet be registered, let's ignore this
                }
            }
        }
    }

    @Override
    protected void onAddElement(Item element) throws IllegalArgumentException {
        initializeItem(element);
    }

    @Override
    protected void onRemoveElement(Item element) {
        if (element instanceof GenericItem) {
            ((GenericItem) element).dispose();
        }
        removeFromGroupItems(element, element.getGroupNames());
    }

    @Override
    protected void beforeUpdateElement(Item existingElement) {
        if (existingElement instanceof GenericItem) {
            ((GenericItem) existingElement).dispose();
        }
    }

    @Override
    protected void onUpdateElement(Item oldItem, Item item) {
        // don't use #initialize and retain order of items in groups:
        List<String> oldNames = oldItem.getGroupNames();
        List<String> newNames = item.getGroupNames();
        List<String> commonNames = oldNames.stream().filter(name -> newNames.contains(name)).collect(toList());

        removeFromGroupItems(oldItem, oldNames.stream().filter(name -> !commonNames.contains(name)).collect(toList()));
        replaceInGroupItems(oldItem, item, commonNames);
        addToGroupItems(item, newNames.stream().filter(name -> !commonNames.contains(name)).collect(toList()));
        if (item instanceof GroupItem) {
            addMembersToGroupItem((GroupItem) item);
        }
        injectServices(item);
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
        for (Item item : getItems()) {
            ((GenericItem) item).setEventPublisher(eventPublisher);
        }
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
        for (Item item : getItems()) {
            ((GenericItem) item).setEventPublisher(null);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setUnitProvider(UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
        for (Item item : getItems()) {
            ((GenericItem) item).setUnitProvider(unitProvider);
        }
    }

    protected void unsetUnitProvider(UnitProvider unitProvider) {
        this.unitProvider = null;
        for (Item item : getItems()) {
            ((GenericItem) item).setUnitProvider(null);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemStateConverter(ItemStateConverter itemStateConverter) {
        this.itemStateConverter = itemStateConverter;
        for (Item item : getItems()) {
            ((GenericItem) item).setItemStateConverter(itemStateConverter);
        }
    }

    protected void unsetItemStateConverter(ItemStateConverter itemStateConverter) {
        this.itemStateConverter = null;
        for (Item item : getItems()) {
            ((GenericItem) item).setItemStateConverter(null);
        }
    }

    @Override
    public Collection<Item> getItemsByTag(String... tags) {
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : getItems()) {
            if (itemHasTags(item, tags)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    private boolean itemHasTags(Item item, String... tags) {
        for (String tag : tags) {
            if (!item.hasTag(tag)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Item> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags) {
        Collection<T> filteredItems = new ArrayList<>();

        Collection<Item> items = getItemsByTag(tags);
        for (Item item : items) {
            if (typeFilter.isInstance(item)) {
                filteredItems.add((T) item);
            }
        }
        return filteredItems;
    }

    @Override
    public Collection<Item> getItemsByTagAndType(String type, String... tags) {
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : getItemsOfType(type)) {
            if (itemHasTags(item, tags)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    @Override
    public @Nullable Item remove(String itemName, boolean recursive) {
        return ((ManagedItemProvider) getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"))).remove(itemName,
                        recursive);
    }

    @Override
    protected void notifyListenersAboutAddedElement(Item element) {
        super.notifyListenersAboutAddedElement(element);
        postEvent(ItemEventFactory.createAddedEvent(element));
    }

    @Override
    protected void notifyListenersAboutRemovedElement(Item element) {
        super.notifyListenersAboutRemovedElement(element);
        postEvent(ItemEventFactory.createRemovedEvent(element));
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(Item oldElement, Item element) {
        super.notifyListenersAboutUpdatedElement(oldElement, element);
        postEvent(ItemEventFactory.createUpdateEvent(element, oldElement));
    }

    @Override
    public void added(Provider<Item> provider, Item element) {
        for (RegistryHook<Item> registryHook : registryHooks) {
            registryHook.beforeAdding(element);
        }
        super.added(provider, element);
    }

    @Override
    protected void addProvider(Provider<Item> provider) {
        for (Item element : provider.getAll()) {
            for (RegistryHook<Item> registryHook : registryHooks) {
                registryHook.beforeAdding(element);
            }
        }
        super.addProvider(provider);
    }

    @Override
    public void removed(Provider<Item> provider, Item element) {
        super.removed(provider, element);
        for (RegistryHook<Item> registryHook : registryHooks) {
            registryHook.afterRemoving(element);
        }
        if (provider instanceof ManagedItemProvider) {
            // remove our metadata for that item
            logger.debug("Item {} was removed, trying to clean up corresponding metadata", element.getUID());
            metadataRegistry.removeItemMetadata(element.getName());
        }
    }

    @Override
    protected void removeProvider(Provider<Item> provider) {
        super.removeProvider(provider);
        for (Item element : provider.getAll()) {
            for (RegistryHook<Item> registryHook : registryHooks) {
                registryHook.afterRemoving(element);
            }
        }
    }

    @Override
    public void addRegistryHook(RegistryHook<Item> hook) {
        registryHooks.add(hook);
    }

    @Override
    public void removeRegistryHook(RegistryHook<Item> hook) {
        registryHooks.remove(hook);
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        super.activate(componentContext.getBundleContext());
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setStateDescriptionService(StateDescriptionService stateDescriptionService) {
        this.stateDescriptionService = stateDescriptionService;

        for (Item item : getItems()) {
            ((GenericItem) item).setStateDescriptionService(stateDescriptionService);
        }
    }

    protected void unsetStateDescriptionService(StateDescriptionService stateDescriptionService) {
        this.stateDescriptionService = null;

        for (Item item : getItems()) {
            ((GenericItem) item).setStateDescriptionService(null);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setCommandDescriptionService(CommandDescriptionService commandDescriptionService) {
        this.commandDescriptionService = commandDescriptionService;

        for (Item item : getItems()) {
            ((GenericItem) item).setCommandDescriptionService(commandDescriptionService);
        }
    }

    public void unsetCommandDescriptionService(CommandDescriptionService commandDescriptionService) {
        this.commandDescriptionService = null;

        for (Item item : getItems()) {
            ((GenericItem) item).setCommandDescriptionService(null);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedItemProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedItemProvider provider) {
        super.unsetManagedProvider(provider);
    }

}
