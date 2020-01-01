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
package org.openhab.core.model.script.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.junit.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SmartHomeUnits;
import org.openhab.core.types.Type;

/**
 *
 * @author Henning Treu - Initial contribution
 */
public class NumberExtensionsTest {

    private static final DecimalType DECIMAL1 = new DecimalType(1);
    private static final DecimalType DECIMAL2 = new DecimalType(2);

    private static final QuantityType<Temperature> Q_CELSIUS_1 = new QuantityType<>("1 °C");
    private static final QuantityType<Temperature> Q_CELSIUS_2 = new QuantityType<>("2 °C");

    private static final QuantityType<Length> Q_LENGTH_1_M = new QuantityType<>("1 m");
    private static final QuantityType<Length> Q_LENGTH_2_CM = new QuantityType<>("2 cm");

    private static final QuantityType<Dimensionless> Q_ONE_1 = new QuantityType<>(1, SmartHomeUnits.ONE);
    private static final QuantityType<Dimensionless> Q_ONE_2 = new QuantityType<>(2, SmartHomeUnits.ONE);

    @Test
    public void operatorPlusNumberNumber() {
        assertThat(NumberExtensions.operator_plus(DECIMAL1, DECIMAL2), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void operatorPlusNumberQuantityOne() {
        assertThat(NumberExtensions.operator_plus(Q_ONE_1, DECIMAL2), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void operatorPlusQuantityQuantity() {
        assertThat(NumberExtensions.operator_plus(Q_CELSIUS_1, Q_CELSIUS_2), is(QuantityType.valueOf("3 °C")));
    }

    @Test
    public void operatorMinusNumber() {
        assertThat(NumberExtensions.operator_minus(DECIMAL1), is(BigDecimal.valueOf(-1)));
    }

    @Test
    public void operatorMinusQuantity() {
        assertThat(NumberExtensions.operator_minus(Q_CELSIUS_1), is(QuantityType.valueOf("-1 °C")));
    }

    @Test
    public void operatorMinusNumberNumber() {
        assertThat(NumberExtensions.operator_minus(DECIMAL2, DECIMAL1), is(BigDecimal.valueOf(1)));
    }

    @Test
    public void operatorMinusNumberQuantityOne() {
        assertThat(NumberExtensions.operator_minus(Q_ONE_2, DECIMAL1), is(BigDecimal.valueOf(1)));
    }

    @Test
    public void operatorMinusQuantityQuantity() {
        assertThat(NumberExtensions.operator_minus(Q_LENGTH_1_M, Q_LENGTH_2_CM), is(QuantityType.valueOf("0.98 m")));
    }

    @Test
    public void operatorMultiplyNumberQuantity() {
        assertThat(NumberExtensions.operator_multiply(DECIMAL2, Q_LENGTH_2_CM), is(QuantityType.valueOf("4 cm")));
    }

    @Test
    public void operatorMultiplyQuantityQuantity() {
        assertThat(NumberExtensions.operator_multiply(Q_LENGTH_1_M, Q_LENGTH_2_CM), is(QuantityType.valueOf("2 m·cm")));
    }

    @Test
    public void operatorDivideQuantityNumber() {
        assertThat(NumberExtensions.operator_divide(Q_LENGTH_1_M, DECIMAL2), is(QuantityType.valueOf("0.5 m")));
    }

    @Test
    public void operatorDivideQuantityQuantity() {
        assertThat(NumberExtensions.operator_divide(Q_LENGTH_1_M, Q_LENGTH_2_CM), is(QuantityType.valueOf("0.5 m/cm")));
    }

    @Test
    public void operatorDivideNumberQuantity() {
        assertThat(NumberExtensions.operator_divide(DECIMAL1, Q_LENGTH_2_CM), is(QuantityType.valueOf("0.5 one/cm")));
    }

    @Test
    public void operatorEqualsNumberQuantity() {
        assertFalse(NumberExtensions.operator_equals((Number) DECIMAL1, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorEqualsQuantityNumber() {
        assertFalse(NumberExtensions.operator_equals((Number) Q_LENGTH_2_CM, DECIMAL1));
    }

    @Test
    public void operatorEqualsQuantityQuantityFalse() {
        assertFalse(NumberExtensions.operator_equals(Q_LENGTH_1_M, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorEqualsQuantityQuantityTrue() {
        assertTrue(NumberExtensions.operator_equals(Q_LENGTH_1_M, new QuantityType<>("100 cm")));
    }

    @Test
    public void operatorLessThanNumberQuantity() {
        assertFalse(NumberExtensions.operator_lessThan((Number) Q_LENGTH_1_M, Q_LENGTH_1_M));
    }

    @Test
    public void operatorLessThanTypeQuantity() {
        assertFalse(NumberExtensions.operator_lessThan((Type) Q_LENGTH_1_M, Q_LENGTH_1_M));
    }

    @Test
    public void operatorLessThanQuantityQuantityFalse() {
        assertFalse(NumberExtensions.operator_lessThan(Q_LENGTH_1_M, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorLessThanQuantityQuantityTrue() {
        assertTrue(NumberExtensions.operator_lessThan(Q_LENGTH_2_CM, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterThanNumberQuantity() {
        assertFalse(NumberExtensions.operator_greaterThan((Number) Q_LENGTH_1_M, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterThanTypeQuantity() {
        assertFalse(NumberExtensions.operator_greaterThan((Type) Q_LENGTH_1_M, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterThanQuantityQuantityFalse() {
        assertFalse(NumberExtensions.operator_greaterThan(Q_LENGTH_2_CM, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterThanQuantityQuantityTrue() {
        assertTrue(NumberExtensions.operator_greaterThan(Q_LENGTH_1_M, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorLessEqualsThanNumberQuantity() {
        assertFalse(NumberExtensions.operator_lessEqualsThan(BigDecimal.valueOf(100), new QuantityType<>("100 cm")));
    }

    @Test
    public void operatorLessEqualsThanTypeQuantity() {
        assertTrue(NumberExtensions.operator_lessEqualsThan((Type) Q_LENGTH_1_M, Q_LENGTH_1_M));
    }

    @Test
    public void operatorLessEqualsThanQuantityQuantityFalse() {
        assertFalse(NumberExtensions.operator_lessEqualsThan(Q_LENGTH_1_M, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorLessEqualsThanQuantityQuantityTrue() {
        assertTrue(NumberExtensions.operator_lessEqualsThan(Q_LENGTH_2_CM, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterEqualsThanNumberQuantity() {
        assertFalse(NumberExtensions.operator_greaterEqualsThan(BigDecimal.valueOf(1), new QuantityType<>("1 km")));
    }

    @Test
    public void operatorGreaterEqualsThanTypeQuantity() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan((Type) Q_LENGTH_1_M, new QuantityType<>("100 cm")));
    }

    @Test
    public void operatorGreaterEqualsThanQuantityQuantityFalse() {
        assertFalse(NumberExtensions.operator_greaterEqualsThan(Q_LENGTH_2_CM, Q_LENGTH_1_M));
    }

    @Test
    public void operatorGreaterEqualsThanQuantityQuantityTrue() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan(Q_LENGTH_1_M, Q_LENGTH_2_CM));
    }

    @Test
    public void operatorEqualsQuantityOneNumber() {
        assertTrue(NumberExtensions.operator_equals(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_equals((Number) DECIMAL1, Q_ONE_1));
    }

    @Test
    public void operatorLessThanQuantityOneNumber() {
        assertTrue(NumberExtensions.operator_lessThan(Q_ONE_1, DECIMAL2));
        assertTrue(NumberExtensions.operator_lessThan((Number) DECIMAL1, Q_ONE_2));
    }

    @Test
    public void operatorLessEqualsThanQuantityOneNumber() {
        assertTrue(NumberExtensions.operator_lessEqualsThan(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_lessEqualsThan((Number) DECIMAL1, Q_ONE_1));
    }

    @Test
    public void operatorGreaterThanQuantityOneNumber() {
        assertTrue(NumberExtensions.operator_greaterThan(Q_ONE_2, DECIMAL1));
        assertTrue(NumberExtensions.operator_greaterThan((Number) DECIMAL2, Q_ONE_1));
    }

    @Test
    public void operatorGreaterEqualsThanQuantityOneNumber() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_greaterEqualsThan((Number) DECIMAL1, Q_ONE_1));
    }

}
