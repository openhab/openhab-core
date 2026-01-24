/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        assertFalse(item.isValid(null, null));
        item.name = "my_name";
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

        item.channel = "binding:type:channelid";
        assertFalse(item.isValid(null, null));
        item.channel = "binding:$type:uid:group#channelid";
        assertFalse(item.isValid(null, null));
        item.channel = "binding:type:uid:group$channelid";
        assertFalse(item.isValid(null, null));
        item.channel = "binding:type:uid:channelid";
        assertTrue(item.isValid(null, null));
        item.channel = "binding:type:uid:group#channelid";
        assertTrue(item.isValid(null, null));

        item.channels = Map.of("binding:type:uid:channelid2", Map.of());
        assertTrue(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50));
        assertTrue(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50, "profile", "system.offset"));
        assertFalse(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50, "profile", "system:off.set"));
        assertFalse(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50, "profile", "xxx:system:offset"));
        assertFalse(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50, "profile", "system:offset"));
        assertTrue(item.isValid(null, null));
        item.channels = Map.of("binding:type:uid:channelid2", Map.of("param", 50, "profile", "offset"));
        assertTrue(item.isValid(null, null));
    }

    @Test
    public void testEquals() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name_2";
        assertFalse(item1.equals(item2));
        item2.name = "item_name";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.type = "Number";
        item2.type = "String";
        assertFalse(item1.equals(item2));
        item2.type = "Number";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item2.type = "number";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.dimension = "Temperature";
        assertFalse(item1.equals(item2));
        item2.dimension = "Humidity";
        assertFalse(item1.equals(item2));
        item2.dimension = "Temperature";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.label = "A label";
        item2.label = "A label";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.icon = "oh:classic:temperature";
        item2.icon = "oh:classic:temperature";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.groups = List.of("group1", "group2");
        item2.groups = List.of("group1", "group2");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.tags = Set.of("Tag1", "Tag 2");
        item2.tags = Set.of("Tag1", "Tag 2");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.group = new YamlGroupDTO();
        item1.group.type = "Switch";
        item1.group.function = "OR";
        item1.group.parameters = List.of("ON", "OFF");
        assertFalse(item1.equals(item2));
        item2.group = new YamlGroupDTO();
        item2.group.type = "Switch";
        item2.group.function = "OR";
        item2.group.parameters = List.of("ON", "OFF");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.channel = "binding:type:uid:channelid";
        item2.channel = "binding:type:uid:channelid";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.channels = Map.of("binding:type:uid:channelid2", Map.of());
        item2.channels = Map.of("binding:type:uid:channelid2", Map.of());
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        YamlMetadataDTO md = new YamlMetadataDTO();
        md.value = "value";
        md.config = Map.of("param", 50);
        item1.metadata = Map.of("namespace", md);
        YamlMetadataDTO md2 = new YamlMetadataDTO();
        md2.value = "value";
        md2.config = Map.of("param", 50);
        item2.metadata = Map.of("namespace", md2);
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithLabel() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "String";
        item2.type = "String";

        item1.label = null;
        item2.label = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
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
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithIcon() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.icon = null;
        item2.icon = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
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
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithFormat() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.format = null;
        item2.format = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
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
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithUnit() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.unit = null;
        item2.unit = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
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
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithAutoupdate() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.autoupdate = null;
        item2.autoupdate = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
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
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.autoupdate = true;
        item2.autoupdate = true;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithExpire() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Switch";
        item2.type = "Switch";

        item1.expire = null;
        item2.expire = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.expire = "5m";
        item2.expire = null;
        assertFalse(item1.equals(item2));
        item1.expire = null;
        item2.expire = "5m";
        assertFalse(item1.equals(item2));
        item1.expire = "5m";
        item2.expire = "1h";
        assertFalse(item1.equals(item2));
        item2.expire = "5m";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithGroups() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.groups = null;
        item2.groups = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.groups = List.of();
        item2.groups = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.groups = null;
        item2.groups = List.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.groups = List.of();
        item2.groups = List.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.groups = List.of("group1", "group2");
        item2.groups = null;
        assertFalse(item1.equals(item2));
        item2.groups = List.of();
        assertFalse(item1.equals(item2));
        item2.groups = List.of("group1");
        assertFalse(item1.equals(item2));
        item2.groups = List.of("group1", "group2", "group3");
        assertFalse(item1.equals(item2));
        item2.groups = List.of("group2", "group1");
        assertFalse(item1.equals(item2));
        item2.groups = List.of("group1", "group2");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithTags() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.tags = null;
        item2.tags = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.tags = Set.of();
        item2.tags = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.tags = null;
        item2.tags = Set.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.tags = Set.of();
        item2.tags = Set.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.tags = Set.of("tag1", "tag2");
        item2.tags = null;
        assertFalse(item1.equals(item2));
        item2.tags = Set.of();
        assertFalse(item1.equals(item2));
        item2.tags = Set.of("tag1");
        assertFalse(item1.equals(item2));
        item2.tags = Set.of("tag1", "tag2", "tag3");
        assertFalse(item1.equals(item2));
        item2.tags = Set.of("tag1", "tag2");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item2.tags = Set.of("tag2", "tag1");
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testEqualsWithChannels() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        item1.channel = "binding:type:uid:channelid";
        assertFalse(item1.equals(item2));
        item2.channel = "binding:type:uid2:channelid";
        assertFalse(item1.equals(item2));
        item2.channel = "binding:type:uid:channelid";
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());

        item1.channels = null;
        item2.channels = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.channels = Map.of();
        item2.channels = Map.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.channels = Map.of("binding:type:uid:channelid2", Map.of());
        item2.channels = null;
        assertFalse(item1.equals(item2));
        item2.channels = Map.of();
        assertFalse(item1.equals(item2));
        item2.channels = Map.of("binding:type:uid:channelid2", Map.of());
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.channels = Map.of("binding:type:uid:channelid2",
                Map.of("profile", "anyprofile", "param", "profile param"));
        item2.channels = Map.of("binding:type:uid:channelid2", Map.of());
        assertFalse(item1.equals(item2));
        item2.channels = Map.of("binding:type:uid:channelid2",
                Map.of("profile", "anyprofile", "param", "profile param"));
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.channels = Map.of("binding:type:uid:channelid2", Map.of());
        item2.channels = Map.of("binding:type:uid:channelid2", Map.of(), "binding:type:uid:channelid3", Map.of());
        assertFalse(item1.equals(item2));
        item1.channels = Map.of("binding:type:uid:channelid2", Map.of(), "binding:type:uid:channelid3", Map.of());
        item2.channels = Map.of("binding:type:uid:channelid2", Map.of());
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testEqualsWithMetadata() throws IOException {
        YamlItemDTO item1 = new YamlItemDTO();
        YamlItemDTO item2 = new YamlItemDTO();

        item1.name = "item_name";
        item2.name = "item_name";
        item1.type = "Number";
        item2.type = "Number";

        YamlMetadataDTO md1 = new YamlMetadataDTO();
        md1.value = "value";
        md1.config = Map.of("param", 50);
        YamlMetadataDTO md2 = new YamlMetadataDTO();
        md2.value = "value";
        md2.config = Map.of("param", "parameter value");
        YamlMetadataDTO md3 = new YamlMetadataDTO();
        md3.value = "value";
        md3.config = Map.of("param", 50);

        item1.metadata = null;
        item2.metadata = null;
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.metadata = Map.of();
        item2.metadata = Map.of();
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item1.metadata = Map.of("namespace", md1);
        assertFalse(item1.equals(item2));
        item2.metadata = Map.of("namespace2", md3);
        assertFalse(item1.equals(item2));
        item2.metadata = Map.of("namespace", md2);
        assertFalse(item1.equals(item2));
        item2.metadata = Map.of("namespace", md3);
        assertTrue(item1.equals(item2));
        assertEquals(item1.hashCode(), item2.hashCode());
        item2.metadata = Map.of("namespace", md3, "namespace2", md2);
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testExpireMetadataWarning() throws IOException {
        YamlItemDTO item = new YamlItemDTO();
        item.name = "item_name";
        item.type = "String";
        item.expire = "10m";

        YamlMetadataDTO md = new YamlMetadataDTO();
        md.value = "5m";
        item.metadata = Map.of("expire", md);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        assertTrue(item.isValid(errors, warnings));
        assertTrue(errors.isEmpty());
        assertEquals(1, warnings.size());
        assertEquals(
                "item \"item_name\": \"expire\" field is redundant with \"expire\" metadata; value \"5m\" will be considered",
                warnings.get(0));
    }
}
