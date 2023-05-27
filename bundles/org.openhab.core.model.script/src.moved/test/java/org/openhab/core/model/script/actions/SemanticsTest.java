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
package org.openhab.core.model.script.actions;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

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
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.Tag;
import org.openhab.core.semantics.internal.SemanticTagRegistryImpl;
import org.openhab.core.semantics.model.DefaultSemanticTagProvider;

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
    private @Mock @NonNullByDefault({}) ManagedSemanticTagProvider managedSemanticTagProviderMock;

    private @NonNullByDefault({}) GroupItem indoorLocationItem;
    private @NonNullByDefault({}) GroupItem bathroomLocationItem;
    private @NonNullByDefault({}) GroupItem equipmentItem;
    private @NonNullByDefault({}) GenericItem temperaturePointItem;
    private @NonNullByDefault({}) GenericItem humidityPointItem;
    private @NonNullByDefault({}) GenericItem subEquipmentItem;

    private @NonNullByDefault({}) Class<? extends Tag> indoorTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> bathroomTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> cleaningRobotTagClass;
    private @NonNullByDefault({}) Class<? extends Tag> batteryTagClass;

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

        when(managedSemanticTagProviderMock.getAll()).thenReturn(List.of());
        SemanticTagRegistryImpl semanticTagRegistryImpl = new SemanticTagRegistryImpl(new DefaultSemanticTagProvider(),
                managedSemanticTagProviderMock);

        indoorTagClass = semanticTagRegistryImpl.getTagClassById("Location_Indoor");
        bathroomTagClass = semanticTagRegistryImpl.getTagClassById("Location_Indoor_Room_Bathroom");
        cleaningRobotTagClass = semanticTagRegistryImpl.getTagClassById("Equipment_CleaningRobot");
        batteryTagClass = semanticTagRegistryImpl.getTagClassById("Equipment_Battery");

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
        assertThat(Semantics.getLocationType(indoorLocationItem), is(indoorTagClass));

        assertThat(Semantics.getLocationType(bathroomLocationItem), is(bathroomTagClass));

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
        assertThat(Semantics.getEquipmentType(equipmentItem), is(cleaningRobotTagClass));

        assertThat(Semantics.getEquipmentType(temperaturePointItem), is(cleaningRobotTagClass));

        assertThat(Semantics.getEquipmentType(subEquipmentItem), is(batteryTagClass));

        assertNull(Semantics.getEquipmentType(humidityPointItem));
    }
}
