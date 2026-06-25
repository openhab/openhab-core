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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
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

    private interface MockPowerProperty extends Property {
    }

    @BeforeAll
    public static void setUpTags() {
        SemanticTags.addTagSet("Mock_Location_Floor", MockFloor.class);
        SemanticTags.addTagSet("Mock_Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.addTagSet("Mock_Equipment_Entertainment_TV", MockTV.class);
        SemanticTags.addTagSet("Mock_Point_Control_Light", MockLight.class);
        SemanticTags.addTagSet("Mock_Point_Control_Power", MockPower.class);
        SemanticTags.addTagSet("Mock_Property_Power", MockPowerProperty.class);
    }

    @AfterAll
    public static void tearDownTags() {
        SemanticTags.removeTagSet("Mock_Location_Floor", MockFloor.class);
        SemanticTags.removeTagSet("Mock_Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.removeTagSet("Mock_Equipment_Entertainment_TV", MockTV.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Light", MockLight.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Power", MockPower.class);
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

    @Test
    public void testSerializeNullOrEmpty() {
        assertEquals("", LLMItemSerializer.serialize(Collections.emptyList(), null));
    }

    @Test
    public void testSerializeNonSemanticItemsOnly() {
        Item item1 = mockItem("ItemB", "Label B", "Switch", Set.of(), List.of());
        Item item2 = mockItem("ItemA", null, "Dimmer", Set.of(), List.of());

        String expected = """
                items:
                - name: ItemA
                  type: Dimmer
                - name: ItemB
                  label: Label B
                  type: Switch
                """;

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2), null));
    }

    @Test
    public void testSerializeHierarchicalModel() {
        // GF Floor
        Item gf = mockItem("GF", "Ground Floor", "Group", Set.of("Mock_Location_Floor"), List.of());

        // LivingRoom Room inside GF
        Item livingRoom = mockItem("LivingRoom", "Living Room", "Group", Set.of("Mock_Location_Room_LivingRoom"),
                List.of("GF"));

        // TV Equipment inside LivingRoom
        Item tv = mockItem("TV", "Living Room TV", "Group", Set.of("Mock_Equipment_Entertainment_TV"),
                List.of("LivingRoom"));

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
                locationItems:
                - name: GF
                  label: Ground Floor
                  type: Group
                  semanticType: MockFloor
                  locationItems:
                  - name: LivingRoom
                    label: Living Room
                    type: Group
                    semanticType: MockLivingRoom
                    equipmentItems:
                    - name: TV
                      label: Living Room TV
                      type: Group
                      semanticType: MockTV
                      pointItems:
                      - name: TV_Power
                        label: TV Power
                        type: Switch
                        semanticType: MockPower
                        properties:
                        - MockPowerProperty
                    pointItems:
                    - name: LivingRoom_Light
                      label: Living Room Light
                      type: Dimmer
                      semanticType: MockLight
                items:
                - name: System_Mode
                  label: System Mode
                  type: String
                """;

        assertEquals(expected, LLMItemSerializer.serialize(items, null));
    }

    @Test
    public void testSerializeEscapingAndReservedWords() {
        Item item1 = mockItem("item:with:colon", "label # with hash", "Switch", Set.of(), List.of());
        Item item2 = mockItem("yes", "true", "String", Set.of(), List.of());

        String expected = """
                items:
                - name: item:with:colon
                  label: "label # with hash"
                  type: Switch
                - name: "yes"
                  label: "true"
                  type: String
                """;

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2), null));
    }

    @Test
    public void testSerializeWithCommandOptions() {
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
        Item eqItem = mockItem("TV", "Living Room TV", "Group", Set.of("Mock_Equipment_Entertainment_TV"),
                List.of("LocationA"), List.of());

        // Point Node should have command options
        Item ptItem = mockItem("Light", "Living Room Light", "Dimmer", Set.of("Mock_Point_Control_Light"),
                List.of("TV"), List.of(new CommandOption("ON", "On"), new CommandOption("OFF", "Off")));

        String expected = """
                locationItems:
                - name: LocationA
                  label: Room
                  type: Group
                  semanticType: MockLivingRoom
                  equipmentItems:
                  - name: TV
                    label: Living Room TV
                    type: Group
                    semanticType: MockTV
                    pointItems:
                    - name: Light
                      label: Living Room Light
                      type: Dimmer
                      semanticType: MockLight
                      commandOptions:
                      - command: "ON"
                        label: "On"
                      - command: "OFF"
                        label: "Off"
                items:
                - name: Audio_Source
                  label: Audio Source
                  type: String
                  commandOptions:
                  - command: TUNER
                    label: Tuner
                  - command: DAB
                    label: DAB
                  - command: AIRPLAY
                    label: AirPlay
                - name: ItemA
                  type: Dimmer
                - name: ItemB
                  label: Label B
                  type: Switch
                  commandOptions:
                  - command: "ON"
                    label: "On"
                  - command: "OFF"
                """;

        assertEquals(expected,
                LLMItemSerializer.serialize(List.of(item1, item2, item3, locationItem, eqItem, ptItem), null));
    }
}
