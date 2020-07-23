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
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Simon Kaufmann - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
public class UpDownTypeTest {

    @Test
    public void testConversionToPercentType() {
        assertEquals(PercentType.ZERO, UpDownType.UP.as(PercentType.class));
        assertEquals(PercentType.HUNDRED, UpDownType.DOWN.as(PercentType.class));
    }

    @Test
    public void testConversionToDecimalType() {
        assertEquals(DecimalType.ZERO, UpDownType.UP.as(DecimalType.class));
        assertEquals(new DecimalType(1), UpDownType.DOWN.as(DecimalType.class));
    }

    @Test
    public void testConversionToPointType() {
        // should not be possible => null
        assertNull(UpDownType.UP.as(PointType.class));
        assertNull(UpDownType.DOWN.as(PointType.class));
    }
}
