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
package org.eclipse.smarthome.core.persistence;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.types.State;

/**
 * This interface is used by persistence services to represent an item
 * with a certain state at a given point in time.
 *
 * <p>
 * Note that this interface does not extend {@link Item} as the persistence services could not provide an implementation
 * that correctly implement getAcceptedXTypes() and getGroupNames().
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public interface HistoricItem {

    /**
     * returns the timestamp of the persisted item
     *
     * @return the timestamp of the item
     */
    @NonNull
    Date getTimestamp();

    /**
     * returns the current state of the item
     *
     * @return the current state
     */
    @NonNull
    State getState();

    /**
     * returns the name of the item
     *
     * @return the name of the item
     */
    @NonNull
    String getName();

}
