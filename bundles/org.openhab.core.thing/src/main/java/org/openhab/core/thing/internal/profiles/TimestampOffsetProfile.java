/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.time.Duration;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
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

/**
 * Applies the given parameter "offset" to a {@link DateTimeType} state.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimestampOffsetProfile implements StateProfile {

    static final String OFFSET_PARAM = "offset";

    private final Logger logger = LoggerFactory.getLogger(TimestampOffsetProfile.class);

    private final ProfileCallback callback;

    private final Duration offset;

    public TimestampOffsetProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        Object offsetParam = context.getConfiguration().get(OFFSET_PARAM);
        logger.debug("Configuring profile with {} parameter '{}'", OFFSET_PARAM, offsetParam);
        if (offsetParam instanceof Number bd) {
            offset = Duration.ofSeconds(bd.longValue());
        } else if (offsetParam instanceof String s) {
            offset = Duration.ofSeconds(Long.parseLong(s));
        } else {
            logger.error(
                    "Parameter '{}' is not of type String or Number. Please make sure it is one of both, e.g. 3 or \"-1\".",
                    OFFSET_PARAM);
            offset = Duration.ZERO;
        }
    }

    private @Nullable String toStringOrNull(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.TIMESTAMP_OFFSET;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand((Command) applyOffset(command, false));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand((Command) applyOffset(command, true));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate((State) applyOffset(state, true));
    }

    private Type applyOffset(Type type, boolean towardsItem) {
        if (type instanceof UnDefType) {
            // we cannot adjust UNDEF or NULL values, thus we simply return them without reporting an error or warning
            return type;
        }

        Duration finalOffset = towardsItem ? offset : offset.negated();
        Type result;
        if (type instanceof DateTimeType timeType) {
            Instant instant = timeType.getInstant();

            // apply offset
            if (!Duration.ZERO.equals(offset)) {
                // we do not need apply an offset equals to 0
                instant = instant.plus(finalOffset);
            }

            result = new DateTimeType(instant);
        } else {
            logger.warn(
                    "Offset '{}' cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    offset, type);
            result = type;
        }
        return result;
    }
}
