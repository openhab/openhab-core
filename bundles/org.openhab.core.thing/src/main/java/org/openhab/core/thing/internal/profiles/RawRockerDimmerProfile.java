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

import static org.openhab.core.thing.profiles.SystemProfiles.RAWROCKER_DIMMER;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The {@link RawRockerDimmerProfile} transforms rocker switch channel events into dimmer commands.
 *
 * @author Jan Kemmler - Initial contribution
 */
@NonNullByDefault
public class RawRockerDimmerProfile implements TriggerProfile {

    private final ProfileCallback callback;

    private final ProfileContext context;

    private @Nullable ScheduledFuture<?> dimmFuture;
    private @Nullable ScheduledFuture<?> timeoutFuture;

    private long pressedTime = 0;

    RawRockerDimmerProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return RAWROCKER_DIMMER;
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
