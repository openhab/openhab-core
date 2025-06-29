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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemUtil;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ItemProvider} keeps items provided by scripts during runtime.
 * This ensures that items are not kept on reboot, but have to be provided by the scripts again.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemProvider.class, ScriptedItemProvider.class })
public class ScriptedItemProvider extends AbstractProvider<Item>
        implements ItemProvider, ManagedProvider<Item, String> {
    private final Logger logger = LoggerFactory.getLogger(ScriptedItemProvider.class);
    private final Map<String, Item> items = new HashMap<>();

    @Override
    public Collection<Item> getAll() {
        return items.values();
    }

    @Override
    public @Nullable Item get(String itemName) {
        return items.get(itemName);
    }

    @Override
    public void add(Item item) {
        if (!ItemUtil.isValidItemName(item.getName())) {
            throw new IllegalArgumentException("The item name '" + item.getName() + "' is invalid.");
        }
        if (items.get(item.getName()) != null) {
            throw new IllegalArgumentException(
                    "Cannot add item, because an item with same name (" + item.getName() + ") already exists.");
        }
        items.put(item.getName(), item);

        notifyListenersAboutAddedElement(item);
    }

    @Override
    public @Nullable Item update(Item item) {
        Item oldItem = items.get(item.getName());
        if (oldItem != null) {
            items.put(item.getName(), item);
            notifyListenersAboutUpdatedElement(oldItem, item);
        } else {
            logger.warn("Could not update item with name '{}', because it does not exist.", item.getName());
        }
        return oldItem;
    }

    @Override
    public @Nullable Item remove(String itemName) {
        Item item = items.remove(itemName);
        if (item != null) {
            notifyListenersAboutRemovedElement(item);
        }
        return item;
    }
}
