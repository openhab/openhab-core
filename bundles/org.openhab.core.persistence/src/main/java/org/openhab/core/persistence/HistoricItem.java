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
package org.openhab.core.persistence;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.types.State;

/**
 * This interface is used by persistence services to represent an item
 * with a certain state at a given point in time.
 *
 * <p>
 * Note that this interface does not extend {@link Item} as the persistence services could not provide an implementation
 * that correctly implement getAcceptedXTypes() and getGroupNames().
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface HistoricItem {

    /**
     * returns the timestamp of the persisted item
     *
     * @return the timestamp of the item
     */
    ZonedDateTime getTimestamp();

    /**
     * returns the current state of the item
     *
     * @return the current state
     */
    State getState();

    /**
     * returns the name of the item
     *
     * @return the name of the item
     */
    String getName();
}
