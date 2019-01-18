/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.library.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * A RollershutterItem allows the control of roller shutters, i.e.
 * moving them up, down, stopping or setting it to close to a certain percentage.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Markus Rathgeb - Support more types for getStateAs
 *
 */
@NonNullByDefault
public class RollershutterItem extends GenericItem {

    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<Class<? extends State>>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<Class<? extends Command>>();

    static {
        acceptedDataTypes.add(PercentType.class);
        acceptedDataTypes.add(UpDownType.class);
        acceptedDataTypes.add(UnDefType.class);

        acceptedCommandTypes.add(UpDownType.class);
        acceptedCommandTypes.add(StopMoveType.class);
        acceptedCommandTypes.add(PercentType.class);

        acceptedCommandTypes.add(RefreshType.class);
    }

    public RollershutterItem(String name) {
        super(CoreItemFactory.ROLLERSHUTTER, name);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return Collections.unmodifiableList(acceptedDataTypes);
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return Collections.unmodifiableList(acceptedCommandTypes);
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
        if (isAcceptedState(acceptedDataTypes, state)) {
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
