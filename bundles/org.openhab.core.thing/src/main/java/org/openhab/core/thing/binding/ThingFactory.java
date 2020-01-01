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
package org.openhab.core.thing.binding;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.ThingFactoryHelper;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ThingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ThingFactory} helps to create thing based on a given {@link ThingType} .
 *
 * @author Dennis Nobel - Initial contribution, added support for channel groups
 * @author Benedikt Niehues - fix for Bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=445137 considering default
 *         values
 * @author Thomas Höfer - added thing and thing type properties
 * @author Chris Jackson - Added properties, label, description
 */
@NonNullByDefault
public class ThingFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThingFactory.class);

    /**
     * Generates a random Thing UID for the given thingType
     *
     * @param thingTypeUID thing type (must not be null)
     * @return random Thing UID
     */
    public static ThingUID generateRandomThingUID(ThingTypeUID thingTypeUID) {
        String uuid = UUID.randomUUID().toString();
        String thingId = uuid.substring(uuid.length() - 12, uuid.length());
        return new ThingUID(thingTypeUID, thingId);
    }

    /**
     * Creates a thing based on a given thing type.
     *
     * @param thingType thing type (must not be null)
     * @param thingUID thindUID (must not be null)
     * @param configuration (must not be null)
     * @param bridge (can be null)
     * @return thing the thing
     */
    public static Thing createThing(ThingType thingType, ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID) {
        return createThing(thingType, thingUID, configuration, bridgeUID, null);
    }

    /**
     * Creates a thing based on a given thing type. It also creates the
     * default-configuration given in the configDescriptions if the
     * configDescriptionRegistry is not null
     *
     * @param thingType (must not be null)
     * @param thingUID (must not be null)
     * @param configuration (must not be null)
     * @param bridgeUID (can be null)
     * @param configDescriptionRegistry (can be null)
     * @return thing the thing
     */
    public static Thing createThing(ThingType thingType, ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID, @Nullable ConfigDescriptionRegistry configDescriptionRegistry) {
        ThingFactoryHelper.applyDefaultConfiguration(configuration, thingType, configDescriptionRegistry);

        List<Channel> channels = ThingFactoryHelper.createChannels(thingType, thingUID, configDescriptionRegistry);

        return createThingBuilder(thingType, thingUID).withConfiguration(configuration).withChannels(channels)
                .withProperties(thingType.getProperties()).withBridge(bridgeUID).build();
    }

    public static @Nullable Thing createThing(ThingUID thingUID, Configuration configuration,
            @Nullable Map<String, String> properties, @Nullable ThingUID bridgeUID, ThingTypeUID thingTypeUID,
            List<ThingHandlerFactory> thingHandlerFactories) {
        for (ThingHandlerFactory thingHandlerFactory : thingHandlerFactories) {
            if (thingHandlerFactory.supportsThingType(thingTypeUID)) {
                Thing thing = thingHandlerFactory.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
                if (thing == null) {
                    LOGGER.error(
                            "Thing factory ({}) returned null on create thing when it reports to support the thing type ({}).",
                            thingHandlerFactory.getClass(), thingTypeUID);
                } else {
                    if (properties != null) {
                        for (String key : properties.keySet()) {
                            thing.setProperty(key, properties.get(key));
                        }
                    }
                }
                return thing;
            }
        }
        return null;
    }

    /**
     *
     * Creates a thing based on given thing type.
     *
     * @param thingType thing type (must not be null)
     * @param thingUID thingUID (must not be null)
     * @param configuration (must not be null)
     * @return thing the thing
     */
    public static Thing createThing(ThingType thingType, ThingUID thingUID, Configuration configuration) {
        return createThing(thingType, thingUID, configuration, null);
    }

    private static ThingBuilder createThingBuilder(ThingType thingType, ThingUID thingUID) {
        if (thingType instanceof BridgeType) {
            return BridgeBuilder.create(thingType.getUID(), thingUID);
        }
        return ThingBuilder.create(thingType.getUID(), thingUID);
    }

}
