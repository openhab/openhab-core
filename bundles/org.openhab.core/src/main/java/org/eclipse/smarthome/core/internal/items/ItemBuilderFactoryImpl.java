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
package org.eclipse.smarthome.core.internal.items;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemBuilder;
import org.eclipse.smarthome.core.items.ItemBuilderFactory;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Provides an {@link ItemBuilder} with all available {@link ItemFactory}s set. The {@link CoreItemFactory} will always
 * be present.
 *
 * @author Henning Treu - initial contribution and API
 *
 */
@NonNullByDefault
@Component
public class ItemBuilderFactoryImpl implements ItemBuilderFactory {

    private final @NonNullByDefault({}) Set<ItemFactory> itemFactories = new CopyOnWriteArraySet<>();

    @Override
    public ItemBuilder newItemBuilder(Item item) {
        return new ItemBuilderImpl(itemFactories, item);
    }

    @Override
    public ItemBuilder newItemBuilder(String itemType, String itemName) {
        return new ItemBuilderImpl(itemFactories, itemType, itemName);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
    }

    @Reference(target = "(component.name=org.eclipse.smarthome.core.library.CoreItemFactory)")
    protected void setCoreItemFactory(ItemFactory coreItemFactory) {
        itemFactories.add(coreItemFactory);
    }

    protected void unsetCoreItemFactory(ItemFactory coreItemFactory) {
        itemFactories.remove(coreItemFactory);
    }

}
