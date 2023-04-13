/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
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
 * Applies the given parameter "offset" to a {@link QuantityType} or {@link DecimalType} state.
 *
 * @author Stefan Triller - Initial contribution
 * @author Jimmy Tanagra - Add mutiplier parameter
 */
@NonNullByDefault
public class SystemOffsetProfile implements StateProfile {

    static final String OFFSET_PARAM = "offset";
    static final String MULTIPLIER_PARAM = "multiplier";

    private final Logger logger = LoggerFactory.getLogger(SystemOffsetProfile.class);

    private final ProfileCallback callback;

    private final QuantityType<?> offset;
    private final BigDecimal multiplier;

    public SystemOffsetProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        Object offsetParam = context.getConfiguration().get(OFFSET_PARAM);
        if (offsetParam == null) {
            this.offset = QuantityType.ZERO;
        } else {
            this.offset = parseQuantity(offsetParam).orElseGet(() -> {
                logger.warn("Parameter '{}' is not a QuantityType or a BigDecimal, e.g. '3', '-1.4' or '3.2 °C'.",
                        OFFSET_PARAM);
                return QuantityType.ZERO;
            });
        }

        Object multiplierParam = context.getConfiguration().get(MULTIPLIER_PARAM);
        if (multiplierParam == null) {
            this.multiplier = BigDecimal.ONE;
        } else {
            this.multiplier = ConfigParser.valueAsOrElse(multiplierParam, BigDecimal.class, BigDecimal.ONE);
        }

        if (offsetParam == null && multiplierParam == null) {
            logger.warn("Neither an offset nor a multiplier was defined for this profile.");
        }
    }

    private Optional<QuantityType<?>> parseQuantity(Object value) {
        if (value instanceof String strValue) {
            try {
                return Optional.of(new QuantityType<>(strValue));
            } catch (IllegalArgumentException e) {
                // return an empty Optional below.
            }
        } else if (value instanceof BigDecimal offsetDecimal) {
            return Optional.of(new QuantityType<>(offsetDecimal, Units.ONE));
        }
        return Optional.empty();
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.OFFSET;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        Type result = transformValue(command, false);
        if (result instanceof Command commandResult) {
            callback.handleCommand(commandResult);
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        Type result = transformValue(command, true);
        if (result instanceof Command commandResult) {
            callback.sendCommand(commandResult);
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        Type result = transformValue(state, true);
        if (result instanceof State stateResult) {
            callback.sendUpdate(stateResult);
        }
    }

    private @Nullable Type transformValue(Type value, boolean towardsItem) {
        if (towardsItem) {
            return applyOffset(applyMultiplier(value, true), true);
        } else {
            return applyMultiplier(applyOffset(value, false), false);
        }
    }

    private @Nullable Type applyMultiplier(@Nullable Type value, boolean towardsItem) {
        if (multiplier.equals(BigDecimal.ONE)) {
            return value;
        }

        if (value instanceof QuantityType<?> qtyValue) {
            return towardsItem ? qtyValue.multiply(multiplier) : qtyValue.divide(multiplier);
        } else if (value instanceof DecimalType decValue) {
            BigDecimal bdValue = decValue.toBigDecimal();
            bdValue = towardsItem ? bdValue.multiply(multiplier) : bdValue.divide(multiplier);
            return new DecimalType(bdValue);
        }

        return value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private @Nullable Type applyOffset(@Nullable Type state, boolean towardsItem) {
        if (state instanceof UnDefType || state == null) {
            // we cannot adjust UNDEF or NULL values, thus we simply return them without reporting an error or warning
            return state;
        }

        if (offset.equals(QuantityType.ZERO)) {
            return state;
        }

        QuantityType finalOffset = towardsItem ? offset : offset.negate();
        if (state instanceof QuantityType qtState) {
            if (Units.ONE.equals(finalOffset.getUnit()) && !Units.ONE.equals(qtState.getUnit())) {
                // allow offsets without unit -> implicitly assume it's the same as the one from the state, but warn
                // the user
                logger.warn(
                        "Received a QuantityType state '{}' with unit, but the offset is defined as a plain number without unit ({}), please consider adding a unit to the profile offset.",
                        state, offset);
                finalOffset = new QuantityType<>(finalOffset.toBigDecimal(), qtState.getUnit());
            } else {
                finalOffset = finalOffset.toUnitRelative(qtState.getUnit());
                if (finalOffset == null) {
                    logger.warn("Cannot apply offset '{}' to state '{}' because types do not match.", finalOffset,
                            qtState);
                    return null;
                }
            }
            return qtState.add(finalOffset);
        } else if (state instanceof DecimalType decState && Units.ONE.equals(finalOffset.getUnit())) {
            return new DecimalType(decState.toBigDecimal().add(finalOffset.toBigDecimal()));
        }

        logger.warn(
                "Offset '{}' cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                offset, state);
        return state;
    }
}
