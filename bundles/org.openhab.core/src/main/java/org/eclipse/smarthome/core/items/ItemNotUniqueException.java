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

import java.util.Collection;

/**
 * This exception can be thrown whenever a search pattern does not uniquely identify
 * an item. The list of matching items must be made available through this exception.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ItemNotUniqueException extends ItemLookupException {

    private static final long serialVersionUID = 5154625234283910124L;

    private final Collection<Item> matchingItems;

    public ItemNotUniqueException(String string, Collection<Item> items) {
        super("Item cannot be uniquely identified by '" + string + "'");
        this.matchingItems = items;
    }

    /**
     * Returns all items that match the search pattern
     * 
     * @return collection of items matching the search pattern
     */
    public Collection<Item> getMatchingItems() {
        return matchingItems;
    }

}
