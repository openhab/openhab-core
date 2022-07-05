/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * This is profile to convert non-UNDEF non-NULL values from {@link ThingHandler} as ON/OFF.
 *
 * State updates from {@link ThingHandler} that are UNDEF or NULL are converted to OFF, while other values are converted
 * to ON.
 * Commands (never UNDEF/NULL) from {@link ThingHandler} towards items are always converted to ON.
 * Commands from items are forwarded to the {@link ThingHandler} as-is.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class SystemNonUndefProfile implements StateProfile {

    private final ProfileCallback callback;

    private static final String INVERTED_PARAM = "inverted";

    private final boolean inverted;

    public SystemNonUndefProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        final Object paramValue = context.getConfiguration().get(INVERTED_PARAM);
        inverted = paramValue == null ? false : Boolean.valueOf(paramValue.toString());
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.DEFAULT;
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand(command);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        OnOffType convertedState = state instanceof UnDefType ? OnOffType.OFF : OnOffType.ON;
        if (inverted) {
            convertedState = convertedState == OnOffType.ON ? OnOffType.OFF : OnOffType.ON;
        }
        callback.sendUpdate(convertedState);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        // Command cannot be of UndefType, so we do not need to inspect the command
        callback.sendCommand(inverted ? OnOffType.OFF : OnOffType.ON);
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }
}
