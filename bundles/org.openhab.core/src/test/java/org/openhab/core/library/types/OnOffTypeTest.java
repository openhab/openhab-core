/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Simon Kaufmann - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
@NonNullByDefault
public class OnOffTypeTest {

    @Test
    public void testConversionToPercentType() {
        assertEquals(PercentType.HUNDRED, OnOffType.ON.as(PercentType.class));
        assertEquals(PercentType.ZERO, OnOffType.OFF.as(PercentType.class));
    }

    @Test
    public void testConversionToDecimalType() {
        assertEquals(new DecimalType("1.0"), OnOffType.ON.as(DecimalType.class));
        assertEquals(DecimalType.ZERO, OnOffType.OFF.as(DecimalType.class));
    }

    @Test
    public void testConversionToHSBType() {
        assertEquals(HSBType.WHITE, OnOffType.ON.as(HSBType.class));
        assertEquals(HSBType.BLACK, OnOffType.OFF.as(HSBType.class));
    }

    @Test
    public void testConversionToPointType() {
        // should not be possible => null
        assertNull(OnOffType.ON.as(PointType.class));
        assertNull(OnOffType.OFF.as(PointType.class));
    }

    @Test
    public void testBooleanValue() {
        assertEquals(true, OnOffType.ON.booleanValue());
        assertEquals(false, OnOffType.OFF.booleanValue());
    }

    @Test
    public void testByteValue() {
        assertEquals((byte) 1, OnOffType.ON.byteValue());
        assertEquals((byte) 0, OnOffType.OFF.byteValue());
    }

    @Test
    public void testDoubleValue() {
        assertEquals(1.0, OnOffType.ON.doubleValue());
        assertEquals(0.0, OnOffType.OFF.doubleValue());
    }

    @Test
    public void testFloatValue() {
        assertEquals(1.0f, OnOffType.ON.floatValue());
        assertEquals(0.0f, OnOffType.OFF.floatValue());
    }

    @Test
    public void testIntValue() {
        assertEquals(1, OnOffType.ON.intValue());
        assertEquals(0, OnOffType.OFF.intValue());
    }

    @Test
    public void testLongValue() {
        assertEquals((long) 1, OnOffType.ON.longValue());
        assertEquals((long) 0, OnOffType.OFF.longValue());
    }

    @Test
    public void testShortValue() {
        assertEquals((short) 1, OnOffType.ON.shortValue());
        assertEquals((short) 0, OnOffType.OFF.shortValue());
    }
}
