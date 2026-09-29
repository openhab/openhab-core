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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;
import java.util.Map;

import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.model.script.internal.engine.action.BusEventActionService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

/**
 * The {@link BusEvent} is a wrapper for the BusEvent actions.
 *
 * @author Florian Hotze - Initial contribution
 * @author Mark Herwege - DecimalType and QuantityType overrides
 */
public class BusEvent {

    public static Object sendCommand(Item item, String commandString) {
        sendCommand(item, commandString, null);
        return null;
    }

    public static Object sendCommand(Item item, String commandString, String source) {
        if (item != null && commandString != null) {
            BusEventActionService.getBusEvent().sendCommand(item, commandString, source);
        }
        return null;
    }

    public static Object sendCommand(Item item, Number number) {
        sendCommand(item, number, null);
        return null;
    }

    public static Object sendCommand(Item item, Number number, String source) {
        if (item != null && number != null) {
            BusEventActionService.getBusEvent().sendCommand(item, number, source);
        }
        return null;
    }

    public static Object sendCommand(String itemName, String commandString) {
        sendCommand(itemName, commandString, null);
        return null;
    }

    public static Object sendCommand(String itemName, String commandString, String source) {
        if (itemName != null && commandString != null) {
            BusEventActionService.getBusEvent().sendCommand(itemName, commandString, source);
        }
        return null;
    }

    public static Object sendCommand(Item item, Command command) {
        sendCommand(item, command, null);
        return null;
    }

    public static Object sendCommand(Item item, Command command, String source) {
        if (item != null && command != null) {
            BusEventActionService.getBusEvent().sendCommand(item, command, source);
        }
        return null;
    }

    public static Object sendCommand(Item item, DecimalType command) {
        sendCommand(item, command, null);
        return null;
    }

    public static Object sendCommand(Item item, DecimalType command, String source) {
        if (item != null && command != null) {
            BusEventActionService.getBusEvent().sendCommand(item, command, source);
        }
        return null;
    }

    public static Object sendCommand(Item item, QuantityType<?> command) {
        sendCommand(item, command, null);
        return null;
    }

    public static Object sendCommand(Item item, QuantityType<?> command, String source) {
        if (item != null && command != null) {
            BusEventActionService.getBusEvent().sendCommand(item, command, source);
        }
        return null;
    }

    public static Object postUpdate(Item item, Number state) {
        postUpdate(item, state, null);
        return null;
    }

    public static Object postUpdate(Item item, Number state, String source) {
        if (item != null && state != null) {
            BusEventActionService.getBusEvent().postUpdate(item, state, source);
        }
        return null;
    }

    public static Object postUpdate(Item item, String stateAsString) {
        postUpdate(item, stateAsString, null);
        return null;
    }

    public static Object postUpdate(Item item, String stateAsString, String source) {
        if (item != null && stateAsString != null) {
            BusEventActionService.getBusEvent().postUpdate(item, stateAsString, source);
        }
        return null;
    }

    public static Object postUpdate(String itemName, String stateString) {
        postUpdate(itemName, stateString, null);
        return null;
    }

    public static Object postUpdate(String itemName, String stateString, String source) {
        if (itemName != null && stateString != null) {
            BusEventActionService.getBusEvent().postUpdate(itemName, stateString, source);
        }
        return null;
    }

    public static Object postUpdate(Item item, State state) {
        postUpdate(item, state, null);
        return null;
    }

    public static Object postUpdate(Item item, State state, String source) {
        if (item != null && state != null) {
            BusEventActionService.getBusEvent().postUpdate(item, state, source);
        }
        return null;
    }

    public static Object postUpdate(Item item, DecimalType state) {
        postUpdate(item, state, null);
        return null;
    }

    public static Object postUpdate(Item item, DecimalType state, String source) {
        if (item != null && state != null) {
            BusEventActionService.getBusEvent().postUpdate(item, state, source);
        }
        return null;
    }

    public static Object postUpdate(Item item, QuantityType<?> state) {
        postUpdate(item, state, null);
        return null;
    }

    public static Object postUpdate(Item item, QuantityType<?> state, String source) {
        if (item != null && state != null) {
            BusEventActionService.getBusEvent().postUpdate(item, state, source);
        }
        return null;
    }

    public static Object sendTimeSeries(Item item, TimeSeries timeSeries) {
        sendTimeSeries(item, timeSeries, null);
        return null;
    }

    public static Object sendTimeSeries(Item item, TimeSeries timeSeries, String source) {
        if (item != null && timeSeries != null) {
            BusEventActionService.getBusEvent().sendTimeSeries(item, timeSeries, source);
        }
        return null;
    }

    public static Object sendTimeSeries(String itemName, Map<ZonedDateTime, State> values, String policy) {
        sendTimeSeries(itemName, values, policy, null);
        return null;
    }

    public static Object sendTimeSeries(String itemName, Map<ZonedDateTime, State> values, String policy,
            String source) {
        if (itemName != null && values != null && policy != null) {
            BusEventActionService.getBusEvent().sendTimeSeries(itemName, values, policy, source);
        }
        return null;
    }

    public static Map<Item, State> storeStates(Item... items) {
        if (items == null || items.length == 0) {
            return Map.of();
        }
        return BusEventActionService.getBusEvent().storeStates(items);
    }

    public static Object restoreStates(Map<Item, State> statesMap) {
        if (statesMap != null) {
            BusEventActionService.getBusEvent().restoreStates(statesMap);
        }
        return null;
    }
}
