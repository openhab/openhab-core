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
        SemanticTags.addTagSet("Location_Floor", MockFloor.class);
        SemanticTags.addTagSet("Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.addTagSet("Equipment_Entertainment_TV", MockTV.class);
        SemanticTags.addTagSet("Point_Control_Light", MockLight.class);
        SemanticTags.addTagSet("Point_Control_Power", MockPower.class);
        SemanticTags.addTagSet("Property_Power", MockPowerProperty.class);
    }

    @AfterAll
    public static void tearDownTags() {
        SemanticTags.removeTagSet("Location_Floor", MockFloor.class);
        SemanticTags.removeTagSet("Location_Room_LivingRoom", MockLivingRoom.class);
        SemanticTags.removeTagSet("Equipment_Entertainment_TV", MockTV.class);
        SemanticTags.removeTagSet("Point_Control_Light", MockLight.class);
        SemanticTags.removeTagSet("Point_Control_Power", MockPower.class);
        SemanticTags.removeTagSet("Property_Power", MockPowerProperty.class);
    }

    private Item mockItem(String name, @Nullable String label, String type, Set<String> tags, List<String> groupNames) {
        Item item = mock(Item.class);
        when(item.getName()).thenReturn(name);
        when(item.getLabel()).thenReturn(label);
        when(item.getType()).thenReturn(type);
        when(item.getTags()).thenReturn(tags);
        when(item.getGroupNames()).thenReturn(groupNames);
        return item;
    }

    @Test
    public void testSerializeNullOrEmpty() {
        assertEquals("", LLMItemSerializer.serialize(Collections.emptyList()));
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

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2)));
    }

    @Test
    public void testSerializeHierarchicalModel() {
        // GF Floor
        Item gf = mockItem("GF", "Ground Floor", "Group", Set.of("Location_Floor"), List.of());

        // LivingRoom Room inside GF
        Item livingRoom = mockItem("LivingRoom", "Living Room", "Group", Set.of("Location_Room_LivingRoom"),
                List.of("GF"));

        // TV Equipment inside LivingRoom
        Item tv = mockItem("TV", "Living Room TV", "Group", Set.of("Equipment_Entertainment_TV"),
                List.of("LivingRoom"));

        // TV Power Control inside TV
        Item tvPower = mockItem("TV_Power", "TV Power", "Switch", Set.of("Point_Control_Power", "Property_Power"),
                List.of("TV"));

        // Living Room Light control inside LivingRoom directly
        Item lrLight = mockItem("LivingRoom_Light", "Living Room Light", "Dimmer", Set.of("Point_Control_Light"),
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

        assertEquals(expected, LLMItemSerializer.serialize(items));
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

        assertEquals(expected, LLMItemSerializer.serialize(List.of(item1, item2)));
    }
}
