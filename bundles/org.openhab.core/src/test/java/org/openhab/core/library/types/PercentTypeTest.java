/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openhab.core.library.unit.Units;

/**
 * @author Kai Kreuzer - Initial contribution
 */
public class PercentTypeTest {

    @Test
    public void negativeNumber() {
        assertThrows(IllegalArgumentException.class, () -> new PercentType(-3));
    }

    @Test
    public void moreThan100() {
        assertThrows(IllegalArgumentException.class, () -> new PercentType("100.2"));
    }

    @Test
    public void doubleValue() {
        PercentType pt = new PercentType("0.0001");
        assertEquals("0.0001", pt.toString());
    }

    @Test
    public void intValue() {
        PercentType pt = new PercentType(100);
        assertEquals("100", pt.toString());
    }

    @Test
    public void testEquals() {
        PercentType pt1 = new PercentType(Integer.valueOf(100));
        PercentType pt2 = new PercentType("100.0");
        PercentType pt3 = new PercentType(0);
        PercentType pt4 = new PercentType(0);

        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertTrue(pt1.equals(pt2));
        assertTrue(pt3.equals(pt4));
        assertFalse(pt3.equals(pt1));
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new PercentType("100.0").as(OnOffType.class));
        assertEquals(OnOffType.ON, new PercentType("1.0").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new PercentType("0.0").as(OnOffType.class));
    }

    @Test
    public void testConversionToDecimalType() {
        assertEquals(new DecimalType("1.0"), new PercentType("100.0").as(DecimalType.class));
        assertEquals(new DecimalType("0.01"), new PercentType("1.0").as(DecimalType.class));
        assertEquals(DecimalType.ZERO, new PercentType("0.0").as(DecimalType.class));
    }

    @Test
    public void testConversionToQuantityType() {
        assertEquals(new QuantityType<>("100 %"), PercentType.HUNDRED.as(QuantityType.class));
        assertEquals(new QuantityType<>("1 one"),
                ((QuantityType<?>) PercentType.HUNDRED.as(QuantityType.class)).toUnit(Units.ONE));
    }

    @Test
    public void testConversionToOpenCloseType() {
        assertEquals(OpenClosedType.OPEN, new PercentType("100.0").as(OpenClosedType.class));
        assertEquals(OpenClosedType.CLOSED, new PercentType("0.0").as(OpenClosedType.class));
        assertNull(new PercentType("50.0").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToUpDownType() {
        assertEquals(UpDownType.UP, new PercentType("0.0").as(UpDownType.class));
        assertEquals(UpDownType.DOWN, new PercentType("100.0").as(UpDownType.class));
        assertNull(new PercentType("50.0").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToHSBType() {
        assertEquals(new HSBType("0,0,0"), new PercentType("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new PercentType("100.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new PercentType("50.0").as(HSBType.class));
    }
}
