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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.semantics.model.location.Bathroom;
import org.openhab.core.semantics.model.location.LivingRoom;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class SemanticsServiceImplTest {

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;

    private @NonNullByDefault({}) GroupItem locationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem pointItem;

    private @NonNullByDefault({}) SemanticsServiceImpl service;

    @BeforeEach
    public void setup() throws Exception {
        CoreItemFactory itemFactory = new CoreItemFactory();
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

        when(itemRegistryMock.stream()).thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem));

        service = new SemanticsServiceImpl(itemRegistryMock, metadataRegistryMock);
    }

    @Test
    public void testGetItemsInLocation() throws Exception {
        Set<Item> items = service.getItemsInLocation(Bathroom.class);
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation("Room", Locale.ENGLISH);
        assertTrue(items.contains(pointItem));
    }

    @Test
    public void testGetItemsInLocationByString() throws Exception {
        Set<Item> items = service.getItemsInLocation("joe's room", Locale.ENGLISH);
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation(LivingRoom.class);
        assertTrue(items.isEmpty());
    }
}
