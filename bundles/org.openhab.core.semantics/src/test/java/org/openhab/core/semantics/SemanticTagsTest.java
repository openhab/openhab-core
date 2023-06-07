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
package org.openhab.core.semantics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.library.CoreItemFactory;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class SemanticTagsTest {

    private @NonNullByDefault({}) GroupItem locationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem pointItem;

    private @NonNullByDefault({}) Class<? extends Tag> roomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> bathroomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> cleaningRobotTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> televisionTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> measurementTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> temperatureTagClass;

    @BeforeEach
    public void setup() {
        CoreItemFactory itemFactory = new CoreItemFactory(mock(UnitProvider.class));

        locationItem = new GroupItem("TestBathRoom");
        locationItem.addTag("Bathroom");

        equipmentItem = new GroupItem("Test08");
        equipmentItem.addTag("CleaningRobot");

        pointItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestTemperature");
        pointItem.addTag("Measurement");
        pointItem.addTag("Temperature");

        SemanticTags.add("Indoor", Location.class);
        SemanticTags.add("Room", "Location_Indoor");
        SemanticTags.add("Bathroom", "Location_Indoor_Room");
        SemanticTags.add("CleaningRobot", Equipment.class);
        SemanticTags.add("Screen", Equipment.class);
        SemanticTags.add("Television", "Equipment_Screen");
        SemanticTags.add("Measurement", Point.class);
        SemanticTags.add("Control", Point.class);
        SemanticTags.add("Temperature", Property.class);

        roomTagClass = SemanticTags.getById("Location_Indoor_Room");
        bathroomTagClass = SemanticTags.getById("Location_Indoor_Room_Bathroom");
        cleaningRobotTagClass = SemanticTags.getById("Equipment_CleaningRobot");
        televisionTagClass = SemanticTags.getById("Equipment_Screen_Television");
        measurementTagClass = SemanticTags.getById("Point_Measurement");
        temperatureTagClass = SemanticTags.getById("Property_Temperature");
    }

    @Test
    public void testTagClasses() {
        assertNotNull(roomTagClass);
        assertNotNull(bathroomTagClass);
        assertNotNull(cleaningRobotTagClass);
        assertNotNull(televisionTagClass);
        assertNotNull(measurementTagClass);
        assertNotNull(temperatureTagClass);
    }

    @Test
    public void testByTagId() {
        assertEquals(Location.class, SemanticTags.getById("Location"));
        assertEquals(roomTagClass, SemanticTags.getById("Room"));
        assertEquals(roomTagClass, SemanticTags.getById("Location_Indoor_Room"));
        assertEquals(bathroomTagClass, SemanticTags.getById("Bathroom"));
        assertEquals(bathroomTagClass, SemanticTags.getById("Room_Bathroom"));
        assertEquals(bathroomTagClass, SemanticTags.getById("Indoor_Room_Bathroom"));
        assertEquals(bathroomTagClass, SemanticTags.getById("Location_Indoor_Room_Bathroom"));
    }

    @Test
    public void testGetSemanticType() {
        assertEquals(bathroomTagClass, SemanticTags.getSemanticType(locationItem));
        assertEquals(cleaningRobotTagClass, SemanticTags.getSemanticType(equipmentItem));
        assertEquals(measurementTagClass, SemanticTags.getSemanticType(pointItem));
    }

    @Test
    public void testGetLocation() {
        assertEquals(bathroomTagClass, SemanticTags.getLocation(locationItem));
    }

    @Test
    public void testGetEquipment() {
        assertEquals(cleaningRobotTagClass, SemanticTags.getEquipment(equipmentItem));
    }

    @Test
    public void testGetPoint() {
        assertEquals(measurementTagClass, SemanticTags.getPoint(pointItem));
    }

    @Test
    public void testGetProperty() {
        assertEquals(temperatureTagClass, SemanticTags.getProperty(pointItem));
    }

    @Test
    public void testAddLocation() {
        String tagName = "CustomLocation";
        Class customTag = SemanticTags.add(tagName, Location.class);
        assertNotNull(customTag);
        assertEquals(customTag, SemanticTags.getById(tagName));
        assertTrue(Locations.stream().toList().contains(customTag));

        GroupItem myItem = new GroupItem("MyLocation");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getLocation(myItem));
    }

    @Test
    public void testAddLocationWithParentString() {
        String tagName = "CustomLocationParentString";
        Class customTag = SemanticTags.add(tagName, "Location");
        assertNotNull(customTag);
        assertTrue(Locations.stream().toList().contains(customTag));
    }

    @Test
    public void testAddEquipment() {
        String tagName = "CustomEquipment";
        Class customTag = SemanticTags.add(tagName, Equipment.class);
        assertNotNull(customTag);
        assertEquals(customTag, SemanticTags.getById(tagName));
        assertTrue(Equipments.stream().toList().contains(customTag));

        GroupItem myItem = new GroupItem("MyEquipment");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getEquipment(myItem));
    }

    @Test
    public void testAddEquipmentWithParentString() {
        String tagName = "CustomEquipmentParentString";
        Class customTag = SemanticTags.add(tagName, "Television");
        assertNotNull(customTag);
        assertTrue(Equipments.stream().toList().contains(customTag));
    }

    @Test
    public void testAddPoint() {
        String tagName = "CustomPoint";
        Class customTag = SemanticTags.add(tagName, Point.class);
        assertNotNull(customTag);
        assertEquals(customTag, SemanticTags.getById(tagName));
        assertTrue(Points.stream().toList().contains(customTag));

        GroupItem myItem = new GroupItem("MyItem");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getPoint(myItem));
    }

    @Test
    public void testAddPointParentString() {
        String tagName = "CustomPointParentString";
        Class customTag = SemanticTags.add(tagName, "Control");
        assertNotNull(customTag);
        assertTrue(Points.stream().toList().contains(customTag));
    }

    @Test
    public void testAddProperty() {
        String tagName = "CustomProperty";
        Class customTag = SemanticTags.add(tagName, Property.class);
        assertNotNull(customTag);
        assertEquals(customTag, SemanticTags.getById(tagName));
        assertTrue(Properties.stream().toList().contains(customTag));

        GroupItem myItem = new GroupItem("MyItem");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getProperty(myItem));
    }

    @Test
    public void testAddPropertyParentString() {
        String tagName = "CustomPropertyParentString";
        Class customTag = SemanticTags.add(tagName, "Property");
        assertNotNull(customTag);
        assertTrue(Properties.stream().toList().contains(customTag));
    }

    @Test
    public void testAddingExistingTagShouldFail() {
        assertNull(SemanticTags.add("Room", Location.class));

        assertNotNull(SemanticTags.add("CustomLocation1", Location.class));
        assertNull(SemanticTags.add("CustomLocation1", Location.class));
    }
}
