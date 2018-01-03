/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.library.items;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * A DimmerItem can be used as a switch (ON/OFF), but it also accepts percent values
 * to reflect the dimmed state.
 *
 * @author Kai Kreuzer
 * @since 0.1.0
 *
 */
public class DimmerItem extends SwitchItem {

    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<>();

    static {
        acceptedDataTypes.add(OnOffType.class);
        acceptedDataTypes.add(PercentType.class);
        acceptedDataTypes.add(UnDefType.class);

        acceptedCommandTypes.add(OnOffType.class);
        acceptedCommandTypes.add(IncreaseDecreaseType.class);
        acceptedCommandTypes.add(PercentType.class);
    }

    public DimmerItem(String name) {
        super(name);
    }

    public void send(PercentType command) {
        internalSend(command);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return acceptedDataTypes;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return acceptedCommandTypes;
    }

    @Override
    public void setState(State state) {
        // we map ON/OFF values to the percent values 0 and 100
        if (state == OnOffType.OFF) {
            super.setState(PercentType.ZERO);
        } else if (state == OnOffType.ON) {
            super.setState(PercentType.HUNDRED);
        } else {
            super.setState(state);
        }
    }

    @Override
    public State getStateAs(Class<? extends State> typeClass) {
        if (state.getClass() == typeClass) {
            return state;
        } else if (typeClass == OnOffType.class) {
            // if it is not completely off, we consider the dimmer to be on
            return state.equals(PercentType.ZERO) ? OnOffType.OFF : OnOffType.ON;
        } else if (typeClass == DecimalType.class) {
            if (state instanceof PercentType) {
                return new DecimalType(
                        ((PercentType) state).toBigDecimal().divide(new BigDecimal(100), 8, RoundingMode.UP));
            }
        } else if (typeClass == PercentType.class) {
            if (state instanceof DecimalType) {
                return new PercentType(((DecimalType) state).toBigDecimal().multiply(new BigDecimal(100)));
            }
        }

        return super.getStateAs(typeClass);
    }
}
