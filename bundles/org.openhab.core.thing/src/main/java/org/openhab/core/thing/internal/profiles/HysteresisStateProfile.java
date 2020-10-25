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
import org.openhab.core.thing.internal.profiles.config.HysteresisStateProfileConfig;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * This is the default implementation for a {@link HysteresisStateProfile}}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class HysteresisStateProfile implements StateProfile {

    private static final String PARAM_LOWER = "lower";

    private final Logger logger = LoggerFactory.getLogger(HysteresisStateProfile.class);

    private final ProfileCallback callback;
    private final double lower;
    private final double upper;

    private Type previousType = UnDefType.UNDEF;

    public HysteresisStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        HysteresisStateProfileConfig config = context.getConfiguration().as(HysteresisStateProfileConfig.class);
        if (!(config.lower instanceof Number)) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' is not a Number value, using default value: 10", PARAM_LOWER));
        }
        lower = config.lower.doubleValue();
        upper = config.upper != null ? config.upper.doubleValue() : lower;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.HYSTERESIS;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    @Override
    public void onCommandFromHandler(Command command) {
        final Type mappedCommand = mapValue(command);
        logger.trace("Mapped command from '{}' to command '{}'.", command, mappedCommand);
        if (mappedCommand instanceof Command) {
            callback.sendCommand((Command) mappedCommand);
        }
    }

    @Override
    public void onCommandFromItem(Command command) {
        // do nothing
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        final Type mappedState = mapValue(state);
        logger.trace("Mapped state from '{}' to state '{}'.", state, mappedState);
        if (mappedState instanceof State) {
            callback.sendUpdate((State) mappedState);
        }
    }

    private Type mapValue(Type value) {
        Type newType = previousType;
        if (value instanceof Number) {
            double theValue = ((Number) value).doubleValue();
            if (theValue <= lower) {
                newType = previousType = OnOffType.OFF;
            } else if (theValue >= upper) {
                newType = previousType = OnOffType.ON;
            }
        }
        return newType;
    }
}
