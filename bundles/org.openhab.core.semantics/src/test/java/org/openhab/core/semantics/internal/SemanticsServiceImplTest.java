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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagRegistry;
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
        assertEquals("bathroom", result.get(0));
        assertEquals("bathrooms", result.get(1));
        assertEquals("bath", result.get(2));
        assertEquals("baths", result.get(3));
        assertEquals("powder room", result.get(4));
        assertEquals("powder rooms", result.get(5));

        result = service.getLabelAndSynonyms(cleaningRobotTagClass, Locale.FRENCH);
        assertEquals(4, result.size());
        assertEquals("robot de nettoyage", result.get(0));
        assertEquals("robos de nettoyage", result.get(1));
        assertEquals("robot aspirateur", result.get(2));
        assertEquals("robots aspirateur", result.get(3));

        result = service.getLabelAndSynonyms(userLocationTagClass, Locale.ENGLISH);
        assertEquals(4, result.size());
        assertEquals("custom label", result.get(0));
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
        assertEquals(bathroomTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("POWDER Rooms", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(bathroomTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("other bath", Locale.ENGLISH);
        assertTrue(tags.isEmpty());

        tags = service.getByLabelOrSynonym("ROBOT de nettoyage", Locale.FRENCH);
        assertEquals(1, tags.size());
        assertEquals(cleaningRobotTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("ROBOTS aspirateur", Locale.FRENCH);
        assertEquals(1, tags.size());
        assertEquals(cleaningRobotTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("Robot cuiseur", Locale.FRENCH);
        assertTrue(tags.isEmpty());

        tags = service.getByLabelOrSynonym("CUSTOM label", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(userLocationTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("Synonym with space", Locale.ENGLISH);
        assertEquals(1, tags.size());
        assertEquals(userLocationTagClass, tags.get(0));
        tags = service.getByLabelOrSynonym("wrong label", Locale.ENGLISH);
        assertTrue(tags.isEmpty());
    }
}
