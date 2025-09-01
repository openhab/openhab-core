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
 * The {@link LightModel} provides a state machine model for maintaining and modifying the state of a light, which is
 * intended to be used within the Thing Handler of a lighting binding.
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
 * 'supportsBrightness', 'supportsColorTemperature' and 'supportsBrightness' to true):
 * <ul>
 * <li>Initialize 'supportsBrightness' to true if the light shall support brightness control.</li>
 * <li>Initialize 'supportsColorTemperature' to true if the light shall support color temperature control.</li>
 * <li>Initialize 'supportsColor' to true if the light shall supports color control.</li>
 * <li>Initialize 'supportsRgbDimming' to true if the light shall support RGB color control with dimming.</li>
 * <li>Initialize 'supportsRgbWhite' to true if the light shall support RGBW rather than RGB color control.</li>
 * </ul>
 * <p>
 *
 * You can also override the following configuration parameters during initialization:
 * <ul>
 * <li>Optionally override 'minimumOnBrightness' to a minimum brightness percent in the range [0.1..10.0]
 * percent, to consider as being "ON". The default is 1 percent.</li>
 *
 * <li>Optionally override 'warmestMired' to a 'warmest' white color temperature in the range
 * [{@link coolestMired}..1000.0] Mired. The default is 500 Mired.</li>
 *
 * <li>Optionally override 'coolestMired' to a 'coolest' white color temperaturein the
 * range[100.0..{@link warmestMired}] Mired. The default is 153 Mired.</li>
 *
 * <li>Optionally override 'stepSize' to a step size for the IncreaseDecreaseType commands in the range
 * [1.0..50.0] percent. The default is 10 percent.</li>
 * </ul>
 * <p>
 *
 * The model specifically handles the following "exotic" cases:
 * <ol>
 *
 * <li>It handles inter relationships between the brightness PercentType state, the 'B' part of the HSBType state, and
 * the OnOffType state. Where if the brightness goes below the configured 'minimumOnBrightness' level the on/off
 * state changes from ON to OFF, and the brightness is clamped to 0%. And analogously if the on/off state changes from
 * OFF to ON, the brightness changes from 0% to its last non zero value.</li>
 *
 * <li>It handles IncreaseDecreaseType commands to change the brightness up or down by the configured
 * 'stepSize', and ensures that the brightness is clamped in the range [0%..100%].</li>
 *
 * <li>It handles both color temperature PercentType states and QuantityType states (which may be either in Mired or
 * Kelvin). Where color temperature PercentType values are internally converted to Mired values on the percentage scale
 * between the configured 'coolestMired' and 'warmestMired' Mired values, and vice versa.</li>
 *
 * <li>It handles inter relationships between color temperature states and the 'HS' part of the HSBType state. Where if
 * the color temperature changes then the HS values are adapted to match the corresponding color tempearture point on
 * the Planckian Locus in the CIE color chart.</li>
 *
 * <li>It handles input/output values in RGB format in the range [0..255]. The behavior depends on the
 * 'supportsRgbDimming' setting. If 'supportsRgbDimming' is true the RGB values read/write all three
 * parts of the HSBType state. Whereas if it is false the RGB values read/write only the 'HS' parts. NOTE: in the latter
 * case, a 'setRGBx()' call followed by a 'getRGBx()' call do not necessarily return the same values,
 * since the values are normalised to 100%. Neverthless the ratios between the RGB values do remain unchanged.</li>
 *
 * <li>If 'supportsRgbWhite' is configured it handles values in RGBW format. The behavior is similar to the RGB
 * case above except that the white channel is derived from the lowest of the RGB values and all values are clamped in
 * the range [0..255]. The 'supportsRgbDimming' changes the behavior in relation to 'HS' versus 'HSB' exactly as
 * in the case of RGB above</li>
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
 *     &#x40;Override
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
 *         model.setRGB(receivedData[1], receivedData[2], receivedData[3]);
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

    /**
     * The internal logic is implementated via an instance of this 'hidden' class
     */
    private final LightModelAbstractLogicImpl model;

    /*********************************************************************************
     * SECTION: Constructors
     *********************************************************************************/

    /**
     * Create a {@link LightModel} with default capabilities and parameters as follows:
     * <ul>
     * <li>'supportsBrightness' is true (the light supports brightness control)</li>
     * <li>'supportsColorTemperature' is true (the light supports color temperature control)</li>
     * <li>'supportsColor' is true (the light supports color control)</li>
     * <li>'rgbLinkedToBrightness' is false (the RGB values are not linked to 'B' part of {@link HSBType}))</li>
     * <li>'supportsRgbWhite' is false (the light does not support RGB with White)</li>
     * <li>'minimumOnBrightness' is 1.0 (the minimum brightness percent to consider as light "ON")</li>
     * <li>'warmestMired' is 500 (the 'warmest' white color temperature)</li>
     * <li>'coolestMired' is 153 (the 'coolest' white color temperature)</li>
     * <li>'stepSize' is 10.0 (the step size for IncreaseDecreaseType commands)</li>
     * </ul>
     */
    public LightModel() {
        this(true, true, true, false, false, null, null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param rgbLinkedToBrightness true if RGB vales are linked with the 'B' part of the {@link HSBType}
     * @param supportsRgbWhite true if the light supports RGBW rather than RGB color control
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            boolean rgbLinkedToBrightness, boolean supportsRgbWhite) {
        this(supportsBrightness, supportsColorTemperature, supportsColor, rgbLinkedToBrightness, supportsRgbWhite, null,
                null, null, null);
    }

    /**
     * Create a {@link LightModel} with the given capabilities and parameters.
     * The parameters can be null to use the default.
     *
     * @param supportsBrightness true if the light supports brightness control
     * @param supportsColorTemperature true if the light supports color temperature control
     * @param supportsColor true if the light supports color control
     * @param rgbLinkedToBrightness true if RGB vales are linked with the 'B' part of the {@link HSBType}
     * @param supportsRgbWhite true if the light supports RGBW rather than RGB color control
     * @param minimumOnBrightness the minimum brightness percent to consider as light "ON"
     * @param warmestMired the 'warmest' white color temperature in Mired
     * @param coolestMired the 'coolest' white color temperature in Mired
     * @param stepSize the step size for IncreaseDecreaseType commands
     * @throws IllegalArgumentException if any of the parameters are out of range
     */
    public LightModel(boolean supportsBrightness, boolean supportsColorTemperature, boolean supportsColor,
            boolean rgbLinkedToBrightness, boolean supportsRgbWhite, @Nullable Double minimumOnBrightness,
            @Nullable Double warmestMired, @Nullable Double coolestMired, @Nullable Double stepSize)
            throws IllegalArgumentException {
        // instantiate an inline implementation of the abstract logic implementation class
        model = new LightModelAbstractLogicImpl(supportsBrightness, supportsColorTemperature, supportsColor,
                rgbLinkedToBrightness, supportsRgbWhite, minimumOnBrightness, warmestMired, coolestMired, stepSize) {
        };
    }

    /*********************************************************************************
     * SECTION: Configuration getters and setters. Only used during initialization.
     *********************************************************************************/

    /**
     * Configuration: get the step size for IncreaseDecreaseType commands
     */
    public double configGetIncreaseDecreaseStep() {
        return model.configGetIncreaseDecreaseStep();
    }

    /**
     * Configuration: get the minimum brightness percent to consider as light "ON"
     */
    public double configGetMinimumOnBrightness() {
        return model.configGetMinimumOnBrightness();
    }

    /**
     * Configuration: get the Mired that corresponds to the coolest white light supported by the light
     */
    public double configGetMiredCoolest() {
        return model.cfgGetMiredCoolest();
    }

    /**
     * Configuration: get the Mired that corresponds to the warmest white light supported by the light
     */
    public double configGetMiredWarmest() {
        return model.cfgGetMiredWarmest();
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
    public boolean configGetRgbLinkedToBrightness() {
        return model.configGetRgbLinkedToBrightness();
    }

    /**
     * Configuration: check if brightness control is supported
     */
    public boolean configGetSupportsBrightness() {
        return model.configGetSupportsBrightness();
    }

    /**
     * Configuration: check if color control is supported
     */
    public boolean configGetSupportsColor() {
        return model.configGetSupportsColor();
    }

    /**
     * Configuration: check if color temperature control is supported
     */
    public boolean configGetSupportsColorTemperature() {
        return model.configGetSupportsColorTemperature();
    }

    /**
     * Configuration: check if RGBW color control is supported versus RGB only
     */
    public boolean configGetSupportsRgbWhite() {
        return model.configGetSupportsRgbWhite();
    }

    /**
     * Configuration: set the step size for IncreaseDecreaseType commands
     *
     * @param stepSize the step size in percent
     * @throws IllegalArgumentException if the stepSize parameter is out of range
     */
    public void configSetIncreaseDecreaseStep(double stepSize) throws IllegalArgumentException {
        model.configSetIncreaseDecreaseStep(stepSize);
    }

    /**
     * Configuration: set the minimum brightness percent to consider as light "ON"
     *
     * @param minimumOnBrightness the minimum brightness percent
     * @throws IllegalArgumentException if the minimumBrightness parameter is out of range
     */
    public void configSetMinimumOnBrightness(double minimumOnBrightness) throws IllegalArgumentException {
        model.configSetMinimumOnBrightness(minimumOnBrightness);
    }

    /**
     * Configuration: set the coolest supported color temperature in Mired
     *
     * @param coolestMired the coolest color temperature in Mired
     * @throws IllegalArgumentException if the coolestMired parameter is out of range or not less than warmestMired
     */
    public void configSetMiredCoolest(double coolestMired) throws IllegalArgumentException {
        model.configSetMiredCoolest(coolestMired);
    }

    /**
     * Configuration: set the warmest supported color temperature in Mired
     *
     * @param warmestMired the warmest color temperature in Mired
     *
     * @throws IllegalArgumentException if the warmestMired parameter is out of range or not greater than coolestMired
     */
    public void configSetMiredWarmest(double warmestMired) throws IllegalArgumentException {
        model.configSetMiredWarmest(warmestMired);
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
     * @param rgbLinkedToBrightness true if RGB values are raw
     */
    public void configSetRgbLinkedToBrightness(boolean rgbLinkedToBrightness) {
        model.configSetRgbLinkedToBrightness(rgbLinkedToBrightness);
    }

    /**
     * Configuration: set whether brightness control is supported
     *
     * @param supportsBrightness true if brightness control is supported
     */
    public void configSetSupportsBrightness(boolean supportsBrightness) {
        model.configSetSupportsBrightness(supportsBrightness);
    }

    /**
     * Configuration: set whether color control is supported
     *
     * @param supportsColor true if color control is supported
     */
    public void configSetSupportsColor(boolean supportsColor) {
        model.configSetSupportsColor(supportsColor);
    }

    /**
     * Configuration: set whether color temperature control is supported
     *
     * @param supportsColorTemperature true if color temperature control is supported
     */
    public void configSetSupportsColorTemperature(boolean supportsColorTemperature) {
        model.configSetSupportsColorTemperature(supportsColorTemperature);
    }

    /**
     * Configuration: set whether RGBW color control is supported versus RGB only
     *
     * @param supportsRgbWhite true if RGBW color control is supported
     */
    public void configSetSupportsRgbWhite(boolean supportsRgbWhite) {
        model.configSetSupportsRgbWhite(supportsRgbWhite);
    }

    /*********************************************************************************
     * SECTION: Runtime State getters, setters, and handlers. Only used at runtime.
     *********************************************************************************/

    /**
     * Runtime State: get the brightness or return null if the capability is not supported
     *
     * @return PercentType representing the brightness, or null if not supported
     */
    public @Nullable PercentType getBrightness() {
        return model.getBrightness();
    }

    /**
     * Runtime State: get the color or return null if the capability is not supported
     *
     * @return HSBType representing the color, or null if not supported
     */
    public @Nullable HSBType getColor() {
        return model.getColor();
    }

    /**
     * Runtime State: get the color temperature or return null if the capability is not supported
     *
     * @return QuantityType in Mired representing the color temperature, or null if not supported
     */
    public @Nullable QuantityType<?> getColorTemperature() {
        return model.getColorTemperature();
    }

    /**
     * Runtime State: get the color temperature in percent or return null if the capability is not supported
     *
     * @return PercentType in range [0..100] representing [coolest..warmest], or null if not supported
     */
    public @Nullable PercentType getColorTemperaturePercent() {
        return model.getColorTemperaturePercent();
    }

    /**
     * Runtime State: get the hue in range [0..360].
     *
     * @return double representing the hue in range [0..360].
     */
    public double getHue() {
        return model.getHue();
    }

    /**
     * Runtime State: get the color temperature in Mired.
     *
     * @return double representing the color temperature in Mired.
     */
    public double getMired() {
        return model.getMired();
    }

    /**
     * Runtime State: get the on/off state
     *
     * @return OnOffType.ON if brightness > minimumOnBrightness, otherwise OnOffType.OFF
     */
    public OnOffType getOnOff() {
        return model.getOnOff();
    }

    /**
     * Runtime State: get the RGB(W) values as an array of doubles in range [0..255]. Depending on the value of
     * 'supportsRgbWhite', the array length is either 3 (RGB) or 4 for (RGBW). The array is in the order [red, green,
     * blue, (white)]. Depending on the value of 'supportsRgbDimming}', the brightness may or may not be used
     * follows:
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
     * @return double[] representing the RGB(W) components in range [0..255.0]
     */
    public double[] getRGBx() {
        return model.getRGBx();
    }

    /**
     * Runtime State: get the saturation in range [0..100].
     *
     * @return double representing the saturation in range [0..100].
     */
    public double getSaturation() {
        return model.getSaturation();
    }

    /**
     * Runtime State: get the CIE XY color values in range [0.0..1.0].
     *
     * @return double[] representing the CIE XY color values in range [0.0..1.0].
     */
    public double[] getXY() {
        return model.getXY();
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
        model.handleColorTemperatureCommand(command);
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
        model.handleCommand(command);
    }

    /**
     * Runtime State: update the brightness from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param brightness in the range [0..100]
     * @throws IllegalArgumentException if the value is outside the range [0.0 to 100.0]
     */
    public void setBrightness(double brightness) throws IllegalArgumentException {
        model.setBrightness(brightness);
    }

    /**
     * Runtime State: update the hue from the remote light, ensuring it is in the range 0.0 to 360.0
     *
     * @param hue in the range [0..360]
     * @throws IllegalArgumentException if the hue parameter is not in the range 0.0 to 360.0
     */
    public void setHue(double hue) throws IllegalArgumentException {
        model.setHue(hue);
    }

    /**
     * Runtime State: update the mired color temperature from the remote light, and update the cached HSB color
     * accordingly. Constrain the mired value to be within the warmest and coolest limits.
     *
     * @param mired the color temperature in Mired
     * @throws IllegalArgumentException if the hue parameter is not in the range [coolestMired..warmestMired]
     */
    public void setMired(double mired) throws IllegalArgumentException {
        model.setMired(mired);
    }

    /**
     * Runtime State: update the color with RGB(W) fields from the remote light, and update the cached HSB color
     * accordingly. The array must be in the order [red, green, blue, (white)]. If white is present but the light does
     * not support white channel then IllegalArgumentException is thrown. Depending on the value of
     * 'supportsRgbDimming}', the brightness may or may not change as follows:
     *
     * <ul>
     * <li>{@code supportsRgbDimming == false} both [255,0,0] and [127.5,0,0] change the color to RED without a change
     * in brightness. In other words the values only relate to the 'HS' part of the {@link HSBType} state. Note: this
     * means that in this case a round trip of setRGBx() followed by getRGBx() will NOT necessarily contain identical
     * values, although the RGB ratios will certainly be the same.</li>
     *
     * <li>{@code supportsRgbDimming == true} both [255,0,0] and [127.5,0,0] change the color to RED and the former
     * changes the brightness to 100 percent, whereas the latter changes it to 50 percent. In other words the values
     * relate to all the 'HSB' parts of the {@link HSBType} state.</li>
     * <ul>
     *
     * @param rgbx an array of double representing RGB or RGBW values in range [0..255]
     */
    public void setRGBx(double[] rgbx) throws IllegalArgumentException {
        model.setRGBx(rgbx);
    }

    /**
     * Runtime State: update the saturation from the remote light, ensuring it is in the range 0.0 to 100.0
     *
     * @param saturation in the range [0..100]
     * @throws IllegalArgumentException if the saturation parameter is not in the range 0.0 to 100.0
     */
    public void setSaturation(double saturation) throws IllegalArgumentException {
        model.setSaturation(saturation);
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
        model.setXY(x, y);
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
