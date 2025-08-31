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
    private boolean supportsBrightness = false; // true if the light supports dimming
    private boolean supportsColorTemperature = false; // true if the light supports color temperature

    /*********************************************************************************
     * SECTION: Light state variables. Used at run time only.
     *********************************************************************************/

    private Optional<OnOffType> cachedOnOff = Optional.empty();
    private PercentType cachedBrightness = PercentType.ZERO;
    private HSBType cachedColor = new HSBType();
    private double cachedMired = warmestMired;

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
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     */
    LightModelAbstractLogicImpl(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            @Nullable Double minimumOnBrightness, @Nullable Double warmestMired, @Nullable Double coolestMired,
            @Nullable Double stepSize) {
        this.supportsColor = supportsColor;
        this.supportsBrightness = supportsBrightness;
        this.supportsColorTemperature = supportsColorTemperature;
        this.minimumOnBrightness = minimumOnBrightness != null ? minimumOnBrightness : this.minimumOnBrightness;
        this.warmestMired = warmestMired != null ? warmestMired : this.warmestMired;
        this.coolestMired = coolestMired != null ? coolestMired : this.coolestMired;
        this.stepSize = stepSize != null ? stepSize : this.stepSize;
        internalValidateParameters();
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
    double cfgGetMiredCoolest() {
        return coolestMired;
    }

    /**
     * Configuration: get the warmest color temperature in Mired
     */
    double cfgGetMiredWarmest() {
        return warmestMired;
    }

    /**
     * Configuration: set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @throws IllegalArgumentException if the stepSize parameter is out of range
     */
    void configSetIncreaseDecreaseStep(double stepSize) throws IllegalArgumentException {
        if (stepSize < 1.0 || stepSize > 50.0) {
            throw new IllegalArgumentException("Step size '%f' out of range (1.0..50.0]".formatted(stepSize));
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
                    "Warmest mired '%f' must be greater than coolest mired '%f'".formatted(warmestMired, coolestMired));
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
                    "Warmest mired '%f' out of range [100.0..1000.0]".formatted(warmestMired));
        }
        if (warmestMired <= coolestMired) {
            throw new IllegalArgumentException(
                    "Warmest mired '%f' must be greater than coolest mired '%f'".formatted(warmestMired, coolestMired));
        }
        this.warmestMired = warmestMired;
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
     */
    void configSetSupportsColor(boolean supportsColor) {
        this.supportsColor = supportsColor;
    }

    /**
     * Configuration: set whether color temperature control is supported
     *
     * @param supportsColorTemperature true if color temperature control is supported
     */
    void configSetSupportsColorTemperature(boolean supportsColorTemperature) {
        this.supportsColorTemperature = supportsColorTemperature;
    }

    /**
     * Configuration: check if brightness control is supported
     */
    boolean configSupportsBrightness() {
        return supportsBrightness;
    }

    /**
     * Configuration: check if color control is supported
     */
    boolean configSupportsColor() {
        return supportsColor;
    }

    /**
     * Configuration: check if color temperature control is supported
     */
    boolean configSupportsColorTemperature() {
        return supportsColorTemperature;
    }

    /*********************************************************************************
     * SECTION: Light State Getters, Setters, and Handlers. Only used at runtime.
     *********************************************************************************/

    /**
     * Runtime State: get the brightness or return null if the capability is not supported
     */
    @Nullable
    PercentType getBrightness() {
        return supportsBrightness ? cachedColor.getBrightness() : null;
    }

    /**
     * Runtime State: get the color or return null if the capability is not supported
     */
    @Nullable
    HSBType getColor() {
        return supportsColor ? cachedColor : null;
    }

    /**
     * Runtime State: get the color temperature or return null if the capability is not supported
     */
    @Nullable
    QuantityType<?> getColorTemperature() {
        if (supportsColorTemperature) {
            return Objects.requireNonNull( // Mired always converts to Kelvin
                    QuantityType.valueOf(cachedMired, Units.MIRED).toInvertibleUnit(Units.KELVIN));
        }
        return null;
    }

    /**
     * Runtime State: get the color temperature in percent or return null if the capability is not supported
     *
     * @return PercentType in range [0..100] representing [coolest..warmest], or null if not supported
     */
    @Nullable
    PercentType getColorTemperaturePercent() {
        if (supportsColorTemperature) {
            double percent = 100 * (cachedMired - coolestMired) / (warmestMired - coolestMired);
            return new PercentType(new BigDecimal(Math.min(Math.max(percent, 0.0), 100.0)));
        }
        return null;
    }

    /**
     * Runtime State: get the on/off state
     */
    OnOffType getOnOff() {
        return OnOffType.from(cachedColor.getBrightness().doubleValue() >= minimumOnBrightness);
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
            internalHandleColorTemperature(warmness);
        } else if (command instanceof QuantityType<?> temperature) {
            internalHandleColorTemperature(temperature);
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
     * <li>OnOffType for pn/off state setting</li>
     * <li>IncreaseDecreaseType for brightness up/down setting</li>
     * <li>QuantityType for color temperature setting</li>
     * </ul>
     *
     * @param command the command to handle
     * @throws IllegalArgumentException if the command type is not supported
     */
    void handleCommand(Command command) throws IllegalArgumentException {
        if (command instanceof HSBType color) {
            internalHandleColor(color);
        } else if (command instanceof PercentType brightness) {
            internalHandleBrightness(brightness);
        } else if (command instanceof OnOffType onOff) {
            internalHandleOnOff(onOff);
        } else if (command instanceof IncreaseDecreaseType incDec) {
            internalHandleIncreaseDecrease(incDec);
        } else if (command instanceof QuantityType<?> temperature) {
            internalHandleColorTemperature(temperature);
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
        internalHandleBrightness(internalCreatePercentType(brightness));
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
     * Runtime State: update the mired color temperature from the remote light, and update the cached HSB color
     * accordingly. Constrain the mired value to be within the warmest and coolest limits.
     *
     * @param mired the color temperature in Mired
     * @throws IllegalArgumentException if the hue parameter is not in the range [coolestMired..warmestMired]
     */
    void setMired(double mired) throws IllegalArgumentException {
        if (mired < coolestMired || mired > warmestMired) {
            throw new IllegalStateException(
                    "Mired value '%f' out of range [%f..%f]".formatted(mired, coolestMired, warmestMired));
        }
        cachedMired = Math.min(Math.max(mired, coolestMired), warmestMired);
        HSBType hsb = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / cachedMired));
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Runtime State: update the color with RGB fields from the remote light, and update the cached HSB color
     * accordingly
     *
     * @param red optional red value in range [0..255], or null to leave unchanged
     * @param green optional green value in range [0..255], or null to leave unchanged
     * @param blue optional blue value in range [0..255], or null to leave unchanged
     * @throws IllegalArgumentException if any of the RGB values are out of range [0..255]
     */
    void setRGB(@Nullable Integer red, @Nullable Integer green, @Nullable Integer blue)
            throws IllegalArgumentException {
        int[] rgb = ColorUtil.hsbToRgb(cachedColor);
        rgb[0] = red != null ? red : rgb[0];
        rgb[1] = green != null ? green : rgb[1];
        rgb[2] = blue != null ? blue : rgb[2];
        HSBType hsb = ColorUtil.rgbToHsb(rgb);
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Runtime State: update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    void setSaturation(double saturation) throws IllegalArgumentException {
        cachedColor = new HSBType(cachedColor.getHue(), internalCreatePercentType(saturation),
                cachedColor.getBrightness());
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
    }

    /*********************************************************************************
     * SECTION: Internal private methods.
     *********************************************************************************/

    /**
     * Internal: create a PercentType from a double value, ensuring it is in the range 0.0 to 100.0
     *
     * @param value the input value
     * @return a PercentType representing the input value, constrained to the range 0.0 to 100.0
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    private PercentType internalCreatePercentType(double value) throws IllegalArgumentException {
        return new PercentType(new BigDecimal(value));
    }

    /**
     * Internal: handle a write increase/decrease command from OH core, ensuring it is in the range 0.0 to 100.0
     *
     * @param inceaseDecrease the increase/decrease command
     */
    private void internalHandleIncreaseDecrease(IncreaseDecreaseType inceaseDecrease) {
        double brightness = Math.min(Math.max(cachedColor.getBrightness().doubleValue()
                + ((IncreaseDecreaseType.INCREASE == inceaseDecrease ? 1 : -1) * stepSize), 0.0), 100.0);
        setBrightness(brightness);
    }

    /**
     * Internal: handle a write brightness command from OH core
     *
     * @param brightness the brightness to set
     */
    private void internalHandleBrightness(PercentType brightness) {
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
    private void internalHandleColor(HSBType color) {
        cachedBrightness = color.getBrightness();
        cachedColor = color;
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param warmness the color temperature warmness to set as a percent
     */
    private void internalHandleColorTemperature(PercentType warmness) {
        cachedMired = coolestMired + (warmness.doubleValue() * (warmestMired - coolestMired) / 100.0);
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param colorTemperature the color temperature to set as a QuantityType
     * @throws IllegalArgumentException if the colorTemperature parameter is not convertible to Mired
     */
    private void internalHandleColorTemperature(QuantityType<?> colorTemperature) throws IllegalArgumentException {
        try {
            QuantityType<?> mired = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.MIRED));
            setMired(mired.doubleValue());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "Parameter '%s' not convertible to Mired".formatted(colorTemperature.toFullString()));
        }
    }

    /**
     * Internal: handle a write on/off command from OH core
     *
     * @param onOff the on/off command
     */
    private void internalHandleOnOff(OnOffType onOff) {
        if (getOnOff() != onOff) {
            internalHandleBrightness(OnOffType.OFF == onOff ? PercentType.ZERO : cachedBrightness);
        }
    }

    /**
     * Internal: validate the parameters and throw IllegalArgumentException if any are out of range
     *
     * @throws IllegalArgumentException if any parameters are out of range
     */
    private void internalValidateParameters() throws IllegalArgumentException {
        if (minimumOnBrightness < 0.1 || minimumOnBrightness > 10.0) {
            throw new IllegalArgumentException(
                    "Minimum brightness '%f' out of range [0.1..10.0]".formatted(minimumOnBrightness));
        }
        if (coolestMired < 100.0 || coolestMired > 1000.0) {
            throw new IllegalArgumentException(
                    "Coolest mired '%f' out of range [100.0..1000.0]".formatted(coolestMired));
        }
        if (warmestMired < 100.0 || warmestMired > 1000.0) {
            throw new IllegalArgumentException(
                    "Warmest mired '%f' out of range [100.0..1000.0]".formatted(warmestMired));
        }
        if (warmestMired <= coolestMired) {
            throw new IllegalArgumentException(
                    "Warmest mired '%f' must be greater than coolest mired '%f'".formatted(warmestMired, coolestMired));
        }
        if (stepSize < 1.0 || stepSize > 50.0) {
            throw new IllegalArgumentException("Step size '%f' out of range (1.0..50.0]".formatted(stepSize));
        }
    }
}
