/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * @author Robert Michalak - Initial contribution
 */
@NonNullByDefault
public class DateTimeGroupFunctionTest {

    @Test
    public void testLatestFunction() {
        Instant expectedDateTime = Instant.now();
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DateTimeType(expectedDateTime)));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", new DateTimeType(expectedDateTime.minus(10, ChronoUnit.DAYS))));
        items.add(new TestItem("TestItem4", new DateTimeType(expectedDateTime.minus(366, ChronoUnit.DAYS))));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));
        items.add(new TestItem("TestItem6", new DateTimeType(expectedDateTime.minusSeconds(1))));

        GroupFunction function = new DateTimeGroupFunction.Latest();
        State state = function.calculate(items);

        assertTrue(expectedDateTime.equals(((DateTimeType) state).getInstant()));
    }

    @Test
    public void testEarliestFunction() {
        Instant expectedDateTime = Instant.now();
        Set<Item> items = new HashSet<>();
        items.add(new TestItem("TestItem1", new DateTimeType(expectedDateTime)));
        items.add(new TestItem("TestItem2", UnDefType.UNDEF));
        items.add(new TestItem("TestItem3", new DateTimeType(expectedDateTime.plus(10, ChronoUnit.DAYS))));
        items.add(new TestItem("TestItem4", new DateTimeType(expectedDateTime.plus(366, ChronoUnit.DAYS))));
        items.add(new TestItem("TestItem5", UnDefType.UNDEF));
        items.add(new TestItem("TestItem6", new DateTimeType(expectedDateTime.plusSeconds(1))));

        GroupFunction function = new DateTimeGroupFunction.Earliest();
        State state = function.calculate(items);

        assertTrue(expectedDateTime.equals(((DateTimeType) state).getInstant()));
    }

    private static class TestItem extends GenericItem {

        public TestItem(String name, State state) {
            super("Test", name);
            setState(state);
        }

        @Override
        public List<Class<? extends State>> getAcceptedDataTypes() {
            return List.of();
        }

        @Override
        public List<Class<? extends Command>> getAcceptedCommandTypes() {
            return List.of();
        }
    }
}
