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

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * A ColorItem can be used for color values, e.g. for LED lights
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ColorItem extends DimmerItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(HSBType.class, PercentType.class,
            OnOffType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(HSBType.class,
            PercentType.class, OnOffType.class, IncreaseDecreaseType.class, RefreshType.class);

    public ColorItem(String name) {
        super(CoreItemFactory.COLOR, name);
    }

    public void send(HSBType command) {
        internalSend(command);
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
    public void setState(State state) {
        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            State currentState = this.state;

            if (currentState instanceof HSBType) {
                DecimalType hue = ((HSBType) currentState).getHue();
                PercentType saturation = ((HSBType) currentState).getSaturation();
                // we map ON/OFF values to dark/bright, so that the hue and saturation values are not changed
                if (state == OnOffType.OFF) {
                    applyState(new HSBType(hue, saturation, PercentType.ZERO));
                } else if (state == OnOffType.ON) {
                    applyState(new HSBType(hue, saturation, PercentType.HUNDRED));
                } else if (state instanceof PercentType && !(state instanceof HSBType)) {
                    applyState(new HSBType(hue, saturation, (PercentType) state));
                } else if (state instanceof DecimalType && !(state instanceof HSBType)) {
                    applyState(new HSBType(hue, saturation,
                            new PercentType(((DecimalType) state).toBigDecimal().multiply(BigDecimal.valueOf(100)))));
                } else {
                    applyState(state);
                }
            } else {
                // try conversion
                State convertedState = state.as(HSBType.class);
                if (convertedState != null) {
                    applyState(convertedState);
                } else {
                    applyState(state);
                }
            }
        } else {
            logSetTypeError(state);
        }
    }
}
