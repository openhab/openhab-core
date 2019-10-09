/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

/**
 * This is a listener interface which should be implemented where ever item providers or
 * the item registry are used in order to be notified of any dynamic changes in the provided items.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ItemsChangeListener {

    /**
     * Notifies the listener that all items of a provider have changed and thus should be reloaded.
     *
     * @param provider the concerned item provider
     * @param oldItemNames a collection of all previous item names, so that references can be removed
     */
    public void allItemsChanged(ItemProvider provider, Collection<String> oldItemNames);

    /**
     * Notifies the listener that a single item has been added
     *
     * @param provider the concerned item provider
     * @param item the item that has been added
     */
    public void itemAdded(ItemProvider provider, Item item);

    /**
     * Notifies the listener that a single item has been removed
     *
     * @param provider the concerned item provider
     * @param item the item that has been removed
     */
    public void itemRemoved(ItemProvider provider, Item item);

}
