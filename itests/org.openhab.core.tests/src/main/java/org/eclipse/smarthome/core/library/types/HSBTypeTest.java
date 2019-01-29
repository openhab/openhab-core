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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

/**
 *
 * @author Chris Jackson - added fromRGB() test
 * @author Stefan Triller - more tests for type conversions
 *
 */
public class HSBTypeTest {

    @Test
    public void testEquals() {
        HSBType hsb1 = new HSBType("53,86,1");
        HSBType hsb2 = new HSBType("53,86,1");
        assertTrue(hsb1.equals(hsb2));

        hsb1 = new HSBType("0,0,0");
        hsb2 = new HSBType("0,0,0");
        assertTrue(hsb1.equals(hsb2));
    }

    @Test
    public void testHsbToRgbConversion() {
        compareHsbToRgbValues("0,100,100", 255, 0, 0); // red
        compareHsbToRgbValues("0,0,0", 0, 0, 0); // black
        compareHsbToRgbValues("0,0,100", 255, 255, 255); // white
        compareHsbToRgbValues("120,100,100", 0, 255, 0); // green
        compareHsbToRgbValues("240,100,100", 0, 0, 255); // blue
        compareHsbToRgbValues("229,37,62", 99, 110, 158); // blueish
        compareHsbToRgbValues("316,69,47", 119, 37, 97); // purple
        compareHsbToRgbValues("60,60,60", 153, 153, 61); // green
        compareHsbToRgbValues("300,100,40", 102, 0, 102);
    }

    private int convertPercentToByte(PercentType percent) {
        return percent.value.multiply(BigDecimal.valueOf(255))
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP).intValue();
    }

    private void compareHsbToRgbValues(String hsbValues, int red, int green, int blue) {
        HSBType hsb = new HSBType(hsbValues);

        assertEquals(red, convertPercentToByte(hsb.getRed()));
        assertEquals(green, convertPercentToByte(hsb.getGreen()));
        assertEquals(blue, convertPercentToByte(hsb.getBlue()));
    }

    @Test
    public void testRgbToHsbConversion() {
        compareRgbToHsbValues("0,100,100", 255, 0, 0); // red
        compareRgbToHsbValues("0,0,0", 0, 0, 0); // black
        compareRgbToHsbValues("0,0,100", 255, 255, 255); // white
        compareRgbToHsbValues("120,100,100", 0, 255, 0); // green
        compareRgbToHsbValues("240,100,100", 0, 0, 255); // blue
        compareRgbToHsbValues("60,60,60", 153, 153, 61); // green
        compareRgbToHsbValues("300,100,40", 102, 0, 102);
        compareRgbToHsbValues("228,37,61", 99, 110, 158); // blueish
        compareRgbToHsbValues("316,68,46", 119, 37, 97); // purple
    }

    private void compareRgbToHsbValues(String hsbValues, int red, int green, int blue) {
        HSBType hsb = new HSBType(hsbValues);
        HSBType hsbRgb = HSBType.fromRGB(red, green, blue);

        assertEquals(hsb.getHue(), hsbRgb.getHue());
        assertEquals(hsb.getSaturation(), hsbRgb.getSaturation());
        assertEquals(hsb.getBrightness(), hsbRgb.getBrightness());
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new HSBType("100,100,100").as(OnOffType.class));
        assertEquals(OnOffType.ON, new HSBType("100,100,1").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new HSBType("100,100,0").as(OnOffType.class));
    }

    @Test
    public void testConversionToDecimalType() {
        assertEquals(new DecimalType("1.0"), new HSBType("100,100,100").as(DecimalType.class));
        assertEquals(new DecimalType("0.01"), new HSBType("100,100,1").as(DecimalType.class));
        assertEquals(DecimalType.ZERO, new HSBType("100,100,0").as(DecimalType.class));
    }

    @Test
    public void testConversionToPercentType() {
        assertEquals(PercentType.HUNDRED, new HSBType("100,100,100").as(PercentType.class));
        assertEquals(new PercentType("1"), new HSBType("100,100,1").as(PercentType.class));
        assertEquals(PercentType.ZERO, new HSBType("100,100,0").as(PercentType.class));
    }

    @Test
    public void testConversionToPointType() {
        // should not be possible => null
        assertEquals(null, new HSBType("100,100,100").as(PointType.class));
    }

    @Test
    public void testConversionToXY() {
        HSBType hsb = new HSBType("220,90,50");
        PercentType[] xy = hsb.toXY();
        assertEquals(new PercentType("16.969364"), xy[0]);
        assertEquals(new PercentType("12.379659"), xy[1]);
    }

    @Test
    public void testCreateFromXY() {
        HSBType hsb = HSBType.fromXY(5f, 3f);
        assertEquals(new HSBType("11,100,100"), hsb);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithString1() {
        new HSBType("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithString2() {
        new HSBType("1,2");
    }

    @Test
    public void testConstructorWithString3() {
        HSBType hsb = new HSBType("1,2,3");
        assertNotNull(hsb);
        assertThat(hsb.getHue(), is(new DecimalType(1)));
        assertThat(hsb.getSaturation(), is(new PercentType(2)));
        assertThat(hsb.getBrightness(), is(new PercentType(3)));
    }

    @Test
    public void testConstructorWithString4() {
        HSBType hsb = new HSBType("1, 2, 3");
        assertNotNull(hsb);
        assertThat(hsb.getHue(), is(new DecimalType(1)));
        assertThat(hsb.getSaturation(), is(new PercentType(2)));
        assertThat(hsb.getBrightness(), is(new PercentType(3)));
    }

    @Test
    public void testConstructorWithString5() {
        HSBType hsb = new HSBType("  1,    2, 3    ");
        assertNotNull(hsb);
        assertThat(hsb.getHue(), is(new DecimalType(1)));
        assertThat(hsb.getSaturation(), is(new PercentType(2)));
        assertThat(hsb.getBrightness(), is(new PercentType(3)));
    }

    @Test
    public void testConstructorWithString6() {
        HSBType hsb = new HSBType("1  , 2  , 3   ");
        assertNotNull(hsb);
        assertThat(hsb.getHue(), is(new DecimalType(1)));
        assertThat(hsb.getSaturation(), is(new PercentType(2)));
        assertThat(hsb.getBrightness(), is(new PercentType(3)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithIllegalHueValue() {
        new HSBType("-13,85,51");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithIllegalHueValue2() {
        new HSBType("360,85,51");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithIllegalSaturationValue() {
        new HSBType("5,-85,51");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithIllegalBrightnessValue() {
        new HSBType("5,85,151");
    }
}
