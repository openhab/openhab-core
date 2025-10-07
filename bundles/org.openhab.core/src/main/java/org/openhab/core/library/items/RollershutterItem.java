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
package org.openhab.core.library.items;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

/**
 * A RollershutterItem allows the control of roller shutters, i.e.
 * moving them up, down, stopping or setting it to close to a certain percentage.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Support more types for getStateAs
 */
@NonNullByDefault
public class RollershutterItem extends GenericItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(PercentType.class, UpDownType.class,
            UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(PercentType.class,
            UpDownType.class, StopMoveType.class, RefreshType.class);

    public RollershutterItem(String name) {
        super(CoreItemFactory.ROLLERSHUTTER, name);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    /**
     * Send an UP/DOWN command to the item.
     *
     * @param command the command to be sent
     */
    public void send(UpDownType command) {
        internalSend(command, null);
    }

    /**
     * Send an UP/DOWN command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(UpDownType command, @Nullable String source) {
        internalSend(command, source);
    }

    /**
     * Send a STOP/MOVE command to the item.
     *
     * @param command the command to be sent
     */
    public void send(StopMoveType command) {
        internalSend(command, null);
    }

    /**
     * Send a STOP/MOVE command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(StopMoveType command, @Nullable String source) {
        internalSend(command, source);
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

    @Override
    public void setState(State state) {
        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            // try conversion
            State convertedState = state.as(PercentType.class);
            if (convertedState != null) {
                applyState(convertedState);
            } else {
                applyState(state);
            }
        } else {
            logSetTypeError(state);
        }
    }

    @Override
    public void setTimeSeries(TimeSeries timeSeries) {
        if (timeSeries.getStates().allMatch(s -> isAcceptedState(ACCEPTED_DATA_TYPES, s.state()))) {
            applyTimeSeries(timeSeries);
        } else {
            logSetTypeError(timeSeries);
        }
    }
}
