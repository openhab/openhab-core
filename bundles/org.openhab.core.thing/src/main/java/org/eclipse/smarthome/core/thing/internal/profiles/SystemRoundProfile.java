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

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the given parameter "pattern" to a QuantityType or DecimalType state
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class SystemRoundProfile implements StateProfile {

    private final Logger logger = LoggerFactory.getLogger(SystemRoundProfile.class);
    private static final String ROUND_PARAM = "decimals";

    private final ProfileCallback callback;
    private final ProfileContext context;

    private @Nullable DecimalFormat format;
    private int numDecimals = 0;

    public SystemRoundProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;

        Object paramValue = this.context.getConfiguration().get(ROUND_PARAM);
        logger.debug("Configuring profile with {} parameter '{}'", ROUND_PARAM, paramValue);
        try {
            numDecimals = Integer.parseInt((String) paramValue);
            if (Math.abs(numDecimals) <= 10) {
                if (numDecimals <= 0) {
                    format = new DecimalFormat("#");
                } else if (numDecimals > 0) {
                    format = new DecimalFormat("#." + StringUtils.rightPad("#", numDecimals));
                }
            } else {
                logger.error("Parameter '{}' must be between -10 and 10, no rounding will occur", ROUND_PARAM);
            }
        } catch (NullPointerException | NumberFormatException e) {
            logger.error("Parameter '{}' is not a valid integer, no rounding will occur", ROUND_PARAM);
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.ROUND;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        callback.handleUpdate((State) applyRounding(state));
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand((Command) applyRounding(command));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand((Command) applyRounding(command));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate((State) applyRounding(state));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Type applyRounding(Type state) {
        if (state instanceof UnDefType) {
            return state;
        }
        Type result = state;
        if (format != null) {
            if (state instanceof QuantityType) {
                QuantityType qtState = (QuantityType) state;
                BigDecimal originalValue = qtState.toBigDecimal();
                String roundedValue = roundAsString(originalValue);
                result = new QuantityType<>(new BigDecimal(roundedValue), qtState.getUnit());
            } else if (state instanceof DecimalType) {
                BigDecimal originalValue = ((DecimalType) state).toBigDecimal();
                String roundedValue = roundAsString(originalValue);
                result = new DecimalType(roundedValue);
            }
        }
        return result;
    }

    @SuppressWarnings("null")
    private String roundAsString(BigDecimal originalValue) {
        String formattedValue;
        if (numDecimals < 0) {
            double pow = Math.pow(10, Math.abs(numDecimals));
            formattedValue = format.format((Math.round(originalValue.doubleValue() / pow) * pow));
        } else {
            formattedValue = format.format(originalValue.doubleValue());
        }
        return formattedValue;
    }
}
