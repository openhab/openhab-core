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
package org.openhab.core.library.dimension;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.util.UnitUtils;

/**
 * Test {@link RadiationDoseRate} dimension.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class RadiationDoseRateTest {

    public static Stream<Arguments> arguments() {
        return Stream.of(//
                Arguments.of(QuantityType.valueOf(60.0 + " Sv/h"), "Sv/d", 24.0), //
                Arguments.of(QuantityType.valueOf(60.0 + " Sv/h"), "Sv/yr", 8760.0), //
                Arguments.of(QuantityType.valueOf(60.0 + " Sv/h"), "Sv/min", 1.0 / 60.0), //
                Arguments.of(QuantityType.valueOf(60.0 + " Sv/h"), "Sv/s", 1.0 / 3600.0), //
                Arguments.of(QuantityType.valueOf(1.23 + " Sv/h"), "mSv/h", 1000.0), //
                Arguments.of(QuantityType.valueOf(1.23 + " Sv/h"), "μSv/h", 1000000.0) //
        );
    }

    /**
     * Test conversion of radiation dose rate quantities to various target units.
     *
     * @param observable QuantityType representing the original radiation dose rate quantity
     * @param targetUnitSymbol String representing the target unit symbol to convert to
     * @param factor Double representing the expected conversion factor from the original unit to the target unit
     */
    @ParameterizedTest
    @MethodSource("arguments")
    public void testConversion(QuantityType<?> observable, String targetUnitSymbol, Double factor) {
        QuantityType<?> test = observable.toUnit(targetUnitSymbol);
        assertNotNull(test, "Conversion to target unit failed");
        assertEquals(observable.getDimension(), test.getDimension(), "Dimension mismatch after conversion");
        assertEquals(UnitUtils.parseUnit(targetUnitSymbol), test.getUnit(), "Unit symbol mismatch after conversion");
        assertEquals(observable.doubleValue() * factor, test.doubleValue(), 1 * factor, "Converted value mismatch");
    }
}
