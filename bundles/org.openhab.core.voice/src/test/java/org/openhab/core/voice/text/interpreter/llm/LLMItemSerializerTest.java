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
package org.openhab.core.voice.text.interpreter.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openhab.core.semantics.internal.SemanticsMetadataProvider.NAMESPACE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.Equipment;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;

/**
 * Test class for {@link LLMItemSerializer}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMItemSerializerTest {
    // Mock semantic sub-interfaces for registering with SemanticTags
    private interface MockFloor extends Location {
    }

    private interface MockLivingRoom extends Location {
    }

    private interface MockTV extends Equipment {
    }

    private interface MockLight extends Point {
    }

    private interface MockPower extends Point {
    }

    private interface MockSource extends Point {
    }

    private interface MockPowerProperty extends Property {
    }

    @BeforeAll
    public static void setUpTags() {
        SemanticTags.addTagSet("Mock_Location_Floor", MockFloor.class);
        SemanticTags.addTagSet("Mock_Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.addTagSet("Mock_Equipment_Television", MockTV.class);
        SemanticTags.addTagSet("Mock_Point_Control_Light", MockLight.class);
        SemanticTags.addTagSet("Mock_Point_Control_Power", MockPower.class);
        SemanticTags.addTagSet("Mock_Point_Control_Source", MockSource.class);
        SemanticTags.addTagSet("Mock_Property_Power", MockPowerProperty.class);
    }

    @AfterAll
    public static void tearDownTags() {
        SemanticTags.removeTagSet("Mock_Location_Floor", MockFloor.class);
        SemanticTags.removeTagSet("Mock_Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.removeTagSet("Mock_Equipment_Television", MockTV.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Light", MockLight.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Power", MockPower.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Source", MockSource.class);
        SemanticTags.removeTagSet("Mock_Property_Power", MockPowerProperty.class);
    }

    private Item mockItem(String name, @Nullable String label, String type, Set<String> tags, List<String> groupNames) {
        return mockItem(name, label, type, tags, groupNames, null);
    }

    private Item mockItem(String name, @Nullable String label, String type, Set<String> tags, List<String> groupNames,
            @Nullable List<CommandOption> commandOptions) {
        Item item = mock(Item.class);
        when(item.getName()).thenReturn(name);
        when(item.getLabel()).thenReturn(label);
        when(item.getType()).thenReturn(type);
        when(item.getTags()).thenReturn(tags);
        when(item.getGroupNames()).thenReturn(groupNames);
        if (commandOptions != null) {
            CommandDescription desc = mock(CommandDescription.class);
            when(desc.getCommandOptions()).thenReturn(commandOptions);
            when(item.getCommandDescription()).thenReturn(desc);
            when(item.getCommandDescription(any())).thenReturn(desc);
        }
        return item;
    }

    private MetadataRegistry mockMetadataRegistry(Map<String, Map<String, Object>> semanticsConfigMap) {
        MetadataRegistry registry = mock(MetadataRegistry.class);
        when(registry.get(any(MetadataKey.class))).thenAnswer(invocation -> {
            MetadataKey key = invocation.getArgument(0);
            if (NAMESPACE.equals(key.getNamespace())) {
                Map<String, Object> config = semanticsConfigMap.get(key.getItemName());
                if (config != null) {
                    return new Metadata(key, "", config);
                }
            }
            return null;
        });
        return registry;
    }

    @Test
    public void testSerializeNullOrEmpty() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of());
        assertEquals("", LLMItemSerializer.serialize(Collections.emptyList(), metadataRegistry, null));
    }

    @Test
    public void testSerializeNonSemanticItemsOnly() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of());
        Item item1 = mockItem("ItemB", "Label B", "Switch", Set.of(), List.of());
        Item item2 = mockItem("ItemA", null, "Dimmer", Set.of(), List.of());

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Non-semantic Items
                ItemA Dimmer
                ItemB Switch "Label B"
                """;

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2), metadataRegistry, null));
    }

    @Test
    public void testSerializeHierarchicalModel() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of("LivingRoom", Map.of("isPartOf", "GF"), //
                "TV", Map.of("hasLocation", "LivingRoom"), //
                "TV_Power", Map.of("isPointOf", "TV"), //
                "LivingRoom_Light", Map.of("hasLocation", "LivingRoom")));

        // GF Floor
        Item gf = mockItem("GF", "Ground Floor", "Group", Set.of("Mock_Location_Floor"), List.of());

        // LivingRoom Room inside GF
        Item livingRoom = mockItem("LivingRoom", "Living Room", "Group", Set.of("Mock_Location_Room_LivingRoom"),
                List.of("GF"));

        // TV Equipment inside LivingRoom
        Item tv = mockItem("TV", "Living Room TV", "Group", Set.of("Mock_Equipment_Television"), List.of("LivingRoom"));

        // TV Power Control inside TV
        Item tvPower = mockItem("TV_Power", "TV Power", "Switch",
                Set.of("Mock_Point_Control_Power", "Mock_Property_Power"), List.of("TV"));

        // Living Room Light control inside LivingRoom directly
        Item lrLight = mockItem("LivingRoom_Light", "Living Room Light", "Dimmer", Set.of("Mock_Point_Control_Light"),
                List.of("LivingRoom"));

        // Non-semantic Item
        Item systemMode = mockItem("System_Mode", "System Mode", "String", Set.of(), List.of());

        // Intentionally provide an unsorted order to ensure the serializer sorts deterministically
        List<Item> items = List.of(tvPower, livingRoom, systemMode, lrLight, tv, gf);

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Semantic Items
                GF "Ground Floor" :MockFloor
                ..LivingRoom :MockLivingRoom
                ....TV "Living Room TV" :MockTV
                ......TV_Power Switch :MockPower [MockPowerProperty]
                ....LivingRoom_Light Dimmer :MockLight

                # Non-semantic Items
                System_Mode String
                """;

        assertEquals(expected, LLMItemSerializer.serialize(items, metadataRegistry, null));
    }

    @Test
    public void testSerializeSemanticRelationsMetadata() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of("LivingRoom", Map.of("isPartOf", "GF"), //
                "TV", Map.of("hasLocation", "LivingRoom"), //
                "TV_Power", Map.of("isPointOf", "TV"), //
                "LivingRoom_Light", Map.of("hasLocation", "LivingRoom")));

        Item gf = mockItem("GF", "Ground Floor", "Group", Set.of("Mock_Location_Floor"), List.of());
        Item livingRoom = mockItem("LivingRoom", "Living Room", "Group", Set.of("Mock_Location_Room_LivingRoom"),
                List.of());
        Item tv = mockItem("TV", "Living Room TV", "Group", Set.of("Mock_Equipment_Television"), List.of());
        Item tvPower = mockItem("TV_Power", "TV Power", "Switch",
                Set.of("Mock_Point_Control_Power", "Mock_Property_Power"), List.of());
        Item lrLight = mockItem("LivingRoom_Light", "Living Room Light", "Dimmer", Set.of("Mock_Point_Control_Light"),
                List.of());

        List<Item> items = List.of(tvPower, livingRoom, lrLight, tv, gf);

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Semantic Items
                GF "Ground Floor" :MockFloor
                ..LivingRoom :MockLivingRoom
                ....TV "Living Room TV" :MockTV
                ......TV_Power Switch :MockPower [MockPowerProperty]
                ....LivingRoom_Light Dimmer :MockLight
                """;

        assertEquals(expected, LLMItemSerializer.serialize(items, metadataRegistry, null));
    }

    @Test
    public void testSerializeWithCommandOptions() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of("TV", Map.of("hasLocation", "LocationA"), //
                "TV_Channel", Map.of("isPointOf", "TV")));

        Item item1 = mockItem("ItemB", "Label B", "Switch", Set.of(), List.of(),
                List.of(new CommandOption("ON", "On"), new CommandOption("OFF", "")));
        Item item2 = mockItem("ItemA", null, "Dimmer", Set.of(), List.of(), null);
        Item item3 = mockItem("Audio_Source", "Audio Source", "String", Set.of(), List.of(),
                List.of(new CommandOption("TUNER", "Tuner"), new CommandOption("DAB", "DAB"),
                        new CommandOption("AIRPLAY", "AirPlay")));

        // Location Node should NOT have command options
        Item locationItem = mockItem("LocationA", "Room", "Group", Set.of("Mock_Location_Room_LivingRoom"), List.of(),
                List.of(new CommandOption("ON", "On"), new CommandOption("OFF", "Off")));

        // Equipment Node should NOT have command options
        Item eqItem = mockItem("TV", "Living Room TV", "Group", Set.of("Mock_Equipment_Television"),
                List.of("LocationA"), List.of());

        // Point Node should have command options
        Item ptItem = mockItem("TV_Channel", "TV Channel", "String", Set.of("Mock_Point_Control_Source"), List.of("TV"),
                List.of(new CommandOption("CH1", "Channel 1"), new CommandOption("CH2", "Channel 2")));

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Semantic Items
                LocationA "Room" :MockLivingRoom
                ..TV "Living Room TV" :MockTV
                ....TV_Channel String :MockSource (CH1=Channel 1,CH2=Channel 2)

                # Non-semantic Items
                Audio_Source String (TUNER=Tuner,DAB=DAB,AIRPLAY=AirPlay)
                ItemA Dimmer
                ItemB Switch "Label B" (ON=On,OFF)
                """;

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2, item3, locationItem, eqItem, ptItem),
                metadataRegistry, null));
    }

    @Test
    public void testSerializeComplexSemanticModelWithNestedEquipmentAndRelations() {
        /*
         * Complex model hierarchy:
         * House (Location)
         * └── GroundFloor (Location, isPartOf House)
         * __├── LivingRoom (Location, isPartOf GroundFloor)
         * __│ ├── EqA (Equipment, hasLocation LivingRoom)
         * __│ │ └── EqB (Equipment, isPartOf EqA)
         * __│ │ __└── EqC (Equipment, isPartOf EqB)
         * __│ │ ____└── PointC (Point, isPointOf EqC)
         * __│ └── RoomLight (Point, hasLocation LivingRoom)
         * __└── OutdoorEq (Equipment, hasLocation GroundFloor)
         */

        MetadataRegistry metadataRegistry = mockMetadataRegistry(Map.of("GroundFloor", Map.of("isPartOf", "House"), //
                "LivingRoom", Map.of("isPartOf", "GroundFloor"), //
                "EqA", Map.of("hasLocation", "LivingRoom"), //
                "EqB", Map.of("isPartOf", "EqA"), //
                "EqC", Map.of("isPartOf", "EqB"), //
                "PointC", Map.of("isPointOf", "EqC"), //
                "RoomLight", Map.of("hasLocation", "LivingRoom"), //
                "OutdoorEq", Map.of("hasLocation", "GroundFloor")));

        Item house = mockItem("House", "Main House", "Group", Set.of("Mock_Location_Floor"), List.of());
        Item gf = mockItem("GroundFloor", null, "Group", Set.of("Mock_Location_Floor"), List.of());
        Item livingRoom = mockItem("LivingRoom", null, "Group", Set.of("Mock_Location_Room_LivingRoom"), List.of());

        Item eqA = mockItem("EqA", "Equipment A", "Group", Set.of("Mock_Equipment_Television"), List.of());
        Item eqB = mockItem("EqB", "Equipment B", "Group", Set.of("Mock_Equipment_Television"), List.of());
        Item eqC = mockItem("EqC", "Equipment C", "Group", Set.of("Mock_Equipment_Television"), List.of());
        Item pointC = mockItem("PointC", "Point C", "Switch", Set.of("Mock_Point_Control_Power"), List.of());

        Item roomLight = mockItem("RoomLight", "Room Light", "Dimmer", Set.of("Mock_Point_Control_Light"), List.of());

        Item outdoorEq = mockItem("OutdoorEq", "Outdoor Equipment", "Group", Set.of("Mock_Equipment_Television"),
                List.of("GroundFloor"));

        List<Item> items = List.of(pointC, eqC, eqB, eqA, livingRoom, roomLight, outdoorEq, gf, house);

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Semantic Items
                House "Main House" :MockFloor
                ..GroundFloor :MockFloor
                ....LivingRoom :MockLivingRoom
                ......EqA "Equipment A" :MockTV
                ........EqB "Equipment B" :MockTV
                ..........EqC "Equipment C" :MockTV
                ............PointC Switch :MockPower
                ......RoomLight Dimmer :MockLight
                ....OutdoorEq "Outdoor Equipment" :MockTV
                """;

        assertEquals(expected, LLMItemSerializer.serialize(items, metadataRegistry, null));
    }

    @Test
    public void testSerializeItemInSemanticAndNonSemanticGroups() {
        MetadataRegistry metadataRegistry = mockMetadataRegistry(
                Map.of("LivingRoom_Light", Map.of("hasLocation", "LivingRoom")));

        Item livingRoom = mockItem("LivingRoom", null, "Group", Set.of("Mock_Location_Room_LivingRoom"), List.of());
        Item gLights = mockItem("gLights", "All Lights", "Group", Set.of(), List.of());
        Item lrLight = mockItem("LivingRoom_Light", "Living Room Light", "Dimmer", Set.of("Mock_Point_Control_Light"),
                List.of("LivingRoom", "gLights"));

        List<Item> items = List.of(livingRoom, gLights, lrLight);

        String expected = """
                # Format: [..]name [type] ["label"] [:semanticClass] [[properties]] [(commandOptions: COMMAND=Label)]

                # Semantic Items
                LivingRoom :MockLivingRoom
                ..LivingRoom_Light Dimmer :MockLight

                # Non-semantic Items
                gLights "All Lights"
                ..LivingRoom_Light Dimmer
                """;

        assertEquals(expected, LLMItemSerializer.serialize(items, metadataRegistry, null));
    }
}
