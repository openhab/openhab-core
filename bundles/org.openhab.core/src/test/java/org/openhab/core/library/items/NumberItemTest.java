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
package org.openhab.core.library.items;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.measure.quantity.Temperature;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
public class NumberItemTest {

    private static final String ITEM_NAME = "test";

    @Mock
    private StateDescriptionService stateDescriptionService;

    @Before
    public void setup() {
        initMocks(this);

        when(stateDescriptionService.getStateDescription(ITEM_NAME, null)).thenReturn(StateDescriptionFragmentBuilder
                .create().withPattern("%.1f " + UnitUtils.UNIT_PLACEHOLDER).build().toStateDescription());
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

    @SuppressWarnings("null")
    @Test
    public void testStripUnitPlaceholderFromPlainNumberItem() {
        NumberItem item = new NumberItem("Number", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionService);

        assertThat(item.getStateDescription().getPattern(), is("%.1f"));
    }

    @SuppressWarnings("null")
    @Test
    public void testLeaveUnitPlaceholderOnDimensionNumberItem() {
        NumberItem item = new NumberItem("Number:Temperature", ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionService);

        assertThat(item.getStateDescription().getPattern(), is("%.1f " + UnitUtils.UNIT_PLACEHOLDER));
    }

}
