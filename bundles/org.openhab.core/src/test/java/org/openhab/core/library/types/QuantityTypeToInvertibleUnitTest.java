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
        assertNotNull(QuantityType.valueOf(2000, Units.KELVIN).toInvertibleUnit(Units.MIRED));
        assertNotNull(QuantityType.valueOf(2000, SIUnits.CELSIUS).toInvertibleUnit(Units.MIRED));
        assertNotNull(QuantityType.valueOf(3000, ImperialUnits.FAHRENHEIT).toInvertibleUnit(Units.MIRED));
        // fail case
        assertNull(QuantityType.valueOf(500, SIUnits.METRE).toInvertibleUnit(Units.MIRED));
    }

    /**
     * Test conversions from Mirek to other UoM values
     */
    @Test
    void testConversionsFromMirek() {
        // pass cases
        assertNotNull(QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(Units.KELVIN));
        assertNotNull(QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(SIUnits.CELSIUS));
        assertNotNull(QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(ImperialUnits.FAHRENHEIT));
        // fail case
        assertNull(QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(SIUnits.METRE));
    }

    /**
     * Test conversions from UoM values to themselves
     */
    @Test
    void testConversionsToSelf() {
        // pass cases
        assertNotNull(QuantityType.valueOf(500, Units.MIRED).toInvertibleUnit(Units.MIRED));
        assertNotNull(QuantityType.valueOf(2000, Units.KELVIN).toInvertibleUnit(Units.KELVIN));
    }

    /**
     * Test conversion Ohm to/from Siemens
     */
    @Test
    void testConversionsOhmAndSiemens() {
        // pass cases
        assertNotNull(QuantityType.valueOf(500, Units.OHM).toInvertibleUnit(Units.SIEMENS));
        assertNotNull(QuantityType.valueOf(500, Units.SIEMENS).toInvertibleUnit(Units.OHM));
        // fail cases
        assertNull(QuantityType.valueOf(500, Units.OHM).toInvertibleUnit(SIUnits.METRE));
        assertNull(QuantityType.valueOf(500, SIUnits.METRE).toInvertibleUnit(Units.OHM));
    }

    /**
     * Test conversion for dimensionless
     */
    @Test
    void testConversionsDimensionless() {
        assertNotNull(QuantityType.valueOf(100, Units.ONE).toInvertibleUnit(Units.ONE));
    }
}
