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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.openhab.core.types.UnDefType;

/**
 * A StringItem can be used for any kind of string to either send or receive
 * from a device.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class StringItem extends GenericItem {

    // UnDefType has to come before StringType, because otherwise every UNDEF state sent as a string would be
    // interpreted as a StringType
    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(UnDefType.class, StringType.class,
            DateTimeType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(StringType.class,
            RefreshType.class);

    public StringItem(String name) {
        super(CoreItemFactory.STRING, name);
    }

    public void send(StringType command) {
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
    public <T extends State> @Nullable T getStateAs(Class<T> typeClass) {
        List<Class<? extends State>> list = new ArrayList<>();
        list.add(typeClass);
        State convertedState = TypeParser.parseState(list, state.toString());
        if (typeClass.isInstance(convertedState)) {
            return typeClass.cast(convertedState);
        } else {
            return super.getStateAs(typeClass);
        }
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
