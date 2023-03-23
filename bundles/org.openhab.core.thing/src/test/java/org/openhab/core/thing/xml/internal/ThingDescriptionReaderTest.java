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
package org.openhab.core.thing.xml.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;

/**
 * Tests reading thing descriptions from XML using the {@link ThingDescriptionReader}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ThingDescriptionReaderTest {

    @Test
    public void readFromXML() throws Exception {
        URL url = Path.of("src/test/resources/thing/thing-types.xml").toUri().toURL();
        ThingDescriptionReader reader = new ThingDescriptionReader();
        List<?> types = Objects.requireNonNull(reader.readFromXML(url));

        List<ThingTypeXmlResult> thingTypeXmlResults = new ArrayList<>();
        List<ChannelGroupTypeXmlResult> channelGroupTypeXmlResults = new ArrayList<>();
        List<ChannelTypeXmlResult> channelTypeXmlResults = new ArrayList<>();

        for (Object type : types) {
            if (type instanceof ThingTypeXmlResult) {
                thingTypeXmlResults.add((ThingTypeXmlResult) type);
            } else if (type instanceof ChannelGroupTypeXmlResult) {
                channelGroupTypeXmlResults.add((ChannelGroupTypeXmlResult) type);
            } else if (type instanceof ChannelTypeXmlResult) {
                channelTypeXmlResults.add((ChannelTypeXmlResult) type);
            }
        }

        assertThat(thingTypeXmlResults.size(), is(1));

        ThingTypeXmlResult thingTypeXmlResult = thingTypeXmlResults.get(0);
        assertThat(thingTypeXmlResult.getUID().toString(), is("hue:lamp"));
        assertThat(thingTypeXmlResult.label, is("HUE Lamp"));
        assertThat(thingTypeXmlResult.description, is("My own great HUE Lamp."));

        assertThat(channelGroupTypeXmlResults.size(), is(1));

        ChannelGroupTypeXmlResult channelGroupTypeXmlResult = channelGroupTypeXmlResults.get(0);
        ChannelGroupType channelGroupType = channelGroupTypeXmlResult.toChannelGroupType();
        assertThat(channelGroupTypeXmlResult.getUID().toString(), is("hue:alarm_system"));
        assertThat(channelGroupType.getLabel(), is("Alarm System"));
        assertThat(channelGroupType.getDescription(), is("The alarm system."));

        assertThat(channelTypeXmlResults.size(), is(5));

        ChannelType channelType = channelTypeXmlResults.get(0).toChannelType();
        assertThat(channelType.getUID().toString(), is("hue:color"));
        assertThat(channelType.getKind(), is(ChannelKind.STATE));
        assertThat(channelType.isAdvanced(), is(false));
        assertThat(channelType.getItemType(), is("Color"));
        assertThat(channelType.getLabel(), is("Color"));
        assertThat(channelType.getDescription(), is("The color channel allows to control the color of a light. "
                + "It is also possible to dim values and switch the light on and off."));
        assertThat(channelType.getCategory(), is("ColorLight"));
        assertThat(channelType.getTags().size(), is(2));
        assertTrue(channelType.getTags().contains("Control"));
        assertTrue(channelType.getTags().contains("Light"));

        channelType = channelTypeXmlResults.get(1).toChannelType();
        assertThat(channelType.getUID().toString(), is("hue:brightness"));
        assertThat(channelType.getKind(), is(ChannelKind.STATE));
        assertThat(channelType.isAdvanced(), is(false));
        assertThat(channelType.getItemType(), is("Dimmer"));
        assertThat(channelType.getLabel(), is("Brightness"));
        assertThat(channelType.getDescription(),
                is("The brightness channel allows to control the brightness of a light. "
                        + "It is also possible to switch the light on and off."));
        assertThat(channelType.getCategory(), is("Light"));
        assertThat(channelType.getTags().size(), is(2));
        assertTrue(channelType.getTags().contains("Control"));
        assertTrue(channelType.getTags().contains("Light"));

        channelType = channelTypeXmlResults.get(2).toChannelType();
        assertThat(channelType.getUID().toString(), is("hue:color_temperature"));
        assertThat(channelType.getKind(), is(ChannelKind.STATE));
        assertThat(channelType.isAdvanced(), is(false));
        assertThat(channelType.getItemType(), is("Dimmer"));
        assertThat(channelType.getLabel(), is("Color Temperature"));
        assertThat(channelType.getDescription(), is(
                "The color temperature channel allows to set the color temperature of a light from 0 (cold) to 100 (warm)."));
        assertThat(channelType.getCategory(), is(nullValue()));
        assertThat(channelType.getTags().size(), is(2));
        assertTrue(channelType.getTags().contains("Control"));
        assertTrue(channelType.getTags().contains("ColorTemperature"));

        channelType = channelTypeXmlResults.get(3).toChannelType();
        assertThat(channelType.getUID().toString(), is("hue:alarm"));
        assertThat(channelType.getKind(), is(ChannelKind.STATE));
        assertThat(channelType.isAdvanced(), is(false));
        assertThat(channelType.getItemType(), is("Number"));
        assertThat(channelType.getLabel(), is("Alarm System"));
        assertThat(channelType.getDescription(), is("The light blinks if alarm is set."));
        assertThat(channelType.getCategory(), is(nullValue()));
        assertThat(channelType.getTags().size(), is(0));

        channelType = channelTypeXmlResults.get(4).toChannelType();
        assertThat(channelType.getUID().toString(), is("hue:motion"));
        assertThat(channelType.getKind(), is(ChannelKind.TRIGGER));
        assertThat(channelType.isAdvanced(), is(true));
        assertThat(channelType.getItemType(), is(nullValue()));
        assertThat(channelType.getLabel(), is("Motion Sensor"));
        assertThat(channelType.getDescription(), is("The sensor detecting motion."));
        assertThat(channelType.getCategory(), is("Motion"));
        assertThat(channelType.getTags().size(), is(2));
        assertTrue(channelType.getTags().contains("Status"));
        assertTrue(channelType.getTags().contains("Presence"));
    }
}
