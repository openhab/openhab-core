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
package org.openhab.core.library.items;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

/**
 * A DimmerItem can be used as a switch (ON/OFF), but it also accepts percent values
 * to reflect the dimmed state.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Support more types for getStateAs
 */
@NonNullByDefault
public class DimmerItem extends SwitchItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(PercentType.class, OnOffType.class,
            UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(PercentType.class,
            OnOffType.class, IncreaseDecreaseType.class, RefreshType.class);

    public DimmerItem(String name) {
        super(CoreItemFactory.DIMMER, name);
    }

    /* package */ DimmerItem(String type, String name) {
        super(type, name);
    }

    /**
     * Send a PercentType command to the item.
     *
     * @param command the command to be sent
     */
    public void send(PercentType command) {
        internalSend(command, null);
    }

    /**
     * Send a PercentType command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(PercentType command, @Nullable String source) {
        internalSend(command, source);
    }

    /**
     * Send an INCREASE/DECREASE command to the item.
     *
     * @param command the command to be sent
     */
    public void send(IncreaseDecreaseType command) {
        internalSend(command, null);
    }

    /**
     * Send an INCREASE/DECREASE command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(IncreaseDecreaseType command, @Nullable String source) {
        internalSend(command, source);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    @Override
    public void setState(State state, @Nullable String source) {
        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            // try conversion
            State convertedState = state.as(PercentType.class);
            if (convertedState != null) {
                applyState(convertedState, source);
            } else {
                applyState(state, source);
            }
        } else {
            logSetTypeError(state);
        }
    }

    @Override
    public void setTimeSeries(TimeSeries timeSeries) {
        if (timeSeries.getStates().allMatch(s -> s.state() instanceof PercentType)) {
            super.applyTimeSeries(timeSeries);
        } else {
            logSetTypeError(timeSeries);
        }
    }
}
