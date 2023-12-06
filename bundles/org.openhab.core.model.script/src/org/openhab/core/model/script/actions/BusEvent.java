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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TypeParser;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This gives direct write access to the openHAB event bus from within scripts.
 * Items should not be updated directly (setting the state property), but updates should
 * be sent to the bus, so that all interested bundles are notified.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Bußweiler - Migration to new ESH event concept
 */
public class BusEvent {

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     */
    public static Object sendCommand(Item item, String commandString) {
        if (item != null) {
            return sendCommand(item.getName(), commandString);
        } else {
            return null;
        }
    }

    /**
     * Sends a number as a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param number the number to send as a command
     */
    public static Object sendCommand(Item item, Number number) {
        if (item != null && number != null) {
            return sendCommand(item.getName(), number.toString());
        } else {
            return null;
        }
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the command to
     * @param commandString the command to send
     */
    public static Object sendCommand(String itemName, String commandString) {
        ItemRegistry registry = ScriptServiceUtil.getItemRegistry();
        EventPublisher publisher = ScriptServiceUtil.getEventPublisher();
        if (publisher != null && registry != null) {
            try {
                Item item = registry.getItem(itemName);
                Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
                if (command != null) {
                    publisher.post(ItemEventFactory.createCommandEvent(itemName, command));
                } else {
                    LoggerFactory.getLogger(BusEvent.class).warn(
                            "Cannot convert '{}' to a command type which item '{}' accepts: {}.", commandString,
                            itemName, getAcceptedCommandNames(item));
                }

            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(BusEvent.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    private static <T extends State> List<String> getAcceptedCommandNames(Item item) {
        return item.getAcceptedCommandTypes().stream().map(Class::getSimpleName).toList();
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    public static Object sendCommand(Item item, Command command) {
        EventPublisher publisher = ScriptServiceUtil.getEventPublisher();
        if (publisher != null && item != null) {
            publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
        }
        return null;
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     */
    public static Object postUpdate(Item item, Number state) {
        if (item != null && state != null) {
            return postUpdate(item.getName(), state.toString());
        } else {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param stateAsString the new state of the item
     */
    public static Object postUpdate(Item item, String stateAsString) {
        if (item != null) {
            return postUpdate(item.getName(), stateAsString);
        } else {
            return null;
        }
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param itemName the name of the item to send the status update for
     * @param stateString the new state of the item
     */
    public static Object postUpdate(String itemName, String stateString) {
        ItemRegistry registry = ScriptServiceUtil.getItemRegistry();
        EventPublisher publisher = ScriptServiceUtil.getEventPublisher();
        if (publisher != null && registry != null) {
            try {
                Item item = registry.getItem(itemName);
                State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
                if (state != null) {
                    publisher.post(ItemEventFactory.createStateEvent(itemName, state));
                } else {
                    LoggerFactory.getLogger(BusEvent.class).warn(
                            "Cannot convert '{}' to a state type which item '{}' accepts: {}.", stateString, itemName,
                            getAcceptedDataTypeNames(item));
                }
            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(BusEvent.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    /**
     * Sends a time series to the event bus
     *
     * @param item the item to send the time series for
     * @param timeSeries a {@link TimeSeries} containing policy and values
     */
    public static Object sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries) {
        EventPublisher eventPublisher1 = ScriptServiceUtil.getEventPublisher();
        if (eventPublisher1 != null && item != null && timeSeries != null) {
            eventPublisher1.post(ItemEventFactory.createTimeSeriesEvent(item.getName(), timeSeries, null));
        }
        return null;
    }

    /**
     * Sends a time series to the event bus
     *
     * @param itemName the name of the item to send the status update for
     * @param values a {@link Map} containing the timeseries, composed of pairs of {@link ZonedDateTime} and
     *            {@link State}
     * @param policy either <code>ADD</code> or <code>REPLACE</code>
     */
    public static Object sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values,
            String policy) {
        EventPublisher eventPublisher1 = ScriptServiceUtil.getEventPublisher();
        if (eventPublisher1 != null && itemName != null && values != null && policy != null) {
            try {
                TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.valueOf(policy));
                values.forEach((key, value) -> timeSeries.add(key.toInstant(), value));
                eventPublisher1.post(ItemEventFactory.createTimeSeriesEvent(itemName, timeSeries, null));
            } catch (IllegalArgumentException e) {
                LoggerFactory.getLogger(BusEvent.class).warn("Policy '{}' does not exist.", policy);
            }
        }
        return null;
    }

    private static <T extends State> List<String> getAcceptedDataTypeNames(Item item) {
        return item.getAcceptedDataTypes().stream().map(Class::getSimpleName).toList();
    }

    /**
     * Posts a status update for a specified item to the event bus.
     * t
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    public static Object postUpdate(Item item, State state) {
        EventPublisher publisher = ScriptServiceUtil.getEventPublisher();
        if (publisher != null && item != null) {
            publisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
        }
        return null;
    }

    /**
     * Stores the current states for a list of items in a map.
     * A group item is not itself put into the map, but instead all its members.
     *
     * @param items the items for which the state should be stored
     * @return the map of items with their states
     */
    public static Map<Item, State> storeStates(Item... items) {
        Map<Item, State> statesMap = new HashMap<>();
        if (items != null) {
            for (Item item : items) {
                if (item instanceof GroupItem groupItem) {
                    for (Item member : groupItem.getAllMembers()) {
                        statesMap.put(member, member.getState());
                    }
                } else {
                    statesMap.put(item, item.getState());
                }
            }
        }
        return statesMap;
    }

    /**
     * Restores item states from a map.
     * If the saved state can be interpreted as a command, a command is sent for the item
     * (and the physical device can send a status update if occurred). If it is no valid
     * command, the item state is directly updated to the saved value.
     *
     * @param statesMap a map with ({@link Item}, {@link State}) entries
     * @return null
     */
    public static Object restoreStates(Map<Item, State> statesMap) {
        if (statesMap != null) {
            for (Entry<Item, State> entry : statesMap.entrySet()) {
                if (entry.getValue() instanceof Command) {
                    sendCommand(entry.getKey(), (Command) entry.getValue());
                } else {
                    postUpdate(entry.getKey(), entry.getValue());
                }
            }
        }
        return null;
    }
}
