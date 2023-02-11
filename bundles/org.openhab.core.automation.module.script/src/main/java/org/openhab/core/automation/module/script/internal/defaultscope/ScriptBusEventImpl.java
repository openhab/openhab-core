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
package org.openhab.core.automation.module.script.internal.defaultscope;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.defaultscope.ScriptBusEvent;
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
 * Note: This class is a copy from the {@link org.openhab.core.model.script.actions.BusEvent} class
 * 
 * @author Kai Kreuzer - Initial contribution
 * @author Jan N. Klug - Moved implementation to internal class
 */
@NonNullByDefault
public class ScriptBusEventImpl implements ScriptBusEvent {

    private @Nullable ItemRegistry itemRegistry;
    private @Nullable EventPublisher eventPublisher;

    ScriptBusEventImpl(ItemRegistry itemRegistry, EventPublisher eventPublisher) {
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
    }

    public void dispose() {
        this.itemRegistry = null;
        this.eventPublisher = null;
    }

    @Override
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable String commandString) {
        if (item != null) {
            return sendCommand(item.getName(), commandString);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable Number number) {
        if (item != null && number != null) {
            return sendCommand(item.getName(), number.toString());
        } else {
            return null;
        }
    }

    @Override
    public @Nullable Object sendCommand(@Nullable String itemName, @Nullable String commandString) {
        EventPublisher eventPublisher = this.eventPublisher;
        ItemRegistry itemRegistry = this.itemRegistry;
        if (eventPublisher != null && itemRegistry != null && itemName != null && commandString != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
                if (command != null) {
                    eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, command));
                } else {
                    LoggerFactory.getLogger(ScriptBusEventImpl.class)
                            .warn("Command '{}' cannot be parsed for item '{}'.", commandString, item);
                }
            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(ScriptBusEventImpl.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    @Override
    public @Nullable Object sendCommand(@Nullable Item item, @Nullable Command command) {
        EventPublisher eventPublisher = this.eventPublisher;
        if (eventPublisher != null && item != null && command != null) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
        }
        return null;
    }

    @Override
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable Number state) {
        if (item != null && state != null) {
            return postUpdate(item.getName(), state.toString());
        } else {
            return null;
        }
    }

    @Override
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable String stateAsString) {
        if (item != null) {
            return postUpdate(item.getName(), stateAsString);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable Object postUpdate(@Nullable String itemName, @Nullable String stateString) {
        EventPublisher eventPublisher = this.eventPublisher;
        ItemRegistry itemRegistry = this.itemRegistry;
        if (eventPublisher != null && itemRegistry != null && itemName != null && stateString != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
                if (state != null) {
                    eventPublisher.post(ItemEventFactory.createStateEvent(itemName, state));
                } else {
                    LoggerFactory.getLogger(ScriptBusEventImpl.class).warn("State '{}' cannot be parsed for item '{}'.",
                            stateString, itemName);
                }
            } catch (ItemNotFoundException e) {
                LoggerFactory.getLogger(ScriptBusEventImpl.class).warn("Item '{}' does not exist.", itemName);
            }
        }
        return null;
    }

    @Override
    public @Nullable Object postUpdate(@Nullable Item item, @Nullable State state) {
        EventPublisher eventPublisher = this.eventPublisher;
        if (eventPublisher != null && item != null && state != null) {
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
        }
        return null;
    }

    @Override
    public Map<Item, State> storeStates(Item @Nullable... items) {
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

    @Override
    public @Nullable Object restoreStates(@Nullable Map<Item, State> statesMap) {
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
