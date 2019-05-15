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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

/**
 * This is the default implementation for a change timestamp profile.
 * The timestamp updates to now each time the channel or item state changes.
 *
 * @author Gaël L'hopital - initial contribution
 *
 */
@NonNullByDefault
public class TimestampChangeProfile implements StateProfile {
    private final ProfileCallback callback;

    private @Nullable State previousState;

    public TimestampChangeProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.TIMESTAMP_CHANGE;
    }

    @SuppressWarnings("null")
    @Override
    public void onStateUpdateFromItem(State state) {
        if (previousState == null || !state.equals(previousState.as(state.getClass()))) {
            previousState = state;
            callback.sendUpdate(new DateTimeType());
        }
    }

    @SuppressWarnings("null")
    @Override
    public void onStateUpdateFromHandler(State state) {
        if (previousState == null || !state.equals(previousState.as(state.getClass()))) {
            previousState = state;
            callback.sendUpdate(new DateTimeType());
        }
    }

    @Override
    public void onCommandFromItem(Command command) {
        // no-op
    }

    @Override
    public void onCommandFromHandler(Command command) {
        // no-op
    }

}
