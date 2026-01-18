/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;
import org.openhab.core.util.LightModel;

/**
 * {@link BaseLightThingHandler} provides an abstract base implementation for a {@link ThingHandler} for a light.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public abstract class BaseLightThingHandler extends BaseThingHandler {

    /**
     * Light state machine model to manage light capabilities, configuration, and runtime state
     */
    private final LightModel model = new LightModel();

    public BaseLightThingHandler(Thing thing) {
        super(thing);
    }

    /**
     * Override this method to handle commands from OH core.
     * <p>
     * Example: (implementation will depend on the specific binding and device).
     *
     * <pre>
     * {@code
     *
     * // update the model state based on the command from OpenHAB
     * model.handleCommand(command);
     *
     * // or if it is a color temperature command
     * model.handleColorTemperatureCommand(command);
     *
     * // and transmit the appropriate command to the remote light device based on the model state
     * doTransmitBindingSpecificRemoteLightData(model);
     *
     * }
     * </pre>
     */
    @Override
    public abstract void handleCommand(ChannelUID channelUID, Command command);

    /**
     * Override this method to provide initialization of the light state machine capabilities and configuration
     * parameters.
     * <p>
     * Example: (implementation will depend on the specific binding and device).
     *
     * <pre>
     * {@code
     *
     *  // STEP 1: Set up the light state machine capabilities.
     *  model.configSetLightCapabilities(LightCapabilities.COLOR_WITH_COLOR_TEMPERATURE);
     *
     *  // STEP 2: optionally set up the light state machine configuration parameters.
     *  // These would typically be read from the thing configuration or read from the remote device.
     *  model.configSetRgbDataType(RgbDataType.RGB_NO_BRIGHTNESS); // RGB data type
     *  model.configSetMinimumOnBrightness(2); // minimum brightness level in % when on
     *  model.configSetIncreaseDecreaseStep(10); // step size for increase/decrease commands
     *  model.configSetMiredControlCoolest(153); // color temperature control range coolest
     *  model.configSetMiredControlWarmest(500); // color temperature control range warmest
     *
     *  // STEP 3: optionally if the light has warm and cool white LEDS then set up their LED color temperatures.
     *  // These would typically be read from the thing configuration or read from the remote device.
     *  model.configSetMiredCoolWhiteLED(153);
     *  model.configSetMiredWarmWhiteLED(500);
     *
     *  // STEP 4: now set the status to UNKNOWN to indicate that we are initialized
     *  updateStatus(ThingStatus.UNKNOWN);
     *
     *  // STEP 5: finally provide further initialization, e.g. connecting to the remote device
     *  ...
     *
     * }
     * </pre>
     */
    @Override
    public abstract void initialize();

    /**
     * Transmit the appropriate command to the remote light device based on the model state.
     * This method must be overridden in the concrete implementation to transmit the appropriate command(s)
     * to the remote light device based on the model state.
     * <p>
     * Example: (implementation will depend on the specific binding and device).
     *
     * <pre>
     * {@code
     *
     *  if (model.getOnOff() == OnOffType.ON) {
     *      transmit command to turn on the light
     *  } else {
     *      transmit command to turn off the light
     *  }
     *
     *  if (model.getBrightness() != null) {
     *      transmit command to set brightness to model.getBrightness()
     *  }
     *
     *  if (model.getColor() != null) {
     *      transmit command to set color to model.getColor()
     *  }
     *
     *  if (model.getColorTemperature() != null) {
     *      transmit command to set color temperature to model.getColorTemperature()
     *  }
     *
     *  if (model.getColorTemperaturePercent() != null) {
     *      transmit command to set color temperature percent to model.getColorTemperaturePercent()
     *  }
     *
     *  if (model.getRGBx().length == 3) {
     *      transmit command to set RGB value to model.getRGBx()
     *  }
     *
     *  if (model.getXY() != null) {
     *      transmit command to set XY value to model.getXY()
     *  }
     * }
     * </pre>
     *
     * @param model the light model containing the current state
     */
    protected abstract void doTransmitBindingSpecificRemoteLightData(LightModel model);

    /**
     * Receive data from the remote light device and update the model state accordingly.
     * This method must be overridden in the concrete implementation to 1) receive data from the remote light device
     * 2) update the model state accordingly, and 3) update the openHAB channels.
     * <P>
     * Example: (implementation will depend on the specific binding and device).
     *
     * <pre>
     * {@code
     *
     *  STEP 1: Parse the remoteData to extract the relevant information. Depends on specific binding / device
     *
     *  OnOffType onOff = ...; // extract on/off state from remoteData
     *  Integer brightness = ...; // extract brightness from remoteData
     *  HSBType color = ...; // extract color from remoteData
     *  Integer colorTemperature = ...; // extract color temperature from remoteData
     *  Integer colorTemperaturePercent = ...; // extract color temperature percent from remoteData
     *  RGBType rgb = ...; // extract RGB value from remoteData
     *  XYType xy = ...; // extract XY value from remoteData
     *
     *  STEP 2: Update the model state based on the received data
     *
     *  if (onOff != null) {
     *      model.setOnOff(onOff);
     *  }
     *
     *  if (brightness != null) {
     *      model.setBrightness(brightness);
     *  }
     *
     *  if (color != null) {
     *      model.setColor(color);
     *  }
     *
     *  if (colorTemperature != null) {
     *      model.setColorTemperature(colorTemperature);
     *  }
     *
     *  if (colorTemperaturePercent != null) {
     *      model.setColorTemperaturePercent(colorTemperaturePercent);
     *  }
     *
     *  if (rgb != null) {
     *      model.setRGBx(rgb);
     *  }
     *
     *  if (xy != null) {
     *      model.setXY(xy);
     *  }
     *
     *  STEP 3: After updating the model, update the channel states in OpenHAB
     *  Note: Ensure that the channel IDs used in updateState() match those defined in the thing type.
     *
     *  if (model.configGetLightCapabilities().supportsColor()) {
     *      updateState(CHANNEL_COLOR, model.getColor());
     *  } else if (model.configGetLightCapabilities().supportsBrightness()) {
     *      updateState(CHANNEL_BRIGHTNESS, model.getBrightness());
     *  } else {
     *      updateState(CHANNEL_ON_OFF, model.getOnOff());
     *  }
     *
     *  if (model.configGetLightCapabilities().supportsColorTemperature()) {
     *      updateState(CHANNEL_COLOR_TEMPERATURE_ABS, model.getColorTemperature());
     *      updateState(CHANNEL_COLOR_TEMPERATURE_PERCENT, model.getColorTemperaturePercent());
     *  }
     * }
     * </pre>
     *
     * @param remoteData the data received from the remote light device
     */
    protected abstract void onReceiveBindingSpecificRemoteLightData(Object... remoteData);
}
