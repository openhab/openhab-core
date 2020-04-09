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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.items.ManagedItemProvider.PersistedItem;
import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.InstanceCreator;

/**
 * {@link ManagedItemProvider} is an OSGi service, that allows to add or remove
 * items at runtime by calling {@link ManagedItemProvider#addItem(Item)} or {@link ManagedItemProvider#removeItem(Item)}
 * . An added item is automatically
 * exposed to the {@link ItemRegistry}. Persistence of added Items is handled by
 * a {@link StorageService}. Items are being restored using the given {@link ItemFactory}s.
 *
 * @author Dennis Nobel - Initial contribution, added support for GroupItems
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - improved return values
 * @author Alex Tugarev - added tags
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemProvider.class, ManagedItemProvider.class })
public class ManagedItemProvider extends AbstractManagedProvider<Item, String, PersistedItem> implements ItemProvider {

    public static class PersistedItem {

        public PersistedItem(String itemType) {
            this.itemType = itemType;
        }

        public @Nullable String baseItemType;
        public @Nullable List<String> groupNames;
        public String itemType;
        public @Nullable Set<String> tags;
        public @Nullable String label;
        public @Nullable String category;
        public @Nullable String functionName;
        public @Nullable List<String> functionParams;
        public @Nullable String dimension;
    }

    public static class PersistedItemInstanceCreator implements InstanceCreator<PersistedItem> {

        @Override
        public PersistedItem createInstance(@NonNullByDefault({}) Type type) {
            return new PersistedItem("");
        }
    }

    private final Logger logger = LoggerFactory.getLogger(ManagedItemProvider.class);

    private final Collection<ItemFactory> itemFactories = new CopyOnWriteArrayList<>();
    private final Map<String, PersistedItem> failedToCreate = new ConcurrentHashMap<>();

    @Activate
    public ManagedItemProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    /**
     * Removes an item and it´s member if recursive flag is set to true.
     *
     * @param itemName item name to remove
     * @param recursive if set to true all members of the item will be removed, too.
     * @return removed item or null if no item with that name exists
     */
    public @Nullable Item remove(String itemName, boolean recursive) {
        Item item = get(itemName);
        if (recursive && item instanceof GroupItem) {
            for (String member : getMemberNamesRecursively((GroupItem) item, getAll())) {
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

    private List<String> getMemberNamesRecursively(GroupItem groupItem, Collection<Item> allItems) {
        List<String> memberNames = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getGroupNames().contains(groupItem.getName())) {
                memberNames.add(item.getName());
                if (item instanceof GroupItem) {
                    memberNames.addAll(getMemberNamesRecursively((GroupItem) item, allItems));
                }
            }
        }
        return memberNames;
    }

    private @Nullable Item createItem(String itemType, String itemName) {
        for (ItemFactory factory : itemFactories) {
            Item item = factory.createItem(itemType, itemName);
            if (item != null) {
                return item;
            }
        }

        logger.debug("Couldn't find ItemFactory for item '{}' of type '{}'", itemName, itemType);

        return null;
    }

    /**
     * Translates the Items class simple name into a type name understandable by
     * the {@link ItemFactory}s.
     *
     * @param item the Item to translate the name
     * @return the translated ItemTypeName understandable by the {@link ItemFactory}s
     */
    private String toItemFactoryName(Item item) {
        return item.getType();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);

        if (!failedToCreate.isEmpty()) {
            // retry failed creation attempts
            Iterator<Entry<String, PersistedItem>> iterator = failedToCreate.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, PersistedItem> entry = iterator.next();
                String itemName = entry.getKey();
                PersistedItem persistedItem = entry.getValue();
                Item item = itemFactory.createItem(persistedItem.itemType, itemName);
                if (item != null && item instanceof ActiveItem) {
                    iterator.remove();
                    configureItem(persistedItem, (ActiveItem) item);
                    notifyListenersAboutAddedElement(item);
                } else {
                    logger.debug("The added item factory '{}' still could not instantiate item '{}'.", itemFactory,
                            itemName);
                }
            }

            if (failedToCreate.isEmpty()) {
                logger.info("Finished loading the items which could not have been created before.");
            }
        }
    }

    @Override
    protected String getStorageName() {
        return Item.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
    }

    @Override
    protected @Nullable Item toElement(String itemName, PersistedItem persistedItem) {
        Item item = null;

        if (GroupItem.TYPE.equals(persistedItem.itemType)) {
            String baseItemType = persistedItem.baseItemType;
            if (baseItemType != null) {
                Item baseItem = createItem(baseItemType, itemName);
                if (persistedItem.functionName != null) {
                    GroupFunction function = getGroupFunction(persistedItem, baseItem);
                    item = new GroupItem(itemName, baseItem, function);
                } else {
                    item = new GroupItem(itemName, baseItem, new GroupFunction.Equality());
                }
            } else {
                item = new GroupItem(itemName);
            }
        } else {
            item = createItem(persistedItem.itemType, itemName);
        }

        if (item != null && item instanceof ActiveItem) {
            configureItem(persistedItem, (ActiveItem) item);
        }

        if (item == null) {
            failedToCreate.put(itemName, persistedItem);
            logger.debug("Couldn't restore item '{}' of type '{}' ~ there is no appropriate ItemFactory available.",
                    itemName, persistedItem.itemType);
        }

        return item;
    }

    private GroupFunction getGroupFunction(PersistedItem persistedItem, @Nullable Item baseItem) {
        GroupFunctionDTO functionDTO = new GroupFunctionDTO();
        functionDTO.name = persistedItem.functionName;
        if (persistedItem.functionParams != null) {
            functionDTO.params = persistedItem.functionParams.toArray(new String[persistedItem.functionParams.size()]);
        }
        return ItemDTOMapper.mapFunction(baseItem, functionDTO);
    }

    private void configureItem(PersistedItem persistedItem, ActiveItem item) {
        List<String> groupNames = persistedItem.groupNames;
        if (groupNames != null) {
            for (String groupName : groupNames) {
                item.addGroupName(groupName);
            }
        }

        Set<String> tags = persistedItem.tags;
        if (tags != null) {
            for (String tag : tags) {
                item.addTag(tag);
            }
        }

        item.setLabel(persistedItem.label);
        item.setCategory(persistedItem.category);
    }

    @Override
    protected PersistedItem toPersistableElement(Item item) {
        PersistedItem persistedItem = new PersistedItem(
                item instanceof GroupItem ? GroupItem.TYPE : toItemFactoryName(item));

        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            String baseItemType = null;
            Item baseItem = groupItem.getBaseItem();
            if (baseItem != null) {
                baseItemType = toItemFactoryName(baseItem);
            }
            persistedItem.baseItemType = baseItemType;

            addFunctionToPersisedItem(persistedItem, groupItem);
        }

        persistedItem.label = item.getLabel();
        persistedItem.groupNames = new ArrayList<>(item.getGroupNames());
        persistedItem.tags = new HashSet<>(item.getTags());
        persistedItem.category = item.getCategory();

        return persistedItem;
    }

    private void addFunctionToPersisedItem(PersistedItem persistedItem, GroupItem groupItem) {
        if (groupItem.getFunction() != null) {
            GroupFunctionDTO functionDTO = ItemDTOMapper.mapFunction(groupItem.getFunction());
            if (functionDTO != null) {
                persistedItem.functionName = functionDTO.name;
                if (functionDTO.params != null) {
                    persistedItem.functionParams = Arrays.asList(functionDTO.params);
                }
            }
        }
    }
}
