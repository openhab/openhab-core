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

import static org.openhab.core.thing.profiles.SystemProfiles.RAWROCKER_REWIND_FASTFORWARD;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.State;

/**
 * The {@link RawRockerRewindFastforwardProfile} transforms rocker switch channel events into REWIND and FASTFORWARD
 * commands. Can be used on a {@link PlayerItem}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class RawRockerRewindFastforwardProfile implements TriggerProfile {

    private final ProfileCallback callback;

    RawRockerRewindFastforwardProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return RAWROCKER_REWIND_FASTFORWARD;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (CommonTriggerEvents.DIR1_PRESSED.equals(event)) {
            callback.sendCommand(RewindFastforwardType.FASTFORWARD);
        } else if (CommonTriggerEvents.DIR2_PRESSED.equals(event)) {
            callback.sendCommand(RewindFastforwardType.REWIND);
        }
    }
}
