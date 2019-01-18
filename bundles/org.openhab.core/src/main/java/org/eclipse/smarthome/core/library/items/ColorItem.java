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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * A ColorItem can be used for color values, e.g. for LED lights
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public class ColorItem extends DimmerItem {

    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<Class<? extends State>>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<Class<? extends Command>>();

    static {
        acceptedDataTypes.add(HSBType.class);
        acceptedDataTypes.add(PercentType.class);
        acceptedDataTypes.add(OnOffType.class);
        acceptedDataTypes.add(UnDefType.class);

        acceptedCommandTypes.add(HSBType.class);
        acceptedCommandTypes.add(PercentType.class);
        acceptedCommandTypes.add(OnOffType.class);
        acceptedCommandTypes.add(IncreaseDecreaseType.class);
        acceptedCommandTypes.add(RefreshType.class);
    }

    public ColorItem(String name) {
        super(CoreItemFactory.COLOR, name);
    }

    public void send(HSBType command) {
        internalSend(command);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return Collections.unmodifiableList(acceptedDataTypes);
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return Collections.unmodifiableList(acceptedCommandTypes);
    }

    @Override
    public void setState(State state) {
        if (isAcceptedState(acceptedDataTypes, state)) {
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
