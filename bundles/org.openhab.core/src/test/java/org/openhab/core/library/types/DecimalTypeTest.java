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

import static org.junit.Assert.*;

import java.text.DecimalFormatSymbols;

import org.junit.Test;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
public class DecimalTypeTest {

    @Test
    public void testEquals() {
        DecimalType dt1 = new DecimalType("142.8");
        DecimalType dt2 = new DecimalType("142.8");
        DecimalType dt3 = new DecimalType("99.7");
        PercentType pt = new PercentType("99.7");

        assertEquals(true, dt1.equals(dt2));
        assertEquals(false, dt1.equals(dt3));
        assertEquals(true, dt3.equals(pt));
        assertEquals(false, dt1.equals(pt));
    }

    @Test
    public void testIntFormat() {
        DecimalType dt;

        // Basic test with an integer value.
        dt = new DecimalType("87");
        assertEquals("87", dt.format("%d"));

        // Again an integer value, but this time an "advanced" pattern.
        dt = new DecimalType("87");
        assertEquals(" 87", dt.format("%3d"));

        // Again an integer value, but this time an "advanced" pattern.
        dt = new DecimalType("87");
        assertEquals("0x57", dt.format("%#x"));

        // A float value cannot be converted into hex.
        dt = new DecimalType("87.5");
        try {
            dt.format("%x");
            fail();
        } catch (Exception e) {
            // That's what we expect.
        }

        // An integer (with different representation) with int conversion.
        dt = new DecimalType("11.0");
        assertEquals("11", dt.format("%d"));
    }

    @Test
    public void testFloatFormat() {
        DecimalType dt;

        // We know that DecimalType calls "String.format()" without a locale. So
        // we have to do the same thing here in order to get the right decimal
        // separator.
        final char sep = (new DecimalFormatSymbols().getDecimalSeparator());

        // A float value with float conversion.
        dt = new DecimalType("11.123");
        assertEquals("11" + sep + "1", dt.format("%.1f")); // "11.1"

        // An integer value with float conversion. This has to work.
        dt = new DecimalType("11");
        assertEquals("11" + sep + "0", dt.format("%.1f")); // "11.0"

        // An integer value with float conversion. This has to work.
        dt = new DecimalType("11.0");
        assertEquals("11" + sep + "0", dt.format("%.1f")); // "11.0"
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new DecimalType("100.0").as(OnOffType.class));
        assertEquals(OnOffType.ON, new DecimalType("1.0").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new DecimalType("0.0").as(OnOffType.class));
    }

    @Test
    public void testConversionToOpenCloseType() {
        assertEquals(OpenClosedType.OPEN, new DecimalType("1.0").as(OpenClosedType.class));
        assertEquals(OpenClosedType.CLOSED, new DecimalType("0.0").as(OpenClosedType.class));
        assertNull(new DecimalType("0.5").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToUpDownType() {
        assertEquals(UpDownType.UP, new DecimalType("0.0").as(UpDownType.class));
        assertEquals(UpDownType.DOWN, new DecimalType("1.0").as(UpDownType.class));
        assertNull(new DecimalType("0.5").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToHSBType() {
        assertEquals(new HSBType("0,0,0"), new DecimalType("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new DecimalType("1.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new DecimalType("0.5").as(HSBType.class));
    }

    @Test
    public void testConversionToDateTimeType() {
        assertEquals(new DateTimeType("2014-03-30T10:58:47+0000"),
                new DecimalType("1396177127").as(DateTimeType.class));
        assertEquals(new DateTimeType("1969-12-31T23:59:59+0000"), new DecimalType("-1").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:00+0000"), DecimalType.ZERO.as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1.0").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1.5").as(DateTimeType.class));
        assertEquals(new DateTimeType("1969-12-31T23:59:59+0000"), new DecimalType("-1.0").as(DateTimeType.class));
    }

    @Test
    public void testConversionToPercentType() {
        assertEquals(new PercentType(70), new DecimalType("0.7").as(PercentType.class));
    }

    @Test
    public void testConversionToPointType() {
        // should not be possible => null
        assertEquals(null, new DecimalType("0.23").as(PointType.class));
    }
}
