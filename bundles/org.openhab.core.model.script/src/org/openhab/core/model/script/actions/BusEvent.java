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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.model.script.internal.engine.action.BusEventActionService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

/**
 * The {@link BusEvent} is a wrapper for the BusEvent actions.
 *
 * @author Florian Hotze - Initial contribution
 */
public class BusEvent {

    public static Object sendCommand(Item item, String commandString) {
        BusEventActionService.getBusEvent().sendCommand(item, commandString);
        return null;
    }

    public static Object sendCommand(Item item, String commandString, String source) {
        BusEventActionService.getBusEvent().sendCommand(item, commandString, source);
        return null;
    }

    public static Object sendCommand(Item item, Number number) {
        BusEventActionService.getBusEvent().sendCommand(item, number);
        return null;
    }

    public static Object sendCommand(Item item, Number number, String source) {
        BusEventActionService.getBusEvent().sendCommand(item, number, source);
        return null;
    }

    public static Object sendCommand(String itemName, String commandString) {
        BusEventActionService.getBusEvent().sendCommand(itemName, commandString);
        return null;
    }

    public static Object sendCommand(String itemName, String commandString, String source) {
        BusEventActionService.getBusEvent().sendCommand(itemName, commandString, source);
        return null;
    }

    public static Object sendCommand(Item item, Command command) {
        BusEventActionService.getBusEvent().sendCommand(item, command);
        return null;
    }

    public static Object sendCommand(Item item, Command command, String source) {
        BusEventActionService.getBusEvent().sendCommand(item, command, source);
        return null;
    }

    public static Object postUpdate(Item item, Number state) {
        BusEventActionService.getBusEvent().postUpdate(item, state);
        return null;
    }

    public static Object postUpdate(Item item, Number state, String source) {
        BusEventActionService.getBusEvent().postUpdate(item, state, source);
        return null;
    }

    public static Object postUpdate(Item item, String stateAsString) {
        BusEventActionService.getBusEvent().postUpdate(item, stateAsString);
        return null;
    }

    public static Object postUpdate(Item item, String stateAsString, String source) {
        BusEventActionService.getBusEvent().postUpdate(item, stateAsString, source);
        return null;
    }

    public static Object postUpdate(String itemName, String stateString) {
        BusEventActionService.getBusEvent().postUpdate(itemName, stateString);
        return null;
    }

    public static Object postUpdate(String itemName, String stateString, String source) {
        BusEventActionService.getBusEvent().postUpdate(itemName, stateString, source);
        return null;
    }

    public static Object sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries) {
        BusEventActionService.getBusEvent().sendTimeSeries(item, timeSeries);
        return null;
    }

    public static Object sendTimeSeries(@Nullable Item item, @Nullable TimeSeries timeSeries, String source) {
        BusEventActionService.getBusEvent().sendTimeSeries(item, timeSeries, source);
        return null;
    }

    public static Object sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values,
            String policy) {
        BusEventActionService.getBusEvent().sendTimeSeries(itemName, values, policy);
        return null;
    }

    public static Object sendTimeSeries(@Nullable String itemName, @Nullable Map<ZonedDateTime, State> values,
            String policy, String source) {
        BusEventActionService.getBusEvent().sendTimeSeries(itemName, values, policy, source);
        return null;
    }

    public static Object postUpdate(Item item, State state) {
        BusEventActionService.getBusEvent().postUpdate(item, state);
        return null;
    }

    public static Map<Item, State> storeStates(Item... items) {
        return BusEventActionService.getBusEvent().storeStates(items);
    }

    public static Object restoreStates(Map<Item, State> statesMap) {
        BusEventActionService.getBusEvent().restoreStates(statesMap);
        return null;
    }
}
