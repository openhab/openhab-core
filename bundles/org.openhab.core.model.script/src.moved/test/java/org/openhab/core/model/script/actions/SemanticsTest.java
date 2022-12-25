/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.script.internal.engine.action.SemanticsActionService;
import org.openhab.core.semantics.model.equipment.CleaningRobot;
import org.openhab.core.semantics.model.equipment.Battery;
import org.openhab.core.semantics.model.location.Bathroom;
import org.openhab.core.semantics.model.location.Indoor;

/**
 * This are tests for {@link Semantics} actions.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class SemanticsTest {

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) UnitProvider unitProviderMock;

    private @NonNullByDefault({})GroupItem indoorLocationItem;
    private @NonNullByDefault({})GroupItem bathroomLocationItem;
    private @NonNullByDefault({})GroupItem equipmentItem;
    private @NonNullByDefault({})GenericItem temperaturePointItem;
    private @NonNullByDefault({})GenericItem humidityPointItem;
    private @NonNullByDefault({})GenericItem subEquipmentItem;

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        CoreItemFactory itemFactory = new CoreItemFactory(unitProviderMock);

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

        subEquipmentItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestBattery");
        subEquipmentItem.addTag("Battery");

        // Equipment (TestBattery) is a part of Equipment (Cleaning Robot)
        equipmentItem.addMember(subEquipmentItem);
        subEquipmentItem.addGroupName(equipmentItem.getName());

        when(itemRegistryMock.getItem("TestHouse")).thenReturn(indoorLocationItem);
        when(itemRegistryMock.getItem("TestBathRoom")).thenReturn(bathroomLocationItem);
        when(itemRegistryMock.getItem("Test08")).thenReturn(equipmentItem);
        when(itemRegistryMock.getItem("TestTemperature")).thenReturn(temperaturePointItem);
        when(itemRegistryMock.getItem("TestHumidity")).thenReturn(humidityPointItem);

        new SemanticsActionService(itemRegistryMock);
    }

    @Test
    public void testGetLocation() {
        assertThat(Semantics.getLocation(indoorLocationItem), is(nullValue()));

        assertThat(Semantics.getLocation(bathroomLocationItem), is(indoorLocationItem));

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
        assertThat(Semantics.getEquipment(equipmentItem), is(nullValue()));

        assertThat(Semantics.getEquipment(subEquipmentItem), is(equipmentItem));

        assertThat(Semantics.getEquipment(temperaturePointItem), is(equipmentItem));

        assertNull(Semantics.getEquipment(humidityPointItem));
    }

    @Test
    public void testGetEquipmentType() {
        assertThat(Semantics.getEquipmentType(equipmentItem), is(CleaningRobot.class));

        assertThat(Semantics.getEquipmentType(temperaturePointItem), is(CleaningRobot.class));

        assertThat(Semantics.getEquipmentType(subEquipmentItem), is(Battery.class));

        assertNull(Semantics.getEquipmentType(humidityPointItem));
    }
}
