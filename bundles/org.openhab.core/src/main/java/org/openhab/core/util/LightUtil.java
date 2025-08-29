/*
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
package org.openhab.core.util;

import java.math.BigDecimal;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;

/**
 * The {@link LightUtil} provides helper functions for lights.
 * See also {@link ColorUtil} for more general color conversion utilities.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightUtil {

    /**
     * Default minimum brightness percent to consider a light "ON" if no other value is provided.
     */
    public static final PercentType DEFAULT_MINIMUM_BRIGHTNESS = new PercentType("1.0");

    /**
     * Default maximum brightness percent to use if no other value is provided.
     */
    public static final PercentType DEFAULT_MAXIMUM_BRIGHTNESS = PercentType.HUNDRED;

    /**
     * Default 'warmest' white color temperature values.
     */
    public static final double DEFAULT_WARMEST_KELVIN = 2000;
    public static final double DEFAULT_WARMEST_MIRED = 1000000 / DEFAULT_WARMEST_KELVIN;

    /**
     * Default 'coolest' white color temperature values.
     */
    public static final double DEFAULT_COOLEST_KELVIN = 6500;
    public static final double DEFAULT_COOLEST_MIRED = 1000000 / DEFAULT_COOLEST_KELVIN;

    /**
     * Step value for IncreaseDecreaseType commands.
     */
    public static final double INCREASE_DECREASE_STEP = 10.0;

    /**
     * Private helper that returns a PercentType brightness state from the optionally provided brightness
     * provider, or null if no valid brightness provider exists. Valid brightness providers are:
     * <p>
     * <li>a PercentType</li>
     * <li>an HSBType (from which the brightness is extracted)</li>
     * <li>a Number (which is converted to PercentType)</li>
     *
     * @param optionalBrightnessProvider an optional single object argument that provides the target brightness
     *
     * @return the extracted brightness as PercentType, or null if not found
     */
    private static @Nullable PercentType brightnessStateFrom(Object... optionalBrightnessProvider) {
        if (optionalBrightnessProvider.length > 0) {
            Object brightnessProvider = optionalBrightnessProvider[0];
            if (brightnessProvider instanceof PercentType percent) {
                return percent;
            } else if (brightnessProvider instanceof HSBType hsbType) {
                return hsbType.getBrightness();
            } else if (brightnessProvider instanceof Number number) {
                return new PercentType(new BigDecimal(number.doubleValue()));
            }
        }
        return null;
    }

    /**
     * Returns a PercentType brightness state from the given HSBType color state.
     *
     * @param hsbState the HSBType from which to extract the brightness
     *
     * @return the brightness as PercentType
     */
    public static PercentType brightnessStateFrom(HSBType hsbState) {
        return hsbState.getBrightness();
    }

    /**
     * Returns a PercentType brightness state based on the given OnOffType state and the optionally provided brightness.
     * If the OnOffType is OFF, it returns 0%. If it is ON, it returns the provided brightness or the default maximum
     * brightness if no brightness is provided.
     *
     * @param onOffState the OnOffType state
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the 'ON' state
     *            brightness
     *
     * @return the corresponding PercentType brightness state
     */
    public static PercentType brightnessStateFrom(OnOffType onOffState, Object... optionalOnStateBrightnessProvider) {
        return OnOffType.OFF.equals(onOffState) ? PercentType.ZERO
                : brightnessStateFrom(optionalOnStateBrightnessProvider) instanceof PercentType percent ? percent
                        : DEFAULT_MAXIMUM_BRIGHTNESS;
    }

    /**
     * Returns a PercentType brightness state based on the given IncreaseDecreaseType command and the prior brightness.
     * The brightness is increased or decreased by 10% (INCREASE_DECREASE_STEP), clamped to the range of 0% to 100%.
     *
     * @param priorBrightness the original brightness PercentType
     * @param incDec the IncreaseDecreaseType indicating whether to increase or decrease brightness
     *
     * @return a new PercentType with the adjusted brightness
     */
    public static PercentType brightnessStateFrom(PercentType priorBrightness, IncreaseDecreaseType incDec) {
        double brightness = ((IncreaseDecreaseType.INCREASE.equals(incDec) ? 1 : -1) * INCREASE_DECREASE_STEP)
                + priorBrightness.doubleValue();
        return new PercentType(new BigDecimal(Math.min(Math.max(brightness, 0.0), 100.0)));
    }

    /**
     * Returns a PercentType brightness state that results from applying the given Command to the prior brightness.
     *
     * @param priorBrightness the prior brightness PercentType
     * @param command the command from OH core
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the 'ON' state
     *            brightness
     *
     * @return a new PercentType with the adjusted brightness
     *
     * @throws IllegalArgumentException if passed an unsupported command
     */
    public static PercentType brightnessStateFrom(PercentType priorBrightness, Command command,
            Object... optionalOnStateBrightnessProvider) throws IllegalArgumentException {
        if (command instanceof PercentType percentCommand) {
            return percentCommand;
        } else if (command instanceof OnOffType onOffCommand) {
            return brightnessStateFrom(onOffCommand, optionalOnStateBrightnessProvider);
        } else if (command instanceof IncreaseDecreaseType incDecCommand) {
            return brightnessStateFrom(priorBrightness, incDecCommand);
        }
        throw new IllegalArgumentException("Unsupported command type: " + command.getClass().getName());
    }

    /**
     * Returns an HSBType color state based on the given QuantityType color temperature using the optionally provided
     * brightness. If no brightness is provided, the default maximum brightness percent is used.
     *
     * @param colorTemperature the color temperature; must be in a unit that is convertible to Kelvin
     * @param optionalBrightnessProvider an optional single object argument that provides the target brightness
     *
     * @return the corresponding HSBType
     * @throws IllegalArgumentException if the color temperature cannot be converted to Kelvin
     */
    public static HSBType colorStateFrom(QuantityType<?> colorTemperature, Object... optionalBrightnessProvider)
            throws IllegalArgumentException {
        try {
            QuantityType<?> kelvin = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.KELVIN));
            PercentType brightness = brightnessStateFrom(optionalBrightnessProvider) instanceof PercentType percent
                    ? percent
                    : DEFAULT_MAXIMUM_BRIGHTNESS;
            return colorStateFrom(ColorUtil.xyToHsb(ColorUtil.kelvinToXY(kelvin.doubleValue())), brightness);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("colorTemperature parameter must be convertible to Kelvin");
        }
    }

    /**
     * Returns an HSBType color state based on the given prior HSBType color state and the optionally provided new
     * brightness. If no brightness is provided, the brightness from the original HSBType is retained.
     *
     * @param priorHSBState the original HSBType
     * @param optionalBrightnessProvider an optional single object argument that provides the target brightness
     *
     * @return a new HSBType with the adjusted brightness
     */
    public static HSBType colorStateFrom(HSBType priorHSBState, Object... optionalBrightnessProvider) {
        PercentType brightness = brightnessStateFrom(optionalBrightnessProvider) instanceof PercentType percent
                ? percent
                : priorHSBState.getBrightness();
        return new HSBType(priorHSBState.getHue(), priorHSBState.getSaturation(), brightness);
    }

    /**
     * Returns an HSBType color state based on the given OnOffType state and prior HSBType color state. If the
     * OnOffType is OFF, it returns a brightness of 0%. If it is ON, it returns the optionally provided brightness
     * or the brightness from the prior HSBType if no brightness is provided.
     *
     * @param priorHSBState the original HSBType
     * @param onOffState the OnOffType state
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the 'ON' state
     *            brightness
     *
     * @return a new HSBType with the adjusted brightness
     */
    public static HSBType colorStateFrom(HSBType priorHSBState, OnOffType onOffState,
            Object... optionalOnStateBrightnessProvider) {
        return colorStateFrom(priorHSBState, brightnessStateFrom(onOffState, optionalOnStateBrightnessProvider));
    }

    /**
     * Returns an HSBType color state based on the given IncreaseDecreaseType and the brightness from the prior HSBType
     * color state. The brightness is increased or decreased by 10% (INCREASE_DECREASE_STEP), clamped to the range of 0%
     * to 100%.
     *
     * @param priorHSBState the original HSBType state
     * @param incDec the IncreaseDecreaseType indicating whether to increase or decrease brightness
     *
     * @return a new HSBType with the adjusted brightness
     */
    public static HSBType colorStateFrom(HSBType priorHSBState, IncreaseDecreaseType incDec) {
        return colorStateFrom(priorHSBState, brightnessStateFrom(priorHSBState.getBrightness(), incDec));
    }

    /**
     * Returns an HSBType color state that results from applying the given Command to the prior HSBType color state.
     *
     * @param priorHSBState the prior HSBType state
     * @param command the command from OH core
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the 'ON' state
     *            brightness
     *
     * @return a new HSBType with the adjusted brightness
     * @throws IllegalArgumentException if passed an unsupported command
     */
    public static HSBType colorStateFrom(HSBType priorHSBState, Command command,
            Object... optionalOnStateBrightnessProvider) throws IllegalArgumentException {
        if (command instanceof HSBType hsbCommand) {
            return hsbCommand;
        } else if (command instanceof PercentType percent) {
            return colorStateFrom(priorHSBState, percent);
        } else if (command instanceof OnOffType onOff) {
            return colorStateFrom(priorHSBState, onOff, optionalOnStateBrightnessProvider);
        } else if (command instanceof IncreaseDecreaseType incDec) {
            return colorStateFrom(priorHSBState, incDec);
        } else if (command instanceof QuantityType<?> quantity) {
            return colorStateFrom(quantity, priorHSBState.getBrightness());
        }
        throw new IllegalArgumentException("Unsupported command type: " + command.getClass().getName());
    }

    /**
     * Returns an approximate QuantityType color temperature in Kelvin from the given HSBType color state. The
     * brightness component of the HSBType is ignored. The returned value is clamped to the minimum valid Kelvin
     * value in order to address the caveats mentioned in {@link ColorUtil#xyToKelvin(double[])}.
     *
     * @param hsbState the HSBType to convert
     *
     * @return the corresponding color temperature QuantityType in Kelvin, clamped to a minimum valid value
     */
    public static QuantityType<?> colorTemperatureApproximationFrom(HSBType hsbState) {
        return QuantityType.valueOf(
                Math.max(DEFAULT_WARMEST_KELVIN,
                        ColorUtil.xyToKelvin(ColorUtil.hsbToXY(colorStateFrom(hsbState, PercentType.HUNDRED)))),
                Units.KELVIN);
    }

    /**
     * Returns a QuantityType color temperature in Kelvin based on the given PercentType color temperature using the
     * optionally provided minimum and maximum Mired values for scaling. If no min/max values are provided, default
     * values are used.
     *
     * @param colorTemperaturePercent the PercentType color temperature (0% = coolest, 100% = warmest)
     * @param optionalMinMaxMiredProvider an optional argument of up to two doubles that provide minimum and maximum
     *            Mired values. The first must be the coolest white (lower Mired), the second must the warmest white
     *            (higher Mired)
     *
     * @return the corresponding QuantityType color temperature in Kelvin
     * @throws IllegalArgumentException if passed invalid minimum or maximum Mired values
     */
    public static QuantityType<?> colorTemperatureFrom(PercentType colorTemperaturePercent,
            double... optionalMinMaxMiredProvider) throws IllegalArgumentException {
        double cool = optionalMinMaxMiredProvider.length > 0 ? optionalMinMaxMiredProvider[0] : DEFAULT_COOLEST_MIRED;
        double warm = optionalMinMaxMiredProvider.length > 1 ? optionalMinMaxMiredProvider[1] : DEFAULT_WARMEST_MIRED;
        if (warm <= cool) {
            throw new IllegalArgumentException("color temperature warm parameter must be greater than cool parameter");
        }
        double mired = cool + ((warm - cool) * colorTemperaturePercent.doubleValue() / 100.0);
        // Mired always converts to Kelvin, so no need to catch NPE here
        return Objects.requireNonNull(QuantityType.valueOf(mired, Units.MIRED).toInvertibleUnit(Units.KELVIN));
    }

    /**
     * Returns a PercentType color temperature based on the given QuantityType color temperature using the optionally
     * provided minimum and maximum Mired values for scaling. If no min/max values are provided, default values are
     * used. The returned percent is clamped to the range of 0% to 100%.
     *
     * @param colorTemperature the color temperature; must be in a unit that is convertible to Mired
     * @param optionalMinMaxMiredProvider an optional argument of up to two doubles that provide minimum and maximum
     *            Mired values. The first must be the coolest white (lower Mired), the second must the warmest white
     *            (higher Mired)
     *
     * @return the corresponding PercentType color temperature
     * @throws IllegalArgumentException if passed an invalid color temperature, or minimum, maximum Mired values
     */
    public static PercentType colorTemperaturePercentFrom(QuantityType<?> colorTemperature,
            double... optionalMinMaxMiredProvider) throws IllegalArgumentException {
        double cool = optionalMinMaxMiredProvider.length > 0 ? optionalMinMaxMiredProvider[0] : DEFAULT_COOLEST_MIRED;
        double warm = optionalMinMaxMiredProvider.length > 1 ? optionalMinMaxMiredProvider[1] : DEFAULT_WARMEST_MIRED;
        if (warm <= cool) {
            throw new IllegalArgumentException("color temperature warm parameter must be greater than cool parameter");
        }
        try {
            QuantityType<?> mired = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.MIRED));
            double percent = 100.0 * (mired.doubleValue() - cool) / warm - cool;
            return new PercentType(new BigDecimal(Math.min(Math.max(percent, 0.0), 100.0)));
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("colorTemperature parameter must be convertible to Mired");
        }
    }

    /**
     * Returns an OnOffType state based on the given PercentType brightness state. If the brightness is greater than or
     * equal to the optionallly provided minimum brightness, it returns ON; otherwise, OFF. If no minimum brightness is
     * provided, a default value is used.
     *
     * @param brightnessState the brightness PercentType to evaluate
     * @param optionalMinimumBrightnessPovider an optional single object argument that provides the minimum brightness
     *
     * @return OnOffType.ON if brightness >= minimum brightness, otherwise OnOffType.OFF
     */
    public static OnOffType onOffStateFrom(PercentType brightnessState, Object... optionalMinimumBrightnessPovider) {
        PercentType minimumBrightness = brightnessStateFrom(
                optionalMinimumBrightnessPovider) instanceof PercentType percent ? percent : DEFAULT_MINIMUM_BRIGHTNESS;
        return OnOffType.from(brightnessState.doubleValue() >= minimumBrightness.doubleValue());
    }

    /**
     * Returns an OnOffType state based on the brightness of the given HSBType color state. If the brightness is
     * greater than or equal to the optionallly provided minimum brightness, it returns ON; otherwise, OFF. If no
     * minimum brightness is provided, a default value is used.
     *
     * @param hsbState the HSBType to evaluate
     * @param optionalMinimumBrightnessProvider an optional single object argument that provides the minimum brightness
     *
     * @return OnOffType.ON if brightness >= minimum brightness, otherwise OnOffType.OFF
     */
    public static OnOffType onOffStateFrom(HSBType hsbState, Object... optionalMinimumBrightnessProvider) {
        return onOffStateFrom(hsbState.getBrightness(), optionalMinimumBrightnessProvider);
    }
}
