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
package org.eclipse.smarthome.core.semantics.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.semantics.model.location.Bathroom;
import org.eclipse.smarthome.core.semantics.model.location.LivingRoom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;

/**
 * @author Kai Kreuzer - initial contribution
 */
public class SemanticsServiceImplTest {

    private @Mock BundleContext bundleContext;
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

        service = new SemanticsServiceImpl();
        service.setItemRegistry(itemRegistry);
        service.setMetadataRegistry(metadataRegistry);
        service.activate(bundleContext);
    }

    @Test
    public void testGetItemsInLocation() throws Exception {
        Set<@NonNull Item> items = service.getItemsInLocation(Bathroom.class);
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation("Room", Locale.ENGLISH);
        assertTrue(items.contains(pointItem));
    }

    @Test
    public void testGetItemsInLocationByString() throws Exception {
        Set<@NonNull Item> items = service.getItemsInLocation("joe's room", Locale.ENGLISH);
        assertTrue(items.contains(pointItem));

        items = service.getItemsInLocation(LivingRoom.class);
        assertTrue(items.isEmpty());
    }
}
