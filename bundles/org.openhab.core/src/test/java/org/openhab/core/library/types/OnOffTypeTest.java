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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Simon Kaufmann - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
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
        assertEquals(null, OnOffType.ON.as(PointType.class));
        assertEquals(null, OnOffType.OFF.as(PointType.class));
    }
}
