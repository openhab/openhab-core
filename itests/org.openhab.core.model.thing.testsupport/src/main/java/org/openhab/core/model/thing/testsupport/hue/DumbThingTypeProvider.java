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

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@Component
public class DumbThingTypeProvider implements ThingTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(DumbThingTypeProvider.class);
    private static final Map<ThingTypeUID, ThingType> THING_TYPES = new HashMap<>();

    public DumbThingTypeProvider() {
        logger.debug("DumbThingTypeProvider created");
        try {
            ChannelDefinition channel1 = new ChannelDefinitionBuilder("channel1",
                    new ChannelTypeUID(DumbThingHandlerFactory.BINDING_ID, "channel1")).build();
            List<ChannelDefinition> channelDefinitions = List.of(channel1);

            THING_TYPES.put(DumbThingHandlerFactory.THING_TYPE_TEST,
                    ThingTypeBuilder.instance(DumbThingHandlerFactory.THING_TYPE_TEST, "DUMB")
                            .withDescription("Funky Thing").isListed(false).withChannelDefinitions(channelDefinitions)
                            .withConfigDescriptionURI(new URI("dumb:DUMB")).build());
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
        }
    }

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        return THING_TYPES.values();
    }

    @Override
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        return THING_TYPES.get(thingTypeUID);
    }
}
