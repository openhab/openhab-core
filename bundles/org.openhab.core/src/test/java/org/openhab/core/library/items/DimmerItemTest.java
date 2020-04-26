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

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;

/**
 * @author Chris Jackson - Initial contribution
 * @author Markus Rathgeb - Add more tests
 * @author Stefan Triller - tests for type conversions
 */
public class DimmerItemTest {

    private static DimmerItem createDimmerItem(final State state) {
        final DimmerItem item = new DimmerItem("Test");
        item.setState(state);
        return item;
    }

    private static BigDecimal getState(final Item item, Class<? extends State> typeClass) {
        final State state = item.getStateAs(typeClass);
        final String str = state.toString();
        final BigDecimal result = new BigDecimal(str);
        return result;
    }

    @Test
    public void getAsPercentFromPercent() {
        final BigDecimal origin = new BigDecimal(25);
        final DimmerItem item = createDimmerItem(new PercentType(origin));
        final BigDecimal result = getState(item, PercentType.class);
        assertEquals(origin.compareTo(result), 0);
    }

    @Test
    public void getAsPercentFromOn() {
        final DimmerItem item = createDimmerItem(OnOffType.ON);
        final BigDecimal result = getState(item, PercentType.class);
        assertEquals(new BigDecimal(100), result);
    }

    @Test
    public void getAsPercentFromOff() {
        final DimmerItem item = createDimmerItem(OnOffType.OFF);
        final BigDecimal result = getState(item, PercentType.class);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void getAsPercentFromHSB() {
        // HSBType is supported because it is a sub-type of PercentType
        HSBType origin = new HSBType("23,42,75");
        final DimmerItem item = createDimmerItem(origin);
        final BigDecimal result = getState(item, PercentType.class);
        assertEquals(origin.getBrightness().toBigDecimal(), result);
    }

    @Test
    public void testUndefType() {
        DimmerItem item = new DimmerItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        DimmerItem item = new DimmerItem("test");
        StateUtil.testAcceptedStates(item);
    }
}
