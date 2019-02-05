/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.semantics;

import static org.junit.Assert.*;

import java.util.Locale;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.semantics.model.Location;
import org.eclipse.smarthome.core.semantics.model.equipment.CleaningRobot;
import org.eclipse.smarthome.core.semantics.model.location.Bathroom;
import org.eclipse.smarthome.core.semantics.model.location.Kitchen;
import org.eclipse.smarthome.core.semantics.model.location.Room;
import org.eclipse.smarthome.core.semantics.model.point.Measurement;
import org.eclipse.smarthome.core.semantics.model.property.Temperature;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kai Kreuzer - initial contribution and API
 */
public class SemanticTagsTest {

    private GroupItem locationItem;
    private GroupItem equipmentItem;
    private GenericItem pointItem;

    @Before
    public void setup() throws Exception {
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
        assertNull(SemanticTags.getByLabel("Badezimmer", Locale.GERMAN));
    }

    @Test
    public void testByLabelOrSynonym() {
        assertEquals(Kitchen.class, SemanticTags.getByLabelOrSynonym("Kitchen", Locale.ENGLISH).iterator().next());
        assertEquals(Kitchen.class, SemanticTags.getByLabelOrSynonym("Küche", Locale.GERMAN).iterator().next());
        assertEquals(Bathroom.class, SemanticTags.getByLabelOrSynonym("Badezimmer", Locale.GERMAN).iterator().next());
    }

    @Test
    public void testGetSemanticType() {
        assertEquals(Bathroom.class, SemanticTags.getSemanticType(locationItem));
        assertEquals(CleaningRobot.class, SemanticTags.getSemanticType(equipmentItem));
        assertEquals(Measurement.class, SemanticTags.getSemanticType(pointItem));
    }

    @Test
    public void testGetProperty() {
        assertEquals(Temperature.class, SemanticTags.getProperty(pointItem));
    }

}
