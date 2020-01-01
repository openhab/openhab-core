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
/**
 */
package org.openhab.core.model.script.tests.lib;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.Assert;
import org.junit.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.model.script.lib.NumberExtensions;
import org.openhab.core.types.Type;

/**
 * Test case for {@link NumberExtensions} library class
 *
 * @author Fabio Marini - Initial contribution
 */
public class NumberExtensionsTest {

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlus() {
        Number x = 9;
        Number y = 0;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(9));
    }

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlusNullLeft() {
        Number x = null;
        Number y = 5;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(5));
    }

    /**
     * Test method for {@link NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorPlusNullRight() {
        Number x = 10;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_plus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(10));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumber() {
        Number x = 2;

        BigDecimal result = NumberExtensions.operator_minus(x);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(-2));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNull() {
        Number x = null;

        BigDecimal result = NumberExtensions.operator_minus(x);

        Assert.assertNull(result);
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumberNumber() {
        Number x = 10;
        Number y = 100;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(10 - 100));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNullNumber() {
        Number x = null;
        Number y = 100;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(-100));
    }

    /**
     * Test method for {@link NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMinusNumberNull() {
        Number x = 10;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_minus(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(10));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiply() {
        Number x = 20;
        Number y = 30;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(20 * 30));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiplyNullLeft() {
        Number x = null;
        Number y = 30;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(0));
    }

    /**
     * Test method for {@link NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorMultiplyNullRight() {
        Number x = 20;
        Number y = null;

        BigDecimal result = NumberExtensions.operator_multiply(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(0));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorDivide() {
        Number x = 12;
        Number y = 4;

        BigDecimal result = NumberExtensions.operator_divide(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(12).divide(new BigDecimal(4), 8, RoundingMode.HALF_UP));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test(expected = NullPointerException.class)
    public void testOperatorDivideNullLeft() {
        Number x = null;
        Number y = 4;

        BigDecimal result = NumberExtensions.operator_divide(x, y);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, new BigDecimal(0).divide(new BigDecimal(4), 8, RoundingMode.HALF_UP));
    }

    /**
     * Test method for {@link NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
     */
    @Test(expected = NullPointerException.class)
    public void testOperatorDivideNullRight() {
        Number x = 12;
        Number y = null;

        NumberExtensions.operator_divide(x, y);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNumberNumber() {
        Number x = 123;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        Assert.assertTrue(resutl);

        x = 123;
        y = 321;

        resutl = NumberExtensions.operator_equals(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNumberNull() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsNullrNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_equals(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNumberNumber() {
        Number x = 123;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        Assert.assertFalse(resutl);

        x = 123;
        y = 321;

        resutl = NumberExtensions.operator_notEquals(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNullNumber() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorNotEqualsNumberNull() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_notEquals(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        Assert.assertTrue(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_lessThan(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessThan(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        Assert.assertFalse(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_greaterThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterThanNumberNull() {
        Number x = 123;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertTrue(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertFalse(resutl);

        x = 3;
        y = 3;

        resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorLessEqualsThanNullNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNumberNumber() {
        Number x = 12;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertFalse(resutl);

        x = 90;
        y = 2;

        resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertTrue(resutl);

        x = 3;
        y = 3;

        resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNullNumber() {
        Number x = null;
        Number y = 123;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertFalse(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNumberNull() {
        Number x = 12;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
     */
    @Test
    public void testOperatorGreaterEqualsThanNullNull() {
        Number x = null;
        Number y = null;

        boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
        Assert.assertTrue(resutl);
    }

    /**
     * Test method for {@link NumberExtensions#operator_equals(org.openhab.core.types.Type, java.lang.Number)}
     */
    @Test
    public void testOperatorEqualsTypeNumber() {
        DecimalType type = new DecimalType(10);
        Number x = 10;

        boolean result = NumberExtensions.operator_equals((Type) type, x);

        Assert.assertTrue(result);

        x = 1;

        result = NumberExtensions.operator_equals((Type) type, x);

        Assert.assertFalse(result);
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

        Assert.assertFalse(result);

        x = 1;

        result = NumberExtensions.operator_notEquals((Type) type, x);

        Assert.assertTrue(result);
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

        Assert.assertFalse(result);

        x = 2;

        result = NumberExtensions.operator_greaterThan((Type) type, x);

        Assert.assertTrue(result);
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

        Assert.assertFalse(result);

        x = 2;

        result = NumberExtensions.operator_greaterEqualsThan((Type) type, x);

        Assert.assertTrue(result);

        x = 10;

        result = NumberExtensions.operator_greaterEqualsThan((Type) type, x);

        Assert.assertTrue(result);
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

        Assert.assertTrue(result);

        x = 2;

        result = NumberExtensions.operator_lessThan((Type) type, x);

        Assert.assertFalse(result);
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

        Assert.assertTrue(result);

        x = 2;

        result = NumberExtensions.operator_lessEqualsThan((Type) type, x);

        Assert.assertFalse(result);

        x = 10;

        result = NumberExtensions.operator_lessEqualsThan((Type) type, x);

        Assert.assertTrue(result);
    }

}
