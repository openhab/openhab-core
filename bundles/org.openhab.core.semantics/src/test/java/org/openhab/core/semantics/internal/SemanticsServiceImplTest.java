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
        GroupItem location1 = new GroupItem("Location1");
        location1.addTag("Bathroom");

        GroupItem location2 = new GroupItem("Location2");
        location2.addTag("Kitchen");

        GroupItem location1Sub = new GroupItem("Location1Sub");
        location1Sub.addTag("Room");

        GroupItem equipment1 = new GroupItem("Equipment1");
        equipment1.addTag("Lightbulb");

        GroupItem equipment2 = new GroupItem("Equipment2");
        equipment2.addTag("Lightbulb");

        GroupItem equipment1Sub = new GroupItem("Equipment1Sub");
        equipment1Sub.addTag("Lightbulb");

        GroupItem pointGroup = new GroupItem("PointGroup");
        pointGroup.addTag("Switch");

        equipment1.addMember(equipment1Sub);
        equipment1.addMember(pointGroup);

        location1.addMember(location1Sub);
        location1.addMember(equipment1);
        location1.addMember(pointGroup);

        location1Sub.addMember(equipment2);

        Stream.of(location1, location2, location1Sub, equipment1, equipment2, equipment1Sub, pointGroup).forEach(i -> {
            try {
                when(itemRegistryMock.getItem(i.getName())).thenReturn(i);
            } catch (ItemNotFoundException e) {
                // should not happen for mocks
            }
        });

        // Test Items

        // Valid Points
        GenericItem validPoint1 = new NumberItem("ValidPoint1");
        validPoint1.addTag("Switch");
        location1.addMember(validPoint1);
        assertTrue(service.checkSemantics(validPoint1));

        GenericItem validPoint2 = new NumberItem("ValidPoint2");
        validPoint2.addTag("Switch");
        equipment1.addMember(validPoint2);
        assertTrue(service.checkSemantics(validPoint2));

        // A Group Item is a valid Point
        GroupItem validPoint3 = new GroupItem("ValidPoint3");
        validPoint3.addTag("Switch");
        assertTrue(service.checkSemantics(validPoint3));

        // Being a Member of another Point (Group) is OK, they are independent of each other
        GenericItem validPoint4 = new NumberItem("ValidPoint4");
        validPoint4.addTag("Switch");
        pointGroup.addMember(validPoint4);
        assertTrue(service.checkSemantics(validPoint4));

        // Not a member of any Location or Equipment
        GenericItem validPoint5 = new NumberItem("ValidPoint5");
        validPoint5.addTag("Switch");
        assertTrue(service.checkSemantics(validPoint5));

        // Belonging to Location and Equipment is allowed
        // for example:
        // When a location contains multiple equipments / temperature points,
        // a point who is the direct member of the location is preferred
        GenericItem validPoint6 = new NumberItem("ValidPoint6");
        validPoint6.addTag("Switch");
        location1.addMember(validPoint6);
        equipment1.addMember(validPoint6);
        assertTrue(service.checkSemantics(validPoint6));

        // Same case as above, but in a sub equipment
        GenericItem validPoint7 = new NumberItem("ValidPoint7");
        validPoint7.addTag("Switch");
        location1.addMember(validPoint7);
        equipment1Sub.addMember(validPoint7);
        assertTrue(service.checkSemantics(validPoint7));

        // Belongs to two independent locations
        GenericItem invalidPoint1 = new NumberItem("InvalidPoint1");
        invalidPoint1.addTag("Switch");
        location1.addMember(invalidPoint1);
        location2.addMember(invalidPoint1);
        assertTrue(invalidPoint1.getGroupNames().contains(location1.getName()));
        assertFalse(service.checkSemantics(invalidPoint1));

        // Belongs to Location and its sub location
        GenericItem invalidPoint2 = new NumberItem("InvalidPoint2");
        invalidPoint2.addTag("Switch");
        location1.addMember(invalidPoint2);
        location1Sub.addMember(invalidPoint2);
        assertFalse(service.checkSemantics(invalidPoint2));

        // Belongs to two Equipments
        GenericItem invalidPoint3 = new NumberItem("InvalidPoint3");
        invalidPoint3.addTag("Switch");
        equipment1.addMember(invalidPoint3);
        equipment2.addMember(invalidPoint3);
        assertFalse(service.checkSemantics(invalidPoint3));

        // Locations

        // It's OK not to be a member of any Location
        GroupItem validLocation1 = new GroupItem("ValidLocation1");
        validLocation1.addTag("Bathroom");
        assertTrue(service.checkSemantics(validLocation1));

        // Member of a Location
        GroupItem validLocation2 = new GroupItem("ValidLocation2");
        validLocation2.addTag("Bathroom");
        location1.addMember(validLocation2);
        assertTrue(service.checkSemantics(validLocation2));

        // Member of a Point is fine
        GroupItem validLocation3 = new GroupItem("validLocation3");
        validLocation3.addTag("Bathroom");
        pointGroup.addMember(validLocation3);
        assertTrue(service.checkSemantics(validLocation3));

        // Non-GroupItem is not a valid Location
        NumberItem invalidLocation1 = new NumberItem("InvalidLocation1");
        invalidLocation1.addTag("Bathroom");
        assertFalse(service.checkSemantics(invalidLocation1));

        // Belongs to two Locations
        GroupItem invalidLocation2 = new GroupItem("InvalidLocation2");
        invalidLocation2.addTag("Bathroom");
        location1.addMember(invalidLocation2);
        location2.addMember(invalidLocation2);
        assertFalse(service.checkSemantics(invalidLocation2));

        // Belongs to Equipment
        GroupItem invalidLocation3 = new GroupItem("InvalidLocation3");
        invalidLocation3.addTag("Bathroom");
        equipment1.addMember(invalidLocation3);
        assertFalse(service.checkSemantics(invalidLocation3));

        // Belongs to Location and Equipment
        GroupItem invalidLocation4 = new GroupItem("InvalidLocation4");
        invalidLocation4.addTag("Bathroom");
        location1.addMember(invalidLocation4);
        equipment1.addMember(invalidLocation4);
        assertFalse(service.checkSemantics(invalidLocation4));

        // Belongs to Location and Location1Sub
        GroupItem invalidLocation5 = new GroupItem("InvalidLocation5");
        invalidLocation5.addTag("Bathroom");
        location1Sub.addMember(invalidLocation5);
        location1.addMember(invalidLocation5);
        assertFalse(service.checkSemantics(invalidLocation5));

        // Equipments

        // It's OK not to be a member of any Equipment or Location
        GroupItem validEquipment1 = new GroupItem("ValidEquipment1");
        validEquipment1.addTag("Lightbulb");
        assertTrue(service.checkSemantics(validEquipment1));

        // Member of an Equipment
        GroupItem validEquipment2 = new GroupItem("ValidEquipment2");
        validEquipment2.addTag("Lightbulb");
        equipment1.addMember(validEquipment2);
        assertTrue(service.checkSemantics(validEquipment2));

        // Member of a Equipment1Sub
        GroupItem validEquipment3 = new GroupItem("ValidEquipment3");
        validEquipment3.addTag("Lightbulb");
        equipment1Sub.addMember(validEquipment3);
        assertTrue(service.checkSemantics(validEquipment3));

        // Member of a Point is fine
        GroupItem validEquipment4 = new GroupItem("ValidEquipment4");
        validEquipment4.addTag("Lightbulb");
        pointGroup.addMember(validEquipment4);
        assertTrue(service.checkSemantics(validEquipment4));

        // Non-GroupItem is a valid Equipment
        NumberItem validEquipment5 = new NumberItem("ValidEquipment5");
        validEquipment5.addTag("Lightbulb");
        assertTrue(service.checkSemantics(validEquipment5));

        // Belongs to a Location
        GroupItem validEquipment6 = new GroupItem("ValidEquipment6");
        validEquipment6.addTag("Lightbulb");
        location1.addMember(validEquipment6);
        assertTrue(service.checkSemantics(validEquipment6));

        // Belongs to two Equipments
        GroupItem invalidEquipment1 = new GroupItem("InvalidEquipment1");
        invalidEquipment1.addTag("Lightbulb");
        equipment1.addMember(invalidEquipment1);
        equipment2.addMember(invalidEquipment1);
        assertFalse(service.checkSemantics(invalidEquipment1));

        // Belongs to Location and Equipment
        GroupItem invalidEquipment2 = new GroupItem("InvalidEquipment2");
        invalidEquipment2.addTag("Lightbulb");
        location1.addMember(invalidEquipment2);
        equipment1.addMember(invalidEquipment2);
        assertFalse(service.checkSemantics(invalidEquipment2));

        // Belongs to two Locations
        GroupItem invalidEquipment3 = new GroupItem("InvalidEquipment3");
        invalidEquipment3.addTag("Lightbulb");
        location1.addMember(invalidEquipment3);
        location2.addMember(invalidEquipment3);
        assertFalse(service.checkSemantics(invalidEquipment3));
    }
}
