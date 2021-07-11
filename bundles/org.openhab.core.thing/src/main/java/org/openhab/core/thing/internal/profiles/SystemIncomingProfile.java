/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Incoming only profile.
 *
 * Posts events from the {@link ThingHandler} to the event bus for state updates.
 *
 * @author James Hewitt - Initial contribution
 */
@NonNullByDefault
public class SystemIncomingProfile implements StateProfile {

    private final ProfileCallback callback;

    public SystemIncomingProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.INCOMING;
    }

    @Override
    public void onCommandFromItem(Command command) {
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate(state);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand(command);
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }
}
