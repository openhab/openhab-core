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
package org.eclipse.smarthome.core.items;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Creates a new {@link ItemBuilder} which is based on all available {@link ItemFactory}s.
 *
 * @author Henning Treu - initial contribution and API
 *
 */
@NonNullByDefault
public interface ItemBuilderFactory {

    /**
     * Create a new {@link ItemBuilder}, which is initialized by the given item.
     *
     * @param item the template to initialize the builder with
     *
     * @return an ItemBuilder instance
     */
    ItemBuilder newItemBuilder(Item item);

    /**
     * Create a new {@link ItemBuilder}, which is initialized by the given item.
     *
     * @param itemType the item type to create
     * @param itemName the name of the item to create
     *
     * @return an ItemBuilder instance
     */
    ItemBuilder newItemBuilder(String itemType, String itemName);
}
