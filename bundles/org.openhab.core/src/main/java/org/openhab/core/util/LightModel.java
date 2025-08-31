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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link LightModel} provides a state machine model for maintaining and modifying the
 * state of a light. It supports lights with different capabilities, including:
 *
 * <ul>
 * <li>On/Off only
 * <li>On/Off with Brightness
 * <li>On/Off with Brightness and Color Temperature
 * <li>On/Off with Brightness and Color (HSB, RGB, or CIE XY)
 * <li>On/Off with Brightness, Color Temperature, and Color
 * </ul>
 *
 * It maintains an internal representation of the state of the light.
 * It provides methods to handle commands from openHAB and to update the state from the remote light.
 * It also provides configuration methods to set the capabilities and parameters of the light.
 * The state machine maintains a consistent state, ensuring that the On/Off state is derived from the
 * brightness, and that the color temperature and color are only set if the capabilities are supported.
 * It also provides utility methods to convert between different color representations.
 * See also {@link ColorUtil} for other color conversions.
 * A typical use case is within in a ThingHandler as follows:
 *
 * <pre>
 * {@code
 * public class LightModelHandler extends BaseThingHandler {
 *
 *     // initialize the light model with default cabapilities and parameters
 *     private final LightModel model = new LightModel();
 *
 *     @Override
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
 *     &#x40;Override
 *     public void handleCommand(ChannelUID channelUID, Command command) {
 *         // update the model state based on a command from OpenHAB
 *         model.handleCommand(command);
 *
 *         // or if it is a color temperature command
 *         model.handleColorTemperatureCommand(command);
 *
 *         // send the updated state as a command to the remote light
 *         sendBindingSpecificCommandToUpdateRemoteLight(model.getOnOff(), model.getBrightness(), model.getColor(),
 *                 model.getColorTemperature(), model.getColorTemperaturePercent());
 *     }
 *
 *     private void receiveBindingSpecificDataFromRemoteLight(int... receivedData) {
 *         // update the model state based on the data received from the remote
 *         model.setBrightness(receivedData[0]);
 *         model.setRGB(receivedData[1], receivedData[2], receivedData[3]);
 *         model.setMired(receivedData[4]);
 *
 *         // update the OH channels with the new state values
 *         updateState(onOffChannelUID, model.getOnOff());
 *         updateState(brightnessChannelUID, model.getBrightness());
 *         updateState(colorChannelUID, model.getColor());
 *     }
 * }
 * }
 * </pre>
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightModel {

    /*
     * The internal logic is implementated via an instance of this 'hidden' class
     */
    private final LightModelAbstractLogicImpl logic;

    /*********************************************************************************
     * SECTION: Constructors
     *********************************************************************************/

    /**
     * Create a {@link LightModel} with default capabilities and parameters.
     */
    public LightModel() {
        this(true, true, true, null, null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor) {
        this(supportsBrightness, supportsColorTemperature, supportsColor, null, null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities and parameters.
     * The parameters can be null to use the default.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     * @throws IllegalArgumentException if any of the parameters are out of range
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            @Nullable Double minimumOnBrightness, @Nullable Double warmestMired, @Nullable Double coolestMired,
            @Nullable Double stepSize) throws IllegalArgumentException {
        logic = new LightModelAbstractLogicImpl(supportsBrightness, supportsColorTemperature, supportsColor,
                minimumOnBrightness, warmestMired, coolestMired, stepSize) {
            // empty implementation of abstract class
        };
    }

    /*********************************************************************************
     * SECTION: Configuration getters and setters. Only used during initialization.
     *********************************************************************************/

    /**
     * Configuration: get the step size for IncreaseDecreaseType commands
     */
    public double configGetIncreaseDecreaseStep() {
        return logic.configGetIncreaseDecreaseStep();
    }

    /**
     * Configuration: get the minimum brightness percent to consider as light "ON"
     */
    public double configGetMinimumOnBrightness() {
        return logic.configGetMinimumOnBrightness();
    }

    /**
     * Configuration: get the Mired that corresponds to the coolest white light supported by the light
     */
    public double configGetMiredCoolest() {
        return logic.cfgGetMiredCoolest();
    }

    /**
     * Configuration: get the Mired that corresponds to the warmest white light supported by the light
     */
    public double configGetMiredWarmest() {
        return logic.cfgGetMiredWarmest();
    }

    /**
     * Configuration: set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @throws IllegalArgumentException if the stepSize parameter is out of range
     */
    public void configSetIncreaseDecreaseStep(double stepSize) throws IllegalArgumentException {
        logic.configSetIncreaseDecreaseStep(stepSize);
    }

    /**
     * Configuration: set the minimum brightness percent to consider as light "ON"
     *
     * @param minimumOnBrightness the minimum brightness percent
     * @throws IllegalArgumentException if the minimumBrightness parameter is out of range
     */
    public void configSetMinimumOnBrightness(double minimumOnBrightness) throws IllegalArgumentException {
        logic.configSetMinimumOnBrightness(minimumOnBrightness);
    }

    /**
     * Configuration: set the coolest supported color temperature in Mired
     *
     * @param coolestMired the coolest color temperature in Mired
     * @throws IllegalArgumentException if the coolestMired parameter is out of range or not less than warmestMired
     */
    public void configSetMiredCoolest(double coolestMired) throws IllegalArgumentException {
        logic.configSetMiredCoolest(coolestMired);
    }

    /**
     * Configuration: set the warmest supported color temperature in Mired
     *
     * @param warmestMired the warmest color temperature in Mired
     *
     * @throws IllegalArgumentException if the warmestMired parameter is out of range or not greater than coolestMired
     */
    public void configSetMiredWarmest(double warmestMired) throws IllegalArgumentException {
        logic.configSetMiredWarmest(warmestMired);
    }

    /**
     * Configuration: set whether brightness control is supported
     *
     * @param supportsBrightness true if brightness control is supported
     */
    public void configSetSupportsBrightness(boolean supportsBrightness) {
        logic.configSetSupportsBrightness(supportsBrightness);
    }

    /**
     * Configuration: set whether color control is supported
     *
     * @param supportsColor true if color control is supported
     */
    public void configSetSupportsColor(boolean supportsColor) {
        logic.configSetSupportsColor(supportsColor);
    }

    /**
     * Configuration: set whether color temperature control is supported
     *
     * @param supportsColorTemperature true if color temperature control is supported
     */
    public void configSetSupportsColorTemperature(boolean supportsColorTemperature) {
        logic.configSetSupportsColorTemperature(supportsColorTemperature);
    }

    /**
     * Configuration: check if brightness control is supported
     */
    public boolean configSupportsBrightness() {
        return logic.configSupportsBrightness();
    }

    /**
     * Configuration: check if color control is supported
     */
    public boolean configSupportsColor() {
        return logic.configSupportsColor();
    }

    /**
     * Configuration: check if color temperature control is supported
     */
    public boolean configSupportsColorTemperature() {
        return logic.configSupportsColorTemperature();
    }

    /*********************************************************************************
     * SECTION: Light State Getters, Setters, and Handlers. Only used at runtime.
     *********************************************************************************/

    /**
     * Runtime State: get the brightness or return null if the capability is not supported
     *
     * @return PercentType representing the brightness, or null if not supported
     */
    public @Nullable PercentType getBrightness() {
        return logic.getBrightness();
    }

    /**
     * Runtime State: get the color or return null if the capability is not supported
     *
     * @return HSBType representing the color, or null if not supported
     */
    public @Nullable HSBType getColor() {
        return logic.getColor();
    }

    /**
     * Runtime State: get the color temperature or return null if the capability is not supported
     *
     * @return QuantityType in Mired representing the color temperature, or null if not supported
     */
    public @Nullable QuantityType<?> getColorTemperature() {
        return logic.getColorTemperature();
    }

    /**
     * Runtime State: get the color temperature in percent or return null if the capability is not supported
     *
     * @return PercentType in range [0..100] representing [coolest..warmest], or null if not supported
     */
    public @Nullable PercentType getColorTemperaturePercent() {
        return logic.getColorTemperaturePercent();
    }

    /**
     * Runtime State: get the on/off state
     *
     * @return OnOffType.ON if brightness > minimumOnBrightness, otherwise OnOffType.OFF
     */
    public OnOffType getOnOff() {
        return logic.getOnOff();
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
    public void handleColorTemperatureCommand(Command command) throws IllegalArgumentException {
        logic.handleColorTemperatureCommand(command);
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
    public void handleCommand(Command command) throws IllegalArgumentException {
        logic.handleCommand(command);
    }

    /**
     * Runtime State: update the brightness from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param brightness in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    public void setBrightness(double brightness) throws IllegalArgumentException {
        logic.setBrightness(brightness);
    }

    /**
     * Runtime State: update the hue from the remote light, ensuring it is in the range 0.0 to 360.0
     *
     * @param hue in the range [0..360]
     * @throws IllegalArgumentException if the hue parameter is not in the range 0.0 to 360.0
     */
    public void setHue(double hue) throws IllegalArgumentException {
        logic.setHue(hue);
    }

    /**
     * Runtime State: update the mired color temperature from the remote light, and update the cached HSB color
     * accordingly. Constrain the mired value to be within the warmest and coolest limits.
     *
     * @param mired the color temperature in Mired
     * @throws IllegalArgumentException if the hue parameter is not in the range [coolestMired..warmestMired]
     */
    public void setMired(double mired) throws IllegalArgumentException {
        logic.setMired(mired);
    }

    /**
     * Runtime State: update the color with RGB fields from the remote light, and update the cached HSB color
     * accordingly.
     *
     * @param red optional red value in range [0..255], or null to leave unchanged
     * @param green optional green value in range [0..255], or null to leave unchanged
     * @param blue optional blue value in range [0..255], or null to leave unchanged
     * @throws IllegalArgumentException if any of the RGB values are out of range [0..255]
     */
    public void setRGB(@Nullable Integer red, @Nullable Integer green, @Nullable Integer blue)
            throws IllegalArgumentException {
        logic.setRGB(red, green, blue);
    }

    /**
     * Runtime State: update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     * @throws IllegalArgumentException if the saturation parameter is not in the range 0.0 to 100.0
     */
    public void setSaturation(double saturation) throws IllegalArgumentException {
        logic.setSaturation(saturation);
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
        logic.setXY(x, y);
    }

    /**
     * Runtime State: convert a nullable State to a non-null State, using UnDefType.UNDEF if the input is null.
     * <p>
     * {@code State state = xyz.toNonNull(xyz.getColor())} is a common usage.
     *
     * @param state the input State, which may be null
     * @return the input State if it is not null, otherwise UnDefType.UNDEF
     */
    public State toNonNull(@Nullable State state) {
        return state != null ? state : UnDefType.UNDEF;
    }
}
