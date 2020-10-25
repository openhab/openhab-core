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
import org.openhab.core.library.types.StringListType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * This item identifies a telephone call by its origin and destination.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Gaël L'hopital - port to Eclipse SmartHome
 */
@NonNullByDefault
public class CallItem extends GenericItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(StringListType.class,
            UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(RefreshType.class);

    public CallItem(String name) {
        super(CoreItemFactory.CALL, name);
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
            super.setState(state);
        } else {
            logSetTypeError(state);
        }
    }
}
