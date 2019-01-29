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
/**
 *
 */
package org.eclipse.smarthome.model.thing.test.hue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingType Description
 *
 */
@Component
public class TestHueThingTypeProvider implements ThingTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(TestHueThingTypeProvider.class);
    private static final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<ThingTypeUID, ThingType>();

    public TestHueThingTypeProvider() {
        logger.debug("TestHueThingTypeProvider created");
        try {
            thingTypes.put(TestHueThingHandlerFactory.THING_TYPE_BRIDGE,
                    ThingTypeBuilder.instance(TestHueThingHandlerFactory.THING_TYPE_BRIDGE, "HueBridge")
                            .withDescription("HueBridge").isListed(false).buildBridge());

            ChannelDefinition color = new ChannelDefinitionBuilder("color",
                    TestHueChannelTypeProvider.COLOR_CHANNEL_TYPE_UID).build();

            ChannelDefinition colorTemp = new ChannelDefinitionBuilder("color_temperature",
                    TestHueChannelTypeProvider.COLOR_TEMP_CHANNEL_TYPE_UID).build();
            thingTypes.put(TestHueThingHandlerFactory.THING_TYPE_LCT001, ThingTypeBuilder
                    .instance(TestHueThingHandlerFactory.THING_TYPE_LCT001, "LCT001")
                    .withSupportedBridgeTypeUIDs(Arrays.asList(TestHueThingHandlerFactory.THING_TYPE_BRIDGE.toString()))
                    .withDescription("Hue LAMP").isListed(false).withChannelDefinitions(Arrays.asList(color, colorTemp))
                    .withConfigDescriptionURI(new URI("hue", "LCT001", null)).build());

            thingTypes.put(TestHueThingHandlerFactoryX.THING_TYPE_BRIDGE,
                    ThingTypeBuilder.instance(TestHueThingHandlerFactoryX.THING_TYPE_BRIDGE, "HueBridge")
                            .withDescription("HueBridge").isListed(false).buildBridge());

            ChannelDefinition colorX = new ChannelDefinitionBuilder("Xcolor",
                    TestHueChannelTypeProvider.COLORX_CHANNEL_TYPE_UID).build();

            ChannelDefinition colorTempX = new ChannelDefinitionBuilder("Xcolor_temperature",
                    TestHueChannelTypeProvider.COLORX_TEMP_CHANNEL_TYPE_UID).build();
            thingTypes.put(TestHueThingHandlerFactoryX.THING_TYPE_LCT001,
                    ThingTypeBuilder.instance(TestHueThingHandlerFactoryX.THING_TYPE_LCT001, "XLCT001")
                            .withSupportedBridgeTypeUIDs(
                                    Arrays.asList(TestHueThingHandlerFactoryX.THING_TYPE_BRIDGE.toString()))
                            .withDescription("Hue LAMP").isListed(false)
                            .withChannelDefinitions(Arrays.asList(colorX, colorTempX))
                            .withConfigDescriptionURI(new URI("Xhue", "XLCT001", null)).build());

            ChannelGroupDefinition groupDefinition = new ChannelGroupDefinition("group",
                    TestHueChannelTypeProvider.GROUP_CHANNEL_GROUP_TYPE_UID);
            thingTypes.put(TestHueThingHandlerFactory.THING_TYPE_GROUPED, ThingTypeBuilder
                    .instance(TestHueThingHandlerFactory.THING_TYPE_GROUPED, "grouped")
                    .withSupportedBridgeTypeUIDs(Arrays.asList(TestHueThingHandlerFactory.THING_TYPE_BRIDGE.toString()))
                    .withDescription("Grouped Lamp").withChannelGroupDefinitions(Arrays.asList(groupDefinition))
                    .withConfigDescriptionURI(new URI("hue", "grouped", null)).build());

        } catch (Exception e) {
            logger.error("{}", e.getMessage());
        }
    }

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        return thingTypes.values();
    }

    @Override
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        return thingTypes.get(thingTypeUID);
    }

}
