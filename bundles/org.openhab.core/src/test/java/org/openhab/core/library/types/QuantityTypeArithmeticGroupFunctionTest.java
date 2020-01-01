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
package org.openhab.core.library.types;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

import tec.uom.se.unit.Units;

/**
 * @author Henning Treu - Initial contribution
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
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("89 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("122.41 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("234.95 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("192.2 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("395.56 K")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("234.95 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeIncompatibleUnits() {
        items = new LinkedHashSet<>(); // we need an ordered set to guarantee the Unit of the first entry
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("200 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("55.33 °C"), state);
    }

    @Test
    public void testAvgFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("300 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("100 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @Test
    public void testMinFunctionQuantityType() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("100 °C"), state);
    }

    @Test
    public void testMaxFunctionQuantityTypeOnDimensionless() {
        items.add(createNumberItem("TestItem1", Dimensionless.class, new QuantityType<>("48 %")));
        items.add(createNumberItem("TestItem2", Dimensionless.class, new QuantityType<>("36 %")));
        items.add(createNumberItem("TestItem3", Dimensionless.class, new QuantityType<>("0 %")));
        items.add(createNumberItem("TestItem4", Dimensionless.class, new QuantityType<>("48 %")));
        items.add(createNumberItem("TestItem5", Dimensionless.class, new QuantityType<>("0 %")));
        items.add(createNumberItem("TestItem6", Dimensionless.class, new QuantityType<>("0 %")));

        function = new QuantityTypeArithmeticGroupFunction.Max(Dimensionless.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("48 %"), state);
    }

    @Test
    public void testMinFunctionQuantityTypeDifferentUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("294.15 K"), state);
    }

    @Test
    public void testMinFunctionQuantityTypeIncompatibleUnits() {
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @Test
    public void testSumFunctionQuantityTypeWithGroups() {
        items.add(createNumberItem("TestItem1", Power.class, new QuantityType<>("5 W")));
        items.add(createGroupItem("TestGroup1", Power.class, new QuantityType<>("5 W")));

        function = new QuantityTypeArithmeticGroupFunction.Sum(Power.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("10 W"), state);
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
