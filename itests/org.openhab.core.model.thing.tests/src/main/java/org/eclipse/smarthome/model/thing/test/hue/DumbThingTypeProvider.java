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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Kaufmann - Initial contribution and API
 */
@Component
public class DumbThingTypeProvider implements ThingTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(DumbThingTypeProvider.class);
    private static final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<ThingTypeUID, ThingType>();

    public DumbThingTypeProvider() {
        logger.debug("DumbThingTypeProvider created");
        try {
            ChannelDefinition channel1 = new ChannelDefinitionBuilder("channel1",
                    new ChannelTypeUID(DumbThingHandlerFactory.BINDING_ID, "channel1")).build();
            List<ChannelDefinition> channelDefinitions = Collections.singletonList(channel1);

            thingTypes.put(DumbThingHandlerFactory.THING_TYPE_TEST,
                    ThingTypeBuilder.instance(DumbThingHandlerFactory.THING_TYPE_TEST, "DUMB")
                            .withDescription("Funky Thing").isListed(false).withChannelDefinitions(channelDefinitions)
                            .withConfigDescriptionURI(new URI("dumb:DUMB")).build());
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
