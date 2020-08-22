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
import java.util.List;
import java.util.Locale;

import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.service.component.annotations.Component;

/**
 *
 * {@link TestHueChannelTypeProvider} is used for the model thing tests to provide channel types.
 *
 * @author Dennis Nobel - Initial contribution
 */
@Component
public class TestHueChannelTypeProvider implements ChannelTypeProvider, ChannelGroupTypeProvider {

    public static final ChannelTypeUID COLORX_TEMP_CHANNEL_TYPE_UID = new ChannelTypeUID("Xhue:Xcolor_temperature");
    public static final ChannelTypeUID COLORX_CHANNEL_TYPE_UID = new ChannelTypeUID("Xhue:color");
    public static final ChannelTypeUID COLOR_TEMP_CHANNEL_TYPE_UID = new ChannelTypeUID("hue:color_temperature");
    public static final ChannelTypeUID COLOR_CHANNEL_TYPE_UID = new ChannelTypeUID("hue:color");

    public static final ChannelGroupTypeUID GROUP_CHANNEL_GROUP_TYPE_UID = new ChannelGroupTypeUID("hue", "group");

    private List<ChannelType> channelTypes;
    private List<ChannelGroupType> channelGroupTypes;

    public TestHueChannelTypeProvider() {
        try {
            ChannelType ctColor = ChannelTypeBuilder.state(COLOR_CHANNEL_TYPE_UID, "colorLabel", "Color")
                    .withDescription("description").withConfigDescriptionURI(new URI("hue", "LCT001:color", null))
                    .build();

            ChannelType ctColorTemperature = ChannelTypeBuilder
                    .state(COLOR_TEMP_CHANNEL_TYPE_UID, "colorTemperatureLabel", "Dimmer")
                    .withDescription("description")
                    .withConfigDescriptionURI(new URI("hue", "LCT001:color_temperature", null)).build();

            ChannelType ctColorX = ChannelTypeBuilder.state(COLORX_CHANNEL_TYPE_UID, "colorLabel", "Color")
                    .withDescription("description").withConfigDescriptionURI(new URI("Xhue", "XLCT001:Xcolor", null))
                    .build();

            ChannelType ctColorTemperatureX = ChannelTypeBuilder
                    .state(COLORX_TEMP_CHANNEL_TYPE_UID, "colorTemperatureLabel", "Dimmer")
                    .withDescription("description")
                    .withConfigDescriptionURI(new URI("Xhue", "XLCT001:Xcolor_temperature", null)).build();

            channelTypes = List.of(ctColor, ctColorTemperature, ctColorX, ctColorTemperatureX);

            ChannelGroupType groupX = ChannelGroupTypeBuilder.instance(GROUP_CHANNEL_GROUP_TYPE_UID, "Channel Group")
                    .withDescription("Channel Group")
                    .withChannelDefinitions(List.of(
                            new ChannelDefinitionBuilder("foo", TestHueChannelTypeProvider.COLOR_CHANNEL_TYPE_UID)
                                    .build(),
                            new ChannelDefinitionBuilder("bar", TestHueChannelTypeProvider.COLOR_CHANNEL_TYPE_UID)
                                    .build()))
                    .build();
            channelGroupTypes = List.of(groupX);
        } catch (Exception willNeverBeThrown) {
        }
    }

    @Override
    public Collection<ChannelType> getChannelTypes(Locale locale) {
        return channelTypes;
    }

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        for (ChannelType channelType : channelTypes) {
            if (channelType.getUID().equals(channelTypeUID)) {
                return channelType;
            }
        }
        return null;
    }

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        for (ChannelGroupType channelGroupType : channelGroupTypes) {
            if (channelGroupType.getUID().equals(channelGroupTypeUID)) {
                return channelGroupType;
            }
        }
        return null;
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return channelGroupTypes;
    }
}
