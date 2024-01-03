/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Chris Jackson - Initial contribution
 * @author Stefan Triller - More tests for type conversions
 */
@NonNullByDefault
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
    public void testFormat() {
        HSBType hsb = new HSBType("316,69,47");

        assertEquals("color 316,69,47", hsb.format("color %hsb%"));
        assertEquals("color 120,37,98", hsb.format("color %rgb%"));
        assertEquals("color 316,69,47", hsb.format("color %s"));
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

    private void compareHsbToRgbValues(String hsbValues, int red, int green, int blue) {
        HSBType hsb = new HSBType(hsbValues);
        HSBType hsbRgb = HSBType.fromRGB(red, green, blue);

        assertEquals(hsb.getHue().doubleValue(), hsbRgb.getHue().doubleValue(), 0.5);
        assertEquals(hsb.getSaturation().doubleValue(), hsbRgb.getSaturation().doubleValue(), 0.5);
        assertEquals(hsb.getBrightness().doubleValue(), hsbRgb.getBrightness().doubleValue(), 0.5);
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
        compareRgbToHsbValues("229,37,62", 99, 110, 158); // blueish
        compareRgbToHsbValues("316,69,47", 119, 37, 97); // purple
    }

    private void compareRgbToHsbValues(String hsbValues, int red, int green, int blue) {
        HSBType hsb = new HSBType(hsbValues);
        HSBType hsbRgb = HSBType.fromRGB(red, green, blue);

        assertEquals(hsb.getHue().doubleValue(), hsbRgb.getHue().doubleValue(), 0.5);
        assertEquals(hsb.getSaturation().doubleValue(), hsbRgb.getSaturation().doubleValue(), 0.5);
        assertEquals(hsb.getBrightness().doubleValue(), hsbRgb.getBrightness().doubleValue(), 0.5);
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
        assertNull(new HSBType("100,100,100").as(PointType.class));
    }

    @Test
    public void testConversionToXY() {
        HSBType hsb = new HSBType("220,90,50");
        PercentType[] xy = hsb.toXY();
        assertEquals(14.65, xy[0].doubleValue(), 0.01);
        assertEquals(11.56, xy[1].doubleValue(), 0.01);
    }

    @Test
    public void testConstructorWithString1() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType(""));
    }

    @Test
    public void testConstructorWithString2() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType("1,2"));
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

    @Test
    public void testConstructorWithIllegalHueValue() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType("-13,85,51"));
    }

    @Test
    public void testConstructorWithIllegalHueValue2() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType("360,85,51"));
    }

    @Test
    public void testConstructorWithIllegalSaturationValue() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType("5,-85,51"));
    }

    @Test
    public void testConstructorWithIllegalBrightnessValue() {
        assertThrows(IllegalArgumentException.class, () -> new HSBType("5,85,151"));
    }

    @Test
    public void testCloseTo() {
        HSBType hsb1 = new HSBType("5,85,11");
        HSBType hsb2 = new HSBType("4,84,12");
        HSBType hsb3 = new HSBType("1,8,99");

        assertThrows(IllegalArgumentException.class, () -> hsb1.closeTo(hsb2, 0.0));
        assertThrows(IllegalArgumentException.class, () -> hsb1.closeTo(hsb2, 1.1));
        assertDoesNotThrow(() -> hsb1.closeTo(hsb2, 0.1));

        assertTrue(hsb1.closeTo(hsb2, 0.01));
        assertFalse(hsb1.closeTo(hsb3, 0.01));
        assertTrue(hsb1.closeTo(hsb3, 0.5));
    }
}
