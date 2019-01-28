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
package org.eclipse.smarthome.model.script.scoping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;

import com.google.inject.Singleton;

/**
 * This is a class which provides all available states and commands (obviously only the enum-based ones with a fixed
 * name).
 * A future version might gather the sets through an extension mechanism, for the moment it is simply statically coded.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Singleton
public class StateAndCommandProvider {

    final static protected Set<Command> COMMANDS = new HashSet<Command>();
    final static protected Set<State> STATES = new HashSet<State>();
    final static protected Set<Type> TYPES = new HashSet<Type>();

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
