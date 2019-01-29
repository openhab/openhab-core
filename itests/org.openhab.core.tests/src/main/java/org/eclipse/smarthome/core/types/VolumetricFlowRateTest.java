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
package org.eclipse.smarthome.core.types;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import javax.measure.Unit;

import org.eclipse.smarthome.core.library.dimension.VolumetricFlowRate;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.types.util.UnitUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tec.uom.se.ComparableQuantity;
import tec.uom.se.quantity.Quantities;

/**
 * Test for volumentric flow rate constants defined in {@link SmartHomeUnits}.
 *
 * @author Łukasz Dywicki - initial contribution and API
 */
@RunWith(Parameterized.class)
public class VolumetricFlowRateTest {

    /**
     * While its not SI base unit it produces nice and fairly rounded numbers (si base is m3/s).
     */
    private final static Unit<VolumetricFlowRate> BASE_UNIT = SmartHomeUnits.CUBICMETRE_PER_HOUR;

    private final Unit<VolumetricFlowRate> unit;
    private final String symbol;
    private final Double value;
    private final Double valueInBaseUnit;

    public VolumetricFlowRateTest(Unit<VolumetricFlowRate> unit, String symbol, Double value, Double valueInBaseUnit) {
        this.unit = unit;
        this.symbol = symbol;
        this.value = value;
        this.valueInBaseUnit = valueInBaseUnit;
    }

    /**
     * An additional test which converts given test quantity into base unit and then compares it with expected value.
     *
     * This basic test confirms that values of different flow rates can be exchanged to given base unit.
     */
    @Test
    public void testValueConversionToM3s() {
        ComparableQuantity<VolumetricFlowRate> quantity = Quantities.getQuantity(value, unit);
        ComparableQuantity<VolumetricFlowRate> quantityInBase = Quantities.getQuantity(valueInBaseUnit, BASE_UNIT);

        ComparableQuantity<VolumetricFlowRate> convertedQuantity = quantity.to(BASE_UNIT);

        assertThat(convertedQuantity, is(equalTo(quantityInBase)));
    }

    /**
     * Verifies that given symbol is recognized by {@link UnitUtils}.
     */
    @Test
    public void testSymbolLookup() {
        Unit<?> parsedUnit = UnitUtils.parseUnit(symbol);

        assertThat(parsedUnit, is(notNullValue()));
        assertThat(parsedUnit, is(equalTo(unit)));
    }

    @Parameters
    public static Object[] params() {
        return new Object[] { new Object[] { SmartHomeUnits.LITRE_PER_MINUTE, "l/min", 100.0, 6.0 },
                new Object[] { SmartHomeUnits.CUBICMETRE_PER_SECOND, "m³/s", 100.0, 360000.0 },
                new Object[] { SmartHomeUnits.CUBICMETRE_PER_MINUTE, "m³/min", 100.0, 6000.0 },
                new Object[] { SmartHomeUnits.CUBICMETRE_PER_HOUR, "m³/h", 100.0, 100.0 },
                new Object[] { SmartHomeUnits.CUBICMETRE_PER_DAY, "m³/d", 100.0, 4.166666666666667 } };
    }

}
