/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.library.items;

import java.util.ArrayList;
import java.util.List;

import org.openhab.core.items.GenericItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * A ContactItem can be used for sensors that return an "open" or "close" as a state.
 * This is useful for doors, windows, etc.
 *
 * @author Kai Kreuzer
 * @since 0.1.0
 *
 */
public class ContactItem extends GenericItem {

    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<>();

    static {
        acceptedDataTypes.add(OpenClosedType.class);
        acceptedDataTypes.add(UnDefType.class);
    }

    public ContactItem(String name) {
        super(name);
    }

    public void send(OpenClosedType command) {
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
    public State getStateAs(Class<? extends State> typeClass) {
        if (typeClass == DecimalType.class) {
            return state == OpenClosedType.OPEN ? new DecimalType(1) : DecimalType.ZERO;
        } else if (typeClass == PercentType.class) {
            return state == OpenClosedType.OPEN ? PercentType.HUNDRED : PercentType.ZERO;
        } else {
            return super.getStateAs(typeClass);
        }
    }
}
