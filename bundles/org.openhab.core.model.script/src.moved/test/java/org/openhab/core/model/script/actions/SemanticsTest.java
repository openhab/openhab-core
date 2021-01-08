/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.model.script.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.script.internal.engine.action.SemanticsActionService;
import org.openhab.core.semantics.model.equipment.CleaningRobot;
import org.openhab.core.semantics.model.location.Bathroom;
import org.openhab.core.semantics.model.location.Indoor;

/**
 * This are tests for {@link Semantics} actions.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class SemanticsTest {

    private @Mock ItemRegistry mockedItemRegistry;

    private GroupItem indoorLocationItem;
    private GroupItem bathroomLocationItem;
    private GroupItem equipmentItem;
    private GenericItem temperaturePointItem;
    private GenericItem humidityPointItem;

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        CoreItemFactory itemFactory = new CoreItemFactory();

        indoorLocationItem = new GroupItem("TestHouse");
        indoorLocationItem.addTag("Indoor");

        bathroomLocationItem = new GroupItem("TestBathRoom");
        bathroomLocationItem.addTag("Bathroom");

        // Bathroom is placed in Indoor
        indoorLocationItem.addMember(bathroomLocationItem);
        bathroomLocationItem.addGroupName(indoorLocationItem.getName());

        equipmentItem = new GroupItem("Test08");
        equipmentItem.addTag("CleaningRobot");

        // Equipment (Cleaning Robot) is placed in Bathroom
        bathroomLocationItem.addMember(equipmentItem);
        equipmentItem.addGroupName(bathroomLocationItem.getName());

        temperaturePointItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestTemperature");
        temperaturePointItem.addTag("Measurement");
        temperaturePointItem.addTag("Temperature");

        // Temperature Point is Property of Equipment (Cleaning Robot)
        equipmentItem.addMember(temperaturePointItem);
        temperaturePointItem.addGroupName(equipmentItem.getName());

        humidityPointItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestHumidity");
        humidityPointItem.addTag("Measurement");
        humidityPointItem.addTag("Humidity");

        when(mockedItemRegistry.getItem("TestHouse")).thenReturn(indoorLocationItem);
        when(mockedItemRegistry.getItem("TestBathRoom")).thenReturn(bathroomLocationItem);
        when(mockedItemRegistry.getItem("Test08")).thenReturn(equipmentItem);
        when(mockedItemRegistry.getItem("TestTemperature")).thenReturn(temperaturePointItem);
        when(mockedItemRegistry.getItem("TestHumidity")).thenReturn(humidityPointItem);

        new SemanticsActionService(mockedItemRegistry);
    }

    @Test
    public void testGetLocation() {
        assertThat(Semantics.getLocation(indoorLocationItem), is(indoorLocationItem));
        assertThat(Semantics.getLocation(bathroomLocationItem), is(bathroomLocationItem));

        assertThat(Semantics.getLocation(equipmentItem), is(bathroomLocationItem));

        assertThat(Semantics.getLocation(temperaturePointItem), is(bathroomLocationItem));

        assertNull(Semantics.getLocation(humidityPointItem));
    }

    @Test
    public void testGetLocationType() {
        assertThat(Semantics.getLocationType(indoorLocationItem), is(Indoor.class));
        assertThat(Semantics.getLocationType(bathroomLocationItem), is(Bathroom.class));

        assertNull(Semantics.getLocationType(humidityPointItem));
    }

    @Test
    public void testGetEquipment() {
        assertThat(Semantics.getEquipment(equipmentItem), is(equipmentItem));

        assertThat(Semantics.getEquipment(temperaturePointItem), is(equipmentItem));

        assertNull(Semantics.getEquipment(humidityPointItem));
    }

    @Test
    public void testGetEquipmentType() {
        assertThat(Semantics.getEquipmentType(equipmentItem), is(CleaningRobot.class));

        assertThat(Semantics.getEquipmentType(temperaturePointItem), is(CleaningRobot.class));

        assertNull(Semantics.getEquipmentType(humidityPointItem));
    }
}
