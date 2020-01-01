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
package org.openhab.core.automation.module.script.internal.defaultscope;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This gives direct write access to the event bus from within scripts.
 * Items should not be updated directly (setting the state property), but updates should
 * be sent to the bus, so that all interested bundles are notified.
 *
 * Note: This class is a copy from the {@link BusEvent} class, which resides in the model.script bundle.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class ScriptBusEvent {

    ScriptBusEvent(ItemRegistry itemRegistry, EventPublisher eventPublisher) {
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
    }

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    public void dispose() {
        this.itemRegistry = null;
        this.eventPublisher = null;
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param commandString the command to send
     */
    public Object sendCommand(Item item, String commandString) {
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
    public Object sendCommand(Item item, Number number) {
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
    public Object sendCommand(String itemName, String commandString) {
        if (eventPublisher != null && itemRegistry != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
                eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, command));
            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(ScriptBusEvent.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    /**
     * Sends a command for a specified item to the event bus.
     *
     * @param item the item to send the command to
     * @param command the command to send
     */
    public Object sendCommand(Item item, Command command) {
        if (eventPublisher != null && item != null) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
        }
        return null;
    }

    /**
     * Posts a status update for a specified item to the event bus.
     *
     * @param item the item to send the status update for
     * @param state the new state of the item as a number
     */
    public Object postUpdate(Item item, Number state) {
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
    public Object postUpdate(Item item, String stateAsString) {
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
     * @param stateAsString the new state of the item
     */
    public Object postUpdate(String itemName, String stateString) {
        if (eventPublisher != null && itemRegistry != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
                eventPublisher.post(ItemEventFactory.createStateEvent(itemName, state));
            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(ScriptBusEvent.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    /**
     * Posts a status update for a specified item to the event bus.
     * t
     *
     * @param item the item to send the status update for
     * @param state the new state of the item
     */
    public Object postUpdate(Item item, State state) {
        if (eventPublisher != null && item != null) {
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
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
    public Map<Item, State> storeStates(Item... items) {
        Map<Item, State> statesMap = new HashMap<>();
        if (items != null) {
            for (Item item : items) {
                if (item instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) item;
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
    public Object restoreStates(Map<Item, State> statesMap) {
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
