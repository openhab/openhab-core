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
package org.openhab.core.library.dimension;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.util.stream.Stream;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.util.UnitUtils;

import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

/**
 * Test for volumentric flow rate constants defined in {@link Units}.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public class VolumetricFlowRateTest {

    /**
     * While its not SI base unit it produces nice and fairly rounded numbers (si base is m3/s).
     */
    private static final Unit<VolumetricFlowRate> BASE_UNIT = Units.CUBICMETRE_PER_HOUR;

    /**
     * An additional test which converts given test quantity into base unit and then compares it with expected value.
     *
     * This basic test confirms that values of different flow rates can be exchanged to given base unit.
     */
    @ParameterizedTest
    @MethodSource("arguments")
    public void testValueConversionToM3s(Unit<VolumetricFlowRate> unit, String symbol, Double value,
            Double valueInBaseUnit) {
        ComparableQuantity<VolumetricFlowRate> quantity = Quantities.getQuantity(value, unit);
        ComparableQuantity<VolumetricFlowRate> quantityInBase = Quantities.getQuantity(valueInBaseUnit, BASE_UNIT);

        ComparableQuantity<VolumetricFlowRate> convertedQuantity = quantity.to(BASE_UNIT);

        assertThat(convertedQuantity.getValue().doubleValue(),
                is(closeTo(quantityInBase.getValue().doubleValue(), 1e-10)));
    }

    /**
     * Verifies that given symbol is recognized by {@link UnitUtils}.
     */
    @ParameterizedTest
    @MethodSource("arguments")
    public void testSymbolLookup(Unit<VolumetricFlowRate> unit, String symbol, Double value, Double valueInBaseUnit) {
        Unit<?> parsedUnit = UnitUtils.parseUnit(symbol);

        assertThat(parsedUnit, is(notNullValue()));
        assertThat(parsedUnit, is(equalTo(unit)));
    }

    public static Stream<Arguments> arguments() {
        return Stream.of(Arguments.of(Units.LITRE_PER_MINUTE, "l/min", 100.0, 6.0),
                Arguments.of(Units.CUBICMETRE_PER_SECOND, "m³/s", 100.0, 360000.0),
                Arguments.of(Units.CUBICMETRE_PER_MINUTE, "m³/min", 100.0, 6000.0),
                Arguments.of(Units.CUBICMETRE_PER_HOUR, "m³/h", 100.0, 100.0),
                Arguments.of(Units.CUBICMETRE_PER_DAY, "m³/d", 100.0, 4.166666666666666666666666666666667));
    }
}
