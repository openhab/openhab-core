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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.internal.profiles.config.BatteryLowStateProfileConfig;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * This is the default implementation for a {@link BatteryLowStateProfile}}. Triggers ON/OFF behavior when being linked
 * to a Switch item if value is below threshold (default: 10).
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class BatteryLowStateProfile implements StateProfile {

    private final Logger logger = LoggerFactory.getLogger(BatteryLowStateProfile.class);

    private final ProfileCallback callback;
    private final BatteryLowStateProfileConfig config;

    public BatteryLowStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.config = context.getConfiguration().as(BatteryLowStateProfileConfig.class);
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.BATTERY_LOW;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    @Override
    public void onCommandFromHandler(Command command) {
        final Type mappedCommand = mapValue(command);
        logger.trace("Mapped command from '{}' to command '{}'.", command, mappedCommand);
        callback.sendCommand((Command) mappedCommand);
    }

    @Override
    public void onCommandFromItem(Command command) {
        // do nothing
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        final Type mappedState = mapValue(state);
        logger.trace("Mapped state from '{}' to state '{}'.", state, mappedState);
        callback.sendUpdate((State) mappedState);
    }

    private Type mapValue(Type value) {
        if (value instanceof Number) {
            return ((Number) value).intValue() <= config.threshold ? OnOffType.ON : OnOffType.OFF;
        }
        return OnOffType.OFF;
    }
}
