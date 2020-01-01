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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * This is a listener interface which should be implemented where ever the item registry is
 * used in order to be notified of any dynamic changes in the provided items.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface ItemRegistryChangeListener extends RegistryChangeListener<Item> {

    /**
     * Notifies the listener that all items in the registry have changed and thus should be reloaded.
     *
     * @param oldItemNames a collection of all previous item names, so that references can be removed
     */
    void allItemsChanged(Collection<String> oldItemNames);

}
