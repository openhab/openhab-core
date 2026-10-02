/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.action;

import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

/**
 * The {@link BusEvent} allows write access to the openHAB event bus from within scripts.
 * Items should not be updated directly (setting the state property), but updates should
 * be sent to the bus, so that all interested bundles are notified.
 *
 * All methods in this interface should accept null arguments and handle them gracefully, without throwing exceptions.
 * This is to allow scripts in various languages to be more robust and not fail due to null values.
 *
 * @author Florian Hotze - Initial contribution
 * @author Mark Herwege - DecimalType and QuantityType overrides
 */
@NonNullByDefault
public interface BusEvent {
    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    void sendCommand(@Nullable Item item, @Nullable Command command);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     * @param source the source of the command
     */
    void sendCommand(@Nullable Item item, @Nullable Command command, @Nullable String source);

    /**
     * Sends a number as a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the number to send as a command
     */
    void sendCommand(@Nullable Item item, @Nullable Number command);

    /**
     * Sends a number as a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the number to send as a command
     * @param source the source of the command
     */
    void sendCommand(@Nullable Item item, @Nullable Number command, @Nullable String source);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    void sendCommand(@Nullable Item item, @Nullable DecimalType command);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     * @param source the source of the command
     */
    void sendCommand(@Nullable Item item, @Nullable DecimalType command, @Nullable String source);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    void sendCommand(@Nullable Item item, @Nullable QuantityType<?> command);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     * @param source the source of the command
     */
    void sendCommand(@Nullable Item item, @Nullable QuantityType<?> command, @Nullable String source);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     */
    void sendCommand(@Nullable Item item, @Nullable String commandString);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     * @param source the source of the command
     */
    void sendCommand(@Nullable Item item, @Nullable String commandString, @Nullable String source);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param commandString the command to send
     */
    void sendCommand(@Nullable String itemName, @Nullable String commandString);

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param commandString the command to send
     * @param source the source of the command
     */
    void sendCommand(@Nullable String itemName, @Nullable String commandString, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    void postUpdate(@Nullable Item item, @Nullable State state);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     * @param source the source of the status update
     */
    void postUpdate(@Nullable Item item, @Nullable State state, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     */
    void postUpdate(@Nullable Item item, @Nullable Number state);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     * @param source the source of the status update
     */
    void postUpdate(@Nullable Item item, @Nullable Number state, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    void postUpdate(@Nullable Item item, @Nullable DecimalType state);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     * @param source the source of the status update
     */
    void postUpdate(@Nullable Item item, @Nullable DecimalType state, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    void postUpdate(@Nullable Item item, @Nullable QuantityType<?> state);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     * @param source the source of the status update
     */
    void postUpdate(@Nullable Item item, @Nullable QuantityType<?> state, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param stateString the new state of the item
     */
    void postUpdate(@Nullable Item item, @Nullable String stateString);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param stateString the new state of the item
     * @param source the source of the status update
     */
    void postUpdate(@Nullable Item item, @Nullable String stateString, @Nullable String source);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the status update for
     * @param stateString the new state of the item
     */
    void postUpdate(@Nullable String itemName, @Nullable String stateString);

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the status update for
     * @param stateString the new state of the item
     * @param source the source of the status update
     */
    void postUpdate(@Nullable String itemName, @Nullable String stateString, @Nullable String source);

    /**
     * Sends a time series to the event bus
     *
     * @param item the item to send the time series for
     * @param timeSeries a {@link TimeSeries} containing policy and values
     */
    void sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries);

    /**
     * Sends a time series to the event bus
     *
     * @param item the item to send the time series for
     * @param timeSeries a {@link TimeSeries} containing policy and values
     * @param source the source of the time series
     */
    void sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries, @Nullable String source);

    /**
     * Sends a time series to the event bus
     *
     * @param itemName the name of the item to send the status update for
     * @param values a {@link Map} containing the timeseries, composed of pairs of {@link ZonedDateTime} and
     *            {@link State}
     * @param policy either <code>ADD</code> or <code>REPLACE</code>
     */
    void sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values, @Nullable String policy);

    /**
     * Sends a time series to the event bus
     *
     * @param itemName the name of the item to send the status update for
     * @param values a {@link Map} containing the timeseries, composed of pairs of {@link ZonedDateTime} and
     *            {@link State}
     * @param policy either <code>ADD</code> or <code>REPLACE</code>
     * @param source the source of the time series
     */
    void sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values, @Nullable String policy,
            @Nullable String source);

    /**
     * Stores the current states for a list of items in a map.
     * A group item is not itself put into the map, but instead all its members.
     *
     * @param items the items for which the state should be stored
     * @return the map of items with their states
     */
    Map<Item, State> storeStates(@Nullable Item @Nullable... items);

    /**
     * Restores item states from a map.
     * If the saved state can be interpreted as a command, a command is sent for the item
     * (and the physical device can send a status update if occurred). If it is no valid
     * command, the item state is directly updated to the saved value.
     *
     * @param statesMap a map with ({@link Item}, {@link State}) entries
     */
    void restoreStates(@Nullable Map<Item, @Nullable State> statesMap);
}
