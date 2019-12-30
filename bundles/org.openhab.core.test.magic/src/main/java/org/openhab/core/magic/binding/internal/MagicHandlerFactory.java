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
package org.openhab.core.magic.binding.internal;

import static org.openhab.core.magic.binding.MagicBindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openhab.core.magic.binding.handler.MagicActionModuleThingHandler;
import org.openhab.core.magic.binding.handler.MagicBridgeHandler;
import org.openhab.core.magic.binding.handler.MagicBridgedThingHandler;
import org.openhab.core.magic.binding.handler.MagicChattyThingHandler;
import org.openhab.core.magic.binding.handler.MagicColorLightHandler;
import org.openhab.core.magic.binding.handler.MagicConfigurableThingHandler;
import org.openhab.core.magic.binding.handler.MagicContactHandler;
import org.openhab.core.magic.binding.handler.MagicDelayedOnlineHandler;
import org.openhab.core.magic.binding.handler.MagicDimmableLightHandler;
import org.openhab.core.magic.binding.handler.MagicDynamicStateDescriptionThingHandler;
import org.openhab.core.magic.binding.handler.MagicExtensibleThingHandler;
import org.openhab.core.magic.binding.handler.MagicFirmwareUpdateThingHandler;
import org.openhab.core.magic.binding.handler.MagicImageHandler;
import org.openhab.core.magic.binding.handler.MagicLocationThingHandler;
import org.openhab.core.magic.binding.handler.MagicOnOffLightHandler;
import org.openhab.core.magic.binding.handler.MagicOnlineOfflineHandler;
import org.openhab.core.magic.binding.handler.MagicPlayerHandler;
import org.openhab.core.magic.binding.handler.MagicRolllershutterHandler;
import org.openhab.core.magic.binding.handler.MagicThermostatThingHandler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link MagicHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Henning Treu - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.magic")
public class MagicHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_EXTENSIBLE_THING, THING_TYPE_ON_OFF_LIGHT, THING_TYPE_DIMMABLE_LIGHT,
                    THING_TYPE_COLOR_LIGHT, THING_TYPE_CONTACT_SENSOR, THING_TYPE_CONFIG_THING,
                    THING_TYPE_DELAYED_THING, THING_TYPE_LOCATION, THING_TYPE_THERMOSTAT, THING_TYPE_FIRMWARE_UPDATE,
                    THING_TYPE_BRIDGE_1, THING_TYPE_BRIDGE_2, THING_TYPE_BRIDGED_THING, THING_TYPE_CHATTY_THING,
                    THING_TYPE_ROLLERSHUTTER, THING_TYPE_PLAYER, THING_TYPE_IMAGE, THING_TYPE_ACTION_MODULE,
                    THING_TYPE_DYNAMIC_STATE_DESCRIPTION, THING_TYPE_ONLINE_OFFLINE).collect(Collectors.toSet()));

    private MagicDynamicStateDescriptionProvider stateDescriptionProvider;

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
            return new MagicActionModuleThingHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_DYNAMIC_STATE_DESCRIPTION)) {
            return new MagicDynamicStateDescriptionThingHandler(thing, stateDescriptionProvider);
        }
        if (thingTypeUID.equals(THING_TYPE_ONLINE_OFFLINE)) {
            return new MagicOnlineOfflineHandler(thing);
        }

        if (thingTypeUID.equals(THING_TYPE_BRIDGE_1) || thingTypeUID.equals(THING_TYPE_BRIDGE_2)) {
            return new MagicBridgeHandler((Bridge) thing);
        }

        return null;
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(MagicDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    protected void unsetDynamicStateDescriptionProvider(MagicDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.stateDescriptionProvider = null;
    }
}
