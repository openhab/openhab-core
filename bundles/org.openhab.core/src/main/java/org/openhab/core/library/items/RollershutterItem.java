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
package org.openhab.core.library.items;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
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

    public void send(UpDownType command) {
        internalSend(command);
    }

    public void send(StopMoveType command) {
        internalSend(command);
    }

    public void send(PercentType command) {
        internalSend(command);
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
}
