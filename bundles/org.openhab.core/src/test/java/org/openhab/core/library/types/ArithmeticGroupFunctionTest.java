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
package org.openhab.core.library.types;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 */
@NonNullByDefault
public class ArithmeticGroupFunctionTest {

    @Test
    public void testOrFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testOrFunctionNegative() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testOrFunctionJustsOneItem() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.Or(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testOrFunctionDifferentTypes() {
        DimmerItem dimmer1 = new DimmerItem("TestDimmer1");
        dimmer1.setState(new PercentType("42"));
        DimmerItem dimmer2 = new DimmerItem("TestDimmer2");
        dimmer2.setState(new DecimalType("0"));
        SwitchItem switch1 = new SwitchItem("TestSwitch1");
        switch1.setState(OnOffType.ON);
        SwitchItem switch2 = new SwitchItem("TestSwitch2");
        switch2.setState(OnOffType.OFF);

        Set<Item> items = new HashSet<>();
        items.add(dimmer1);
        items.add(dimmer2);
        items.add(switch1);
        items.add(switch2);

        GroupFunction function = new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF);
        State state = function.calculate(items);
        State decimalState = function.getStateAs(items, DecimalType.class);

        assertEquals(OnOffType.ON, state);
        assertEquals(new DecimalType("2"), decimalState);
    }

    @Test
    public void testNOrFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.NOr(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNOrFunctionNegative() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem4", OpenClosedType.CLOSED));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.NOr(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testAndFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));

        GroupFunction function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testAndFunctionNegative() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem4", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testAndFunctionJustsOneItem() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", UnDefType.UNDEF));

        GroupFunction function = new ArithmeticGroupFunction.And(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNAndFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.OPEN));

        GroupFunction function = new ArithmeticGroupFunction.NAnd(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.CLOSED, state);
    }

    @Test
    public void testNAndFunctionNegative() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem2", OpenClosedType.OPEN));
        items.add(new TestItem("TestItem3", OpenClosedType.CLOSED));

        GroupFunction function = new ArithmeticGroupFunction.NAnd(OpenClosedType.OPEN, OpenClosedType.CLOSED);
        State state = function.calculate(items);

        assertEquals(OpenClosedType.OPEN, state);
    }

    @Test
    public void testAvgFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DecimalType("23.54")));
        items.add(new TestItem("TestItem2", UnDefType.NULL));
        items.add(new TestItem("TestItem3", new DecimalType("89")));
        items.add(new TestItem("TestItem4", UnDefType.UNDEF));
        items.add(new TestItem("TestItem5", new DecimalType("122.41")));

        GroupFunction function = new ArithmeticGroupFunction.Avg();
        State state = function.calculate(items);

        assertThat(state, instanceOf(DecimalType.class));
        assertThat(((DecimalType) state).doubleValue(), is(closeTo(78.32, 0.01d)));
    }

    @Test
    public void testSumFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DecimalType("23.54")));
        items.add(new TestItem("TestItem2", UnDefType.NULL));
        items.add(new TestItem("TestItem3", new DecimalType("89")));
        items.add(new TestItem("TestItem4", UnDefType.UNDEF));
        items.add(new TestItem("TestItem5", new DecimalType("122.41")));

        GroupFunction function = new ArithmeticGroupFunction.Sum();
        State state = function.calculate(items);

        assertEquals(new DecimalType("234.95"), state);
    }

    @Test
    public void testMinFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DecimalType("23.54")));
        items.add(new TestItem("TestItem2", UnDefType.NULL));
        items.add(new TestItem("TestItem3", new DecimalType("89")));
        items.add(new TestItem("TestItem4", UnDefType.UNDEF));
        items.add(new TestItem("TestItem5", new DecimalType("122.41")));

        GroupFunction function = new ArithmeticGroupFunction.Min();
        State state = function.calculate(items);

        assertThat(state, is(new DecimalType("23.54")));
    }

    @Test
    public void testMaxFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DecimalType("23.54")));
        items.add(new TestItem("TestItem2", UnDefType.NULL));
        items.add(new TestItem("TestItem3", new DecimalType("89")));
        items.add(new TestItem("TestItem4", UnDefType.UNDEF));
        items.add(new TestItem("TestItem5", new DecimalType("122.41")));

        GroupFunction function = new ArithmeticGroupFunction.Max();
        State state = function.calculate(items);

        assertThat(state, is(new DecimalType("122.41")));
    }

    @Test
    public void testCountFunction() {
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new StringType("hello world")));
        items.add(new TestItem("TestItem2", new StringType("world")));
        items.add(new TestItem("TestItem3", new StringType("foo bar")));

        GroupFunction function = new ArithmeticGroupFunction.Count(new StringType(".*world.*"));
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
