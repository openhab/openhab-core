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
package org.openhab.core.semantics.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Tests added for methods moved from SemanticTags to SemanticsService
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class SemanticsServiceImplTest {

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @Mock @NonNullByDefault({}) UnitProvider unitProviderMock;
    private @Mock @NonNullByDefault({}) ManagedSemanticTagProvider managedSemanticTagProviderMock;

    private @NonNullByDefault({}) GroupItem locationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem pointItem;

    private @NonNullByDefault({}) Class<? extends Tag> roomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> bathroomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> livingRoomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> userLocationTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> cleaningRobotTagClass;

    private @NonNullByDefault({}) SemanticsServiceImpl service;

    @BeforeEach
    public void setup() throws Exception {
        CoreItemFactory itemFactory = new CoreItemFactory(unitProviderMock);
        locationItem = new GroupItem("TestBathRoom");
        locationItem.addTag("Bathroom");
        locationItem.setLabel("Joe's Room");

        equipmentItem = new GroupItem("Test08");
        equipmentItem.addTag("CleaningRobot");

        pointItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestTemperature");
        pointItem.addTag("Sensor");
        pointItem.addTag("Temperature");
        pointItem.setLabel("my Test label");
        pointItem.addGroupName(locationItem.getName());
        locationItem.addMember(pointItem);

        SemanticTag userLocationTag = new SemanticTagImpl("Location_UserLocation", "Custom label", "Custom description",
                " Synonym1, Synonym2 , Synonym With Space ");
        when(managedSemanticTagProviderMock.getAll()).thenReturn(List.of(userLocationTag));
        SemanticTagRegistry semanticTagRegistry = new SemanticTagRegistryImpl(new DefaultSemanticTagProvider(),
                managedSemanticTagProviderMock);

        roomTagClass = semanticTagRegistry.getTagClassById("Location_Indoor_Room");
        bathroomTagClass = semanticTagRegistry.getTagClassById("Location_Indoor_Room_Bathroom");
        livingRoomTagClass = semanticTagRegistry.getTagClassById("Location_Indoor_Room_LivingRoom");
        userLocationTagClass = semanticTagRegistry.getTagClassById("Location_UserLocation");
        cleaningRobotTagClass = semanticTagRegistry.getTagClassById("Equipment_CleaningRobot");

        service = new SemanticsServiceImpl(itemRegistryMock, metadataRegistryMock, semanticTagRegistry);
    }

    @Test
    public void testGetItemsInLocation() throws Exception {
        when(itemRegistryMock.stream()).thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem));

        Set<Item> items = service.getItemsInLocation((Class<? extends Location>) bathroomTagClass);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation((Class<? extends Location>) roomTagClass);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation((Class<? extends Location>) livingRoomTagClass);
        assertTrue(items.isEmpty());
    }

    @Test
    public void testGetItemsInLocationByString() throws Exception {
        when(itemRegistryMock.stream()).thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem));
        when(metadataRegistryMock.get(any())).thenReturn(null);

        // Label of a location group item
        Set<Item> items = service.getItemsInLocation("joe's room", Locale.ENGLISH);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        // Location tag label
        items = service.getItemsInLocation("bathroom", Locale.ENGLISH);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        // Location tag synonym
        items = service.getItemsInLocation("powder room", Locale.ENGLISH);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        // Location parent tag label
        items = service.getItemsInLocation("Room", Locale.ENGLISH);
        assertEquals(1, items.size());
        assertTrue(items.contains(pointItem));

        // Existing item label
        items = service.getItemsInLocation("my Test label", Locale.ENGLISH);
        assertTrue(items.isEmpty());

        // Unknown item label
        items = service.getItemsInLocation("wrong label", Locale.ENGLISH);
        assertTrue(items.isEmpty());
    }

    @Test
    public void testGetLabelAndSynonyms() {
        List<String> result = service.getLabelAndSynonyms(bathroomTagClass, Locale.ENGLISH);
        assertEquals(6, result.size());
        assertEquals("bathroom", result.getFirst());
        assertEquals("bathrooms", result.get(1));
        assertEquals("bath", result.get(2));
        assertEquals("baths", result.get(3));
        assertEquals("powder room", result.get(4));
        assertEquals("powder rooms", result.get(5));

        result = service.getLabelAndSynonyms(cleaningRobotTagClass, Locale.FRENCH);
        assertEquals(4, result.size());
        assertEquals("robot de nettoyage", result.getFirst());
        assertEquals("robos de nettoyage", result.get(1));
        assertEquals("robot aspirateur", result.get(2));
        assertEquals("robots aspirateur", result.get(3));

        result = service.getLabelAndSynonyms(userLocationTagClass, Locale.ENGLISH);
        assertEquals(4, result.size());
        assertEquals("custom label", result.getFirst());
        assertEquals("synonym1", result.get(1));
        assertEquals("synonym2", result.get(2));
        assertEquals("synonym with space", result.get(3));
    }

    @Test
    public void testGetByLabel() {
        Class<? extends Tag> tag = service.getByLabel("BATHROOM", Locale.ENGLISH);
        assertEquals(bathroomTagClass, tag);
        tag = service.getByLabel("Bath", Locale.ENGLISH);
        assertNull(tag);

        tag = service.getByLabel("ROBOT de nettoyage", Locale.FRENCH);
        assertEquals(cleaningRobotTagClass, tag);
        tag = service.getByLabel("Robot aspirateur", Locale.FRENCH);
        assertNull(tag);

        tag = service.getByLabel("CUSTOM label", Locale.ENGLISH);
        assertEquals(userLocationTagClass, tag);
        tag = service.getByLabel("Synonym1", Locale.ENGLISH);
        assertNull(tag);
    }

    @Test
    public void testGetByLabelOrSynonym() {
        List<Class<? extends Tag>> tags = service.getByLabelOrSynonym("BATHROOM", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(bathroomTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("POWDER Rooms", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(bathroomTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("other bath", Locale.ENGLISH);
        assertTrue(tags.isEmpty());

        tags = service.getByLabelOrSynonym("ROBOT de nettoyage", Locale.FRENCH);
        assertEquals(1, tags.size());
        assertEquals(cleaningRobotTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("ROBOTS aspirateur", Locale.FRENCH);
        assertEquals(1, tags.size());
        assertEquals(cleaningRobotTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("Robot cuiseur", Locale.FRENCH);
        assertTrue(tags.isEmpty());

        tags = service.getByLabelOrSynonym("CUSTOM label", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(userLocationTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("Synonym with space", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(userLocationTagClass, tags.getFirst());
        tags = service.getByLabelOrSynonym("wrong label", Locale.ENGLISH);
        assertTrue(tags.isEmpty());
    }

    private static Stream<Arguments> testValidateTags() {
        return Stream.of( //
                Arguments.of(true, List.of()), //
                Arguments.of(true, List.of("Tag1")), //
                Arguments.of(true, List.of("Tag1", "Tag2")), //

                Arguments.of(true, List.of("Point")), //
                Arguments.of(true, List.of("Point", "Property")), //
                Arguments.of(true, List.of("Property")), //
                Arguments.of(true, List.of("Location")), //
                Arguments.of(true, List.of("Equipment")), //

                Arguments.of(true, List.of("Control")), // Point
                Arguments.of(true, List.of("Control", "Power")), // Point, Property
                Arguments.of(true, List.of("Level")), // Property
                Arguments.of(true, List.of("Kitchen")), // Location
                Arguments.of(true, List.of("Lightbulb")), // Equipment

                Arguments.of(false, List.of("Point", "Location")), //
                Arguments.of(false, List.of("Property", "Location")), //
                Arguments.of(false, List.of("Point", "Property", "Location")), //
                Arguments.of(false, List.of("Point", "Equipment")), //
                Arguments.of(false, List.of("Property", "Equipment")), //
                Arguments.of(false, List.of("Point", "Property", "Equipment")), //
                Arguments.of(false, List.of("Location", "Equipment")), //

                Arguments.of(false, List.of("Control", "Switch")), // Point, Point
                Arguments.of(false, List.of("Power", "Level")), // Property, Property
                Arguments.of(false, List.of("Control", "Lightbulb")), // Point, Equipment
                Arguments.of(false, List.of("Level", "Lightbulb")), // Property, Equipment
                Arguments.of(false, List.of("Control", "Level", "Lightbulb")), // Point, Property, Equipment
                Arguments.of(false, List.of("Control", "Kitchen")), // Point, Location
                Arguments.of(false, List.of("Level", "Kitchen")), // Property, Location
                Arguments.of(false, List.of("Control", "Level", "Kitchen")), // Point, Property, Location
                Arguments.of(false, List.of("Lightbulb", "Kitchen")), // Equipment, Location
                Arguments.of(false, List.of("Lightbulb", "Speaker")), // Equipment, Equipment
                Arguments.of(false, List.of("Kitchen", "FirstFloor")), // Location, Location
                Arguments.of(false, List.of("Switch", "Lightbulb", "Kitchen")), // Point, Equipment, Location
                Arguments.of(false, List.of("Power", "Lightbulb", "Kitchen")), // Property, Equipment, Location
                Arguments.of(false, List.of("Switch", "Power", "Lightbulb", "Kitchen")) // Point, Property, Equipment,
                                                                                        // Location
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testValidateTags(boolean expected, List<String> tags) {
        GenericItem item = new NumberItem("TestTag");
        item.addTags(tags);
        Class<? extends Tag> semanticTag = SemanticTags.getSemanticType(item);
        assertEquals(expected, service.validateTags(item, semanticTag));
    }

    @Test
    public void testCheckSemantics() {
        // Valid Locations and Equipments to be used for the tests
        GroupItem Location1 = new GroupItem("Location1");
        Location1.addTag("Bathroom");

        GroupItem Location2 = new GroupItem("Location2");
        Location2.addTag("Kitchen");

        GroupItem Location1Sub = new GroupItem("Location1Sub");
        Location1Sub.addTag("Room");

        GroupItem Equipment1 = new GroupItem("Equipment1");
        Equipment1.addTag("Lightbulb");

        GroupItem Equipment2 = new GroupItem("Equipment2");
        Equipment2.addTag("Lightbulb");

        GroupItem Equipment1Sub = new GroupItem("Equipment1Sub");
        Equipment1Sub.addTag("Lightbulb");

        GroupItem PointGroup = new GroupItem("PointGroup");
        PointGroup.addTag("Switch");

        Equipment1.addMember(Equipment1Sub);
        Equipment1.addMember(PointGroup);

        Location1.addMember(Location1Sub);
        Location1.addMember(Equipment1);
        Location1.addMember(PointGroup);

        Location1Sub.addMember(Equipment2);

        Stream.of(Location1, Location2, Location1Sub, Equipment1, Equipment2, Equipment1Sub, PointGroup).forEach(i -> {
            try {
                when(itemRegistryMock.getItem(i.getName())).thenReturn(i);
            } catch (ItemNotFoundException e) {
                // should not happen for mocks
            }
        });

        // Test Items

        // Valid Points
        GenericItem ValidPoint1 = new NumberItem("ValidPoint1");
        ValidPoint1.addTag("Switch");
        Location1.addMember(ValidPoint1);
        assertTrue(service.checkSemantics(ValidPoint1));

        GenericItem ValidPoint2 = new NumberItem("ValidPoint2");
        ValidPoint2.addTag("Switch");
        Equipment1.addMember(ValidPoint2);
        assertTrue(service.checkSemantics(ValidPoint2));

        // A Group Item is a valid Point
        GroupItem ValidPoint3 = new GroupItem("ValidPoint3");
        ValidPoint3.addTag("Switch");
        assertTrue(service.checkSemantics(ValidPoint3));

        // Being a Member of another Point (Group) is OK, they are independent of each other
        GenericItem ValidPoint4 = new NumberItem("ValidPoint4");
        ValidPoint4.addTag("Switch");
        PointGroup.addMember(ValidPoint4);
        assertTrue(service.checkSemantics(ValidPoint4));

        // Not a member of any Location or Equipment
        GenericItem ValidPoint5 = new NumberItem("ValidPoint5");
        ValidPoint5.addTag("Switch");
        assertTrue(service.checkSemantics(ValidPoint5));

        // Belonging to Location and Equipment is allowed
        // for example:
        // When a location contains multiple equipments / temperature points,
        // a point who is the direct member of the location is preferred
        GenericItem ValidPoint6 = new NumberItem("ValidPoint6");
        ValidPoint6.addTag("Switch");
        Location1.addMember(ValidPoint6);
        Equipment1.addMember(ValidPoint6);
        assertTrue(service.checkSemantics(ValidPoint6));

        // Same case as above, but in a sub equipment
        GenericItem ValidPoint7 = new NumberItem("ValidPoint7");
        ValidPoint7.addTag("Switch");
        Location1.addMember(ValidPoint7);
        Equipment1Sub.addMember(ValidPoint7);
        assertTrue(service.checkSemantics(ValidPoint7));

        // Belongs to two independent locations
        GenericItem InvalidPoint1 = new NumberItem("InvalidPoint1");
        InvalidPoint1.addTag("Switch");
        Location1.addMember(InvalidPoint1);
        Location2.addMember(InvalidPoint1);
        assertTrue(InvalidPoint1.getGroupNames().contains(Location1.getName()));
        assertFalse(service.checkSemantics(InvalidPoint1));

        // Belongs to Location and its sub location
        GenericItem InvalidPoint2 = new NumberItem("InvalidPoint2");
        InvalidPoint2.addTag("Switch");
        Location1.addMember(InvalidPoint2);
        Location1Sub.addMember(InvalidPoint2);
        assertFalse(service.checkSemantics(InvalidPoint2));

        // Belongs to two Equipments
        GenericItem InvalidPoint3 = new NumberItem("InvalidPoint3");
        InvalidPoint3.addTag("Switch");
        Equipment1.addMember(InvalidPoint3);
        Equipment2.addMember(InvalidPoint3);
        assertFalse(service.checkSemantics(InvalidPoint3));

        // Locations

        // It's OK not to be a member of any Location
        GroupItem ValidLocation1 = new GroupItem("ValidLocation1");
        ValidLocation1.addTag("Bathroom");
        assertTrue(service.checkSemantics(ValidLocation1));

        // Member of a Location
        GroupItem ValidLocation2 = new GroupItem("ValidLocation2");
        ValidLocation2.addTag("Bathroom");
        Location1.addMember(ValidLocation2);
        assertTrue(service.checkSemantics(ValidLocation2));

        // Member of a Point is fine
        GroupItem ValidLocation3 = new GroupItem("ValidLocation3");
        ValidLocation3.addTag("Bathroom");
        PointGroup.addMember(ValidLocation3);
        assertTrue(service.checkSemantics(ValidLocation3));

        // Non-GroupItem is not a valid Location
        NumberItem InvalidLocation1 = new NumberItem("InvalidLocation1");
        InvalidLocation1.addTag("Bathroom");
        assertFalse(service.checkSemantics(InvalidLocation1));

        // Belongs to two Locations
        GroupItem InvalidLocation2 = new GroupItem("InvalidLocation2");
        InvalidLocation2.addTag("Bathroom");
        Location1.addMember(InvalidLocation2);
        Location2.addMember(InvalidLocation2);
        assertFalse(service.checkSemantics(InvalidLocation2));

        // Belongs to Equipment
        GroupItem InvalidLocation3 = new GroupItem("InvalidLocation3");
        InvalidLocation3.addTag("Bathroom");
        Equipment1.addMember(InvalidLocation3);
        assertFalse(service.checkSemantics(InvalidLocation3));

        // Belongs to Location and Equipment
        GroupItem InvalidLocation4 = new GroupItem("InvalidLocation4");
        InvalidLocation4.addTag("Bathroom");
        Location1.addMember(InvalidLocation4);
        Equipment1.addMember(InvalidLocation4);
        assertFalse(service.checkSemantics(InvalidLocation4));

        // Belongs to Location and Location1Sub
        GroupItem InvalidLocation5 = new GroupItem("InvalidLocation5");
        InvalidLocation5.addTag("Bathroom");
        Location1Sub.addMember(InvalidLocation5);
        Location1.addMember(InvalidLocation5);
        assertFalse(service.checkSemantics(InvalidLocation5));

        // Equipments

        // It's OK not to be a member of any Equipment or Location
        GroupItem ValidEquipment1 = new GroupItem("ValidEquipment1");
        ValidEquipment1.addTag("Lightbulb");
        assertTrue(service.checkSemantics(ValidEquipment1));

        // Member of an Equipment
        GroupItem ValidEquipment2 = new GroupItem("ValidEquipment2");
        ValidEquipment2.addTag("Lightbulb");
        Equipment1.addMember(ValidEquipment2);
        assertTrue(service.checkSemantics(ValidEquipment2));

        // Member of a Equipment1Sub
        GroupItem ValidEquipment3 = new GroupItem("ValidEquipment3");
        ValidEquipment3.addTag("Lightbulb");
        Equipment1Sub.addMember(ValidEquipment3);
        assertTrue(service.checkSemantics(ValidEquipment3));

        // Member of a Point is fine
        GroupItem ValidEquipment4 = new GroupItem("ValidEquipment4");
        ValidEquipment4.addTag("Lightbulb");
        PointGroup.addMember(ValidEquipment4);
        assertTrue(service.checkSemantics(ValidEquipment4));

        // Non-GroupItem is a valid Equipment
        NumberItem ValidEquipment5 = new NumberItem("ValidEquipment5");
        ValidEquipment5.addTag("Lightbulb");
        assertTrue(service.checkSemantics(ValidEquipment5));

        // Belongs to a Location
        GroupItem ValidEquipment6 = new GroupItem("ValidEquipment6");
        ValidEquipment6.addTag("Lightbulb");
        Location1.addMember(ValidEquipment6);
        assertTrue(service.checkSemantics(ValidEquipment6));

        // Belongs to two Equipments
        GroupItem InvalidEquipment1 = new GroupItem("InvalidEquipment1");
        InvalidEquipment1.addTag("Lightbulb");
        Equipment1.addMember(InvalidEquipment1);
        Equipment2.addMember(InvalidEquipment1);
        assertFalse(service.checkSemantics(InvalidEquipment1));

        // Belongs to Location and Equipment
        GroupItem InvalidEquipment2 = new GroupItem("InvalidEquipment2");
        InvalidEquipment2.addTag("Lightbulb");
        Location1.addMember(InvalidEquipment2);
        Equipment1.addMember(InvalidEquipment2);
        assertFalse(service.checkSemantics(InvalidEquipment2));

        // Belongs to two Locations
        GroupItem InvalidEquipment3 = new GroupItem("InvalidEquipment3");
        InvalidEquipment3.addTag("Lightbulb");
        Location1.addMember(InvalidEquipment3);
        Location2.addMember(InvalidEquipment3);
        assertFalse(service.checkSemantics(InvalidEquipment3));
    }
}
