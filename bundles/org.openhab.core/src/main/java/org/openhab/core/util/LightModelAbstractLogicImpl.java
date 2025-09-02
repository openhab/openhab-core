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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Command;

/**
 * The {@link LightModelAbstractLogicImpl} is an abstract class that implements the internal
 * logic for maintaining and modifying the state model of a light. It is used internally (only)
 * by the {@link LightModel} class.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
abstract class LightModelAbstractLogicImpl {

    /*********************************************************************************
     * SECTION: Default Parameters. May be modified during initialization.
     *********************************************************************************/

    private double minimumOnBrightness = 1.0; // minimum brightness percent to consider as light "ON"
    private double warmestMired = 500; // 'warmest' white color temperature
    private double coolestMired = 153; // 'coolest' white color temperature
    private double stepSize = 10.0; // step size for IncreaseDecreaseType commands

    /*********************************************************************************
     * SECTION: Capabilities. May be modified during initialization.
     *********************************************************************************/

    private boolean supportsColor = false; // true if the light supports color
    private boolean rgbLinkedToBrightness = false; // true if RGB(C)(W) values are linked to the brightness
    private boolean supportsRGBW = false; // true if the light supports RGBW
    private boolean supportsRGBCW = false; // true if the light supports RGBCW
    private boolean supportsBrightness = false; // true if the light supports brightness
    private boolean supportsColorTemperature = false; // true if the light supports color temperature

    /*********************************************************************************
     * SECTION: Light state variables. Used at run time only.
     *********************************************************************************/

    private Optional<OnOffType> cachedOnOff = Optional.empty();
    private PercentType cachedBrightness = PercentType.ZERO;
    private HSBType cachedColor = new HSBType();
    private double cachedMired = Double.NaN; // not (yet) known

    /*********************************************************************************
     * SECTION: Constructor.
     *********************************************************************************/

    /**
     * Create a {@link LightModelAbstractLogicImpl} with the given capabilities and parameters.
     * The parameters can be null to use the default.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param supportsRGBW true if the light supports RGBW rather than RGB color control
     * @param supportsRGBCW true if the light supports RGBCW rather than RGB or RGBW color control
     * @param rgbLinkedToBrightness true if RGB values are linked with the 'B' part of the {@link HSBType}
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     */
    LightModelAbstractLogicImpl(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            boolean supportsRGBW, boolean supportsRGBCW, boolean rgbLinkedToBrightness,
            @Nullable Double minimumOnBrightness, @Nullable Double warmestMired, @Nullable Double coolestMired,
            @Nullable Double stepSize) {
        configSetSupportsBrightness(supportsBrightness);
        configSetSupportsColorTemperature(supportsColorTemperature);
        configSetSupportsColor(supportsColor);
        configSetSupportsRGBW(supportsRGBW);
        configSetSupportsRGBCW(supportsRGBCW);
        configSetRgbLinkedToBrightness(rgbLinkedToBrightness);
        if (minimumOnBrightness != null) {
            configSetMinimumOnBrightness(minimumOnBrightness);
        }
        if (warmestMired != null) {
            configSetMiredWarmest(warmestMired);
        }
        if (coolestMired != null) {
            configSetMiredCoolest(coolestMired);
        }
        if (stepSize != null) {
            configSetIncreaseDecreaseStep(stepSize);
        }
    }

    /*********************************************************************************
     * SECTION: Configuration getters and setters. May be used during initialization.
     *********************************************************************************/

    /**
     * Configuration: get the step size for IncreaseDecreaseType commands
     */
    double configGetIncreaseDecreaseStep() {
        return stepSize;
    }

    /**
     * Configuration: get the minimum brightness percent to consider as light "ON"
     */
    double configGetMinimumOnBrightness() {
        return minimumOnBrightness;
    }

    /**
     * Configuration: get the coolest color temperature in Mired
     */
    double configGetMiredCoolest() {
        return coolestMired;
    }

    /**
     * Configuration: get the warmest color temperature in Mired
     */
    double configGetMiredWarmest() {
        return warmestMired;
    }

    /**
     * Configuration: check whether RGB values are linked to the 'HS' parts or all of the 'HSB' parts of {@link HSBType}
     * state, as follows:
     *
     * <ul>
     * <li>If false the RGB values only relate to the 'HS' parts and will not influence or depend on the
     * brightness.</li>
     * <li>If true, the RGB values relate to all of the 'HSB' parts and will influence and depend on the
     * brightness.</li>
     * </ul>
     */
    boolean configGetRgbLinkedToBrightness() {
        return rgbLinkedToBrightness;
    }

    /**
     * Configuration: check if brightness control is supported
     */
    boolean configGetSupportsBrightness() {
        return supportsBrightness;
    }

    /**
     * Configuration: check if color control is supported
     */
    boolean configGetSupportsColor() {
        return supportsColor;
    }

    /**
     * Configuration: check if color temperature control is supported
     */
    boolean configGetSupportsColorTemperature() {
        return supportsColorTemperature;
    }

    /**
     * Configuration: check if RGBCW color control is supported
     */
    boolean configGetSupportsRGBCW() {
        return supportsRGBCW;
    }

    /**
     * Configuration: check if RGBW color control is supported versus RGB only
     */
    boolean configGetSupportsRGBW() {
        return supportsRGBW;
    }

    /**
     * Configuration: set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @throws IllegalArgumentException if the stepSize parameter is out of range
     */
    void configSetIncreaseDecreaseStep(double stepSize) throws IllegalArgumentException {
        if (stepSize < 1.0 || stepSize > 50.0) {
            throw new IllegalArgumentException("Step size '%f' out of range [1.0..50.0]".formatted(stepSize));
        }
        this.stepSize = stepSize;
    }

    /**
     * Configuration: set the minimum brightness percent to consider as light "ON"
     *
     * @param minimumOnBrightness the minimum brightness percent
     * @throws IllegalArgumentException if the minimumBrightness parameter is out of range
     */
    void configSetMinimumOnBrightness(double minimumOnBrightness) throws IllegalArgumentException {
        if (minimumOnBrightness < 0.1 || minimumOnBrightness > 10.0) {
            throw new IllegalArgumentException(
                    "Minimum brightness '%f' out of range [0.1..10.0]".formatted(minimumOnBrightness));
        }
        this.minimumOnBrightness = minimumOnBrightness;
    }

    /**
     * Configuration: set the coolest color temperature in Mired
     *
     * @param coolestMired the coolest supported color temperature in Mired
     * @throws IllegalArgumentException if the coolestMired parameter is out of range or not less than warmestMired
     */
    void configSetMiredCoolest(double coolestMired) throws IllegalArgumentException {
        if (coolestMired < 100.0 || coolestMired > 1000.0) {
            throw new IllegalArgumentException(
                    "Coolest mired '%f' out of range [100.0..1000.0]".formatted(coolestMired));
        }
        if (warmestMired <= coolestMired) {
            throw new IllegalArgumentException(
                    "Warmest Mired '%f' must be greater than coolest Mired '%f'".formatted(warmestMired, coolestMired));
        }
        this.coolestMired = coolestMired;
    }

    /**
     * Configuration: set the warmest color temperature in Mired
     *
     * @param warmestMired the warmest supported color temperature in Mired
     * @throws IllegalArgumentException if the warmestMired parameter is out of range or not greater than coolestMired
     */
    void configSetMiredWarmest(double warmestMired) throws IllegalArgumentException {
        if (warmestMired < 100.0 || warmestMired > 1000.0) {
            throw new IllegalArgumentException(
                    "Warmest Mired '%f' out of range [100.0..1000.0]".formatted(warmestMired));
        }
        if (warmestMired <= coolestMired) {
            throw new IllegalArgumentException(
                    "Warmest Mired '%f' must be greater than coolest mired '%f'".formatted(warmestMired, coolestMired));
        }
        this.warmestMired = warmestMired;
    }

    /**
     * Configuration: set whether RGB values are linked to the 'HS' parts or all of the 'HSB' parts of {@link HSBType}
     * state, as follows:
     *
     * <ul>
     * <li>If false the RGB values only relate to the 'HS' parts and will not influence or depend on the
     * brightness.</li>
     * <li>If true, the RGB values relate to all of the 'HSB' parts and will influence and depend on the
     * brightness.</li>
     * </ul>
     *
     * @param rgbLinkedToBrightness true if RGB values are linked to brightness (i.e., relate to all HSB parts and will
     *            influence and depend on brightness); false if RGB values only relate to hue and saturation and do not
     *            influence or depend on brightness
     */
    void configSetRgbLinkedToBrightness(boolean rgbLinkedToBrightness) {
        this.rgbLinkedToBrightness = rgbLinkedToBrightness;
    }

    /**
     * Configuration: set whether brightness control is supported
     *
     * @param supportsBrightness true if brightness control is supported
     */
    void configSetSupportsBrightness(boolean supportsBrightness) {
        this.supportsBrightness = supportsBrightness;
    }

    /**
     * Configuration: set whether color control is supported
     *
     * @param supportsColor true if color control is supported
     * @throws IllegalArgumentException if color is set false but RGBW or RGBCW are supported
     */
    void configSetSupportsColor(boolean supportsColor) throws IllegalArgumentException {
        if (!supportsColor && (supportsRGBW || supportsRGBCW)) {
            throw new IllegalArgumentException("Light cannot support RGBCW or RGBW without supporting color");
        }
        this.supportsColor = supportsColor;
    }

    /**
     * Configuration: set whether color temperature control is supported
     *
     * @param supportsColorTemperature true if color temperature control is supported
     * @throws IllegalArgumentException if color temperature is set false but RGBCW is supported
     */
    void configSetSupportsColorTemperature(boolean supportsColorTemperature) throws IllegalArgumentException {
        if (!supportsColorTemperature && supportsRGBCW) {
            throw new IllegalArgumentException("Light cannot support RGBCW without supporting color temperature");
        }
        this.supportsColorTemperature = supportsColorTemperature;
    }

    /**
     * Configuration: set whether RGBCW color control is supported
     *
     * @param supportsRGBCW true if RGBCW color control is supported
     * @throws IllegalArgumentException if RGBCW is set true but color or color temperature are not supported, or if
     *             RGBW is also supported
     */
    void configSetSupportsRGBCW(boolean supportsRGBCW) throws IllegalArgumentException {
        if (supportsRGBCW) {
            if (!supportsColor || !supportsColorTemperature) {
                throw new IllegalArgumentException(
                        "Light cannot support RGBCW without supporting both color and color temperature");
            }
            if (supportsRGBW) {
                throw new IllegalArgumentException("Light cannot support both RGBCW  and RGBW color control");
            }
        }
        this.supportsRGBCW = supportsRGBCW;
    }

    /**
     * Configuration: set whether RGBW color control is supported versus RGB only
     *
     * @param supportsRGBW true if RGBW color control is supported
     * @throws IllegalArgumentException if RGBW is set true but color is not supported
     */
    void configSetSupportsRGBW(boolean supportsRGBW) throws IllegalArgumentException {
        if (supportsRGBW && !supportsColor) {
            throw new IllegalArgumentException("Light cannot support RGBW without supporting color");
        }
        this.supportsRGBW = supportsRGBW;
    }

    /*********************************************************************************
     * SECTION: Runtime State getters, setters, and handlers. Only used at runtime.
     *********************************************************************************/

    /**
     * Runtime State: get the brightness or return null if the capability is not supported.
     *
     * @param forceChannelVisible if present and true, return a non-null value even when color is supported.
     * @return PercentType, or null if not supported.
     */
    @Nullable
    PercentType getBrightness(boolean... forceChannelVisible) {
        return supportsBrightness & (!supportsColor || (forceChannelVisible.length > 0 && forceChannelVisible[0]))
                ? cachedColor.getBrightness()
                : null;
    }

    /**
     * Runtime State: get the color or return null if the capability is not supported
     *
     * @return HSBType, or null if not supported.
     */
    @Nullable
    HSBType getColor() {
        return supportsColor ? cachedColor : null;
    }

    /**
     * Runtime State: get the color temperature or return null if the capability is not supported
     * or the Mired value is not known.
     *
     * @return QuantityType in Kelvin representing the color temperature, or null if not supported
     *         or the Mired value is not known.
     */
    @Nullable
    QuantityType<?> getColorTemperature() {
        if (supportsColorTemperature && !Double.isNaN(cachedMired)) {
            return Objects.requireNonNull( // Mired always converts to Kelvin
                    QuantityType.valueOf(cachedMired, Units.MIRED).toInvertibleUnit(Units.KELVIN));
        }
        return null;
    }

    /**
     * Runtime State: get the color temperature in percent or return null if the capability is not supported
     * or the Mired value is not known.
     *
     * @return PercentType in range [0..100] representing [coolest..warmest], or null if not supported
     *         or the Mired value is not known.
     */
    @Nullable
    PercentType getColorTemperaturePercent() {
        if (supportsColorTemperature && !Double.isNaN(cachedMired)) {
            double percent = 100 * (cachedMired - coolestMired) / (warmestMired - coolestMired);
            return new PercentType(new BigDecimal(Math.min(Math.max(percent, 0.0), 100.0)));
        }
        return null;
    }

    /**
     * Runtime State: get the hue in range [0..360].
     *
     * @return double representing the hue in range [0..360].
     */
    double getHue() {
        return cachedColor.getHue().doubleValue();
    }

    /**
     * Runtime State: get the color temperature in Mired, may be NaN if not known.
     *
     * @return double representing the color temperature in Mired.
     */
    double getMired() {
        return cachedMired;
    }

    /**
     * Runtime State: get the on/off state.
     *
     * @param forceChannelVisible if present and true, return a non-null value even if brightness or color are
     *            supported.
     * @return OnOffType representing the on/off state.
     */
    @Nullable
    OnOffType getOnOff(boolean... forceChannelVisible) {
        return (!supportsColor && !supportsBrightness) || (forceChannelVisible.length > 0 && forceChannelVisible[0])
                ? OnOffType.from(cachedColor.getBrightness().doubleValue() >= minimumOnBrightness)
                : null;
    }

    /**
     * Runtime State: get the RGB(C)(W) values as an array of doubles in range [0..255]. Depending on the value of
     * 'supportsRGBW' and 'supportsRGBCW', the array length is either 3 (RGB), 4 (RGBW), or 5 (RGBCW). The array is in
     * the order [red, green, blue, (cold-)(white), (warm-white)]. Depending on the value of 'rgbLinkedToBrightness',
     * the brightness may or may not be used as follows:
     *
     * <ul>
     * <li>{@code supportsRgbDimming == false}: The return result does not depend on the current brightness. In other
     * words the values only relate to the 'HS' part of the {@link HSBType} state. Note: this means that in this case a
     * round trip of setRGBx() followed by getRGBx() will NOT necessarily contain identical values, although the RGB
     * ratios will certainly be the same.</li>
     *
     * <li>{@code supportsRgbDimming == true}: The return result depends on the current brightness. In other words the
     * values relate to all the 'HSB' parts of the {@link HSBType} state.</li>
     * <ul>
     *
     * @return double[] representing the RGB(C)(W) components in range [0..255.0]
     */
    double[] getRGBx() {
        HSBType hsb = rgbLinkedToBrightness ? cachedColor
                : new HSBType(cachedColor.getHue(), cachedColor.getSaturation(), PercentType.HUNDRED);
        if (supportsRGBCW) {
            double[] rgb = Arrays.stream(ColorUtil.hsbToRgbPercent(hsb))
                    .mapToDouble(p -> p.doubleValue() * 255.0 / 100.0).toArray();
            double white = Arrays.stream(rgb).min().orElse(0.0);
            double mired = Double.isNaN(cachedMired) ? (coolestMired + warmestMired) / 2 : cachedMired;
            double warm = (mired - coolestMired) / (warmestMired - coolestMired);
            double[] rgbcw = new double[5];
            System.arraycopy(Arrays.stream(rgb).map(v -> v - white).toArray(), 0, rgbcw, 0, 3);
            rgbcw[3] = (1.0 - warm) * white;
            rgbcw[4] = warm * white;
            return rgbcw;
        } else if (supportsRGBW) {
            return Arrays.stream(ColorUtil.hsbToRgbwPercent(hsb)).mapToDouble(p -> p.doubleValue() * 255.0 / 100.0)
                    .toArray();
        } else { // default to RGB only
            return Arrays.stream(ColorUtil.hsbToRgbPercent(hsb)).mapToDouble(p -> p.doubleValue() * 255.0 / 100.0)
                    .toArray();
        }
    }

    /**
     * Runtime State: get the saturation in range [0..100].
     *
     * @return double representing the saturation in range [0..100].
     */
    double getSaturation() {
        return cachedColor.getSaturation().doubleValue();
    }

    /**
     * Runtime State: get the CIE XY values as an array of doubles in range [0.0..1.0].
     *
     * @return double[] representing the XY components in range [0.0..1.0].
     */
    double[] getXY() {
        return ColorUtil.hsbToXY(new HSBType(cachedColor.getHue(), cachedColor.getSaturation(), PercentType.HUNDRED));
    }

    /**
     * Runtime State: handle a command to change the light's color temperature state. Commands may be one of:
     * <ul>
     * <li>PercentType for color temperature setting</li>
     * <li>QuantityType for color temperature setting</li>
     * </ul>
     * Other commands are deferred to {@link #handleCommand(Command)} for processing just-in-case.
     *
     * @param command the command to handle
     * @throws IllegalArgumentException if the command type is not supported
     */
    void handleColorTemperatureCommand(Command command) throws IllegalArgumentException {
        if (command instanceof PercentType warmness) {
            zInternalHandleColorTemperature(warmness);
        } else if (command instanceof QuantityType<?> temperature) {
            zInternalHandleColorTemperature(temperature);
        } else {
            // defer to the main handler for other command types just-in-case
            handleCommand(command);
        }
    }

    /**
     * Runtime State: handle a command to change the light's state. Commands may be one of:
     * <ul>
     * <li>HSBType for color setting</li>
     * <li>PercentType for brightness setting</li>
     * <li>OnOffType for on/off state setting</li>
     * <li>IncreaseDecreaseType for brightness up/down setting</li>
     * <li>QuantityType for color temperature setting</li>
     * </ul>
     *
     * @param command the command to handle
     * @throws IllegalArgumentException if the command type is not supported
     */
    void handleCommand(Command command) throws IllegalArgumentException {
        if (command instanceof HSBType color) {
            zInternalHandleColor(color);
        } else if (command instanceof PercentType brightness) {
            zInternalHandleBrightness(brightness);
        } else if (command instanceof OnOffType onOff) {
            zInternalHandleOnOff(onOff);
        } else if (command instanceof IncreaseDecreaseType incDec) {
            zInternalHandleIncreaseDecrease(incDec);
        } else if (command instanceof QuantityType<?> temperature) {
            zInternalHandleColorTemperature(temperature);
        } else {
            throw new IllegalArgumentException(
                    "Command '%s' not supported for light states".formatted(command.getClass().getName()));
        }
    }

    /**
     * Runtime State: update the brightness from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param brightness in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    void setBrightness(double brightness) throws IllegalArgumentException {
        zInternalHandleBrightness(zInternalPercentTypeOf(brightness));
    }

    /**
     * Runtime State: update the hue from the remote light, ensuring it is in the range 0.0 to 360.0
     *
     * @param hue in the range [0..360]
     * @throws IllegalArgumentException if the hue parameter is not in the range 0.0 to 360.0
     */
    void setHue(double hue) throws IllegalArgumentException {
        cachedColor = new HSBType(new DecimalType(hue), cachedColor.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Runtime State: update the Mired color temperature from the remote light, and update the cached HSB color
     * accordingly. Constrain the Mired value to be within the warmest and coolest limits. If the mired
     * value is NaN then the cached color is not updated as we cannot determine what it should be.
     *
     * @param mired the color temperature in Mired or NaN if not known
     * @throws IllegalArgumentException if the mired parameter is not in the range [coolestMired..warmestMired]
     */
    void setMired(double mired) throws IllegalArgumentException {
        if (mired < coolestMired || mired > warmestMired) { // NaN is not less than or greater than anything
            throw new IllegalArgumentException(
                    "Mired value '%f' out of range [%f..%f]".formatted(mired, coolestMired, warmestMired));
        }
        if (!Double.isNaN(mired)) { // don't update color if Mired is not known
            HSBType hsb = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / cachedMired));
            cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
        }
        cachedMired = mired;
    }

    /**
     * Runtime State: update the color with RGB(C)(W) fields from the remote light, and update the cached HSB color
     * accordingly. The array must be in the order [red, green, blue, (cold-)(white), (warm-white)]. If white is
     * present but the light does not support white channel(s) then IllegalArgumentException is thrown. Depending
     * on the value of 'rgbLinkedToBrightness', the brightness may or may not change as follows:
     *
     * <ul>
     * <li>{@code rgbLinkedToBrightness == false} both [255,0,0] and [127.5,0,0] change the color to RED without a
     * change in brightness. In other words the values only relate to the 'HS' part of the {@link HSBType} state. Note:
     * this means that in this case a round trip of setRGBx() followed by getRGBx() will NOT necessarily contain
     * identical values, although the RGB ratios will certainly be the same.</li>
     *
     * <li>{@code rgbLinkedToBrightness == true} both [255,0,0] and [127.5,0,0] change the color to RED and the former
     * changes the brightness to 100 percent, whereas the latter changes it to 50 percent. In other words the values
     * relate to all the 'HSB' parts of the {@link HSBType} state.</li>
     * <ul>
     *
     * @param rgbxIn an array of double representing RGB or RGBW values in range [0..255]
     * @throws IllegalArgumentException if the array length is not 3, 4, or 5 depending on the light's capabilities,
     *             or if any of the values are outside the range [0.0 to 255.0]
     */
    void setRGBx(double[] rgbxIn) throws IllegalArgumentException {
        if (rgbxIn.length > 5) {
            throw new IllegalArgumentException("Too many arguments in RGBx array");
        }
        if (rgbxIn.length < 3 || (supportsRGBW && rgbxIn.length < 4) || (supportsRGBCW && rgbxIn.length < 5)) {
            throw new IllegalArgumentException("Too few arguments in RGBx array");
        }
        if (Arrays.stream(rgbxIn).anyMatch(v -> v < 0.0 || v > 255.0)) {
            throw new IllegalArgumentException("RGBx value out of range [0.0..255.0]");
        }

        double[] rgbxOut = rgbxIn;
        if (supportsRGBCW) {
            // merge RGBCW into a RGBW by setting white = cool + warm
            rgbxOut = new double[] { rgbxIn[0], rgbxIn[1], rgbxIn[2], rgbxIn[3] + rgbxIn[4] };
            // set mired according to warm / white ratio
            cachedMired = coolestMired + ((warmestMired - coolestMired) * rgbxIn[4] / rgbxOut[3]);
            // normalise values in case of white over-range
            double max = Arrays.stream(rgbxOut).max().orElse(255.0);
            if (max > 255.0) {
                rgbxOut = Arrays.stream(rgbxOut).map(v -> v * 255.0 / max).toArray();
            }
            // fall through to RGBW processing
        } else if (supportsRGBW) {
            cachedMired = (coolestMired + warmestMired) / 2; // set average mired
        } else {
            cachedMired = Double.NaN; // invalidate mired as we don't know what it should be
        }

        HSBType dimmedHSB = ColorUtil.rgbToHsb(Arrays.stream(rgbxOut).map(d -> d * 100.0 / 255.0)
                .mapToObj(d -> zInternalPercentTypeOf(d)).toArray(PercentType[]::new));

        if (rgbLinkedToBrightness) {
            cachedColor = dimmedHSB;
            zInternalHandleBrightness(dimmedHSB.getBrightness());
        } else {
            cachedColor = new HSBType(dimmedHSB.getHue(), dimmedHSB.getSaturation(), cachedColor.getBrightness());
        }
    }

    /**
     * Runtime State: update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    void setSaturation(double saturation) throws IllegalArgumentException {
        cachedColor = new HSBType(cachedColor.getHue(), zInternalPercentTypeOf(saturation),
                cachedColor.getBrightness());
        cachedMired = Double.NaN; // invalidate mired as we don't know what it should be
    }

    /**
     * Runtime State: update the color with CIE XY fields from the remote light, and update the cached HSB color
     * accordingly.
     *
     * @param x the x field in range [0.0..1.0]
     * @param y the y field in range [0.0..1.0]
     * @throws IllegalArgumentException if any of the XY values are out of range [0.0..1.0]
     */
    void setXY(double x, double y) throws IllegalArgumentException {
        HSBType hsb = ColorUtil.xyToHsb(new double[] { x, y });
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
        cachedMired = Double.NaN; // invalidate mired as we don't know what it should be
    }

    /*********************************************************************************
     * SECTION: Internal private methods. Names have 'z' prefix to indicate private.
     *********************************************************************************/

    /**
     * Internal: handle a write brightness command from OH core
     *
     * @param brightness the brightness to set
     */
    private void zInternalHandleBrightness(PercentType brightness) {
        if (brightness.doubleValue() >= minimumOnBrightness) {
            cachedBrightness = brightness;
            cachedColor = new HSBType(cachedColor.getHue(), cachedColor.getSaturation(), brightness);
            cachedOnOff = Optional.of(OnOffType.ON);
        } else {
            if (!cachedOnOff.isPresent() || OnOffType.ON == cachedOnOff.get()) {
                cachedBrightness = cachedColor.getBrightness();
            }
            cachedColor = new HSBType(cachedColor.getHue(), cachedColor.getSaturation(), PercentType.ZERO);
            cachedOnOff = Optional.of(OnOffType.OFF);
        }
    }

    /**
     * Internal: handle a write color command from OH core
     *
     * @param color the color to set
     */
    private void zInternalHandleColor(HSBType color) {
        cachedBrightness = color.getBrightness();
        cachedColor = color;
        cachedMired = Double.NaN; // invalidate mired as we don't know what it should be
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param warmness the color temperature warmness to set as a percent
     */
    private void zInternalHandleColorTemperature(PercentType warmness) {
        cachedMired = coolestMired + (warmness.doubleValue() * (warmestMired - coolestMired) / 100.0);
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param colorTemperature the color temperature to set as a QuantityType
     * @throws IllegalArgumentException if the colorTemperature parameter is not convertible to Mired
     */
    private void zInternalHandleColorTemperature(QuantityType<?> colorTemperature) throws IllegalArgumentException {
        try {
            QuantityType<?> mired = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.MIRED));
            setMired(mired.doubleValue());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "Parameter '%s' not convertible to Mired".formatted(colorTemperature.toFullString()));
        }
    }

    /**
     * Internal: handle a write increase/decrease command from OH core, ensuring it is in the range 0.0 to 100.0
     *
     * @param increaseDecrease the increase/decrease command
     */
    private void zInternalHandleIncreaseDecrease(IncreaseDecreaseType increaseDecrease) {
        double brightness = Math.min(Math.max(cachedColor.getBrightness().doubleValue()
                + ((IncreaseDecreaseType.INCREASE == increaseDecrease ? 1 : -1) * stepSize), 0.0), 100.0);
        setBrightness(brightness);
    }

    /**
     * Internal: handle a write on/off command from OH core
     *
     * @param onOff the on/off command
     */
    private void zInternalHandleOnOff(OnOffType onOff) {
        if (getOnOff() != onOff) {
            zInternalHandleBrightness(OnOffType.OFF == onOff ? PercentType.ZERO : cachedBrightness);
        }
    }

    /**
     * Internal: create a PercentType from a double value, ensuring it is in the range 0.0 to 100.0
     *
     * @param value the input value
     * @return a PercentType representing the input value, constrained to the range 0.0 to 100.0
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    private PercentType zInternalPercentTypeOf(double value) throws IllegalArgumentException {
        return new PercentType(new BigDecimal(value));
    }
}
