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
package org.openhab.core.model.script.scoping;

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

import com.google.inject.Singleton;

/**
 * This is a class which provides all available states and commands (obviously only the enum-based ones with a fixed
 * name).
 * A future version might gather the sets through an extension mechanism, for the moment it is simply statically coded.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Singleton
public class StateAndCommandProvider {

    protected static final Set<Command> COMMANDS = new HashSet<>();
    protected static final Set<State> STATES = new HashSet<>();
    protected static final Set<Type> TYPES = new HashSet<>();

    static {
        COMMANDS.add(OnOffType.ON);
        COMMANDS.add(OnOffType.OFF);
        COMMANDS.add(UpDownType.UP);
        COMMANDS.add(UpDownType.DOWN);
        COMMANDS.add(IncreaseDecreaseType.INCREASE);
        COMMANDS.add(IncreaseDecreaseType.DECREASE);
        COMMANDS.add(StopMoveType.STOP);
        COMMANDS.add(StopMoveType.MOVE);
        COMMANDS.add(PlayPauseType.PLAY);
        COMMANDS.add(PlayPauseType.PAUSE);
        COMMANDS.add(NextPreviousType.NEXT);
        COMMANDS.add(NextPreviousType.PREVIOUS);
        COMMANDS.add(RewindFastforwardType.REWIND);
        COMMANDS.add(RewindFastforwardType.FASTFORWARD);
        COMMANDS.add(RefreshType.REFRESH);

        STATES.add(UnDefType.UNDEF);
        STATES.add(UnDefType.NULL);
        STATES.add(OnOffType.ON);
        STATES.add(OnOffType.OFF);
        STATES.add(UpDownType.UP);
        STATES.add(UpDownType.DOWN);
        STATES.add(OpenClosedType.OPEN);
        STATES.add(OpenClosedType.CLOSED);
        STATES.add(PlayPauseType.PLAY);
        STATES.add(PlayPauseType.PAUSE);
        STATES.add(RewindFastforwardType.REWIND);
        STATES.add(RewindFastforwardType.FASTFORWARD);

        TYPES.addAll(COMMANDS);
        TYPES.addAll(STATES);
    }

    public Iterable<Type> getAllTypes() {
        return TYPES;
    }

    public Iterable<Command> getAllCommands() {
        return COMMANDS;
    }

    public Iterable<State> getAllStates() {
        return STATES;
    }

}
