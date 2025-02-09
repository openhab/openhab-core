/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;

/**
 * Tests for {@link QuantityType#toInvertibleUnit}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class QuantityTypeToInvertibleUnitTest {

    /**
     * Test conversion some other UoM values to Mirek
     */
    @Test
    void testConversionsToMirek() {
        // pass cases
        QuantityType<?> v1 = QuantityType.valueOf(2000, Units.KELVIN).toInvertibleUnit(Units.MIRED);
        QuantityType<?> v2 = QuantityType.valueOf(1726.85, SIUnits.CELSIUS).toInvertibleUnit(Units.MIRED);
        QuantityType<?> v3 = QuantityType.valueOf(3140.33, ImperialUnits.FAHRENHEIT).toInvertibleUnit(Units.MIRED);

        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);

        assertEquals(500, v1.doubleValue(), 0.01);
        assertEquals(500, v2.doubleValue(), 0.01);
        assertEquals(500, v3.doubleValue(), 0.01);

        // fail case
        assertNull(QuantityType.valueOf(500, SIUnits.METRE).toInvertibleUnit(Units.MIRED));
    }

    /**
     * Test conversions from Mirek to other UoM values
     */
    @Test
    void testConversionsFromMirek() {
        // pass cases
        QuantityType<?> m = QuantityType.valueOf(500, Units.MIRED);

        QuantityType<?> v1 = m.toInvertibleUnit(Units.KELVIN);
        QuantityType<?> v2 = m.toInvertibleUnit(SIUnits.CELSIUS);
        QuantityType<?> v3 = m.toInvertibleUnit(ImperialUnits.FAHRENHEIT);

        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);

        assertEquals(2000, v1.doubleValue(), 0.01);
        assertEquals(1726.85, v2.doubleValue(), 0.01);
        assertEquals(3140.33, v3.doubleValue(), 0.01);

        // fail case
        assertNull(m.toInvertibleUnit(SIUnits.METRE));
    }

    /**
     * Test conversions from UoM values to themselves
     */
    @Test
    void testConversionsToSelf() {
        // pass cases
        QuantityType<?> v1 = QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(Units.MIRED);
        QuantityType<?> v2 = QuantityType.valueOf(2000, Units.KELVIN).toInvertibleUnit(Units.KELVIN);

        assertNotNull(v1);
        assertNotNull(v2);

        assertEquals(500, v1.doubleValue(), 0.01);
        assertEquals(2000, v2.doubleValue(), 0.01);
    }

    /**
     * Test conversion Ohm to/from Siemens
     */
    @Test
    void testOhmAndSiemensConversions() {
        // pass cases
        QuantityType<?> v1 = QuantityType.valueOf(10, Units.OHM).toInvertibleUnit(Units.SIEMENS);
        QuantityType<?> v2 = QuantityType.valueOf(0.1, Units.SIEMENS).toInvertibleUnit(Units.OHM);

        assertNotNull(v1);
        assertNotNull(v2);

        assertEquals(0.1, v1.doubleValue(), 0.01);
        assertEquals(10, v2.doubleValue(), 0.01);

        // fail cases
        assertNull(v1.toInvertibleUnit(SIUnits.METRE));
        assertNull(v2.toInvertibleUnit(SIUnits.METRE));
        assertNull(QuantityType.valueOf(5, Units.OHM).toInvertibleUnit(SIUnits.METRE));
    }

    /**
     * Test time and frequency conversion
     */
    @Test
    void testHertzSecondConversions() {
        // pass cases
        QuantityType<?> v1 = QuantityType.valueOf(10, Units.HERTZ).toInvertibleUnit(Units.SECOND);
        QuantityType<?> v2 = QuantityType.valueOf(0.1, Units.SECOND).toInvertibleUnit(Units.HERTZ);

        assertNotNull(v1);
        assertNotNull(v2);

        assertEquals(0.1, v1.doubleValue(), 0.01);
        assertEquals(10, v2.doubleValue(), 0.01);

        // fail cases
        assertNull(v1.toInvertibleUnit(SIUnits.METRE));
        assertNull(v2.toInvertibleUnit(SIUnits.METRE));
        assertNull(QuantityType.valueOf(5, Units.HERTZ).toInvertibleUnit(SIUnits.METRE));
    }

    /**
     * Test dimensionless conversion
     */
    @Test
    void testDimensionlessConversion() {
        assertNotNull(QuantityType.valueOf(100, Units.ONE).toInvertibleUnit(Units.ONE));
    }

    /**
     * Some addons mistakenly call {@link QuantityType#toInvertibleUnit} instead of {@link QuantityType#toUnit}. The
     * good news is that when the target unit is not an inversion then the former method falls through to the latter.
     * However for good hygiene we should test that such calls do indeed also return the proper results.
     */
    @Test
    void testNonInvertingConversions() {
        QuantityType<?> v1 = QuantityType.valueOf(600, Units.SECOND).toInvertibleUnit(Units.MINUTE);
        QuantityType<?> v2 = QuantityType.valueOf(100, ImperialUnits.FAHRENHEIT).toInvertibleUnit(SIUnits.CELSIUS);
        QuantityType<?> v3 = QuantityType.valueOf(100, ImperialUnits.FOOT).toInvertibleUnit(SIUnits.METRE);

        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);

        assertEquals(10, v1.doubleValue(), 0.01);
        assertEquals(37.78, v2.doubleValue(), 0.01);
        assertEquals(30.48, v3.doubleValue(), 0.01);
    }
}
