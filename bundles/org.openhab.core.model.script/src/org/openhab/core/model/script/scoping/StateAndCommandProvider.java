/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Set;

import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.RefreshType;
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

    protected static final Set<Type> TYPES = Set.of(OnOffType.ON, OnOffType.OFF, UpDownType.UP, UpDownType.DOWN,
            IncreaseDecreaseType.INCREASE, IncreaseDecreaseType.DECREASE, StopMoveType.STOP, StopMoveType.MOVE,
            PlayPauseType.PLAY, PlayPauseType.PAUSE, NextPreviousType.NEXT, NextPreviousType.PREVIOUS,
            RewindFastforwardType.REWIND, RewindFastforwardType.FASTFORWARD, RefreshType.REFRESH, UnDefType.UNDEF,
            UnDefType.NULL, OpenClosedType.OPEN, OpenClosedType.CLOSED);

    public Iterable<Type> getAllTypes() {
        return TYPES;
    }
}
