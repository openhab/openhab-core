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
package org.openhab.core.automation.module.script.internal.action;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.action.BusEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TypeParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to write to the openHAB event bus.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = BusEvent.class)
@NonNullByDefault
public class BusEventImpl implements BusEvent {

    private final Logger logger = LoggerFactory.getLogger(BusEventImpl.class);
    private final ItemRegistry itemRegistry;
    private final EventPublisher publisher;

    @Activate
    public BusEventImpl(final @Reference ItemRegistry itemRegistry, final @Reference EventPublisher publisher) {
        this.itemRegistry = itemRegistry;
        this.publisher = publisher;
    }

    @Override
    public void sendCommand(Item item, String commandString) {
        if (item != null) {
            sendCommand(item.getName(), commandString);
        }
    }

    @Override
    public void sendCommand(Item item, Number command) {
        if (item != null && command != null) {
            sendCommand(item.getName(), command.toString());
        }
    }

    @Override
    public void sendCommand(String itemName, String commandString) {
        try {
            Item item = itemRegistry.getItem(itemName);
            Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
            if (command != null) {
                publisher.post(ItemEventFactory.createCommandEvent(itemName, command));
            } else {
                logger.warn("Cannot convert '{}' to a command type which item '{}' accepts: {}.", commandString,
                        itemName, getAcceptedCommandNames(item));
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Item '{}' does not exist.", itemName);
        }
    }

    private static <T extends State> List<String> getAcceptedCommandNames(Item item) {
        return item.getAcceptedCommandTypes().stream().map(Class::getSimpleName).toList();
    }

    @Override
    public void sendCommand(Item item, Command command) {
        if (item != null) {
            publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command));
        }
    }

    @Override
    public void postUpdate(Item item, String stateString) {
        if (item != null) {
            postUpdate(item.getName(), stateString);
        }
    }

    @Override
    public void postUpdate(Item item, Number state) {
        if (item != null && state != null) {
            postUpdate(item.getName(), state.toString());
        }
    }

    private static <T extends State> List<String> getAcceptedDataTypeNames(Item item) {
        return item.getAcceptedDataTypes().stream().map(Class::getSimpleName).toList();
    }

    @Override
    public void postUpdate(String itemName, String stateString) {
        try {
            Item item = itemRegistry.getItem(itemName);
            State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
            if (state != null) {
                publisher.post(ItemEventFactory.createStateEvent(itemName, state));
            } else {
                logger.warn("Cannot convert '{}' to a state type which item '{}' accepts: {}.", stateString, itemName,
                        getAcceptedDataTypeNames(item));
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Item '{}' does not exist.", itemName);
        }
    }

    @Override
    public void postUpdate(Item item, State state) {
        if (item != null) {
            publisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
        }
    }

    @Override
    public void sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries) {
        if (item != null && timeSeries != null) {
            publisher.post(ItemEventFactory.createTimeSeriesEvent(item.getName(), timeSeries, null));
        }
    }

    @Override
    public void sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values, String policy) {
        if (itemName != null && values != null) {
            try {
                TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.valueOf(policy));
                values.forEach((key, value) -> timeSeries.add(key.toInstant(), value));
                publisher.post(ItemEventFactory.createTimeSeriesEvent(itemName, timeSeries, null));
            } catch (IllegalArgumentException e) {
                logger.warn("Policy '{}' does not exist.", policy);
            }
        }
    }

    @Override
    public Map<Item, State> storeStates(Item... items) {
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

    @Override
    public void restoreStates(Map<Item, State> statesMap) {
        if (statesMap != null) {
            for (Map.Entry<Item, State> entry : statesMap.entrySet()) {
                if (entry.getValue() instanceof Command) {
                    sendCommand(entry.getKey(), (Command) entry.getValue());
                } else {
                    postUpdate(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
