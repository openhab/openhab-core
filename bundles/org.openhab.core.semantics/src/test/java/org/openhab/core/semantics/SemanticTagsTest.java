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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.semantics.model.equipment.CleaningRobot;
import org.openhab.core.semantics.model.equipment.Equipments;
import org.openhab.core.semantics.model.location.Bathroom;
import org.openhab.core.semantics.model.location.Kitchen;
import org.openhab.core.semantics.model.location.Locations;
import org.openhab.core.semantics.model.location.Room;
import org.openhab.core.semantics.model.point.Measurement;
import org.openhab.core.semantics.model.point.Points;
import org.openhab.core.semantics.model.property.Light;
import org.openhab.core.semantics.model.property.Properties;
import org.openhab.core.semantics.model.property.SoundVolume;
import org.openhab.core.semantics.model.property.Temperature;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class SemanticTagsTest {

    private @NonNullByDefault({}) GroupItem locationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem pointItem;

    @BeforeEach
    public void setup() {
        CoreItemFactory itemFactory = new CoreItemFactory();

        locationItem = new GroupItem("TestBathRoom");
        locationItem.addTag("Bathroom");

        equipmentItem = new GroupItem("Test08");
        equipmentItem.addTag("CleaningRobot");

        pointItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestTemperature");
        pointItem.addTag("Measurement");
        pointItem.addTag("Temperature");
    }

    @Test
    public void testByTagId() {
        assertEquals(Location.class, SemanticTags.getById("Location"));
        assertEquals(Room.class, SemanticTags.getById("Room"));
        assertEquals(Room.class, SemanticTags.getById("Location_Indoor_Room"));
        assertEquals(Bathroom.class, SemanticTags.getById("Bathroom"));
        assertEquals(Bathroom.class, SemanticTags.getById("Room_Bathroom"));
        assertEquals(Bathroom.class, SemanticTags.getById("Indoor_Room_Bathroom"));
        assertEquals(Bathroom.class, SemanticTags.getById("Location_Indoor_Room_Bathroom"));
    }

    @Test
    public void testByLabel() {
        assertEquals(Kitchen.class, SemanticTags.getByLabel("Kitchen", Locale.ENGLISH));
        assertEquals(Kitchen.class, SemanticTags.getByLabel("Küche", Locale.GERMAN));
        assertNull(SemanticTags.getByLabel("Bad", Locale.GERMAN));
    }

    @Test
    public void testByLabelOrSynonym() {
        assertEquals(Kitchen.class, SemanticTags.getByLabelOrSynonym("Kitchen", Locale.ENGLISH).iterator().next());
        assertEquals(Kitchen.class, SemanticTags.getByLabelOrSynonym("Küche", Locale.GERMAN).iterator().next());
        assertEquals(Bathroom.class, SemanticTags.getByLabelOrSynonym("Badezimmer", Locale.GERMAN).iterator().next());
    }

    @Test
    public void testGetLabel() {
        assertEquals("Kitchen", SemanticTags.getLabel(Kitchen.class, Locale.ENGLISH));
        assertEquals("Sound Volume", SemanticTags.getLabel(SoundVolume.class, Locale.ENGLISH));
    }

    @Test
    public void testGetSynonyms() {
        assertThat(SemanticTags.getSynonyms(Light.class, Locale.ENGLISH), hasItems("Lights", "Lighting"));
    }

    @Test
    public void testGetDescription() {
        Class<? extends Tag> tag = SemanticTags.add("TestDesc", Light.class, null, null, "Test Description");
        assertEquals("Test Description", SemanticTags.getDescription(tag, Locale.ENGLISH));
    }

    @Test
    public void testGetSemanticType() {
        assertEquals(Bathroom.class, SemanticTags.getSemanticType(locationItem));
        assertEquals(CleaningRobot.class, SemanticTags.getSemanticType(equipmentItem));
        assertEquals(Measurement.class, SemanticTags.getSemanticType(pointItem));
    }

    @Test
    public void testGetLocation() {
        assertEquals(Bathroom.class, SemanticTags.getLocation(locationItem));
    }

    @Test
    public void testGetEquipment() {
        assertEquals(CleaningRobot.class, SemanticTags.getEquipment(equipmentItem));
    }

    @Test
    public void testGetPoint() {
        assertEquals(Measurement.class, SemanticTags.getPoint(pointItem));
    }

    @Test
    public void testGetProperty() {
        assertEquals(Temperature.class, SemanticTags.getProperty(pointItem));
    }

    @Test
    public void testAddLocation() {
        String tagName = "CustomLocation";
        Class customTag = SemanticTags.add(tagName, Location.class);
        assertNotNull(customTag);
        assertEquals(customTag, SemanticTags.getById(tagName));
        assertEquals(customTag, SemanticTags.getByLabel("Custom Location", Locale.getDefault()));
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
        assertEquals(customTag, SemanticTags.getByLabel("Custom Equipment", Locale.getDefault()));
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
        assertEquals(customTag, SemanticTags.getByLabel("Custom Point", Locale.getDefault()));
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
        assertEquals(customTag, SemanticTags.getByLabel("Custom Property", Locale.getDefault()));
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

    @Test
    public void testAddWithCustomLabel() {
        Class tag = SemanticTags.add("CustomProperty2", Property.class, " Custom Label ", null, null);
        assertEquals(tag, SemanticTags.getByLabel("Custom Label", Locale.getDefault()));
    }

    @Test
    public void testAddWithSynonyms() {
        String synonyms = " Synonym1, Synonym2 , Synonym With Space ";
        Class tag = SemanticTags.add("CustomProperty3", Property.class, null, synonyms, null);
        assertEquals(tag, SemanticTags.getByLabelOrSynonym("Synonym1", Locale.getDefault()).get(0));
        assertEquals(tag, SemanticTags.getByLabelOrSynonym("Synonym2", Locale.getDefault()).get(0));
        assertEquals(tag, SemanticTags.getByLabelOrSynonym("Synonym With Space", Locale.getDefault()).get(0));
    }
}
