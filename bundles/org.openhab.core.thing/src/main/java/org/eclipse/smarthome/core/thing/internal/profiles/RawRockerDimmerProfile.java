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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.CommonTriggerEvents;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfile;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

/**
 * The {@link RawRockerDimmerProfile} transforms rocker switch channel events into dimmer commands.
 *
 * @author Jan Kemmler - Initial contribution
 */
@NonNullByDefault
public class RawRockerDimmerProfile implements TriggerProfile {

    private final ProfileCallback callback;

    private final ProfileContext context;

    @Nullable
    private ScheduledFuture<?> dimmFuture;
    @Nullable
    private ScheduledFuture<?> timeoutFuture;

    private long pressedTime = 0;

    RawRockerDimmerProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.RAWROCKER_DIMMER;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (CommonTriggerEvents.DIR1_PRESSED.equals(event)) {
            buttonPressed(IncreaseDecreaseType.INCREASE);
        } else if (CommonTriggerEvents.DIR1_RELEASED.equals(event)) {
            buttonReleased(OnOffType.ON);
        } else if (CommonTriggerEvents.DIR2_PRESSED.equals(event)) {
            buttonPressed(IncreaseDecreaseType.DECREASE);
        } else if (CommonTriggerEvents.DIR2_RELEASED.equals(event)) {
            buttonReleased(OnOffType.OFF);
        }
    }

    private synchronized void buttonPressed(Command commandToSend) {
        if (null != timeoutFuture) {
            timeoutFuture.cancel(false);
        }

        this.cancelDimmFuture();

        dimmFuture = context.getExecutorService().scheduleWithFixedDelay(() -> callback.sendCommand(commandToSend), 550,
                200, TimeUnit.MILLISECONDS);
        timeoutFuture = context.getExecutorService().schedule(() -> this.cancelDimmFuture(), 10, TimeUnit.SECONDS);
        pressedTime = System.currentTimeMillis();

    }

    private synchronized void buttonReleased(Command commandToSend) {
        if (null != timeoutFuture) {
            timeoutFuture.cancel(false);
        }

        this.cancelDimmFuture();

        if (System.currentTimeMillis() - pressedTime <= 500) {
            callback.sendCommand(commandToSend);
        }
    }

    private synchronized void cancelDimmFuture() {
        if (null != dimmFuture) {
            dimmFuture.cancel(false);
        }
    }

}
