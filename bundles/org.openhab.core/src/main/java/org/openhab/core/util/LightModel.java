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
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link LightModel} provides a state machine model for maintaining and modifying the state of a light,
 * which is intended to be used within the Thing Handler of a lighting binding.
 * <p>
 *
 * It supports lights with different capabilities, including:
 * <ul>
 * <li>On/Off only</li>
 * <li>On/Off with Brightness</li>
 * <li>On/Off with Brightness and Color Temperature</li>
 * <li>On/Off with Brightness and Color (HSB, RGB, or CIE XY)</li>
 * <li>On/Off with Brightness, Color Temperature, and Color</li>
 * </ul>
 *
 * It maintains an internal representation of the state of the light.
 * It provides methods to handle commands from openHAB and to update the state from the remote light.
 * It also provides configuration methods to set the capabilities and parameters of the light.
 * The state machine maintains a consistent state, ensuring that the On/Off state is derived from the
 * brightness, and that the color temperature and color are only set if the capabilities are supported.
 * It also provides utility methods to convert between different color representations.
 * <p>
 *
 * See also {@link ColorUtil} for other color conversions.
 * <p>
 *
 * To use the model you must initialize the following configuration capabilities (the default constructor initializes
 * {@link #supportsBrightness}, {@link #supportsColorTemperature} and {@link #supportsColor} to true):
 * <ul>
 * <li>Initialize {@link #supportsBrightness} to true if the light shall support brightness control.</li>
 * <li>Initialize {@link #supportsColorTemperature} to true if the light shall support color temperature control.</li>
 * <li>Initialize {@link #supportsColor} to true if the light shall support color control.</li>
 * <li>Initialize {@link #supportsRGBW} to true if the light shall support RGBW rather than RGB color control.</li>
 * <li>Initialize {@link #supportsRGBCW} to true if the light shall support RGBCW color control.</li>
 * <li>Initialize {@link #rgbIgnoreBrightness} to true if the light shall support RGB color control without
 * dimming.</li>
 * </ul>
 * <p>
 *
 * You can also override the following configuration parameters during initialization:
 * <ul>
 * <li>Optionally override {@link #minimumOnBrightness} to a minimum brightness percent in the range [0.1..10.0]
 * percent, to consider as being "ON". The default is 1 percent.</li>
 *
 * <li>Optionally override {@link #warmestMired} to a 'warmest' white color temperature in the range
 * [{@link #coolestMired}..1000.0] Mired. The default is 500 Mired.</li>
 *
 * <li>Optionally override {@link #coolestMired} to a 'coolest' white color temperature in the range
 * [100.0.. {@link #warmestMired}] Mired. The default is 153 Mired.</li>
 *
 * <li>Optionally override {@link #stepSize} to a step size for the IncreaseDecreaseType commands in the range
 * [1.0..50.0] percent. The default is 10.0 percent.</li>
 * </ul>
 * <p>
 *
 * The model specifically handles the following "exotic" cases:
 * <ol>
 *
 * <li>It handles inter relationships between the brightness PercentType state, the 'B' part of the HSBType state, and
 * the OnOffType state. Where if the brightness goes below the configured {@link #minimumOnBrightness} level the on/off
 * state changes from ON to OFF, and the brightness is clamped to 0%. And analogously if the on/off state changes from
 * OFF to ON, the brightness changes from 0% to its last non zero value.</li>
 *
 * <li>It handles IncreaseDecreaseType commands to change the brightness up or down by the configured
 * {@link #stepSize}, and ensures that the brightness is clamped in the range [0%..100%].</li>
 *
 * <li>It handles both color temperature PercentType states and QuantityType states (which may be either in Mired or
 * Kelvin). Where color temperature PercentType values are internally converted to Mired values on the percentage scale
 * between the configured {@link #coolestMired} and {@link #warmestMired} Mired values, and vice versa.</li>
 *
 * <li>When the color temperature changes then the HS values are adapted to match the corresponding color temperature
 * point on the Planckian Locus in the CIE color chart.</li>
 *
 * <li>It handles input/output values in RGB format in the range [0..255]. The behavior depends on the
 * {@link #rgbIgnoreBrightness} setting. If {@link #rgbIgnoreBrightness} is false the RGB values read/write all three
 * parts of the HSBType state. Whereas if it is true the RGB values read/write only the 'HS' parts. NOTE: in the latter
 * case, a 'setRGBx()' call followed by a 'getRGBx()' call do not necessarily return the same values,
 * since the values are normalized to 100%. Neverthless the ratios between the RGB values do remain unchanged.</li>
 *
 * <li>If {@link #supportsRGBW} is configured it handles values in RGBW format. The behavior is similar to the RGB
 * case above except that the white channel is derived from the lowest of the RGB values.
 * The {@link #rgbIgnoreBrightness} changes the behavior in relation to 'HS' versus 'HSB' exactly as in the case of
 * RGB above</li>
 *
 * <li>If {@link #supportsRGBCW} is configured it handles values in RGBCW format. The behavior is similar to the RGBW
 * case above except that the white channel is derived from the RGB values by a custom algorithm.
 * The {@link #rgbIgnoreBrightness} changes the behavior in relation to 'HS' versus 'HSB' exactly as in the case of
 * RGBW above</li>
 *
 * </ol>
 * <p>
 * A typical use case is within in a ThingHandler as follows:
 *
 * <pre>
 * {@code
 * public class LightModelHandler extends BaseThingHandler {
 *
 *     // initialize the light model with default capabilities and parameters
 *     private final LightModel model = new LightModel();
 *
 *     &#64;Override
 *     public void initialize() {
 *         // adjust the light model capabilities
 *         model.configSetSupportsBrightness(true);
 *         model.configSetSupportsColorTemperature(true);
 *         model.configSetSupportsColor(true);
 *
 *         // adjust the light model configuration parameters
 *         model.configSetMiredCoolest(153);
 *         model.configSetMiredWarmest(500);
 *     }
 *
 *     &#64;Override
 *     public void handleCommand(ChannelUID channelUID, Command command) {
 *         // update the model state based on a command from OpenHAB
 *         model.handleCommand(command);
 *
 *         // or if it is a color temperature command
 *         model.handleColorTemperatureCommand(command);
 *
 *         sendBindingSpecificCommandToUpdateRemoteLight(
 *              .. model.getOnOff() or
 *              .. model.getBrightness() or
 *              .. model.getColor() or
 *              .. model.getColorTemperature() or
 *              .. model.getColorTemperaturePercent() or
 *              .. model.getRGBx() or
 *              .. model.getXY() or
 *         );
 *     }
 *
 *     // method that sends the updated state data to the remote light
 *     private void sendBindingSpecificCommandToUpdateRemoteLight(..) {
 *       // binding specific code
 *     }
 *
 *     // method that receives data from remote light, and updates the model, and then OH
 *     private void receiveBindingSpecificDataFromRemoteLight(double... receivedData) {
 *         // update the model state based on the data received from the remote
 *         model.setBrightness(receivedData[0]);
 *         model.setRGBx(receivedData[1], receivedData[2], receivedData[3]);
 *         model.setMired(receivedData[4]);
 *
 *         // update the OH channels with the new state values
 *         updateState(onOffChannelUID, model.getOnOff());
 *         updateState(brightnessChannelUID, model.getBrightness());
 *         updateState(colorChannelUID, model.getColor());
 *         updateState(colorTemperatureChannelUID, model.getColorTemperature());
 *     }
 * }
 * }
 * </pre>
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightModel {

    /*********************************************************************************
     * SECTION: Default Parameters. May be modified during initialization.
     *********************************************************************************/

    /**
     * Minimum brightness percent to consider as light "ON"
     */
    private double minimumOnBrightness = 1.0;

    /**
     * The 'warmest' white color temperature
     */
    private double warmestMired = 500;

    /**
     * The 'coolest' white color temperature
     */
    private double coolestMired = 153;

    /*
     * Step size for IncreaseDecreaseType commands
     */
    private double stepSize = 10.0; // step size for IncreaseDecreaseType commands

    /*********************************************************************************
     * SECTION: Capabilities. May be modified during initialization.
     *********************************************************************************/

    /**
     * True if the light supports color
     */
    private boolean supportsColor = false;

    /**
     * True if RGB(C)(W) values are not linked to the brightness
     */
    private boolean rgbIgnoreBrightness = false;

    /**
     * True if the light supports RGBW
     */
    private boolean supportsRGBW = false;

    /**
     * True if the light supports RGBCW
     */
    private boolean supportsRGBCW = false;

    /**
     * True if the light supports brightness
     */
    private boolean supportsBrightness = false;

    /**
     * True if the light supports color temperature
     */
    private boolean supportsColorTemperature = false;

    /*********************************************************************************
     * SECTION: Light state variables. Used at run time only.
     *********************************************************************************/

    /**
     * Cached OnOff state, may be empty if not (yet) known
     */
    private Optional<OnOffType> cachedOnOff = Optional.empty();

    /**
     * Cached Brightness state, never null
     */
    private PercentType cachedBrightness = PercentType.ZERO;

    /**
     * Cached Color state, never null
     */
    private HSBType cachedHSB = new HSBType();

    /**
     * Cached Mired state, may be NaN if not (yet) known
     */
    private double cachedMired = Double.NaN;

    /*********************************************************************************
     * SECTION: Constructors
     *********************************************************************************/

    /**
     * Create a {@link LightModel} with default capabilities and parameters as follows:
     * <ul>
     * <li>{@link #supportsBrightness} is true (the light supports brightness control)</li>
     * <li>{@link #supportsColorTemperature} is true (the light supports color temperature control)</li>
     * <li>{@link #supportsColor} is true (the light supports color control)</li>
     * <li>{@link #rgbIgnoreBrightness} is false (the RGB values are linked to 'B' part of {@link HSBType}))</li>
     * <li>{@link #supportsRGBW} is false (the light does not support RGB with White)</li>
     * <li>{@link #supportsRGBCW} is false (the light does not support RGBCW)</li>
     * <li>{@link #minimumOnBrightness} is 1.0 (the minimum brightness percent to consider as light "ON")</li>
     * <li>{@link #warmestMired} is 500 (the 'warmest' white color temperature)</li>
     * <li>{@link #coolestMired} is 153 (the 'coolest' white color temperature)</li>
     * <li>{@link #stepSize} is 10.0 (the step size for IncreaseDecreaseType commands)</li>
     * </ul>
     */
    public LightModel() {
        this(true, true, true, false, false, false, null, null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param supportsRGBW true if the light supports RGBW rather than RGB color control
     * @param supportsRGBCW true if the light supports RGBCW color control
     * @param rgbIgnoreBrightness true if RGB values are not linked with the 'B' part of the {@link HSBType}
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            boolean supportsRGBW, boolean supportsRGBCW, boolean rgbIgnoreBrightness) {
        this(supportsBrightness, supportsColorTemperature, supportsColor, supportsRGBW, supportsRGBCW,
                rgbIgnoreBrightness, null, null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities and parameters.
     * The parameters can be null to use the default.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param supportsRGBW true if the light supports RGBW rather than RGB color control
     * @param supportsRGBCW true if the light supports RGBCW color control
     * @param rgbIgnoreBrightness true if RGB values are not linked with the 'B' part of the {@link HSBType}
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     * @throws IllegalArgumentException if any of the parameters are out of range
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            boolean supportsRGBW, boolean supportsRGBCW, boolean rgbIgnoreBrightness,
            @Nullable Double minimumOnBrightness, @Nullable Double warmestMired, @Nullable Double coolestMired,
            @Nullable Double stepSize) throws IllegalArgumentException {
        configSetSupportsBrightness(supportsBrightness);
        configSetSupportsColorTemperature(supportsColorTemperature);
        configSetSupportsColor(supportsColor);
        configSetSupportsRGBW(supportsRGBW);
        configSetSupportsRGBCW(supportsRGBCW);
        configSetRgbIgnoreBrightness(rgbIgnoreBrightness);
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
    public double configGetIncreaseDecreaseStep() {
        return stepSize;
    }

    /**
     * Configuration: get the minimum brightness percent to consider as light "ON"
     */
    public double configGetMinimumOnBrightness() {
        return minimumOnBrightness;
    }

    /**
     * Configuration: get the coolest color temperature in Mired
     */
    public double configGetMiredCoolest() {
        return coolestMired;
    }

    /**
     * Configuration: get the warmest color temperature in Mired
     */
    public double configGetMiredWarmest() {
        return warmestMired;
    }

    /**
     * Configuration: check whether RGB values are linked to the 'HS' parts or all of the 'HSB' parts of {@link HSBType}
     * state, as follows:
     *
     * <ul>
     * <li>If true the RGB values only relate to the 'HS' parts and will not influence or depend on the
     * brightness.</li>
     * <li>If false, the RGB values relate to all of the 'HSB' parts and will influence and depend on the
     * brightness.</li>
     * </ul>
     */
    public boolean configGetRgbIgnoreBrightness() {
        return rgbIgnoreBrightness;
    }

    /**
     * Configuration: check if brightness control is supported
     */
    public boolean configGetSupportsBrightness() {
        return supportsBrightness;
    }

    /**
     * Configuration: check if color control is supported
     */
    public boolean configGetSupportsColor() {
        return supportsColor;
    }

    /**
     * Configuration: check if color temperature control is supported
     */
    public boolean configGetSupportsColorTemperature() {
        return supportsColorTemperature;
    }

    /**
     * Configuration: check if RGBCW color control is supported
     */
    public boolean configGetSupportsRGBCW() {
        return supportsRGBCW;
    }

    /**
     * Configuration: check if RGBW color control is supported versus RGB only
     */
    public boolean configGetSupportsRGBW() {
        return supportsRGBW;
    }

    /**
     * Configuration: set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @throws IllegalArgumentException if the stepSize parameter is out of range
     */
    public void configSetIncreaseDecreaseStep(double stepSize) throws IllegalArgumentException {
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
    public void configSetMinimumOnBrightness(double minimumOnBrightness) throws IllegalArgumentException {
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
    public void configSetMiredCoolest(double coolestMired) throws IllegalArgumentException {
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
    public void configSetMiredWarmest(double warmestMired) throws IllegalArgumentException {
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
     * <li>If true the RGB values only relate to the 'HS' parts and will not influence or depend on the
     * brightness.</li>
     * <li>If false the RGB values relate to all of the 'HSB' parts and will influence and depend on the
     * brightness.</li>
     * </ul>
     *
     * @param rgbIgnoreBrightness true if RGB values are not linked to brightness
     * @throws IllegalArgumentException if rgbIgnoreBrightness is set true when RGBW or RGBCW are supported
     */
    public void configSetRgbIgnoreBrightness(boolean rgbIgnoreBrightness) throws IllegalArgumentException {
        if (rgbIgnoreBrightness && (supportsRGBW || supportsRGBCW)) {
            throw new IllegalArgumentException(
                    "Setting rgbIgnoreBrightness to true makes no sense if the light supports RGBW or RGBCW");
        }
        this.rgbIgnoreBrightness = rgbIgnoreBrightness;
    }

    /**
     * Configuration: set whether brightness control is supported
     *
     * @param supportsBrightness true if brightness control is supported
     */
    public void configSetSupportsBrightness(boolean supportsBrightness) {
        this.supportsBrightness = supportsBrightness;
    }

    /**
     * Configuration: set whether color control is supported
     *
     * @param supportsColor true if color control is supported
     * @throws IllegalArgumentException if color is set false but RGBW or RGBCW are supported
     */
    public void configSetSupportsColor(boolean supportsColor) throws IllegalArgumentException {
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
    public void configSetSupportsColorTemperature(boolean supportsColorTemperature) throws IllegalArgumentException {
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
    public void configSetSupportsRGBCW(boolean supportsRGBCW) throws IllegalArgumentException {
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
    public void configSetSupportsRGBW(boolean supportsRGBW) throws IllegalArgumentException {
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
    public @Nullable PercentType getBrightness(boolean... forceChannelVisible) {
        return supportsBrightness & (!supportsColor || (forceChannelVisible.length > 0 && forceChannelVisible[0]))
                ? cachedHSB.getBrightness()
                : null;
    }

    /**
     * Runtime State: get the color or return null if the capability is not supported
     *
     * @return HSBType, or null if not supported.
     */
    public @Nullable HSBType getColor() {
        return supportsColor ? cachedHSB : null;
    }

    /**
     * Runtime State: get the color temperature or return null if the capability is not supported
     * or the Mired value is not known.
     *
     * @return QuantityType in Kelvin representing the color temperature, or null if not supported
     *         or the Mired value is not known.
     */
    public @Nullable QuantityType<?> getColorTemperature() {
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
    public @Nullable PercentType getColorTemperaturePercent() {
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
    public double getHue() {
        return cachedHSB.getHue().doubleValue();
    }

    /**
     * Runtime State: get the color temperature in Mired, may be NaN if not known.
     *
     * @return double representing the color temperature in Mired.
     */
    public double getMired() {
        return cachedMired;
    }

    /**
     * Runtime State: get the on/off state.
     *
     * @param forceChannelVisible if present and true, return a non-null value even if brightness or color are
     *            supported.
     * @return OnOffType representing the on/off state.
     */
    public @Nullable OnOffType getOnOff(boolean... forceChannelVisible) {
        return (!supportsColor && !supportsBrightness) || (forceChannelVisible.length > 0 && forceChannelVisible[0])
                ? OnOffType.from(cachedHSB.getBrightness().doubleValue() >= minimumOnBrightness)
                : null;
    }

    /**
     * Runtime State: get the RGB(C)(W) values as an array of doubles in range [0..255]. Depending on the value of
     * {@link #supportsRGBW} and {@link #supportsRGBCW}, the array length is either 3 (RGB), 4 (RGBW), or 5 (RGBCW). The
     * array is in the order [red, green, blue, (cold-)(white), (warm-white)]. Depending on the value of
     * {@link #rgbIgnoreBrightness}, the brightness may or may not be used as follows:
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
    public double[] getRGBx() {
        HSBType hsb = rgbIgnoreBrightness
                ? new HSBType(cachedHSB.getHue(), cachedHSB.getSaturation(), PercentType.HUNDRED)
                : cachedHSB;
        PercentType[] rgbx;
        if (supportsRGBW || supportsRGBCW) {
            rgbx = ColorUtil.hsbToRgbwPercent(hsb);
            if (supportsRGBCW) {
                double hue = hsb.getHue().doubleValue();
                /*
                 * Simple model to split RGBW white value into RGBCW cool/ warm based on hue.
                 * This is probably not physically accurate, but is not that bad either.
                 *
                 * The model is:
                 * - Red => Green => Blue: increasing cool bias (0.0 @0°, 0.25 @60°, 0.5 @120°, 0.75 @180°, 1.0 @240°)
                 * - Blue => Magenta => Red: decreasing cool bias (1.0 @240°, 0.75 @300°, 0.0 @360°)
                 */
                double coolBias = hue < 240 ? hue / 240 : 1.0 + ((240 - hue) / 120);
                rgbx = new PercentType[] { rgbx[0], rgbx[1], rgbx[2],
                        zPercentTypeFrom(rgbx[3].doubleValue() * coolBias),
                        zPercentTypeFrom(rgbx[3].doubleValue() * (1.0 - coolBias)) };
            }
        } else {
            rgbx = ColorUtil.hsbToRgbPercent(hsb);
        }
        return Arrays.stream(rgbx).mapToDouble(p -> p.doubleValue() * 255.0 / 100.0).toArray();
    }

    /**
     * Runtime State: get the saturation in range [0..100].
     *
     * @return double representing the saturation in range [0..100].
     */
    public double getSaturation() {
        return cachedHSB.getSaturation().doubleValue();
    }

    /**
     * Runtime State: get the CIE XY values as an array of doubles in range [0.0..1.0].
     *
     * @return double[] representing the XY components in range [0.0..1.0].
     */
    public double[] getXY() {
        return ColorUtil.hsbToXY(new HSBType(cachedHSB.getHue(), cachedHSB.getSaturation(), PercentType.HUNDRED));
    }

    /**
     * Runtime State: handle a command to change the light's color temperature state. Commands may be one of:
     * <ul>
     * <li>{@link PercentType} for color temperature setting</li>
     * <li>{@link QuantityType} for color temperature setting</li>
     * </ul>
     * Other commands are deferred to {@link #handleCommand(Command)} for processing just-in-case.
     *
     * @param command the command to handle
     * @throws IllegalArgumentException if the command type is not supported
     */
    public void handleColorTemperatureCommand(Command command) throws IllegalArgumentException {
        if (command instanceof PercentType warmness) {
            zzHandleColorTemperature(warmness);
        } else if (command instanceof QuantityType<?> temperature) {
            zzHandleColorTemperature(temperature);
        } else {
            // defer to the main handler for other command types just-in-case
            handleCommand(command);
        }
    }

    /**
     * Runtime State: handle a command to change the light's state. Commands may be one of:
     * <ul>
     * <li>{@link HSBType} for color setting</li>
     * <li>{@link PercentType} for brightness setting</li>
     * <li>{@link OnOffType} for on/off state setting</li>
     * <li>{@link IncreaseDecreaseType} for brightness up/down setting</li>
     * <li>{@link QuantityType} for color temperature setting</li>
     * </ul>
     *
     * @param command the command to handle
     * @throws IllegalArgumentException if the command type is not supported
     */
    public void handleCommand(Command command) throws IllegalArgumentException {
        if (command instanceof HSBType color) {
            zzHandleHSBType(color);
        } else if (command instanceof PercentType brightness) {
            zzHandleBrightness(brightness);
        } else if (command instanceof OnOffType onOff) {
            zzHandleOnOff(onOff);
        } else if (command instanceof IncreaseDecreaseType incDec) {
            zzHandleIncreaseDecrease(incDec);
        } else if (command instanceof QuantityType<?> temperature) {
            zzHandleColorTemperature(temperature);
        } else {
            throw new IllegalArgumentException(
                    "Command '%s' not supported for light states".formatted(command.getClass().getName()));
        }
    }

    /**
     * Runtime State: update the brightness from the remote light, ensuring it is in the range [0.0..100.0]
     *
     * @param brightness in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    public void setBrightness(double brightness) throws IllegalArgumentException {
        zzHandleBrightness(zPercentTypeFrom(brightness));
    }

    /**
     * Runtime State: update the hue from the remote light, ensuring it is in the range [0.0..360.0]
     *
     * @param hue in the range [0..360]
     * @throws IllegalArgumentException if the hue parameter is not in the range 0.0 to 360.0
     */
    public void setHue(double hue) throws IllegalArgumentException {
        HSBType hsb = new HSBType(new DecimalType(hue), cachedHSB.getSaturation(), cachedHSB.getBrightness());
        cachedHSB = hsb;
        cachedMired = zzMiredFrom(hsb);
    }

    /**
     * Runtime State: update the Mired color temperature from the remote light, and update the cached HSB color
     * accordingly. Constrain the Mired value to be within the warmest and coolest limits. If the mired
     * value is NaN then the cached color is not updated as we cannot determine what it should be.
     *
     * @param mired the color temperature in Mired or NaN if not known
     * @throws IllegalArgumentException if the mired parameter is not in the range [coolestMired..warmestMired]
     */
    public void setMired(double mired) throws IllegalArgumentException {
        if (mired < coolestMired || mired > warmestMired) { // NaN is not less than or greater than anything
            throw new IllegalArgumentException(
                    "Mired value '%f' out of range [%f..%f]".formatted(mired, coolestMired, warmestMired));
        }
        if (!Double.isNaN(mired)) { // don't update color if Mired is not known
            HSBType hsb = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / mired));
            cachedHSB = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedHSB.getBrightness());
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
     * this means that in this case a round trip of 'setRGBx()' followed by 'getRGBx()' will NOT necessarily contain
     * identical values, although the RGB ratios will certainly be the same.</li>
     *
     * <li>{@code rgbLinkedToBrightness == true} both [255,0,0] and [127.5,0,0] change the color to RED and the former
     * changes the brightness to 100 percent, whereas the latter changes it to 50 percent. In other words the values
     * relate to all the 'HSB' parts of the {@link HSBType} state.</li>
     * <ul>
     *
     * @param rgbxIn an array of double representing RGB or RGBW values in range [0.0..255.0]
     * @throws IllegalArgumentException if the array length is not 3, 4, or 5 depending on the light's capabilities,
     *             or if any of the values are outside the range [0.0 to 255.0]
     */
    public void setRGBx(double[] rgbxIn) throws IllegalArgumentException {
        if (rgbxIn.length > 5) {
            throw new IllegalArgumentException("Too many arguments in RGBx array");
        }
        if (rgbxIn.length < 3 || (supportsRGBW && rgbxIn.length < 4) || (supportsRGBCW && rgbxIn.length < 5)) {
            throw new IllegalArgumentException("Too few arguments in RGBx array");
        }
        if (Arrays.stream(rgbxIn).anyMatch(v -> v < 0.0 || v > 255.0)) {
            throw new IllegalArgumentException("RGBx value out of range [0.0..255.0]");
        }
        double[] rgbxOut;
        if (supportsRGBCW) {
            // simple merge RGBCW to RGBW and normalize if necessary
            rgbxOut = new double[] { rgbxIn[0], rgbxIn[1], rgbxIn[2], rgbxIn[3] + rgbxIn[4] };
            double max = Arrays.stream(rgbxOut).max().orElse(255.0);
            if (max > 255.0) {
                rgbxOut = Arrays.stream(rgbxOut).map(v -> v * 255.0 / max).toArray();
            }
        } else {
            rgbxOut = rgbxIn;
        }
        HSBType hsb = ColorUtil.rgbToHsb(Arrays.stream(rgbxOut).map(d -> d * 100.0 / 255.0)
                .mapToObj(d -> zPercentTypeFrom(d)).toArray(PercentType[]::new));
        PercentType bri = hsb.getBrightness();
        if (rgbIgnoreBrightness) {
            hsb = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedHSB.getBrightness());
        }
        cachedHSB = hsb;
        cachedMired = zzMiredFrom(hsb);
        if (!rgbIgnoreBrightness) {
            zzHandleBrightness(bri);
        }
    }

    /**
     * Runtime State: update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    public void setSaturation(double saturation) throws IllegalArgumentException {
        HSBType hsb = new HSBType(cachedHSB.getHue(), zPercentTypeFrom(saturation), cachedHSB.getBrightness());
        cachedHSB = hsb;
        cachedMired = zzMiredFrom(hsb);
    }

    /**
     * Runtime State: update the color with CIE XY fields from the remote light, and update the cached HSB color
     * accordingly.
     *
     * @param x the x field in range [0.0..1.0]
     * @param y the y field in range [0.0..1.0]
     * @throws IllegalArgumentException if any of the XY values are out of range [0.0..1.0]
     */
    public void setXY(double x, double y) throws IllegalArgumentException {
        double[] xy = new double[] { x, y };
        HSBType hsb = ColorUtil.xyToHsb(xy);
        cachedHSB = new HSBType(hsb.getHue(), hsb.getSaturation(), cachedHSB.getBrightness());
        cachedMired = 1000000 / ColorUtil.xyToKelvin(xy);
    }

    /**
     * Runtime State: convert a nullable State to a non-null State, using {@link UnDefType}.UNDEF if the input is null.
     * <p>
     * {@code State state = xyz.toNonNull(xyz.getColor())} is a common usage.
     *
     * @param state the input State, which may be null
     * @return the input State if it is not null, otherwise 'UnDefType.UNDEF'
     */
    public State toNonNull(@Nullable State state) {
        return state != null ? state : UnDefType.UNDEF;
    }

    /*********************************************************************************
     * SECTION: Internal private methods. Names have 'z' prefix to indicate private.
     *********************************************************************************/

    /**
     * Internal: handle a write brightness command from OH core
     *
     * @param brightness the brightness {@link PercentType} to set
     */
    private void zzHandleBrightness(PercentType brightness) {
        if (brightness.doubleValue() >= minimumOnBrightness) {
            cachedBrightness = brightness;
            cachedHSB = new HSBType(cachedHSB.getHue(), cachedHSB.getSaturation(), brightness);
            cachedOnOff = Optional.of(OnOffType.ON);
        } else {
            if (!cachedOnOff.isPresent() || OnOffType.ON == cachedOnOff.get()) {
                cachedBrightness = cachedHSB.getBrightness();
            }
            cachedHSB = new HSBType(cachedHSB.getHue(), cachedHSB.getSaturation(), PercentType.ZERO);
            cachedOnOff = Optional.of(OnOffType.OFF);
        }
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param warmness the color temperature warmness {@link PercentType} to set
     */
    private void zzHandleColorTemperature(PercentType warmness) {
        setMired(coolestMired + ((warmestMired - coolestMired) * warmness.doubleValue() / 100.0));
    }

    /**
     * Internal: handle a write color temperature command from OH core
     *
     * @param colorTemperature the color temperature {@link QuantityType} to set
     * @throws IllegalArgumentException if the colorTemperature parameter is not convertible to Mired
     */
    private void zzHandleColorTemperature(QuantityType<?> colorTemperature) throws IllegalArgumentException {
        try {
            QuantityType<?> mired = Objects.requireNonNull(colorTemperature.toInvertibleUnit(Units.MIRED));
            setMired(mired.doubleValue());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "Parameter '%s' not convertible to Mired".formatted(colorTemperature.toFullString()));
        }
    }

    /**
     * Internal: handle a write color command from OH core
     *
     * @param hsb the color {@link HSBType} to set
     */
    private void zzHandleHSBType(HSBType hsb) {
        cachedBrightness = hsb.getBrightness();
        cachedHSB = hsb;
        cachedMired = zzMiredFrom(hsb);
    }

    /**
     * Internal: handle a write increase/decrease command from OH core, ensuring it is in the range 0.0 to 100.0
     *
     * @param increaseDecrease the {@link IncreaseDecreaseType} command
     */
    private void zzHandleIncreaseDecrease(IncreaseDecreaseType increaseDecrease) {
        double bri = Math.min(Math.max(cachedHSB.getBrightness().doubleValue()
                + ((IncreaseDecreaseType.INCREASE == increaseDecrease ? 1 : -1) * stepSize), 0.0), 100.0);
        setBrightness(bri);
    }

    /**
     * Internal: handle a write on/off command from OH core
     *
     * @param onOff the {@link OnOffType} command
     */
    private void zzHandleOnOff(OnOffType onOff) {
        if (getOnOff() != onOff) {
            zzHandleBrightness(OnOffType.OFF == onOff ? PercentType.ZERO : cachedBrightness);
        }
    }

    /**
     * Internal: return the Mired value from the given {@link HSBType} color. The Mired value is constrained to be
     * within the warmest and coolest limits.
     *
     * @param hsb the {@link HSBType} color to use to determine the Mired value
     */
    private double zzMiredFrom(HSBType hsb) {
        double[] xyY = ColorUtil.hsbToXY(new HSBType(hsb.getHue(), hsb.getSaturation(), PercentType.HUNDRED));
        double mired = 1000000 / ColorUtil.xyToKelvin(new double[] { xyY[0], xyY[1] });
        return Math.min(Math.max(mired, coolestMired), warmestMired);
    }

    /**
     * Internal: create a {@link PercentType} from a double value, ensuring it is in the range 0.0 to 100.0
     *
     * @param value the input value
     * @return a {@link PercentType} representing the input value, constrained to the range 0.0 to 100.0
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    private PercentType zPercentTypeFrom(double value) throws IllegalArgumentException {
        return new PercentType(new BigDecimal(value));
    }
}
