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
package org.eclipse.smarthome.magic.binding.internal;

import static org.eclipse.smarthome.magic.binding.MagicBindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.magic.binding.handler.MagicActionModuleThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicBridgeHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicBridgedThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicChattyThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicColorLightHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicConfigurableThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicContactHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicDelayedOnlineHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicDimmableLightHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicExtensibleThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicFirmwareUpdateThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicImageHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicLocationThingHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicOnOffLightHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicOnlineOfflineHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicPlayerHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicRolllershutterHandler;
import org.eclipse.smarthome.magic.binding.handler.MagicThermostatThingHandler;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link MagicHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Henning Treu - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.magic")
public class MagicHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(Stream
            .of(THING_TYPE_EXTENSIBLE_THING, THING_TYPE_ON_OFF_LIGHT, THING_TYPE_DIMMABLE_LIGHT, THING_TYPE_COLOR_LIGHT,
                    THING_TYPE_CONTACT_SENSOR, THING_TYPE_CONFIG_THING, THING_TYPE_DELAYED_THING, THING_TYPE_LOCATION,
                    THING_TYPE_THERMOSTAT, THING_TYPE_FIRMWARE_UPDATE, THING_TYPE_BRIDGE_1, THING_TYPE_BRIDGE_2,
                    THING_TYPE_BRIDGED_THING, THING_TYPE_CHATTY_THING, THING_TYPE_ROLLERSHUTTER, THING_TYPE_PLAYER,
                    THING_TYPE_IMAGE, THING_TYPE_ACTION_MODULE, THING_TYPE_ONLINE_OFFLINE)
            .collect(Collectors.toSet()));

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_EXTENSIBLE_THING)) {
            return new MagicExtensibleThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_ON_OFF_LIGHT)) {
            return new MagicOnOffLightHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_DIMMABLE_LIGHT)) {
            return new MagicDimmableLightHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_COLOR_LIGHT)) {
            return new MagicColorLightHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_CONTACT_SENSOR)) {
            return new MagicContactHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_CONFIG_THING)) {
            return new MagicConfigurableThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_DELAYED_THING)) {
            return new MagicDelayedOnlineHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_LOCATION)) {
            return new MagicLocationThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_THERMOSTAT)) {
            return new MagicThermostatThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_FIRMWARE_UPDATE)) {
            return new MagicFirmwareUpdateThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_BRIDGED_THING)) {
            return new MagicBridgedThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_CHATTY_THING)) {
            return new MagicChattyThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_ROLLERSHUTTER)) {
            return new MagicRolllershutterHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_PLAYER)) {
            return new MagicPlayerHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_IMAGE)) {
            return new MagicImageHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_ACTION_MODULE)) {
            MagicActionModuleThingHandler handler = new MagicActionModuleThingHandler(thing);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_ONLINE_OFFLINE)) {
            return new MagicOnlineOfflineHandler(thing);
        }

        if (thingTypeUID.equals(THING_TYPE_BRIDGE_1) || thingTypeUID.equals(THING_TYPE_BRIDGE_2)) {
            return new MagicBridgeHandler((Bridge) thing);
        }

        return null;
    }
}
