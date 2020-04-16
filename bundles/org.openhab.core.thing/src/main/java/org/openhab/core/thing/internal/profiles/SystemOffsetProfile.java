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

import java.math.BigDecimal;

import javax.measure.UnconvertibleException;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;
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
 * Applies the given parameter "offset" to a QuantityType or DecimalType state
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class SystemOffsetProfile implements StateProfile {

    private static final @Nullable QuantityType<Temperature> ZERO_CELSIUS_IN_KELVIN = new QuantityType<>(0,
            SIUnits.CELSIUS).toUnit(SmartHomeUnits.KELVIN);
    private static final @Nullable QuantityType<Temperature> ZERO_FAHRENHEIT_IN_KELVIN = new QuantityType<>(0,
            ImperialUnits.FAHRENHEIT).toUnit(SmartHomeUnits.KELVIN);

    private final Logger logger = LoggerFactory.getLogger(SystemOffsetProfile.class);
    private static final String OFFSET_PARAM = "offset";

    private final ProfileCallback callback;
    private final ProfileContext context;

    private @Nullable QuantityType<?> offset;

    public SystemOffsetProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;

        Object paramValue = this.context.getConfiguration().get(OFFSET_PARAM);
        logger.debug("Configuring profile with {} parameter '{}'", OFFSET_PARAM, paramValue);
        if (paramValue instanceof String) {
            try {
                offset = new QuantityType<>((String) paramValue);
            } catch (IllegalArgumentException e) {
                logger.error("Cannot convert value '{}' of parameter '{}' into a valid offset of type QuantityType.",
                        paramValue, OFFSET_PARAM);
            }
        } else if (paramValue instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) paramValue;
            offset = new QuantityType<>(bd.toString());
        } else {
            logger.error("Parameter '{}' is not of type String or BigDecimal", OFFSET_PARAM);
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.OFFSET;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        callback.handleUpdate((State) applyOffset(state, false));
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Type applyOffset(Type state, boolean towardsItem) {
        if (state instanceof UnDefType) {
            // we cannot adjust undef or null values, thus we simply return them without reporting an error or warning
            return state;
        }

        QuantityType finalOffset = offset;
        if (finalOffset == null) {
            logger.warn(
                    "Offset not configured correctly, please make sure it is of type QuantityType, e.g. \"3\", \"-1.4\", \"3.2°C\". Using offset 0 now.");
            finalOffset = new QuantityType<>("0");
        }

        if (!towardsItem) {
            finalOffset = finalOffset.negate();
        }

        Type result = UnDefType.UNDEF;

        if (state instanceof QuantityType) {
            QuantityType qtState = (QuantityType) state;
            try {
                if (finalOffset.getUnit() == SmartHomeUnits.ONE) {
                    // allow offsets without unit -> implicitly assume its the same as the one from the state, but warn
                    // the user
                    finalOffset = new QuantityType<>(finalOffset.toBigDecimal(), qtState.getUnit());
                    logger.warn(
                            "Received a QuantityType state '{}' with unit, but the offset is defined as a plain number without unit ({}), please consider adding a unit to the profile offset.",
                            state, offset);
                }
                // take care of temperatures because they start at offset -273°C = 0K
                if (SmartHomeUnits.KELVIN.equals(qtState.getUnit().getSystemUnit())) {
                    QuantityType<Temperature> tmp = handleTemperature(qtState, finalOffset);
                    if (tmp != null) {
                        result = tmp;
                    }
                } else {
                    result = qtState.add(finalOffset);
                }

            } catch (UnconvertibleException e) {
                logger.warn("Cannot apply offset '{}' to state '{}' because types do not match.", finalOffset, qtState);
            }
        } else if (state instanceof DecimalType && SmartHomeUnits.ONE.equals(finalOffset.getUnit())) {
            DecimalType decState = (DecimalType) state;
            result = new DecimalType(decState.doubleValue() + finalOffset.doubleValue());
        } else {
            logger.warn(
                    "Offset '{}' cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    offset, state);
            result = state;
        }

        return result;
    }

    private @Nullable QuantityType<Temperature> handleTemperature(QuantityType<Temperature> qtState,
            QuantityType<Temperature> offset) {
        QuantityType<Temperature> finalOffset;

        // do the math in kelvin and afterwards convert it back to the unit of the state
        QuantityType<Temperature> kelvinState = qtState.toUnit(SmartHomeUnits.KELVIN);
        QuantityType<Temperature> kelvinOffset = offset.toUnit(SmartHomeUnits.KELVIN);

        if (SIUnits.CELSIUS.equals(offset.getUnit())) {
            finalOffset = kelvinOffset.add(ZERO_CELSIUS_IN_KELVIN.negate());
        } else if (ImperialUnits.FAHRENHEIT.equals(offset.getUnit())) {
            finalOffset = kelvinOffset.add(ZERO_FAHRENHEIT_IN_KELVIN.negate());
        } else {
            // offset is already in kelvin
            finalOffset = offset;
        }

        kelvinState = kelvinState.add(finalOffset);
        return kelvinState.toUnit(qtState.getUnit());
    }
}
