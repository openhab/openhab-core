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
package org.eclipse.smarthome.model.thing.test.hue;

import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Sets;

/**
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingType Description
 */
public class TestHueThingHandlerFactoryX extends BaseThingHandlerFactory implements ThingHandlerFactory {

    public static final String BINDING_ID = "Xhue";

    public final static ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "Xbridge");
    public final static ThingTypeUID THING_TYPE_LCT001 = new ThingTypeUID(BINDING_ID, "XLCT001");
    public final static ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "XTEST");

    public final static Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES = Sets.newHashSet(THING_TYPE_BRIDGE);
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_LCT001, THING_TYPE_TEST);
    public final static Set<ThingTypeUID> SUPPORTED_TYPES = Sets.union(SUPPORTED_BRIDGE_TYPES, SUPPORTED_THING_TYPES);

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
