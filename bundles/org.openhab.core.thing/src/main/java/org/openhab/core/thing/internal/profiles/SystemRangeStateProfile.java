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

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
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

import tec.uom.se.AbstractUnit;

/***
 * This is the default implementation for a {@link SystemRangeStateProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class SystemRangeStateProfile implements StateProfile {

    static final String LOWER_PARAM = "lower";
    static final String UPPER_PARAM = "upper";
    static final String INVERTED_PARAM = "inverted";

    private final Logger logger = LoggerFactory.getLogger(SystemRangeStateProfile.class);

    private final ProfileCallback callback;

    private final QuantityType<?> lower;
    private final QuantityType<?> upper;
    private final OnOffType inRange;
    private final OnOffType notInRange;

    private Type previousType = UnDefType.UNDEF;

    public SystemRangeStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        final QuantityType<?> lowerParam = getParam(context, LOWER_PARAM);
        if (lowerParam == null) {
            throw new IllegalArgumentException(String.format("Parameter '%s' is not a Number value.", LOWER_PARAM));
        }
        this.lower = lowerParam;
        final QuantityType<?> upperParam = getParam(context, UPPER_PARAM);
        if (upperParam == null) {
            throw new IllegalArgumentException(String.format("Parameter '%s' is not a Number value.", UPPER_PARAM));
        }
        final QuantityType<?> convertedUpperParam = upperParam.toUnit(lower.getUnit());
        if (convertedUpperParam == null) {
            throw new IllegalArgumentException(
                    String.format("Units of parameters '%s' and '%s' are not compatible: %s != %s", LOWER_PARAM,
                            UPPER_PARAM, lower, upperParam));
        }
        if (convertedUpperParam.doubleValue() <= lower.doubleValue()) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' (%s) is less than or equal to '%s' (%s) parameter.", UPPER_PARAM,
                            convertedUpperParam, LOWER_PARAM, lower));
        }
        this.upper = convertedUpperParam;

        final Object paramValue = context.getConfiguration().get(INVERTED_PARAM);
        final boolean inverted = paramValue == null ? false : Boolean.valueOf(paramValue.toString());
        this.inRange = inverted ? OnOffType.OFF : OnOffType.ON;
        this.notInRange = inverted ? OnOffType.ON : OnOffType.OFF;
    }

    private @Nullable QuantityType<?> getParam(ProfileContext context, String param) {
        final Object paramValue = context.getConfiguration().get(param);
        logger.debug("Configuring profile with {} parameter '{}'", param, paramValue);
        if (paramValue instanceof String) {
            try {
                return new QuantityType<>((String) paramValue);
            } catch (IllegalArgumentException e) {
                logger.error("Cannot convert value '{}' of parameter {} into a valid QuantityType.", paramValue, param);
            }
        } else if (paramValue instanceof BigDecimal) {
            final BigDecimal value = (BigDecimal) paramValue;
            return QuantityType.valueOf(value.doubleValue(), AbstractUnit.ONE);
        }
        return null;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.RANGE;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    @Override
    public void onCommandFromHandler(Command command) {
        final Type mappedCommand = mapValue(command);
        logger.trace("Mapped command from '{}' to command '{}'.", command, mappedCommand);
        if (mappedCommand instanceof Command) {
            callback.sendCommand((Command) mappedCommand);
        }
    }

    @Override
    public void onCommandFromItem(Command command) {
        // do nothing
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        final Type mappedState = mapValue(state);
        logger.trace("Mapped state from '{}' to state '{}'.", state, mappedState);
        if (mappedState instanceof State) {
            callback.sendUpdate((State) mappedState);
        }
    }

    private Type mapValue(Type value) {
        if (value instanceof QuantityType) {
            final QuantityType<?> qtState = (QuantityType<?>) value;
            final QuantityType<?> finalLower;
            final QuantityType<?> finalUpper;
            if (lower.getUnit() == Units.ONE && upper.getUnit() == Units.ONE) {
                // allow bounds without unit -> implicitly assume its the same as the one from the state, but warn
                // the user
                finalLower = new QuantityType<>(lower.toBigDecimal(), qtState.getUnit());
                finalUpper = new QuantityType<>(upper.toBigDecimal(), qtState.getUnit());
                logger.warn(
                        "Received a QuantityType '{}' with unit, but the boundaries are defined as a plain number without units (lower={}, upper={}), please consider adding units to them.",
                        value, lower, upper);
            } else {
                finalLower = lower.toUnit(qtState.getUnit());
                finalUpper = upper.toUnit(qtState.getUnit());
                if (finalLower == null || finalUpper == null) {
                    logger.warn(
                            "Cannot compare state '{}' to boundaries because units (lower={}, upper={}) do not match.",
                            qtState, lower, upper);
                    return previousType;
                }
            }
            return previousType = mapValue(finalLower.doubleValue(), finalUpper.doubleValue(), qtState.doubleValue());
        } else if (value instanceof DecimalType) {
            return previousType = mapValue(lower.doubleValue(), upper.doubleValue(),
                    ((DecimalType) value).doubleValue());
        }
        return previousType;
    }

    private Type mapValue(double lower, double upper, double value) {
        return lower <= value && value <= upper ? inRange : notInRange;
    }
}
