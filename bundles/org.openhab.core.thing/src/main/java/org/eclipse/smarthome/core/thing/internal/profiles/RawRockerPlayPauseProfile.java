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
package org.eclipse.smarthome.core.thing.internal.profiles;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.thing.CommonTriggerEvents;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfile;
import org.eclipse.smarthome.core.types.State;

/**
 * The {@link RawRockerPlayPauseProfile} transforms rocker switch channel events into PLAY and PAUSE commands.
 *
 * @author Daniel Weber - Initial contribution
 */
public class RawRockerPlayPauseProfile implements TriggerProfile {

    private final ProfileCallback callback;

    RawRockerPlayPauseProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.RAWROCKER_ON_OFF;
    }

    /**
     * Will be called if an item has changed its state and this information should be forwarded to the binding.
     *
     * @param state
     */
    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (CommonTriggerEvents.DIR1_PRESSED.equals(event)) {
            callback.sendCommand(PlayPauseType.PLAY);
        } else if (CommonTriggerEvents.DIR2_PRESSED.equals(event)) {
            callback.sendCommand(PlayPauseType.PAUSE);
        }
    }

}
