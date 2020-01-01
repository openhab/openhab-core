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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Gaël L'hopital - Initial contribution
 */
public class PointTypeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmpty() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated2() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2,");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated3() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2,3,4,5");
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
    public void testDistance() {
        // Ensure presence of default constructor
        PointType middleOfTheOcean = new PointType();
        assertEquals(0, middleOfTheOcean.getLongitude().doubleValue(), 0.0000001);
        assertEquals(0, middleOfTheOcean.getLatitude().doubleValue(), 0.0000001);
        assertEquals(0, middleOfTheOcean.getAltitude().doubleValue(), 0.0000001);

        PointType pointParis = PointType.valueOf("48.8566140,2.3522219");

        assertEquals(2.3522219, pointParis.getLongitude().doubleValue(), 0.0000001);
        assertEquals(48.856614, pointParis.getLatitude().doubleValue(), 0.0000001);

        double gravParis = pointParis.getGravity().doubleValue();
        assertEquals(gravParis, 9.809, 0.001);

        // Check canonization of position
        PointType point3 = PointType.valueOf("-100,200");
        double lat3 = point3.getLatitude().doubleValue();
        double lon3 = point3.getLongitude().doubleValue();
        assertTrue(lat3 > -90);
        assertTrue(lat3 < 90);
        assertTrue(lon3 < 180);
        assertTrue(lon3 > -180);

        PointType pointTest1 = new PointType("48.8566140,2.3522219,118");
        PointType pointTest2 = new PointType(pointTest1.toString());
        assertEquals(pointTest1.getAltitude().longValue(), pointTest2.getAltitude().longValue());
        assertEquals(pointTest1.getLatitude().longValue(), pointTest2.getLatitude().longValue());
        assertEquals(pointTest1.getLongitude().longValue(), pointTest2.getLongitude().longValue());

        // Ensure that constructor and toString are consistent
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=467612#c17
        assertTrue(point3.equals(PointType.valueOf(point3.toString())));

    }

}
