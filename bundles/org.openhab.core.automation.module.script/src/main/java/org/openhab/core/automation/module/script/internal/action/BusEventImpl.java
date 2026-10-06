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
package org.openhab.core.automation.module.script.internal.action;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.action.BusEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
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
 * @author Florian Hotze - Refactored to OSGi service
 * @author Mark Herwege - DecimalType and QuantityType overrides
 */
@Component(immediate = true, service = BusEvent.class)
@NonNullByDefault
public class BusEventImpl implements BusEvent {
    private static final String AUTOMATION_SOURCE = "org.openhab.core.automation.module.script";

    private final Logger logger = LoggerFactory.getLogger(BusEventImpl.class);
    private final ItemRegistry itemRegistry;
    private final EventPublisher publisher;

    @Activate
    public BusEventImpl(final @Reference ItemRegistry itemRegistry, final @Reference EventPublisher publisher) {
        this.itemRegistry = itemRegistry;
        this.publisher = publisher;
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable Command command) {
        sendCommand(item, command, null);
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable Command command, @Nullable String source) {
        if (item != null && command != null) {
            publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command, buildSource(source)));
        }
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable Number command) {
        sendCommand(item, command, null);
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable Number command, @Nullable String source) {
        if (item != null && command != null) {
            sendCommand(item, command.toString(), source);
        }
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable DecimalType command) {
        sendCommand(item, command, null);
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable DecimalType command, @Nullable String source) {
        if (item != null && command != null) {
            publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command, buildSource(source)));
        }
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable QuantityType<?> command) {
        sendCommand(item, command, null);
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable QuantityType<?> command, @Nullable String source) {
        if (item != null && command != null) {
            publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command, buildSource(source)));
        }
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable String commandString) {
        sendCommand(item, commandString, null);
    }

    @Override
    public void sendCommand(@Nullable Item item, @Nullable String commandString, @Nullable String source) {
        if (item != null && commandString != null) {
            Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
            if (command != null) {
                publisher.post(ItemEventFactory.createCommandEvent(item.getName(), command, buildSource(source)));
            } else {
                logger.warn("Cannot convert '{}' to a command type which item '{}' accepts: {}.", commandString,
                        item.getName(), getAcceptedCommandNames(item));
            }
        }
    }

    @Override
    public void sendCommand(@Nullable String itemName, @Nullable String commandString) {
        sendCommand(itemName, commandString, null);
    }

    @Override
    public void sendCommand(@Nullable String itemName, @Nullable String commandString, @Nullable String source) {
        if (itemName != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                sendCommand(item, commandString, source);

            } catch (ItemNotFoundException e) {
                logger.warn("Item '{}' does not exist.", itemName);
            }
        }
    }

    private static <T extends State> List<String> getAcceptedCommandNames(Item item) {
        return item.getAcceptedCommandTypes().stream().map(Class::getSimpleName).toList();
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable State state) {
        postUpdate(item, state, null);
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable State state, @Nullable String source) {
        if (item != null && state != null) {
            publisher.post(ItemEventFactory.createStateEvent(item.getName(), state, buildSource(source)));
        }
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable Number state) {
        postUpdate(item, state, null);
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable Number state, @Nullable String source) {
        if (item != null && state != null) {
            postUpdate(item.getName(), state.toString(), source);
        }
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable DecimalType state) {
        postUpdate(item, state, null);
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable DecimalType state, @Nullable String source) {
        if (item != null && state != null) {
            publisher.post(ItemEventFactory.createStateEvent(item.getName(), state, buildSource(source)));
        }
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable QuantityType<?> state) {
        postUpdate(item, state, null);
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable QuantityType<?> state, @Nullable String source) {
        if (item != null && state != null) {
            publisher.post(ItemEventFactory.createStateEvent(item.getName(), state, buildSource(source)));
        }
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable String stateString) {
        postUpdate(item, stateString, null);
    }

    @Override
    public void postUpdate(@Nullable Item item, @Nullable String stateString, @Nullable String source) {
        if (item != null && stateString != null) {
            State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
            if (state != null) {
                publisher.post(ItemEventFactory.createStateEvent(item.getName(), state, buildSource(source)));
            } else {
                logger.warn("Cannot convert '{}' to a state type which item '{}' accepts: {}.", stateString,
                        item.getName(), getAcceptedDataTypeNames(item));
            }
        }
    }

    @Override
    public void postUpdate(@Nullable String itemName, @Nullable String stateString) {
        postUpdate(itemName, stateString, null);
    }

    @Override
    public void postUpdate(@Nullable String itemName, @Nullable String stateString, @Nullable String source) {
        if (itemName != null) {
            try {
                Item item = itemRegistry.getItem(itemName);
                postUpdate(item, stateString, source);

            } catch (ItemNotFoundException e) {
                logger.warn("Item '{}' does not exist.", itemName);
            }
        }
    }

    private static <T extends State> List<String> getAcceptedDataTypeNames(Item item) {
        return item.getAcceptedDataTypes().stream().map(Class::getSimpleName).toList();
    }

    @Override
    public void sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries) {
        sendTimeSeries(item, timeSeries, null);
    }

    @Override
    public void sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries, @Nullable String source) {
        if (item != null && timeSeries != null) {
            publisher.post(ItemEventFactory.createTimeSeriesEvent(item.getName(), timeSeries, buildSource(source)));
        }
    }

    @Override
    public void sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values,
            @Nullable String policy) {
        sendTimeSeries(itemName, values, policy, null);
    }

    @Override
    public void sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values,
            @Nullable String policy, @Nullable String source) {
        if (itemName != null && values != null && policy != null) {
            try {
                TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.valueOf(policy));
                values.forEach((key, value) -> timeSeries.add(key.toInstant(), value));
                publisher.post(ItemEventFactory.createTimeSeriesEvent(itemName, timeSeries, buildSource(source)));
            } catch (IllegalArgumentException e) {
                logger.warn("Policy '{}' does not exist.", policy);
            }
        }
    }

    @Override
    public Map<Item, State> storeStates(@Nullable Item @Nullable... items) {
        Map<Item, State> statesMap = new HashMap<>();
        if (items != null) {
            for (Item item : items) {
                if (item instanceof GroupItem groupItem) {
                    for (Item member : groupItem.getAllMembers()) {
                        statesMap.put(member, member.getState());
                    }
                } else if (item != null) {
                    statesMap.put(item, item.getState());
                }
            }
        }
        return statesMap;
    }

    @Override
    public void restoreStates(@Nullable Map<Item, @Nullable State> statesMap) {
        if (statesMap != null) {
            for (Map.Entry<Item, @Nullable State> entry : statesMap.entrySet()) {
                if (entry.getValue() instanceof Command) {
                    sendCommand(entry.getKey(), (Command) entry.getValue());
                } else {
                    postUpdate(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private String buildSource(@Nullable String source) {
        return Objects.requireNonNullElse(source, AUTOMATION_SOURCE);
    }
}
