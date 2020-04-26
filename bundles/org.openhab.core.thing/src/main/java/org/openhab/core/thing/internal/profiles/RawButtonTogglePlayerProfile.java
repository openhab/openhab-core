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
package org.openhab.core.thing.internal.profiles;

import static org.openhab.core.thing.profiles.SystemProfiles.RAWBUTTON_TOGGLE_PLAYER;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.State;

/**
 * This profile allows a channel of the "system:rawbutton" type to be bound to an item.
 *
 * It reads the triggered events and uses the item's current state and toggles it once it detects that the
 * button was pressed.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class RawButtonTogglePlayerProfile implements TriggerProfile {

    private final ProfileCallback callback;

    private @Nullable State previousState;

    public RawButtonTogglePlayerProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return RAWBUTTON_TOGGLE_PLAYER;
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (CommonTriggerEvents.PRESSED.equals(event)) {
            PlayPauseType newState = PlayPauseType.PLAY.equals(previousState) ? PlayPauseType.PAUSE
                    : PlayPauseType.PLAY;
            callback.sendCommand(newState);
            previousState = newState;
        }
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        previousState = state.as(PlayPauseType.class);
    }
}
