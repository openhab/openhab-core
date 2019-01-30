/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.core.internal.items;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link ItemStateConverterImpl}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class ItemStateConverterImplTest {

    private ItemStateConverterImpl itemStateConverter;

    @Before
    public void setup() {
        itemStateConverter = new ItemStateConverterImpl();
    }

    @Test
    public void testNullState() {
        State undef = itemStateConverter.convertToAcceptedState(null, null);

        assertThat(undef, is(UnDefType.NULL));
    }

    @Test
    public void testNoConversion() {
        Item item = new NumberItem("number");
        State originalState = new DecimalType(12.34);
        State state = itemStateConverter.convertToAcceptedState(originalState, item);

        assertTrue(originalState == state);
    }

    @Test
    public void testStateConversion() {
        Item item = new NumberItem("number");
        State originalState = new PercentType("42");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new DecimalType("0.42")));
    }

    @Test
    public void numberItemWithoutDimensionShouldConvertToDecimalType() {
        Item item = new NumberItem("number");
        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new DecimalType("12.34")));
    }

    @Test
    public void numberItemWitDimensionShouldConvertToItemStateDescriptionUnit() {
        NumberItem item = mock(NumberItem.class);
        StateDescription stateDescription = mock(StateDescription.class);
        when(item.getStateDescription()).thenReturn(stateDescription);
        doReturn(Temperature.class).when(item).getDimension();
        when(stateDescription.getPattern()).thenReturn("%.1f K");

        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new QuantityType<>("285.49 K")));
    }

    @Test
    public void numberItemWitDimensionShouldConvertToLocaleBasedUnit() {
        NumberItem item = mock(NumberItem.class);
        doReturn(Temperature.class).when(item).getDimension();

        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Temperature.class)).thenReturn(ImperialUnits.FAHRENHEIT);
        itemStateConverter.setUnitProvider(unitProvider);

        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new QuantityType<>("54.212 °F")));
    }

    @Test
    public void numberItemShouldNotConvertUnitsWhereMeasurmentSystemEquals() {
        NumberItem item = mock(NumberItem.class);
        doReturn(Length.class).when(item).getDimension();

        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Length.class)).thenReturn(SIUnits.METRE);
        itemStateConverter.setUnitProvider(unitProvider);

        QuantityType<Length> originalState = new QuantityType<>("100 cm");

        @SuppressWarnings("unchecked")
        QuantityType<Length> convertedState = (QuantityType<Length>) itemStateConverter
                .convertToAcceptedState(originalState, item);

        assertThat(convertedState.getUnit(), is(originalState.getUnit()));
    }

}
