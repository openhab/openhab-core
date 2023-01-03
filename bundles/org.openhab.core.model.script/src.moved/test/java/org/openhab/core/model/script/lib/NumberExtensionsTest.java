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
package org.openhab.core.model.script.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.Type;

/**
 * @author Henning Treu - Initial contribution
 * @author Fabio Marini - Initial contribution
 */
public class NumberExtensionsTest {

    private static final DecimalType DECIMAL1 = new DecimalType(1);
    private static final DecimalType DECIMAL2 = new DecimalType(2);

    private static final QuantityType<Temperature> Q_CELSIUS_1 = new QuantityType<>("1 °C");
    private static final QuantityType<Temperature> Q_CELSIUS_2 = new QuantityType<>("2 °C");

    private static final QuantityType<Length> Q_LENGTH_1_M = new QuantityType<>("1 m");
    private static final QuantityType<Length> Q_LENGTH_2_CM = new QuantityType<>("2 cm");

    private static final QuantityType<Dimensionless> Q_ONE_1 = new QuantityType<>(1, Units.ONE);
    private static final QuantityType<Dimensionless> Q_ONE_2 = new QuantityType<>(2, Units.ONE);

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
        assertThat(NumberExtensions.operator_minus(DECIMAL2, DECIMAL1), is(BigDecimal.ONE));
    }

    @Test
    public void operatorMinusNumberQuantityOne() {
        assertThat(NumberExtensions.operator_minus(Q_ONE_2, DECIMAL1), is(BigDecimal.ONE));
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
        assertFalse(NumberExtensions.operator_greaterEqualsThan(BigDecimal.ONE, new QuantityType<>("1 km")));
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

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlus() {
        Number x = 9;
        Number y = 0;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(9));
    }

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlusNullLeft() {
        Number x = null;
        Number y = 5;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(5));
    }

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlusNullRight() {
        Number x = 10;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(10));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumber() {
        Number x = 2;

        BigDecimal result = NumberExtensions.operator_minus(x);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(-2));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNull() {
        Number x = null;

        BigDecimal result = NumberExtensions.operator_minus(x);

        assertNull(result);
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumberNumber() {
        Number x = 10;
        Number y = 100;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(10 - 100));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNullNumber() {
        Number x = null;
        Number y = 100;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(-100));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumberNull() {
        Number x = 10;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(10));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiply() {
        Number x = 20;
        Number y = 30;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(20 * 30));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiplyNullLeft() {
        Number x = null;
        Number y = 30;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(0));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiplyNullRight() {
        Number x = 20;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(0));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorDivide() {
        Number x = 12;
        Number y = 4;

        BigDecimal result = NumberExtensions.operator_divide(x, y);

        assertNotNull(result);
        assertEquals(result, new BigDecimal(12).divide(new BigDecimal(4), 8, RoundingMode.HALF_UP));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorDivideNullLeft() {
        Number x = null;
        Number y = 4;

        assertThrows(NullPointerException.class, () -> NumberExtensions.operator_divide(x, y));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorDivideNullRight() {
        Number x = 12;
        Number y = null;

        assertThrows(NullPointerException.class, () -> NumberExtensions.operator_divide(x, y));
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNumberNumber() {
        Number x = 123;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        assertTrue(resutl);

        x = 123;
        y = 321;

        resutl = NumberExtensions.operator_equals(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNumberNull() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNullrNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNumberNumber() {
        Number x = 123;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        assertFalse(resutl);

        x = 123;
        y = 321;

        resutl = NumberExtensions.operator_notEquals(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNullNumber() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNumberNull() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        assertTrue(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_lessThan(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        assertFalse(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_greaterThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNumberNull() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertTrue(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertFalse(resutl);

        x = 3;
        y = 3;

        resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNullNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertFalse(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertTrue(resutl);

        x = 3;
        y = 3;

        resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNullNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 10;

        boolean result = NumberExtensions.operator_equals((Type) type, x);

        assertTrue(result);

        x = 1;

        result = NumberExtensions.operator_equals((Type) type, x);

        assertFalse(result);
    }

    /**
     * Test method for
     * {@link NumberExtensions#operator_notEquals(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 10;

        boolean result = NumberExtensions.operator_notEquals((Type) type, x);

        assertFalse(result);

        x = 1;

        result = NumberExtensions.operator_notEquals((Type) type, x);

        assertTrue(result);
    }

    /**
     * Test method for
     * {@link NumberExtensions#operator_greaterThan(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 123;

        boolean result = NumberExtensions.operator_greaterThan((Type) type, x);

        assertFalse(result);

        x = 2;

        result = NumberExtensions.operator_greaterThan((Type) type, x);

        assertTrue(result);
    }

    /**
     * Test method for
     * {@link NumberExtensions#operator_greaterEqualsThan(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 123;

        boolean result = NumberExtensions.operator_greaterEqualsThan((Type) type, x);

        assertFalse(result);

        x = 2;

        result = NumberExtensions.operator_greaterEqualsThan((Type) type, x);

        assertTrue(result);

        x = 10;

        result = NumberExtensions.operator_greaterEqualsThan((Type) type, x);

        assertTrue(result);
    }

    /**
     * Test method for
     * {@link NumberExtensions#operator_lessThan(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 123;

        boolean result = NumberExtensions.operator_lessThan((Type) type, x);

        assertTrue(result);

        x = 2;

        result = NumberExtensions.operator_lessThan((Type) type, x);

        assertFalse(result);
    }

    /**
     * Test method for
     * {@link NumberExtensions#operator_lessEqualsThan(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 123;

        boolean result = NumberExtensions.operator_lessEqualsThan((Type) type, x);

        assertTrue(result);

        x = 2;

        result = NumberExtensions.operator_lessEqualsThan((Type) type, x);

        assertFalse(result);

        x = 10;

        result = NumberExtensions.operator_lessEqualsThan((Type) type, x);

        assertTrue(result);
    }
}
