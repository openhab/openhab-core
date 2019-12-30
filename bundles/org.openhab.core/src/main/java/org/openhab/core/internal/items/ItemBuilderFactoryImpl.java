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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilder;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.library.CoreItemFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Provides an {@link ItemBuilder} with all available {@link ItemFactory}s set. The {@link CoreItemFactory} will always
 * be present.
 *
 * @author Henning Treu - Initial contribution
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

    @Reference(target = "(component.name=org.openhab.core.library.CoreItemFactory)")
    protected void setCoreItemFactory(ItemFactory coreItemFactory) {
        itemFactories.add(coreItemFactory);
    }

    protected void unsetCoreItemFactory(ItemFactory coreItemFactory) {
        itemFactories.remove(coreItemFactory);
    }

}
