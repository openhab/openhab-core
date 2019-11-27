/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation for an invert rollershutter profile.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class SystemInvertRollershutterProfile implements StateProfile {

    private static final BigDecimal HUNDRED = BigDecimal.TEN.multiply(BigDecimal.TEN);

    private final Logger logger = LoggerFactory.getLogger(SystemInvertRollershutterProfile.class);
    private final ProfileCallback callback;

    public SystemInvertRollershutterProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.INVERT_ROLLERSHUTTER;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        State invertedState = (State) invertType(state);
        logger.info("Inverting state '{}' on update from item to '{}'", state, invertedState);
        callback.handleUpdate(invertedState);
    }

    @Override
    public void onCommandFromItem(Command command) {
        final Command invertedCommand = (Command) invertType(command);
        logger.info("Inverting command '{}' from item to '{}'", command, invertedCommand);
        callback.handleCommand(invertedCommand);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        final Command invertedCommand = (Command) invertType(command);
        logger.info("Inverting command '{}' from handler to '{}'", command, invertedCommand);
        callback.sendCommand(invertedCommand);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        State invertedState = (State) invertType(state);
        logger.info("Inverting state '{}' on update from handler to '{}'", state, invertedState);
        callback.sendUpdate(invertedState);
    }

    private Type invertType(Type type) {
        if (type instanceof PercentType) {
            return new PercentType(HUNDRED.subtract(((PercentType) type).toBigDecimal()));
        } else if (type == UpDownType.UP) {
            return UpDownType.DOWN;
        } else if (type == UpDownType.DOWN) {
            return UpDownType.UP;
        } else {
            return type;
        }
    }
}
