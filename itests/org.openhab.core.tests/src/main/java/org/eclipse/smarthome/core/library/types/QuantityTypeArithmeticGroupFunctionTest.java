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
package org.eclipse.smarthome.core.library.types;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import tec.uom.se.unit.Units;

/**
 * @author Henning Treu
 */
public class QuantityTypeArithmeticGroupFunctionTest {

    private GroupFunction function;
    private Set<Item> items;

    @Mock
    UnitProvider unitProvider;

    @Before
    public void init() {
        initMocks(this);
        items = new LinkedHashSet<>();

        when(unitProvider.getUnit(Temperature.class)).thenReturn(Units.CELSIUS);
    }

    @Test
    public void testSumFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("89 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("122.41 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("234.95 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("192.2 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("395.56 K")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("234.95 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeIncompatibleUnits() {
        items = new LinkedHashSet<Item>(); // we need an ordered set to guarantee the Unit of the first entry
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<Temperature>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("23.54 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("200 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("55.33 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<Temperature>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("23.54 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("300 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("100 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<Pressure>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("23.54 °C"), state);
    }

    @Test
    public void testMinFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("100 °C"), state);
    }

    @Test
    public void testMinFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<Temperature>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<Temperature>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("294.15 K"), state);
    }

    @Test
    public void testMinFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<Temperature>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<Pressure>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Temperature>("23.54 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeWithGroups() {
        items.add(createNumberItem("TestItem1", Power.class, new QuantityType<Power>("5 W")));
        items.add(createGroupItem("TestGroup1", Power.class, new QuantityType<Power>("5 W")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Power.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<Power>("10 W"), state);
    }

    private NumberItem createNumberItem(String name, Class<? extends Quantity<?>> dimension, State state) {
        NumberItem item = new NumberItem(CoreItemFactory.NUMBER + ":" + dimension.getSimpleName(), name);
        item.setUnitProvider(unitProvider);
        item.setState(state);
        return item;
    }

    private GroupItem createGroupItem(String name, Class<? extends Quantity<?>> dimension, State state) {
        GroupItem item = new GroupItem(name,
                new NumberItem(CoreItemFactory.NUMBER + ":" + dimension.getSimpleName(), name));
        item.setUnitProvider(unitProvider);
        item.setState(state);
        return item;
    }

}
