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
package org.openhab.core.model.item;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;

/**
 * {@link StandaloneItemProvider} is the interface to implement by an {@link Item} provider that is able
 * to create a list of items from a model without impacting the item registry.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface StandaloneItemProvider {

    /**
     * Parse the provided syntax and return the corresponding {@link Item} objects without impacting
     * the item registry.
     *
     * @param modelName the model name
     * @return the collection of corresponding {@link Item}
     */
    Collection<Item> getItemsFromStandaloneModel(String modelName);
}
