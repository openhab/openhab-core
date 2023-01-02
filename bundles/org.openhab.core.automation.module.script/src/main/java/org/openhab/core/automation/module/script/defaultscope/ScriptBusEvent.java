/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.defaultscope;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This gives direct write access to the event bus from within scripts.
 * Items should not be updated directly (setting the state property), but updates should
 * be sent to the bus, so that all interested bundles are notified.
 *
 * Note: This class is a copy from the {@link org.openhab.core.model.script.actions.BusEvent} class
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan N. Klug - Refactored to interface
 */
@NonNullByDefault
public interface ScriptBusEvent {
    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     */
    @Nullable
    Object sendCommand(@Nullable Item item, @Nullable String commandString);

    /**
     * Sends a number as a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param number the number to send as a command
     */
    @Nullable
    Object sendCommand(@Nullable Item item, @Nullable Number number);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param commandString the command to send
     */
    @Nullable
    Object sendCommand(@Nullable String itemName, @Nullable String commandString);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    @Nullable
    Object sendCommand(@Nullable Item item, @Nullable Command command);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     */
    @Nullable
    Object postUpdate(@Nullable Item item, @Nullable Number state);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param stateAsString the new state of the item
     */
    @Nullable
    Object postUpdate(@Nullable Item item, @Nullable String stateAsString);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the status update for
     * @param stateAsString the new state of the item
     */
    @Nullable
    Object postUpdate(@Nullable String itemName, @Nullable String stateAsString);

    /**
     * Posts a status update for a specified item to the event bus.
     * t
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    @Nullable
    Object postUpdate(@Nullable Item item, @Nullable State state);

    /**
     * Stores the current states for a list of items in a map.
     * A group item is not itself put into the map, but instead all its members.
     *
     * @param items the items for which the state should be stored
     * @return the map of items with their states
     */
    Map<Item, State> storeStates(Item @Nullable... items);

    /**
     * Restores item states from a map.
     * If the saved state can be interpreted as a command, a command is sent for the item
     * (and the physical device can send a status update if occurred). If it is no valid
     * command, the item state is directly updated to the saved value.
     *
     * @param statesMap a map with ({@link Item}, {@link State}) entries
     * @return null
     */
    @Nullable
    Object restoreStates(@Nullable Map<Item, State> statesMap);
}
