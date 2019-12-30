/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.thing.testsupport.hue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Component;

/**
 * @author Benedikt Niehues - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class)
public class TestHueThingHandlerFactory extends BaseThingHandlerFactory {

    public static final String BINDING_ID = "hue";

    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_LCT001 = new ThingTypeUID(BINDING_ID, "LCT001");
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "SENSOR");
    public static final ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "TEST");
    public static final ThingTypeUID THING_TYPE_GROUPED = new ThingTypeUID(BINDING_ID, "grouped");
    public static final ThingTypeUID THING_TYPE_LONG_NAME = new ThingTypeUID(BINDING_ID,
            "1-thing-id-with-5-dashes_and_3_underscores");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES = Stream.of(THING_TYPE_BRIDGE)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_LCT001, THING_TYPE_SENSOR, THING_TYPE_TEST, THING_TYPE_LONG_NAME, THING_TYPE_GROUPED)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_TYPES = Stream
            .concat(SUPPORTED_BRIDGE_TYPES.stream(), SUPPORTED_THING_TYPES.stream()).collect(Collectors.toSet());

    // List all channels
    public static final String CHANNEL_COLORTEMPERATURE = "color_temperature";
    public static final String CHANNEL_COLOR = "color";
    public static final String CHANNEL_BRIGHTNESS = "brightness";

    // Bridge config properties
    public static final String HOST = "ipAddress";
    public static final String USER_NAME = "userName";
    public static final String SERIAL_NUMBER = "serialNumber";

    // Light config properties
    public static final String LIGHT_ID = "lightId";

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        if (SUPPORTED_BRIDGE_TYPES.contains(thingTypeUID)) {
            ThingUID hueBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, hueBridgeUID, null);
        }
        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID hueLightUID = getLightUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, hueLightUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the hue binding.");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_TYPES.contains(thingTypeUID);
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            Configuration configuration) {
        if (thingUID != null) {
            return thingUID;
        } else {
            String serialNumber = (String) configuration.get(SERIAL_NUMBER);
            return new ThingUID(thingTypeUID, serialNumber);
        }
    }

    private ThingUID getLightUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID) {
        if (thingUID != null) {
            return thingUID;
        } else {
            String lightId = (String) configuration.get(LIGHT_ID);
            return new ThingUID(thingTypeUID, lightId, bridgeUID.getId());
        }
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        if (thing instanceof Bridge) {
            return new BaseBridgeHandler((Bridge) thing) {
                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                }

                @Override
                public void initialize() {
                    updateStatus(ThingStatus.ONLINE);
                }
            };
        } else {
            return new BaseThingHandler(thing) {
                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                }

                @Override
                public void initialize() {
                    updateStatus(ThingStatus.ONLINE);
                }
            };
        }
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
    }
}
