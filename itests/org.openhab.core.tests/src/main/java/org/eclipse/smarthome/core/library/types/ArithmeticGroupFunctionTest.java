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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas.Eichstaedt-Engelen
 */
public class ArithmeticGroupFunctionTest {

    private GroupFunction function;
    private Set<Item> items;

    @Before
    public void init() {
        items = new HashSet<Item>();
    }

    @Test
    public void testOrFunction() {
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testOrFunction_negative() {
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testOrFunction_justsOneItem() {
        items.add(new TestItem("TestItem1", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testOrFunction_differntTypes() {
        DimmerItem dimmer1 = new DimmerItem("TestDimmer1");
        dimmer1.setState(new PercentType("42"));
        DimmerItem dimmer2 = new DimmerItem("TestDimmer2");
        dimmer2.setState(new DecimalType("0"));
        SwitchItem switch1 = new SwitchItem("TestSwitch1");
        switch1.setState(OnOffType.ON);
        SwitchItem switch2 = new SwitchItem("TestSwitch2");
        switch2.setState(OnOffType.OFF);

        items.add(dimmer1);
        items.add(dimmer2);
        items.add(switch1);
        items.add(switch2);

        function = new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF);
        State state = function.calculate(items);
        State decimalState = function.getStateAs(items, DecimalType.class);

        assertEquals(OnOffType.ON, state);
        assertEquals(new DecimalType("2"), decimalState);
    }

    @Test
    public void testNOrFunction() {
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.NOr(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNOrFunction_negative() {
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.NOr(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testAndFunction() {
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));

        function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testAndFunction_negative() {
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testAndFunction_justsOneItem() {
        items.add(new TestItem("TestItem1", UnDefType.UNDEF));

        function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNAndFunction() {
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));

        function = new ArithmeticGroupFunction.NAnd(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNAndFunction_negative() {
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));

        function = new ArithmeticGroupFunction.NAnd(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testSumFunction() {
        items.add(new TestItem("TestItem1", new DecimalType("23.54")));
        items.add(new TestItem("TestItem2", UnDefType.NULL));
        items.add(new TestItem("TestItem3", new DecimalType("89")));
        items.add(new TestItem("TestItem4", UnDefType.UNDEF));
        items.add(new TestItem("TestItem5", new DecimalType("122.41")));

        function = new ArithmeticGroupFunction.Sum();
        State state = function.calculate(items);

        assertEquals(new DecimalType("234.95"), state);
    }

    @Test
    public void testCountFunction() {
        items.add(new TestItem("TestItem1", new StringType("hello world")));
        items.add(new TestItem("TestItem2", new StringType("world")));
        items.add(new TestItem("TestItem3", new StringType("foo bar")));

        function = new ArithmeticGroupFunction.Count(new StringType(".*world.*"));
        State state = function.calculate(items);

        assertEquals(new DecimalType("2"), state);
    }

    private class TestItem extends GenericItem {

        public TestItem(String name, State state) {
            super("Test", name);
            setState(state);
        }

        @Override
        public List<Class<? extends State>> getAcceptedDataTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends Command>> getAcceptedCommandTypes() {
            return Collections.emptyList();
        }

    }

}
