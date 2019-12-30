/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
public class SemanticsServiceImplTest {

    private @Mock ItemRegistry itemRegistry;
    private @Mock MetadataRegistry metadataRegistry;

    private GroupItem locationItem;
    private GroupItem equipmentItem;
    private GenericItem pointItem;

    private SemanticsServiceImpl service;

    @Before
    public void setup() throws Exception {
        initMocks(this);

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

        when(metadataRegistry.get(any())).thenReturn(null);
        when(itemRegistry.stream()).thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem))
                .thenReturn(Stream.of(locationItem, equipmentItem, pointItem));

        service = new SemanticsServiceImpl(itemRegistry, metadataRegistry);
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
