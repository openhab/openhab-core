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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;

/**
 * The {@link LightUtil} provides helper functions for lights.
 * See also {@link ColorUtils} for more general color conversion utilities.
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
     * Default minimum color temperature Kelvin value. Conversions are subject to the caveats mentioned in
     * {@link ColorUtil#xyToKelvin()} so when converting HSBType values to Kelvin, values below this will
     * be clamped to this value.
     */
    public static final double DEFAULT_MINIMUM_KELVIN = 2000; // aka 500 mirek

    /**
     * Default maximum color temperature Kelvin value.
     */
    public static final double DEFAULT_MAXIMUM_KELVIN = 6500; // aka 153 mirek

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
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the on state brightness
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
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the on state brightness
     *
     * @return a new PercentType with the adjusted brightness
     *
     * @throws IllegalArgumentException if passed an unsopported command
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
     * Returns an HSBType color state based on the given color temperature in Kelvin using the optionally provided
     * brightness. If no brightness is provided, the default maximum brightness percent is used.
     *
     * @param kelvin the color temperature in Kelvin
     * @param optionalBrightnessProvider an optional single object argument that provides the target brightness
     *
     * @return the corresponding HSBType
     */
    public static HSBType colorStateFrom(double kelvin, Object... optionalBrightnessProvider) {
        PercentType brightness = brightnessStateFrom(optionalBrightnessProvider) instanceof PercentType percent
                ? percent
                : DEFAULT_MAXIMUM_BRIGHTNESS;
        return colorStateFrom(ColorUtil.xyToHsb(ColorUtil.kelvinToXY(kelvin)), brightness);
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
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the on state brightness
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
     * @param optionalOnStateBrightnessProvider an optional single object argument that provides the on state brightness
     *
     * @return a new HSBType with the adjusted brightness
     *
     * @throws IllegalArgumentException if passed an unsopported command
     */
    public static HSBType colorStateFrom(HSBType priorHSBState, Command command,
            Object... optionalOnStateBrightnessProvider) throws IllegalArgumentException {
        if (command instanceof HSBType hsbCommand) {
            return hsbCommand;
        } else if (command instanceof PercentType percentCommand) {
            return colorStateFrom(priorHSBState, percentCommand);
        } else if (command instanceof OnOffType onOffCommand) {
            return colorStateFrom(priorHSBState, onOffCommand, optionalOnStateBrightnessProvider);
        } else if (command instanceof IncreaseDecreaseType incDecCommand) {
            return colorStateFrom(priorHSBState, incDecCommand);
        }
        throw new IllegalArgumentException("Unsupported command type: " + command.getClass().getName());
    }

    /**
     * Returns an approximate color temperature in Kelvin from the given HSBType color state. The brightness component
     * of the HSBType is ignored. The returned value is clamped to a minimum valid Kelvin value. Subject to the caveats
     * mentioned in {@link ColorUtil#xyToKelvin()}
     *
     * @param hsbState the HSBType to convert
     *
     * @return the corresponding color temperature in Kelvin, clamped to a minimum valid value
     */
    public static DecimalType approximateKelvinFrom(HSBType hsbState) {
        return new DecimalType(Math.max(DEFAULT_MINIMUM_KELVIN, ColorUtil.xyToKelvin(ColorUtil.hsbToXY(hsbState))));
    }

    /**
     * Returns a color temperature in Kelvin based on the given PercentType color temperature using the optionally
     * provided minimum and maximum Kelvin values for scaling. If no min/max values are provided, default values are
     * used.
     *
     * @param colorTemperaturePercent the PercentType color temperature (0% = coolest, 100% = warmest)
     * @param optionalMinMaxKelvinProvider an optional argument of up to two doubles that provide minimum and maximum
     *            Kelvin values. The first value is the minimum (coolest), the second is the maximum (warmest)
     *
     * @return the corresponding color temperature in Kelvin
     */
    public static DecimalType kelvinFrom(PercentType colorTemperaturePercent, double... optionalMinMaxKelvinProvider) {
        double coolMirek = 1000000 / optionalMinMaxKelvinProvider.length > 0 ? optionalMinMaxKelvinProvider[0]
                : DEFAULT_MINIMUM_KELVIN;
        double warmMirek = 1000000 / optionalMinMaxKelvinProvider.length > 1 ? optionalMinMaxKelvinProvider[1]
                : DEFAULT_MAXIMUM_KELVIN;
        double thisMirek = coolMirek + ((warmMirek - coolMirek) * colorTemperaturePercent.doubleValue() / 100.0);
        return new DecimalType(1000000 / thisMirek);
    }

    /**
     * Returns a PercentType color temperature based on the given color temperature in Kelvin using the optionally
     * provided minimum and maximum Kelvin values for scaling. If no min/max values are provided, default values are
     * used. The returned percent is clamped to the range of 0% to 100%.
     *
     * @param kelvin the color temperature in Kelvin
     * @param optionalMinMaxKelvinProvider an optional argument of up to two doubles that provide minimum and maximum
     *            Kelvin values. The first value is the minimum (coolest), the second is the maximum (warmest)
     *
     * @return the corresponding PercentType color temperature
     */
    public static PercentType colorTemperaturePercentFrom(DecimalType kelvin, double... optionalMinMaxKelvinProvider) {
        double thisMirek = 1000000
                / Math.min(Math.max(kelvin.doubleValue(), DEFAULT_MINIMUM_KELVIN), DEFAULT_MAXIMUM_KELVIN);
        double coolMirek = 1000000 / optionalMinMaxKelvinProvider.length > 0 ? optionalMinMaxKelvinProvider[0]
                : DEFAULT_MINIMUM_KELVIN;
        double warmMirek = 1000000 / optionalMinMaxKelvinProvider.length > 1 ? optionalMinMaxKelvinProvider[1]
                : DEFAULT_MAXIMUM_KELVIN;
        double percent = Math.min(Math.max(100.0 * (thisMirek - coolMirek) / (warmMirek - coolMirek), 0.0), 100.0);
        return new PercentType(new BigDecimal(percent));
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
     * @param optionalMinimumBrightnessPovider an optional single object argument that provides the minimum brightness
     *
     * @return OnOffType.ON if brightness >= minimum brightness, otherwise OnOffType.OFF
     */
    public static OnOffType onOffStateFrom(HSBType hsbState, Object... optionalMinimumBrightnessProvider) {
        return onOffStateFrom(hsbState.getBrightness(), optionalMinimumBrightnessProvider);
    }
}
