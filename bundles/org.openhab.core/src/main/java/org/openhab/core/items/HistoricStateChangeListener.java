/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;

/**
 * <p>
 * This interface must be implemented by all classes that want to be notified about changes in the state of an item.
 *
 * <p>
 * The {@link GenericItem} class provides the possibility to register such listeners.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface HistoricStateChangeListener extends StateChangeListener {

    /**
     * This method is called, if a state was updated, but has not changed
     *
     * @param item the item whose state was updated
     * @param state the current state, same before and after the update
     */
    public void historicStateUpdated(Item item, State state, ZonedDateTime dateTime);
}
