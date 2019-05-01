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

import static org.eclipse.smarthome.core.thing.profiles.SystemProfiles.ROUND;

import java.math.RoundingMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies rounding with the specified scale and the rounding mode to a {@link QuantityType} or {@link DecimalType}
 * state. Default rounding mode is {@link RoundingMode#HALF_UP}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class SystemRoundProfile implements StateProfile {

    private static final String PARAM_SCALE = "scale";
    private static final String PARAM_ROUNDING_MODE = "mode";

    private static final Map<String, RoundingMode> SUPPORTED_ROUNDING_MODES = Collections.unmodifiableMap(Stream
            .of(new SimpleEntry<>("UP", RoundingMode.UP), new SimpleEntry<>("DOWN", RoundingMode.DOWN),
                    new SimpleEntry<>("CEILING", RoundingMode.CEILING), new SimpleEntry<>("FLOOR", RoundingMode.FLOOR),
                    new SimpleEntry<>("HALF_UP", RoundingMode.HALF_UP),
                    new SimpleEntry<>("HALF_DOWN", RoundingMode.HALF_DOWN))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

    private final Logger logger = LoggerFactory.getLogger(SystemRoundProfile.class);

    private final ProfileCallback callback;
    private final ProfileContext context;

    private final int scale;
    private final RoundingMode roundingMode;

    public SystemRoundProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;

        Object paramScale = this.context.getConfiguration().get(PARAM_SCALE);
        Object paramRoundingMode = this.context.getConfiguration().get(PARAM_ROUNDING_MODE);
        logger.debug("Configuring profile with parameters: [{scale='{}', mode='{}']", paramScale, paramRoundingMode);

        int scale = 0;
        if (paramScale instanceof String) {
            try {
                scale = Integer.valueOf((String) paramScale);
            } catch (NumberFormatException e) {
                logger.error("Cannot convert value '{}' of parameter 'scale' into a valid integer.", paramScale);
            }
        } else if (paramScale instanceof Number) {
            scale = ((Number) paramScale).intValue();
        } else {
            logger.error("Parameter 'scale' is not of type String or Number.");
        }

        if (scale < 0) {
            logger.error("Parameter 'scale' has to be a non-negative integer. Ignoring it.");
            scale = 0;
        }

        RoundingMode roundingMode = RoundingMode.HALF_UP;
        if (paramRoundingMode instanceof String) {
            if (SUPPORTED_ROUNDING_MODES.containsKey(paramRoundingMode)) {
                roundingMode = SUPPORTED_ROUNDING_MODES.get(paramRoundingMode);
            } else {
                logger.error("Parameter 'mode' is not a supported rounding mode: '{}'", paramRoundingMode);
            }
        } else {
            logger.error("Parameter 'mode' is not of type String.");
        }

        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return ROUND;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        callback.handleUpdate((State) applyRound(state));
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand((Command) applyRound(command));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand((Command) applyRound(command));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate((State) applyRound(state));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Type applyRound(Type state) {
        if (state instanceof UnDefType) {
            // we cannot round UNDEF or NULL values, thus we simply return them without reporting an error or warning
            return state;
        }

        Type result = UnDefType.UNDEF;
        if (state instanceof QuantityType) {
            QuantityType qtState = (QuantityType) state;
            result = new QuantityType<>(qtState.toBigDecimal().setScale(scale, roundingMode), qtState.getUnit());
        } else if (state instanceof DecimalType) {
            DecimalType dtState = (DecimalType) state;
            result = new DecimalType(dtState.toBigDecimal().setScale(scale, roundingMode));
        } else {
            logger.warn(
                    "Round cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    state);
            result = state;
        }
        return result;
    }
}
