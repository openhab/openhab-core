/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.magic.binding;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MagicBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicBindingConstants {

    public static final String BINDING_ID = "magic";

    // List all Thing Type UIDs, related to the Magic Binding

    // generic thing types
    public static final ThingTypeUID THING_TYPE_EXTENSIBLE_THING = new ThingTypeUID(BINDING_ID, "extensible-thing");
    public static final ThingTypeUID THING_TYPE_ON_OFF_LIGHT = new ThingTypeUID(BINDING_ID, "onoff-light");
    public static final ThingTypeUID THING_TYPE_DIMMABLE_LIGHT = new ThingTypeUID(BINDING_ID, "dimmable-light");
    public static final ThingTypeUID THING_TYPE_COLOR_LIGHT = new ThingTypeUID(BINDING_ID, "color-light");
    public static final ThingTypeUID THING_TYPE_CONTACT_SENSOR = new ThingTypeUID(BINDING_ID, "contact-sensor");
    public static final ThingTypeUID THING_TYPE_CONFIG_THING = new ThingTypeUID(BINDING_ID, "configurable-thing");
    public static final ThingTypeUID THING_TYPE_DELAYED_THING = new ThingTypeUID(BINDING_ID, "delayed-thing");
    public static final ThingTypeUID THING_TYPE_LOCATION = new ThingTypeUID(BINDING_ID, "location-thing");
    public static final ThingTypeUID THING_TYPE_THERMOSTAT = new ThingTypeUID(BINDING_ID, "thermostat");
    public static final ThingTypeUID THING_TYPE_FIRMWARE_UPDATE = new ThingTypeUID(BINDING_ID, "firmware-update");
    public static final ThingTypeUID THING_TYPE_CHATTY_THING = new ThingTypeUID(BINDING_ID, "chatty-thing");
    public static final ThingTypeUID THING_TYPE_ROLLERSHUTTER = new ThingTypeUID(BINDING_ID, "rollershutter");
    public static final ThingTypeUID THING_TYPE_PLAYER = new ThingTypeUID(BINDING_ID, "player");
    public static final ThingTypeUID THING_TYPE_IMAGE = new ThingTypeUID(BINDING_ID, "image");
    public static final ThingTypeUID THING_TYPE_ACTION_MODULE = new ThingTypeUID(BINDING_ID, "action-module");
    public static final ThingTypeUID THING_TYPE_ONLINE_OFFLINE = new ThingTypeUID(BINDING_ID, "online-offline");

    // bridged things
    public static final ThingTypeUID THING_TYPE_BRIDGE_1 = new ThingTypeUID(BINDING_ID, "magic-bridge1");
    public static final ThingTypeUID THING_TYPE_BRIDGE_2 = new ThingTypeUID(BINDING_ID, "magic-bridge2");
    public static final ThingTypeUID THING_TYPE_BRIDGED_THING = new ThingTypeUID(BINDING_ID, "bridgedThing");

    // List all channels
    public static final String CHANNEL_SWITCH = "switch";
    public static final String CHANNEL_BRIGHTNESS = "brightness";
    public static final String CHANNEL_COLOR = "color";
    public static final String CHANNEL_CONTACT = "contact";
    public static final String CHANNEL_LOCATION = "location";
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_SET_TEMPERATURE = "set-temperature";

    // Firmware update needed models
    public static final String UPDATE_MODEL_PROPERTY = "updateModel";

    public static final String MODEL_ALOHOMORA = "Alohomora";
    public static final String MODEL_COLLOPORTUS = "Colloportus";
    public static final String MODEL_LUMOS = "Lumos";
    public static final String MODEL_NOX = "Nox";

    public static final String CHANNEL_IMAGE = "image";
}
