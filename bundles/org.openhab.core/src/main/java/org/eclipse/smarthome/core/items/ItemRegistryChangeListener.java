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

import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;

/**
 * This is a listener interface which should be implemented where ever the item registry is
 * used in order to be notified of any dynamic changes in the provided items.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public interface ItemRegistryChangeListener extends RegistryChangeListener<Item> {

    /**
     * Notifies the listener that all items in the registry have changed and thus should be reloaded.
     * 
     * @param oldItemNames a collection of all previous item names, so that references can be removed
     */
    public void allItemsChanged(Collection<String> oldItemNames);

}
