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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class PointTypeTest {

    @Test
    public void testConstructorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new PointType(""));
    }

    @Test
    public void testConstructorBadlyFormated() {
        assertThrows(IllegalArgumentException.class, () -> new PointType("2"));
    }

    @Test
    public void testConstructorBadlyFormated2() {
        assertThrows(IllegalArgumentException.class, () -> new PointType("2,"));
    }

    @Test
    public void testConstructorBadlyFormated3() {
        assertThrows(IllegalArgumentException.class, () -> new PointType("2,3,4,5"));
    }

    @Test
    public void testConstructorTrim1() {
        PointType pt = new PointType("1,2,3");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
        assertThat(pt.getAltitude(), is(new DecimalType(3)));
    }

    @Test
    public void testConstructorTrim2() {
        PointType pt = new PointType("1, 2, 3");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
        assertThat(pt.getAltitude(), is(new DecimalType(3)));
    }

    @Test
    public void testConstructorTrim3() {
        PointType pt = new PointType("1, 2");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
    }

    @Test
    public void testConstructorTrim4() {
        PointType pt = new PointType("1 , 2 , 3");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
        assertThat(pt.getAltitude(), is(new DecimalType(3)));
    }

    @Test
    public void testConstructorTrim5() {
        PointType pt = new PointType("1 , 2");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
    }

    @Test
    public void testConstructorTrim6() {
        PointType pt = new PointType("  1 ,   2   ,  3   ");
        assertNotNull(pt);
        assertThat(pt.getLatitude(), is(new DecimalType(1)));
        assertThat(pt.getLongitude(), is(new DecimalType(2)));
        assertThat(pt.getAltitude(), is(new DecimalType(3)));
    }

    @Test
    public void testDefaultConstructor() {
        // Ensure presence of default constructor
        PointType middleOfTheOcean = new PointType();
        assertEquals(0, middleOfTheOcean.getLongitude().doubleValue(), 0.0000001);
        assertEquals(0, middleOfTheOcean.getLatitude().doubleValue(), 0.0000001);
        assertEquals(0, middleOfTheOcean.getAltitude().doubleValue(), 0.0000001);
    }

    @Test
    public void testGravity() {
        PointType pointParis = PointType.valueOf("48.8566140,2.3522219");

        assertEquals(2.3522219, pointParis.getLongitude().doubleValue(), 0.0000001);
        assertEquals(48.856614, pointParis.getLatitude().doubleValue(), 0.0000001);

        double gravParis = pointParis.getGravity().doubleValue();
        assertEquals(gravParis, 9.809, 0.001);
    }

    @Test
    public void testCanonicalization() {
        PointType point3 = PointType.valueOf("-100,200");
        double lat3 = point3.getLatitude().doubleValue();
        double lon3 = point3.getLongitude().doubleValue();
        assertTrue(lat3 > -90);
        assertTrue(lat3 < 90);
        assertTrue(lon3 < 180);
        assertTrue(lon3 > -180);
    }

    @Test
    public void testConstructorToString() {
        PointType pointTest1 = new PointType("48.8566140,2.3522219,118");
        PointType pointTest2 = new PointType(pointTest1.toString());
        assertEquals(pointTest1.getAltitude().longValue(), pointTest2.getAltitude().longValue());
        assertEquals(pointTest1.getLatitude().longValue(), pointTest2.getLatitude().longValue());
        assertEquals(pointTest1.getLongitude().longValue(), pointTest2.getLongitude().longValue());

        // Ensure that constructor and toString are consistent
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=467612#c17
        PointType point3 = PointType.valueOf("-100,200");
        assertTrue(point3.equals(PointType.valueOf(point3.toString())));
    }

    @Test
    public void testDistance() {
        PointType pointParis = PointType.valueOf("48.8566140,2.3522219");
        PointType greenwich = PointType.valueOf("51.477338,0.0");

        assertEquals(336474.0, pointParis.distanceFrom(greenwich).doubleValue(), 1);
    }

    @Test
    public void testBearing() {
        PointType middleOfTheOcean = PointType.valueOf("0.0,0.0");
        PointType greenwich = PointType.valueOf("51.477338,0.0");
        PointType mudchute = PointType.valueOf("51.492148,-0.012126");

        assertEquals(0.0, middleOfTheOcean.bearingTo(greenwich).doubleValue());
        assertEquals(180, greenwich.bearingTo(middleOfTheOcean).doubleValue());
        assertEquals(344.0, greenwich.bearingTo(mudchute).doubleValue(), 1);
        assertEquals(164.0, mudchute.bearingTo(greenwich).doubleValue(), 1);
    }
}
