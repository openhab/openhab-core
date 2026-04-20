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
package org.openhab.core.model.script.scoping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * This is a class which provides all available states and commands (obviously only the enum-based ones with a fixed
 * name).
 * A future version might gather the sets through an extension mechanism, for the moment it is simply statically coded.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class StateAndCommandProvider {

    protected static final Set<Command> COMMANDS = Set.of( //
            IncreaseDecreaseType.DECREASE, //
            IncreaseDecreaseType.INCREASE, //
            NextPreviousType.NEXT, //
            NextPreviousType.PREVIOUS, //
            OnOffType.OFF, //
            OnOffType.ON, //
            PlayPauseType.PLAY, //
            PlayPauseType.PAUSE, //
            RefreshType.REFRESH, //
            RewindFastforwardType.FASTFORWARD, //
            RewindFastforwardType.REWIND, //
            StopMoveType.MOVE, //
            StopMoveType.STOP, //
            UpDownType.DOWN, //
            UpDownType.UP);
    protected static final Set<State> STATES = Set.of( //
            OnOffType.OFF, //
            OnOffType.ON, //
            OpenClosedType.CLOSED, //
            OpenClosedType.OPEN, //
            PlayPauseType.PAUSE, //
            PlayPauseType.PLAY, //
            RewindFastforwardType.FASTFORWARD, //
            RewindFastforwardType.REWIND, //
            UnDefType.NULL, //
            UnDefType.UNDEF, //
            UpDownType.DOWN, //
            UpDownType.UP);
    protected static final Set<Type> TYPES;

    static {
        Set<Type> types = new HashSet<>();
        types.addAll(COMMANDS);
        types.addAll(STATES);
        TYPES = Collections.unmodifiableSet(types);
    }

    public static Iterable<Type> getAllTypes() {
        return TYPES;
    }

    public static Iterable<Command> getAllCommands() {
        return COMMANDS;
    }

    public static Iterable<State> getAllStates() {
        return STATES;
    }
}
