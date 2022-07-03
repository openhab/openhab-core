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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This profile allows a channel of the "system:rawbutton" type to be bound to an item.
 *
 * It reads the triggered events and uses the item's current state and toggles it once it detects that the
 * button was pressed.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ToggleProfile<StateType extends Command & State> implements TriggerProfile {

    private final Logger logger = LoggerFactory.getLogger(ToggleProfile.class);
    private ProfileTypeUID uid;
    private ChannelType channelType;
    private StateType initialState;
    private StateType alternativeState;
    private final ProfileCallback callback;
    private static final String EVENT_PARAM = "event";

    private final String triggerEvent;

    private @Nullable State previousState;

    public ToggleProfile(ProfileCallback callback, ProfileContext context, ProfileTypeUID uid, ChannelType channelType,
            StateType initialState, StateType alternativeState, String defaultEvent) {
        this.uid = uid;
        this.channelType = channelType;
        this.initialState = initialState;
        this.alternativeState = alternativeState;
        this.callback = callback;

        String triggerEventParam = (String) context.getConfiguration().get(EVENT_PARAM);
        if (isValidEvent(triggerEventParam)) {
            triggerEvent = triggerEventParam;
        } else {
            if (triggerEventParam != null) {
                logger.warn(
                        "'{}' is not a valid trigger event for Profile '{}'. Default trigger event SHORT_PRESSED is used instead.",
                        triggerEventParam, this.getProfileTypeUID().getAsString());
            }
            triggerEvent = defaultEvent;
        }
    }

    public boolean isValidEvent(@Nullable String triggerEvent) {
        return channelType.getEvent().getOptions().stream().anyMatch(e -> e.getValue().equals(triggerEvent));
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return uid;
    }

    @Override
    public void onTriggerFromHandler(String event) {
        if (triggerEvent.equals(event)) {
            StateType newState = initialState.equals(previousState) ? alternativeState : initialState;
            callback.sendCommand(newState);
            previousState = newState;
        }
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        previousState = state.as(OnOffType.class);
    }
}
