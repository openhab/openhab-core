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

import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;

/**
 * @author Benedikt Niehues - Initial contribution
 */
public class TestHueThingHandlerFactoryX extends BaseThingHandlerFactory implements ThingHandlerFactory {

    public static final String BINDING_ID = "Xhue";

    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "Xbridge");
    public static final ThingTypeUID THING_TYPE_LCT001 = new ThingTypeUID(BINDING_ID, "XLCT001");
    public static final ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "XTEST");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES = Stream.of(THING_TYPE_BRIDGE)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream.of(THING_TYPE_LCT001, THING_TYPE_TEST)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_TYPES = Stream
            .concat(SUPPORTED_BRIDGE_TYPES.stream(), SUPPORTED_THING_TYPES.stream()).collect(Collectors.toSet());

    // List all channels
    public static final String CHANNEL_COLORTEMPERATURE = "Xcolor_temperature";
    public static final String CHANNEL_COLOR = "Xcolor";
    public static final String CHANNEL_BRIGHTNESS = "Xbrightness";

    // Bridge config properties
    public static final String HOST = "XipAddress";
    public static final String USER_NAME = "XuserName";
    public static final String SERIAL_NUMBER = "XserialNumber";

    // Light config properties
    public static final String LIGHT_ID = "XlightId";

    public TestHueThingHandlerFactoryX(ComponentContext componentContext) {
        super.activate(componentContext);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
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

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID != null) {
            return thingUID;
        } else {
            String serialNumber = (String) configuration.get(SERIAL_NUMBER);
            return new ThingUID(thingTypeUID, serialNumber);
        }
    }

    private ThingUID getLightUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        if (thingUID != null) {
            return thingUID;
        } else {
            String lightId = (String) configuration.get(LIGHT_ID);
            return new ThingUID(thingTypeUID, lightId, bridgeUID.getId());
        }
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
    }
}
