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

import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Robert Michalak
 */
public class DateTimeGroupFunctionTest {

    private GroupFunction function;
    private Set<Item> items;

    @Before
    public void init() {
        items = new HashSet<Item>();
    }

    @Test
    public void testLatestFunction() {

        ZonedDateTime expectedDateTime = ZonedDateTime.now();
        items.add(new TestItem("TestItem1", new DateTimeType(expectedDateTime)));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", new DateTimeType(expectedDateTime.minusDays(10))));
        items.add(new TestItem("TestItem4", new DateTimeType(expectedDateTime.minusYears(1))));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));
        items.add(new TestItem("TestItem6", new DateTimeType(expectedDateTime.minusSeconds(1))));

        function = new DateTimeGroupFunction.Latest();
        State state = function.calculate(items);

        assertTrue(expectedDateTime.isEqual(((DateTimeType) state).getZonedDateTime()));
    }

    @Test
    public void testEarliestFunction() {

        ZonedDateTime expectedDateTime = ZonedDateTime.now();
        items.add(new TestItem("TestItem1", new DateTimeType(expectedDateTime)));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", new DateTimeType(expectedDateTime.plusDays(10))));
        items.add(new TestItem("TestItem4", new DateTimeType(expectedDateTime.plusYears(1))));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));
        items.add(new TestItem("TestItem6", new DateTimeType(expectedDateTime.plusSeconds(1))));

        function = new DateTimeGroupFunction.Earliest();
        State state = function.calculate(items);

        assertTrue(expectedDateTime.isEqual(((DateTimeType) state).getZonedDateTime()));
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
