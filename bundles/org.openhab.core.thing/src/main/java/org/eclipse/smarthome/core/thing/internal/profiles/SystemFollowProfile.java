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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation for a follow profile.
 *
 * In contrast to the {@link SystemDefaultProfile} it does not forward any commands to the ThingHandler. Instead, it
 * turn {@link State} updates into {@link Command}s (if possible) and then forwards those to the {@link ThingHandler}.
 * <p>
 * This allows devices to be operated as "followers" of another one directly, without the need to write any rules.
 * <p>
 * The ThingHandler may send commands to the framework, but no state updates are forwarded.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public class SystemFollowProfile implements StateProfile {

    private final Logger logger = LoggerFactory.getLogger(SystemFollowProfile.class);
    private final ProfileCallback callback;

    public SystemFollowProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.FOLLOW;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        if (!(state instanceof Command)) {
            logger.debug("The given state {} could not be transformed to a command", state);
            return;
        }
        Command command = (Command) state;
        callback.handleCommand(command);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand(command);
    }

    @Override
    public void onCommandFromItem(Command command) {
        // no-op
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        // no-op
    }

}
