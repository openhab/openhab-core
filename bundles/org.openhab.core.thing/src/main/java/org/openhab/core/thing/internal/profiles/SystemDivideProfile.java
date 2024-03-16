/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import javax.measure.UnconvertibleException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
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

/**
 * Applies the given parameter "divisor" to a {@link QuantityType} or {@link DecimalType} state.
 *
 * @author John Cocula - Initial contribution based closely on SystemOffsetProfile
 */
@NonNullByDefault
public class SystemDivideProfile implements StateProfile {

    static final String DIVISOR_PARAM = "divisor";

    private final Logger logger = LoggerFactory.getLogger(SystemDivideProfile.class);

    private final ProfileCallback callback;

    private QuantityType<?> divisor = QuantityType.ONE;

    public SystemDivideProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        Object paramValue = context.getConfiguration().get(DIVISOR_PARAM);
        logger.debug("Configuring profile with {} parameter '{}'", DIVISOR_PARAM, paramValue);
        if (paramValue instanceof String string) {
            try {
                divisor = new QuantityType<>(string);
            } catch (IllegalArgumentException e) {
                logger.error(
                        "Cannot convert value '{}' of parameter '{}' into a valid divisor of type QuantityType. Using divisor 1 now.",
                        paramValue, DIVISOR_PARAM);
            }
        } else if (paramValue instanceof BigDecimal bd) {
            divisor = new QuantityType<>(bd.toString());
        } else {
            logger.error(
                    "Parameter '{}' is not of type String or BigDecimal. Please make sure it is one of both, e.g. 3, \"-1.4\" or \"3.2°C\".",
                    DIVISOR_PARAM);
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.DIVIDE;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand((Command) applyDivide(command, false));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand((Command) applyDivide(command, true));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate((State) applyDivide(state, true));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Type applyDivide(Type state, boolean towardsItem) {
        if (state instanceof UnDefType) {
            // we cannot adjust UNDEF or NULL values, thus we simply return them without reporting an error or warning
            return state;
        }

        QuantityType finalDivisor = divisor;
        Type result = UnDefType.UNDEF;
        if (state instanceof QuantityType qtState) {
            try {
                if (Units.ONE.equals(finalDivisor.getUnit()) && !Units.ONE.equals(qtState.getUnit())) {
                    // allow divisors without unit -> implicitly assume its the same as the one from the state
                    finalDivisor = new QuantityType<>(finalDivisor.toBigDecimal(), qtState.getUnit());
                }
                result = towardsItem ? qtState.divide(finalDivisor) : qtState.multiply(finalDivisor);
            } catch (UnconvertibleException e) {
                logger.warn("Cannot apply divisor '{}' to state '{}' because types do not match.", finalDivisor,
                        qtState);
            }
        } else if (state instanceof DecimalType decState && Units.ONE.equals(finalDivisor.getUnit())) {
            result = new DecimalType(towardsItem ? decState.toBigDecimal().divide(finalDivisor.toBigDecimal())
                    : decState.toBigDecimal().multiply(finalDivisor.toBigDecimal()));
        } else {
            logger.warn(
                    "Divisor '{}' cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    divisor, state);
            result = state;
        }
        return result;
    }
}
