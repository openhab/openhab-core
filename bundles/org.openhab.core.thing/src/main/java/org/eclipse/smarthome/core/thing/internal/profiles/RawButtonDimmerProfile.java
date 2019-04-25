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

import static org.eclipse.smarthome.core.thing.profiles.SystemProfiles.RAWBUTTON_DIMMER;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.CommonTriggerEvents;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfile;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

/**
 * This profile allows a channel of the "system:rawbutton" type to be bound to a Dimmer item.
 *
 * If the dimmer is fully ON, the profile will send 'decrease' commands while the button is pressed. Otherwise,
 * the profile will send 'increase' commands to the item. A short press will toggle the state of the item.
 *
 * @author Aitor Iturrioz - initial contribution.
 *
 */
@NonNullByDefault
public class RawButtonDimmerProfile implements TriggerProfile {

    private final ProfileCallback callback;

    private final ProfileContext context;

    @Nullable
    private State previousState;
    @Nullable
    private ScheduledFuture<?> dimmFuture;
    @Nullable
    private ScheduledFuture<?> timeoutFuture;

    private long pressedTime = 0;

    public RawButtonDimmerProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return RAWBUTTON_DIMMER;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        previousState = state.as(PercentType.class);
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (CommonTriggerEvents.PRESSED.equals(event)) {
            IncreaseDecreaseType command = previousState.equals(PercentType.HUNDRED) ? IncreaseDecreaseType.DECREASE : 
                    IncreaseDecreaseType.INCREASE;
            buttonPressed(command);
        } else if (CommonTriggerEvents.RELEASED.equals(event)) {
            OnOffType newState = previousState.equals(PercentType.HUNDRED) ? OnOffType.OFF : OnOffType.ON;
            buttonReleased(newState);
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
