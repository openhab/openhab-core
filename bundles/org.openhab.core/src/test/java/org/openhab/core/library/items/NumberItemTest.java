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
package org.openhab.core.library.items;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Objects;

import javax.measure.Unit;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.internal.i18n.TestUnitProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class NumberItemTest {

    private static final String ITEM_NAME = "test";

    private @Mock @NonNullByDefault({}) StateDescriptionService stateDescriptionServiceMock;
    private @Mock @NonNullByDefault({}) UnitProvider unitProviderMock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;

    private final UnitProvider unitProvider = new TestUnitProvider();

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null))
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%.1f " + UnitUtils.UNIT_PLACEHOLDER)
                        .build().toStateDescription());
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
    }

    /*
     * State handling
     */
    @Test
    public void testUndefType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        NumberItem item = new NumberItem(ITEM_NAME);
        StateUtil.testAcceptedStates(item);
    }

    @Test
    public void testSetDecimalTypeToPlainItem() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State decimal = new DecimalType("23");
        item.setState(decimal);
        assertThat(item.getState(), is(decimal));
    }

    @Test
    public void testSetDecimalTypeToDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        State decimal = new DecimalType("23");
        item.setState(decimal);
        assertThat(item.getState(), is(new QuantityType<>("23 °C")));
    }

    @Test
    public void testSetQuantityTypeToPlainItem() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State quantity = new QuantityType<>("23 °C");
        item.setState(quantity);
        assertThat(item.getState(), is(new DecimalType("23")));
    }

    @Test
    public void testSetValidQuantityTypeWithSameUnitToDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        State quantity = new QuantityType<>("23 °C");
        item.setState(quantity);
        assertThat(item.getState(), is(quantity));
    }

    @Test
    public void testSetValidQuantityTypeWithDifferentUnitToDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        QuantityType<?> quantity = new QuantityType<>("23 K");
        item.setState(quantity);
        assertThat(item.getState(),
                is(quantity.toUnit(Objects.requireNonNull(unitProvider.getUnit(Temperature.class)))));
    }

    @Test
    public void testSetInvalidQuantityTypeToDimensionItemIsRejected() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        QuantityType<?> quantity = new QuantityType<>("23 N");
        item.setState(quantity);
        assertThat(item.getState(), is(UnDefType.NULL));
    }

    @Test
    public void testSetPercentType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State percent = new PercentType(50);
        item.setState(percent);
        assertThat(item.getState(), is(percent));
    }

    @Test
    public void testSetHSBType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State hsb = new HSBType("5,23,42");
        item.setState(hsb);
        assertThat(item.getState(), is(hsb));
    }

    /*
     * Command handling
     */
    @Test
    public void testValidCommandUnitIsPassedForDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        QuantityType<?> command = new QuantityType<>("15 °C");
        item.send(command);

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(command));
    }

    @Test
    public void testValidCommandDifferentUnitIsPassedForDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        QuantityType<?> command = new QuantityType<>("15 K");
        item.send(command);

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(command));
    }

    @Test
    public void testInvalidCommandUnitIsRejectedForDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        QuantityType<?> command = new QuantityType<>("15 N");
        item.send(command);

        verify(eventPublisher, never()).post(any());
    }

    @Test
    public void testCommandUnitIsStrippedForDimensionlessItem() {
        NumberItem item = new NumberItem(ITEM_NAME);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        item.send(new QuantityType<>("15 °C"));

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(new DecimalType("15")));
    }

    /*
     * + State description handling
     */
    @SuppressWarnings("null")
    @Test
    public void testStripUnitPlaceholderInStateDescriptionFromPlainNumberItem() {
        NumberItem item = new NumberItem(ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionServiceMock);

        assertThat(item.getStateDescription().getPattern(), is("%.1f"));
    }

    @SuppressWarnings("null")
    @Test
    public void testLeaveUnitPlaceholderInStateDescriptionOnDimensionNumberItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        item.setStateDescriptionService(stateDescriptionServiceMock);

        assertThat(item.getStateDescription().getPattern(), is("%.1f " + UnitUtils.UNIT_PLACEHOLDER));
    }

    /*
     * Unit / metadata handling
     */
    @Test
    void testSystemDefaultUnitIsUsedWithoutMetadata() {
        final NumberItem item = new NumberItem("Number:Mass", ITEM_NAME, unitProvider);
        assertThat(item.getUnit(), is(unitProvider.getUnit(Mass.class)));
    }

    @Test
    void testMetadataUnitLifecycleIsObserved() {
        final NumberItem item = new NumberItem("Number:Mass", ITEM_NAME, unitProvider);

        Metadata initialMetadata = getUnitMetadata(MetricPrefix.MEGA(SIUnits.GRAM));
        item.addedMetadata(initialMetadata);
        assertThat(item.getUnit(), is(MetricPrefix.MEGA(SIUnits.GRAM)));

        Metadata updatedMetadata = getUnitMetadata(MetricPrefix.MILLI(SIUnits.GRAM));
        item.updatedMetadata(initialMetadata, updatedMetadata);
        assertThat(item.getUnit(), is(MetricPrefix.MILLI(SIUnits.GRAM)));

        item.removedMetadata(updatedMetadata);
        assertThat(item.getUnit(), is(unitProvider.getUnit(Mass.class)));
    }

    @Test
    void testInvalidMetadataUnitIsRejected() {
        final NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        item.addedMetadata(getUnitMetadata(MetricPrefix.MEGA(SIUnits.GRAM)));
        assertThat(item.getUnit(), is(unitProvider.getUnit(Temperature.class)));
    }

    /*
     * Other tests
     */

    @SuppressWarnings("null")
    @Test
    public void testMiredToKelvin() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        item.addedMetadata(getUnitMetadata(Units.KELVIN));
        item.setState(new QuantityType<>("370 mired"));

        assertThat(item.getState().format("%.0f K"), is("2703 K"));
    }

    @SuppressWarnings("null")
    @Test
    public void testKelvinToMired() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME, unitProvider);
        item.addedMetadata(getUnitMetadata(Units.MIRED));

        item.setState(new QuantityType<>("2700 K"));

        assertThat(item.getState().format("%.0f mired"), is("370 mired"));
    }

    private Metadata getUnitMetadata(Unit<?> unit) {
        MetadataKey key = new MetadataKey(NumberItem.UNIT_METADATA_NAMESPACE, ITEM_NAME);
        return new Metadata(key, unit.toString(), null);
    }
}
