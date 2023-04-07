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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateUpdatedEvent;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
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

    @BeforeEach
    public void setup() {
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null))
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%.1f " + UnitUtils.UNIT_PLACEHOLDER)
                        .build().toStateDescription());
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
    }

    @Test
    public void setDecimalType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State decimal = new DecimalType("23");
        item.setState(decimal);
        assertEquals(decimal, item.getState());
    }

    @Test
    public void setPercentType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State percent = new PercentType(50);
        item.setState(percent);
        assertEquals(percent, item.getState());
    }

    @Test
    public void setHSBType() {
        NumberItem item = new NumberItem(ITEM_NAME);
        State hsb = new HSBType("5,23,42");
        item.setState(hsb);
        assertEquals(hsb, item.getState());
    }

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
    public void testSetQuantityTypeAccepted() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setState(new QuantityType<>("20 °C"));

        assertThat(item.getState(), is(new QuantityType<>("20 °C")));
    }

    @Test
    public void testSetQuantityOnPlainNumberStripsUnit() {
        NumberItem item = new NumberItem(ITEM_NAME);
        item.setState(new QuantityType<>("20 °C"));

        assertThat(item.getState(), is(new DecimalType("20")));
    }

    @Test
    public void testSetQuantityTypeConverted() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setState(new QuantityType<>(68, ImperialUnits.FAHRENHEIT));

        assertThat(item.getState(), is(new QuantityType<>("20 °C")));
    }

    @Test
    public void testSetQuantityTypeUnconverted() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        item.setUnitProvider(unitProvider);
        item.setState(new QuantityType<>("10 A")); // should not be accepted as valid state

        assertThat(item.getState(), is(UnDefType.NULL));
    }

    @Test
    public void testCommandUnitIsPassedForDimensionItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        item.setUnitProvider(unitProvider);
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
    public void testCommandUnitIsStrippedForDimensionlessItem() {
        NumberItem item = new NumberItem("Number", ITEM_NAME);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        item.send(new QuantityType<>("15 °C"));

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(new DecimalType("15")));
    }

    @SuppressWarnings("null")
    @Test
    public void testStripUnitPlaceholderFromPlainNumberItem() {
        NumberItem item = new NumberItem("Number", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionServiceMock);

        assertThat(item.getStateDescription().getPattern(), is("%.1f"));
    }

    @SuppressWarnings("null")
    @Test
    public void testLeaveUnitPlaceholderOnDimensionNumberItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionServiceMock);

        assertThat(item.getStateDescription().getPattern(), is("%.1f " + UnitUtils.UNIT_PLACEHOLDER));
    }

    @SuppressWarnings("null")
    @Test
    public void testMiredToKelvin() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null)).thenReturn(
                StateDescriptionFragmentBuilder.create().withPattern("%.0f K").build().toStateDescription());
        item.setStateDescriptionService(stateDescriptionServiceMock);
        item.setState(new QuantityType<>("370 mired"));

        assertThat(item.getState().format("%.0f K"), is("2703 K"));
    }

    @SuppressWarnings("null")
    @Test
    public void testKelvinToMired() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null)).thenReturn(
                StateDescriptionFragmentBuilder.create().withPattern("%.0f mired").build().toStateDescription());
        item.setStateDescriptionService(stateDescriptionServiceMock);
        item.setState(new QuantityType<>("2700 K"));

        assertThat(item.getState().format("%.0f mired"), is("370 mired"));
    }

    @Test
    void testStateDescriptionUnitUsedWhenStateDescriptionPresent() {
        UnitProvider unitProviderMock = mock(UnitProvider.class);
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null)).thenReturn(
                StateDescriptionFragmentBuilder.create().withPattern("%.0f °F").build().toStateDescription());

        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionServiceMock);
        item.setUnitProvider(unitProviderMock);

        assertThat(item.getUnit(), is(ImperialUnits.FAHRENHEIT));

        item.setState(new QuantityType<>("429 °F"));
        assertThat(item.getState(), is(new QuantityType<>("429 °F")));

        item.setState(new QuantityType<>("165 °C"));
        assertThat(item.getState(), is(new QuantityType<>("329 °F")));
    }

    @Test
    void testPreservedWhenStateDescriptionContainsWildCard() {
        UnitProvider unitProviderMock = mock(UnitProvider.class);
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        when(stateDescriptionServiceMock.getStateDescription(ITEM_NAME, null))
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%.0f " + UnitUtils.UNIT_PLACEHOLDER)
                        .build().toStateDescription());

        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionServiceMock);
        item.setUnitProvider(unitProviderMock);

        assertThat(item.getUnit(), is(nullValue()));

        item.setState(new QuantityType<>("329 °F"));
        assertThat(item.getState(), is(new QuantityType<>("329 °F")));

        item.setState(new QuantityType<>("100 °C"));
        assertThat(item.getState(), is(new QuantityType<>("100 °C")));
    }

    @Test
    void testDefaultUnitUsedWhenStateDescriptionEmpty() {
        UnitProvider unitProviderMock = mock(UnitProvider.class);
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);

        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setUnitProvider(unitProviderMock);

        assertThat(item.getUnit(), is(SIUnits.CELSIUS));

        item.setState(new QuantityType<>("329 °F"));
        assertThat(item.getState(), is(new QuantityType<>("165 °C")));

        item.setState(new QuantityType<>("100 °C"));
        assertThat(item.getState(), is(new QuantityType<>("100 °C")));
    }

    @Test
    void testNoUnitWhenUnitPlaceholderUsed() {
        final UnitProvider unitProviderMock = mock(UnitProvider.class);
        when(unitProviderMock.getUnit(Energy.class)).thenReturn(Units.JOULE);

        final NumberItem item = new NumberItem("Number:Energy", ITEM_NAME);
        item.setUnitProvider(unitProviderMock);

        assertThat(item.getUnit(), is(Units.JOULE));

        item.setStateDescriptionService(stateDescriptionServiceMock);
        item.setState(new QuantityType<>("329 kWh"));

        assertThat(item.getState(), is(new QuantityType<>("329 kWh")));
        assertThat(item.getUnit(), is(nullValue()));
    }

    public void quantityTypeCorrectlySetWithDifferentUnit() {
        NumberItem numberItem = new NumberItem("Number:Temperature", ITEM_NAME);
        numberItem.setUnitProvider(unitProviderMock);
        numberItem.setEventPublisher(eventPublisherMock);
        numberItem.setState(new QuantityType<>("140 °F"));

        assertThat(numberItem.getState(), Matchers.is(new QuantityType<>("60 °C")));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(2)).post(captor.capture());

        List<Event> events = captor.getAllValues();
        assertThat(events, hasSize(2));

        assertThat(events.get(0), Matchers.is(instanceOf(ItemStateUpdatedEvent.class)));

        ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) events.get(0);
        assertThat(updatedEvent.getItemName(), Matchers.is(ITEM_NAME));
        assertThat(updatedEvent.getItemState(), Matchers.is(new QuantityType<>("60°C")));
    }

    @Test
    public void decimalTypeCorrectlySetWithUnit() {
        NumberItem numberItem = new NumberItem("Number:Temperature", ITEM_NAME);
        numberItem.setUnitProvider(unitProviderMock);
        numberItem.setEventPublisher(eventPublisherMock);
        numberItem.setState(new DecimalType(10));

        assertThat(numberItem.getState(), Matchers.is(new QuantityType<>("10 °C")));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(2)).post(captor.capture());

        List<Event> events = captor.getAllValues();
        assertThat(events, hasSize(2));

        assertThat(events.get(0), Matchers.is(instanceOf(ItemStateUpdatedEvent.class)));

        ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) events.get(0);
        assertThat(updatedEvent.getItemName(), Matchers.is(ITEM_NAME));
        assertThat(updatedEvent.getItemState(), Matchers.is(new QuantityType<>("10°C")));
    }
}
