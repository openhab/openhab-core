/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.magic.binding.handler.MagicActionModuleThingHandler;
import org.openhab.core.magic.binding.handler.MagicBridgeHandler;
import org.openhab.core.magic.binding.handler.MagicBridgedThingHandler;
import org.openhab.core.magic.binding.handler.MagicButtonHandler;
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
import org.openhab.core.magic.binding.handler.MagicRollershutterHandler;
import org.openhab.core.magic.binding.handler.MagicThermostatThingHandler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link MagicHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Henning Treu - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.magic")
@NonNullByDefault
public class MagicHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_EXTENSIBLE_THING,
            THING_TYPE_BUTTON, THING_TYPE_ON_OFF_LIGHT, THING_TYPE_DIMMABLE_LIGHT, THING_TYPE_COLOR_LIGHT,
            THING_TYPE_CONTACT_SENSOR, THING_TYPE_CONFIG_THING, THING_TYPE_DELAYED_THING, THING_TYPE_LOCATION,
            THING_TYPE_THERMOSTAT, THING_TYPE_FIRMWARE_UPDATE, THING_TYPE_BRIDGE_1, THING_TYPE_BRIDGE_2,
            THING_TYPE_BRIDGED_THING, THING_TYPE_CHATTY_THING, THING_TYPE_ROLLERSHUTTER, THING_TYPE_PLAYER,
            THING_TYPE_IMAGE, THING_TYPE_ACTION_MODULE, THING_TYPE_DYNAMIC_STATE_DESCRIPTION,
            THING_TYPE_ONLINE_OFFLINE);

    private final MagicDynamicCommandDescriptionProvider commandDescriptionProvider;
    private final MagicDynamicStateDescriptionProvider stateDescriptionProvider;

    @Activate
    public MagicHandlerFactory(final @Reference MagicDynamicCommandDescriptionProvider commandDescriptionProvider, //
            final @Reference MagicDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.commandDescriptionProvider = commandDescriptionProvider;
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_EXTENSIBLE_THING.equals(thingTypeUID)) {
            return new MagicExtensibleThingHandler(thing);
        } else if (THING_TYPE_BUTTON.equals(thingTypeUID)) {
            return new MagicButtonHandler(thing);
        } else if (THING_TYPE_ON_OFF_LIGHT.equals(thingTypeUID)) {
            return new MagicOnOffLightHandler(thing);
        } else if (THING_TYPE_DIMMABLE_LIGHT.equals(thingTypeUID)) {
            return new MagicDimmableLightHandler(thing);
        } else if (THING_TYPE_COLOR_LIGHT.equals(thingTypeUID)) {
            return new MagicColorLightHandler(thing);
        } else if (THING_TYPE_CONTACT_SENSOR.equals(thingTypeUID)) {
            return new MagicContactHandler(thing);
        } else if (THING_TYPE_CONFIG_THING.equals(thingTypeUID)) {
            return new MagicConfigurableThingHandler(thing);
        } else if (THING_TYPE_DELAYED_THING.equals(thingTypeUID)) {
            return new MagicDelayedOnlineHandler(thing);
        } else if (THING_TYPE_LOCATION.equals(thingTypeUID)) {
            return new MagicLocationThingHandler(thing);
        } else if (THING_TYPE_THERMOSTAT.equals(thingTypeUID)) {
            return new MagicThermostatThingHandler(thing);
        } else if (THING_TYPE_FIRMWARE_UPDATE.equals(thingTypeUID)) {
            return new MagicFirmwareUpdateThingHandler(thing);
        } else if (THING_TYPE_BRIDGED_THING.equals(thingTypeUID)) {
            return new MagicBridgedThingHandler(thing);
        } else if (THING_TYPE_CHATTY_THING.equals(thingTypeUID)) {
            return new MagicChattyThingHandler(thing);
        } else if (THING_TYPE_ROLLERSHUTTER.equals(thingTypeUID)) {
            return new MagicRollershutterHandler(thing);
        } else if (THING_TYPE_PLAYER.equals(thingTypeUID)) {
            return new MagicPlayerHandler(thing);
        } else if (THING_TYPE_IMAGE.equals(thingTypeUID)) {
            return new MagicImageHandler(thing);
        } else if (THING_TYPE_ACTION_MODULE.equals(thingTypeUID)) {
            return new MagicActionModuleThingHandler(thing);
        } else if (THING_TYPE_DYNAMIC_STATE_DESCRIPTION.equals(thingTypeUID)) {
            return new MagicDynamicStateDescriptionThingHandler(thing, commandDescriptionProvider,
                    stateDescriptionProvider);
        } else if (THING_TYPE_ONLINE_OFFLINE.equals(thingTypeUID)) {
            return new MagicOnlineOfflineHandler(thing);
        } else if (THING_TYPE_BRIDGE_1.equals(thingTypeUID) || THING_TYPE_BRIDGE_2.equals(thingTypeUID)) {
            return new MagicBridgeHandler((Bridge) thing);
        }

        return null;
    }
}
