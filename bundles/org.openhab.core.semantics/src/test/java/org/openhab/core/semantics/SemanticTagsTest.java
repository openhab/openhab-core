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
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.semantics.internal.SemanticTagRegistryImpl;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class SemanticTagsTest {

    private static final String CUSTOM_LOCATION = "CustomLocation";
    private static final String CUSTOM_EQUIPMENT = "CustomEquipment";
    private static final String CUSTOM_POINT = "CustomPoint";
    private static final String CUSTOM_PROPERTY = "CustomProperty";

    private @Mock @NonNullByDefault({}) ManagedSemanticTagProvider managedSemanticTagProviderMock;

    private @NonNullByDefault({}) GroupItem locationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem pointItem;

    private @NonNullByDefault({}) Class<? extends Tag> roomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> bathroomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> cleaningRobotTagClass;
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

        SemanticTag customLocationTag = new SemanticTagImpl("Location_" + CUSTOM_LOCATION, null, null, List.of());
        SemanticTag customEquipmentTag = new SemanticTagImpl("Equipment_" + CUSTOM_EQUIPMENT, null, null, List.of());
        SemanticTag customPointTag = new SemanticTagImpl("Point_" + CUSTOM_POINT, null, null, List.of());
        SemanticTag customPropertyTag = new SemanticTagImpl("Property_" + CUSTOM_PROPERTY, null, null, List.of());
        when(managedSemanticTagProviderMock.getAll())
                .thenReturn(List.of(customLocationTag, customEquipmentTag, customPointTag, customPropertyTag));
        new SemanticTagRegistryImpl(new DefaultSemanticTagProvider(), managedSemanticTagProviderMock);

        roomTagClass = SemanticTags.getById("Location_Indoor_Room");
        bathroomTagClass = SemanticTags.getById("Location_Indoor_Room_Bathroom");
        cleaningRobotTagClass = SemanticTags.getById("Equipment_CleaningRobot");
        measurementTagClass = SemanticTags.getById("Point_Measurement");
        temperatureTagClass = SemanticTags.getById("Property_Temperature");
    }

    @Test
    public void testTagClasses() {
        assertNotNull(roomTagClass);
        assertNotNull(bathroomTagClass);
        assertNotNull(cleaningRobotTagClass);
        assertNotNull(measurementTagClass);
        assertNotNull(temperatureTagClass);
    }

    @Test
    public void testByTagId() {
        assertEquals(Location.class, SemanticTags.getById("Location"));
        assertEquals(roomTagClass, SemanticTags.getById("Room"));
        assertEquals(roomTagClass, SemanticTags.getById("Indoor_Room"));
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
        String tagName = CUSTOM_LOCATION;
        Class<? extends Tag> customTag = SemanticTags.getById(tagName);
        assertNotNull(customTag);

        GroupItem myItem = new GroupItem("MyLocation");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getLocation(myItem));
    }

    @Test
    public void testAddEquipment() {
        String tagName = CUSTOM_EQUIPMENT;
        Class<? extends Tag> customTag = SemanticTags.getById(tagName);
        assertNotNull(customTag);

        GroupItem myItem = new GroupItem("MyEquipment");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getEquipment(myItem));
    }

    @Test
    public void testAddPoint() {
        String tagName = CUSTOM_POINT;
        Class<? extends Tag> customTag = SemanticTags.getById(tagName);
        assertNotNull(customTag);

        GroupItem myItem = new GroupItem("MyItem");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getPoint(myItem));
    }

    @Test
    public void testAddProperty() {
        String tagName = CUSTOM_PROPERTY;
        Class<? extends Tag> customTag = SemanticTags.getById(tagName);
        assertNotNull(customTag);

        GroupItem myItem = new GroupItem("MyItem");
        myItem.addTag(tagName);

        assertEquals(customTag, SemanticTags.getProperty(myItem));
    }
}
