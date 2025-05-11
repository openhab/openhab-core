/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.items;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlItemDTOTest} contains tests for the {@link YamlItemDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlItemDTOTest {

    @Test
    public void testGetType() throws IOException {
        YamlItemDTO item = new YamlItemDTO();
        assertEquals(null, item.getType());
        item.type = "Number";
        assertEquals("Number", item.getType());
        item.type = "number";
        assertEquals("Number", item.getType());
        item.dimension = "Dimensionless";
        assertEquals("Number:Dimensionless", item.getType());
        item.dimension = "dimensionless";
        assertEquals("Number:Dimensionless", item.getType());
    }

    @Test
    public void testIsValid() throws IOException {
        YamlItemDTO item = new YamlItemDTO();
        assertFalse(item.isValid(null, null));
        item.type = "Switch";
        item.name = "name";
        assertTrue(item.isValid(null, null));
        item.name = "$name";
        assertFalse(item.isValid(null, null));
        item.name = "my-name";
        assertTrue(item.isValid(null, null));

        item.type = "Group";
        assertTrue(item.isValid(null, null));
        item.type = "GRoup";
        assertTrue(item.isValid(null, null));
        item.group = new YamlGroupDTO();
        item.group.type = "Switch";
        item.group.function = "OR";
        item.group.parameters = List.of("ON", "OFF");
        assertTrue(item.isValid(null, null));

        item.type = "String";
        assertTrue(item.isValid(null, null));
        item.group = null;
        item.type = "string";
        assertTrue(item.isValid(null, null));
        item.type = "Other";
        assertFalse(item.isValid(null, null));
        item.type = "Number";
        assertTrue(item.isValid(null, null));
        item.dimension = "Dimensionless";
        assertTrue(item.isValid(null, null));
        item.type = "number";
        item.dimension = "dimensionless";
        assertTrue(item.isValid(null, null));
        item.dimension = "Other";
        assertFalse(item.isValid(null, null));
        item.type = "Color";
        item.dimension = null;
        assertTrue(item.isValid(null, null));

        item.label = "My label";
        assertTrue(item.isValid(null, null));

        item.icon = "xx:source:set:icon";
        assertFalse(item.isValid(null, null));
        item.icon = "source:set:icon";
        assertTrue(item.isValid(null, null));
        item.icon = "icon-source:$icon-set:my_icon";
        assertFalse(item.isValid(null, null));
        item.icon = "icon-source:icon-set:my_icon";
        assertTrue(item.isValid(null, null));

        item.groups = List.of("group1", "group 2");
        assertFalse(item.isValid(null, null));
        item.groups = List.of("group1", "group2");
        assertTrue(item.isValid(null, null));

        item.tags = Set.of("Tag1", "Tag 2");
        assertTrue(item.isValid(null, null));

        item.channel = "binding:type:uid:channelid";
        assertTrue(item.isValid(null, null));
        item.channel = "binding:type:uid:group#channelid";
        assertTrue(item.isValid(null, null));
        item.channel = "binding:type:channelid";
        assertFalse(item.isValid(null, null));
        item.channel = "binding:$type:uid:group#channelid";
        assertFalse(item.isValid(null, null));
        item.channel = "binding:type:uid:group$channelid";
        assertFalse(item.isValid(null, null));
    }

    @Test
    public void testEquals() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name-2";
        assertFalse(item1.equals(item2));
        item2.name = "item-name";
        assertTrue(item1.equals(item2));

        item1.type = "Number";
        item1.dimension = "Temperature";
        item2.type = "Number";
        item2.dimension = "Temperature";
        assertTrue(item1.equals(item2));

        item1.label = "A label";
        item2.label = "A label";
        assertTrue(item1.equals(item2));

        item1.icon = "oh:classic:temperature";
        item2.icon = "oh:classic:temperature";
        assertTrue(item1.equals(item2));

        item1.groups = List.of("group1", "group2");
        item2.groups = List.of("group1", "group2");
        assertTrue(item1.equals(item2));

        item1.tags = Set.of("Tag1", "Tag 2");
        item2.tags = Set.of("Tag1", "Tag 2");
        assertTrue(item1.equals(item2));
        item2.tags = Set.of("Tag 2", "Tag1");
        assertTrue(item1.equals(item2));

        item1.group = new YamlGroupDTO();
        item1.group.type = "Switch";
        item1.group.function = "OR";
        item1.group.parameters = List.of("ON", "OFF");
        item2.group = new YamlGroupDTO();
        item2.group.type = "Switch";
        item2.group.function = "OR";
        item2.group.parameters = List.of("ON", "OFF");
        assertTrue(item1.equals(item2));
    }

    @Test
    public void testEqualsWithLabel() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name";
        item1.type = "String";
        item2.type = "String";

        item1.label = null;
        item2.label = null;
        assertTrue(item1.equals(item2));
        item1.label = "A label";
        item2.label = null;
        assertFalse(item1.equals(item2));
        item1.label = null;
        item2.label = "A label";
        assertFalse(item1.equals(item2));
        item1.label = "A label";
        item2.label = "A different label";
        assertFalse(item1.equals(item2));
        item1.label = "A label";
        item2.label = "A label";
        assertTrue(item1.equals(item2));
    }

    @Test
    public void testEqualsWithIcon() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name";
        item1.type = "Number";
        item2.type = "Number";

        item1.icon = null;
        item2.icon = null;
        assertTrue(item1.equals(item2));
        item1.icon = "humidity";
        item2.icon = null;
        assertFalse(item1.equals(item2));
        item1.icon = null;
        item2.icon = "humidity";
        assertFalse(item1.equals(item2));
        item1.icon = "humidity";
        item2.icon = "temperature";
        assertFalse(item1.equals(item2));
        item1.icon = "humidity";
        item2.icon = "humidity";
        assertTrue(item1.equals(item2));
    }

    @Test
    public void testEqualsWithFormat() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name";
        item1.type = "Number";
        item2.type = "Number";

        item1.format = null;
        item2.format = null;
        assertTrue(item1.equals(item2));
        item1.format = "%.1f °C";
        item2.format = null;
        assertFalse(item1.equals(item2));
        item1.format = null;
        item2.format = "%.1f °C";
        assertFalse(item1.equals(item2));
        item1.format = "%.1f °C";
        item2.format = "%.0f °C";
        assertFalse(item1.equals(item2));
        item1.format = "%.1f °C";
        item2.format = "%.1f °C";
        assertTrue(item1.equals(item2));
    }

    @Test
    public void testEqualsWithUnit() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name";
        item1.type = "Number";
        item2.type = "Number";

        item1.unit = null;
        item2.unit = null;
        assertTrue(item1.equals(item2));
        item1.unit = "°C";
        item2.unit = null;
        assertFalse(item1.equals(item2));
        item1.unit = null;
        item2.unit = "°C";
        assertFalse(item1.equals(item2));
        item1.unit = "°C";
        item2.unit = "°F";
        assertFalse(item1.equals(item2));
        item1.unit = "°C";
        item2.unit = "°C";
        assertTrue(item1.equals(item2));
    }

    @Test
    public void testEqualsWithAutoupdate() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item-name";
        item2.name = "item-name";
        item1.type = "Number";
        item2.type = "Number";

        item1.autoupdate = null;
        item2.autoupdate = null;
        assertTrue(item1.equals(item2));
        item1.autoupdate = false;
        item2.autoupdate = true;
        assertFalse(item1.equals(item2));
        item1.autoupdate = true;
        item2.autoupdate = false;
        assertFalse(item1.equals(item2));
        item1.autoupdate = true;
        item2.autoupdate = null;
        assertFalse(item1.equals(item2));
        item1.autoupdate = null;
        item2.autoupdate = true;
        assertFalse(item1.equals(item2));
        item1.autoupdate = false;
        item2.autoupdate = null;
        assertFalse(item1.equals(item2));
        item1.autoupdate = null;
        item2.autoupdate = false;
        assertFalse(item1.equals(item2));
        item1.autoupdate = false;
        item2.autoupdate = false;
        assertTrue(item1.equals(item2));
        item1.autoupdate = true;
        item2.autoupdate = true;
        assertTrue(item1.equals(item2));
    }

    // @Test
    // public void testEqualsWithConfigurations() throws IOException {
    // YamlThingDTO th1 = new YamlThingDTO();
    // YamlThingDTO th2 = new YamlThingDTO();
    //
    // th1.uid = "binding:type:id";
    // th2.uid = "binding:type:id";
    //
    // th1.config = null;
    // th2.config = null;
    // assertTrue(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = null;
    // assertFalse(th1.equals(th2));
    // th1.config = null;
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "other value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 25, "param3", true, "param4", List.of("val 1", "val 2"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", false, "param4", List.of("val 1", "val 2"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "value 2"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1"));
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", 75);
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true);
    // assertFalse(th1.equals(th2));
    // th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
    // assertTrue(th1.equals(th2));
    // }
    //
    // @Test
    // public void testEqualsWithChannels() throws IOException {
    // YamlThingDTO th1 = new YamlThingDTO();
    // th1.uid = "binding:type:id";
    // YamlThingDTO th2 = new YamlThingDTO();
    // th2.uid = "binding:type:id";
    //
    // YamlChannelDTO ch1 = new YamlChannelDTO();
    // ch1.type = "channel-type";
    // YamlChannelDTO ch2 = new YamlChannelDTO();
    // ch2.type = "channel-other-type";
    // YamlChannelDTO ch3 = new YamlChannelDTO();
    // ch3.type = "channel-type";
    // YamlChannelDTO ch4 = new YamlChannelDTO();
    // ch4.type = "channel-other-type";
    //
    // th1.channels = Map.of("channel1", ch1);
    // th2.channels = Map.of("channel1", ch3);
    // assertTrue(th1.equals(th2));
    //
    // th1.channels = Map.of("channel1", ch1, "channel2", ch2);
    // th2.channels = Map.of("channel1", ch3, "channel2", ch4);
    // assertTrue(th1.equals(th2));
    //
    // th1.channels = Map.of("channel1", ch1);
    // th2.channels = null;
    // assertFalse(th1.equals(th2));
    //
    // th1.channels = null;
    // th2.channels = Map.of("channel1", ch3);
    // assertFalse(th1.equals(th2));
    //
    // th1.channels = Map.of("channel1", ch1, "channel2", ch2);
    // th2.channels = Map.of("channel1", ch3);
    // assertFalse(th1.equals(th2));
    //
    // th1.channels = Map.of("channel1", ch1);
    // th2.channels = Map.of("channel1", ch4);
    // assertFalse(th1.equals(th2));
    //
    // th1.channels = Map.of("channel", ch1);
    // th2.channels = Map.of("channel1", ch3);
    // assertFalse(th1.equals(th2));
    // }
}
