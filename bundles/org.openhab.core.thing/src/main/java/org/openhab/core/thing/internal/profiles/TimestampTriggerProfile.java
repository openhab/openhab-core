/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.openhab.core.thing.profiles.SystemProfiles.TIMESTAMP_TRIGGER;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation for a trigger timestamp profile.
 * The timestamp updates to now each time the channel is triggered.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimestampTriggerProfile implements TriggerProfile {

    private final Logger logger = LoggerFactory.getLogger(TimestampTriggerProfile.class);
    private final ProfileCallback callback;

    TimestampTriggerProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return TIMESTAMP_TRIGGER;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onTriggerFromHandler(String event) {
        logger.debug("Received trigger from Handler, sending timestamp to callback");
        callback.sendUpdate(new DateTimeType());
    }
}
