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
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link LightStateMachine} provides a state machine with helper functions for controlloing lights.
 * See also {@link ColorUtil} for more general color conversion utilities.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightStateMachine {

    /**
     * Utility method to convert a nullable State to a non-null State, using UnDefType.UNDEF if the input is null.
     * <p>
     * {@code State state = LightStateMachine.requireNonNull(xyz.getColor())} is a common usage.
     *
     * @param state the input State, which may be null
     * @return the input State if it is not null, otherwise UnDefType.UNDEF
     */
    public static State requireNonNull(@Nullable State state) {
        return state != null ? state : UnDefType.UNDEF;
    }

    // default settings
    private double minimumOnBrightness = 1.0; // minimum brightness percent to consider as light "ON"
    private double warmestMired = 500; // 'warmest' white color temperature
    private double coolestMired = 153; // 'coolest' white color temperature
    private double stepSize = 10.0; // step size for IncreaseDecreaseType commands

    // capabilities
    private boolean supportsColor = false; // true if the light supports color
    private boolean supportsBrightness = false; // true if the light supports dimming
    private boolean supportsColorTemperature = false; // true if the light supports color temperature

    // cached state
    private Optional<OnOffType> cachedOnOff = Optional.empty();
    private PercentType cachedBrightness = PercentType.ZERO;
    private HSBType cachedColor = new HSBType();
    private double cachedMired = warmestMired;

    /**
     * Create a LightStateMachine with default capabilities and parameters.
     */
    public LightStateMachine() {
        this(false, false, false, null, null, null, null);
    }

    /**
     * Create a LightStateMachine with the given capabilities.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColor true if the light supports color control
     * @param supportsColorTemperature true if the light supports color temperature control
     */
    public LightStateMachine(boolean supportsBrightness, boolean supportsColor, boolean supportsColorTemperature) {
        this(supportsBrightness, supportsColor, supportsColorTemperature, null, null, null, null);
    }

    /**
     * Create a LightStateMachine with the given capabilities and parameters.
     * The parameters can be null to use the default.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColor true if the light supports color control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     */
    public LightStateMachine(boolean supportsBrightness, boolean supportsColor, boolean supportsColorTemperature,
            @Nullable Double minimumOnBrightness, @Nullable Double warmestMired, @Nullable Double coolestMired,
            @Nullable Double stepSize) {
        this.supportsColor = supportsColor;
        this.supportsBrightness = supportsBrightness;
        this.supportsColorTemperature = supportsColorTemperature;
        this.minimumOnBrightness = minimumOnBrightness != null ? minimumOnBrightness : this.minimumOnBrightness;
        this.warmestMired = warmestMired != null ? warmestMired : this.warmestMired;
        this.coolestMired = coolestMired != null ? coolestMired : this.coolestMired;
        this.stepSize = stepSize != null ? stepSize : this.stepSize;
    }

    /**
     * Read the brightness or return null if the capability is not supported
     */
    public @Nullable PercentType getBrightness() {
        return supportsBrightness ? cachedColor.getBrightness() : null;
    }

    /**
     * Read the color or return null if the capability is not supported
     */
    public @Nullable HSBType getColor() {
        return supportsColor ? cachedColor : null;
    }

    /**
     * Read the color temperature or return null if the capability is not supported
     */
    public @Nullable QuantityType<?> getColorTemperature() {
        if (supportsColorTemperature) {
            return Objects.requireNonNull( // Mired always converts to Kelvin
                    QuantityType.valueOf(cachedMired, Units.MIRED).toInvertibleUnit(Units.KELVIN));
        }
        return null;
    }

    /**
     * Read the color temperature in percent or return null if the capability is not supported
     */
    public @Nullable PercentType getColorTemperaturePercent() {
        if (supportsColorTemperature) {
            double percent = 100 * (cachedMired - coolestMired) / (warmestMired - coolestMired);
            return new PercentType(new BigDecimal(Math.min(Math.max(percent, 0.0), 100.0)));
        }
        return null;
    }

    /**
     * Read the on/off state
     */
    public OnOffType getOnOff() {
        return OnOffType.from(cachedColor.getBrightness().doubleValue() >= minimumOnBrightness);
    }

    /**
     * Handle a color temperature command, which may be one of:
     * <p>
     * <li>PercentType for color temperature setting</li>
     * <li>QuantityType for color temperature setting</li>
     */
    public void handleColorTemperatureCommand(Command command) {
        if (command instanceof PercentType warmness) {
            setColorTemperature(warmness);
        } else if (command instanceof QuantityType<?> temperature) {
            setColorTemperature(temperature);
        }
        throw new IllegalArgumentException(
                "Command '%s' not supported for color temperatures".formatted(command.getClass().getName()));
    }

    /**
     * Handle a command, which may be one of:
     * <p>
     * <li>HSBType for color setting</li>
     * <li>PercentType for brightness setting</li>
     * <li>OnOffType for pn/off state setting</li>
     * <li>IncreaseDecreaseType for brightness up/down setting</li>
     * <li>QuantityType for color temperature setting</li>
     */
    public void handleCommand(Command command) {
        if (command instanceof HSBType color) {
            setColor(color);
        } else if (command instanceof PercentType brightness) {
            setBrightness(brightness);
        } else if (command instanceof OnOffType onOff) {
            setOnOff(onOff);
        } else if (command instanceof IncreaseDecreaseType incDec) {
            setBrightness(incDec);
        } else if (command instanceof QuantityType<?> temperature) {
            setColorTemperature(temperature);
        }
        throw new IllegalArgumentException(
                "Command '%s' not supported for light states".formatted(command.getClass().getName()));
    }

    /**
     * Utility method to create a PercentType from a double value, ensuring it is in the range 0.0 to 100.0
     *
     * @param value the input value
     *
     * @return a PercentType representing the input value, constrained to the range 0.0 to 100.0
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    private PercentType percentFrom(double value) throws IllegalArgumentException {
        return new PercentType(new BigDecimal(value));
    }

    /**
     * Update the brightness from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param brightness
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    public void setBrightness(double brightness) throws IllegalArgumentException {
        setBrightness(percentFrom(brightness));
    }

    /**
     * Handle a write increase/decrease command from OH core, ensuring it is in the range 0.0 to 100.0
     *
     * @param inceaseDecrease the increase/decrease command
     */
    private void setBrightness(IncreaseDecreaseType inceaseDecrease) {
        double brightness = Math.min(Math.max(cachedBrightness.doubleValue()
                + ((IncreaseDecreaseType.INCREASE == inceaseDecrease ? 1 : -1) * stepSize), 0.0), 100.0);
        setBrightness(brightness);
    }

    /**
     * Handle a write brightness command from OH core
     *
     * @param brightness the brightness to set
     */
    private void setBrightness(PercentType brightness) {
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
     * Handle a write color command from OH core
     *
     * @param color the color to set
     */
    private void setColor(HSBType color) {
        cachedBrightness = color.getBrightness();
        cachedColor = color;
    }

    /**
     * Handle a write color temperature command from OH core
     *
     * @param warmness the color temperature warmness to set as a percent
     */
    private void setColorTemperature(PercentType warmness) {
        cachedMired = coolestMired + (warmness.doubleValue() * (warmestMired - coolestMired) / 100.0);
    }

    /**
     * Handle a write color temperature command from OH core
     *
     * @param colorTemperature the color temperature to set as a QuantityType
     * @throws IllegalArgumentException if the colorTemperature parameter is not convertible to Mired
     */
    private void setColorTemperature(QuantityType<?> colorTemperature) throws IllegalArgumentException {
        try {
            QuantityType<?> mired = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.MIRED));
            setMired(mired.doubleValue());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "Parameter '%s' not convertible to Mired".formatted(colorTemperature.toFullString()));
        }
    }

    /**
     * Update the hue from the remote light, ensuring it is in the range 0.0 to 360.0
     *
     * @throws IllegalArgumentException if the hue parameter is not in the range 0.0 to 360.0
     */
    public void setHue(double hue) throws IllegalArgumentException {
        cachedColor = new HSBType(new DecimalType(hue), cachedColor.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Update the mired color temperature from the remote light, and update the cached HSB color accordingly.
     * Constrain the mired value to be within the warmest and coolest limits.
     *
     * @throws IllegalArgumentException if the hue parameter is not in the range [coolestMired..warmestMired]
     */
    public void setMired(double mired) throws IllegalArgumentException {
        if (mired < coolestMired || mired > warmestMired) {
            throw new IllegalStateException(
                    "Mired value '%f' out of range [%f..%f]".formatted(mired, coolestMired, warmestMired));
        }
        cachedMired = Math.min(Math.max(mired, coolestMired), warmestMired);
        HSBType hsb = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / cachedMired));
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Handle a write on/off command from OH core
     *
     * @param onOff the on/off command
     */
    private void setOnOff(OnOffType onOff) {
        if (getOnOff() != onOff) {
            setBrightness(OnOffType.OFF == onOff ? PercentType.ZERO : cachedBrightness);
        }
    }

    /**
     * Update the color with RGB fields from the remote light, and update the cached HSB color accordingly
     *
     * @param red optional red value in range [0..255], or null to leave unchanged
     * @param green optional green value in range [0..255], or null to leave unchanged
     * @param blue optional blue value in range [0..255], or null to leave unchanged
     *
     * @throws IllegalArgumentException if any of the RGB values are out of range [0..255]
     */
    public void setRGB(@Nullable Integer red, @Nullable Integer green, @Nullable Integer blue)
            throws IllegalArgumentException {
        int[] rgb = ColorUtil.hsbToRgb(cachedColor);
        rgb[0] = red != null ? red : rgb[0];
        rgb[1] = green != null ? green : rgb[1];
        rgb[2] = blue != null ? blue : rgb[2];
        HSBType hsb = ColorUtil.rgbToHsb(rgb);
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Update the color with CIE XY fields from the remote light, and update the cached HSB color accordingly
     *
     * @param x the x field in range [0.0..1.0]
     * @param y the y field in range [0.0..1.0]
     *
     * @throws IllegalArgumentException if any of the XY values are out of range [0.0..1.0]
     */
    public void setXY(double x, double y) throws IllegalArgumentException {
        HSBType hsb = ColorUtil.xyToHsb(new double[] { x, y });
        cachedColor = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedColor.getBrightness());
    }

    /**
     * Update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     */
    public void setSaturation(double saturation) {
        cachedColor = new HSBType(cachedColor.getHue(), percentFrom(saturation), cachedColor.getBrightness());
    }

    /**
     * Check if brightness control is supported
     */
    public boolean supportsBrightness() {
        return supportsBrightness;
    }

    /**
     * Check if color control is supported
     */
    public boolean supportsColor() {
        return supportsColor;
    }

    /**
     * Check if color temperature control is supported
     */
    public boolean supportsColorTemperature() {
        return supportsColorTemperature;
    }

    /**
     * Set whether brightness control is supported
     *
     * @param supportsBrightness true if brightness control is supported
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withBrightness(boolean supportsBrightness) {
        this.supportsBrightness = supportsBrightness;
        return this;
    }

    /**
     * Set whether color control is supported
     *
     * @param supportsColor true if color control is supported
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withColor(boolean supportsColor) {
        this.supportsColor = supportsColor;
        return this;
    }

    /**
     * Set whether color temperature control is supported
     *
     * @param supportsColorTemperature true if color temperature control is supported
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withColorTemperature(boolean supportsColorTemperature) {
        this.supportsColorTemperature = supportsColorTemperature;
        return this;
    }

    /**
     * Set the coolest color temperature in Mired
     *
     * @param coolestMired the coolest color temperature in Mired
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withCoolestMired(double coolestMired) {
        this.coolestMired = coolestMired;
        return this;
    }

    /**
     * Set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withIncreaseDecreaseStep(double stepSize) {
        this.stepSize = stepSize;
        return this;
    }

    /**
     * Set the minimum brightness percent to consider as light "ON"
     *
     * @param minimumBrightness the minimum brightness percent
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withMinimumBrightness(double minimumBrightness) {
        this.minimumOnBrightness = minimumBrightness;
        return this;
    }

    /**
     * Set the warmest color temperature in Mired
     *
     * @param warmestMired the warmest color temperature in Mired
     * @return this LightStateMachine for method chaining
     */
    public LightStateMachine withWarmestMired(double warmestMired) {
        this.warmestMired = warmestMired;
        return this;
    }
}
