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
/**
 * 
 */
package org.eclipse.smarthome.model.script.tests.lib;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.smarthome.core.types.Type;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.model.script.lib.NumberExtensions;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link NumberExtensions} library class
 * 
 * @author Fabio Marini
 * 
 */
public class NumberExtensionsTest {

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_plus() {
		Number x = 9;
		Number y = 0;

		BigDecimal result = NumberExtensions.operator_plus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(9));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_plusNullLeft() {
		Number x = null;
		Number y = 5;

		BigDecimal result = NumberExtensions.operator_plus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(5));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_plus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_plusNullRight() {
		Number x = 10;
		Number y = null;

		BigDecimal result = NumberExtensions.operator_plus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(10));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_minus(java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_minusNumber() {
		Number x = 2;

		BigDecimal result = NumberExtensions.operator_minus(x);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(-2));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_minus(java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_minusNull() {
		Number x = null;

		BigDecimal result = NumberExtensions.operator_minus(x);

		Assert.assertNull(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_minusNumberNumber() {
		Number x = 10;
		Number y = 100;

		BigDecimal result = NumberExtensions.operator_minus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(10 - 100));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_minusNullNumber() {
		Number x = null;
		Number y = 100;

		BigDecimal result = NumberExtensions.operator_minus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(-100));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_minus(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_minusNumberNull() {
		Number x = 10;
		Number y = null;

		BigDecimal result = NumberExtensions.operator_minus(x, y);

		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(10));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_multiply() {
		Number x = 20;
		Number y = 30;
		
		BigDecimal result = NumberExtensions.operator_multiply(x, y);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(20 * 30));
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_multiplyNullLeft() {
		Number x = null;
		Number y = 30;
		
		BigDecimal result = NumberExtensions.operator_multiply(x, y);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(0));
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_multiply(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_multiplyNullRight() {
		Number x = 20;
		Number y = null;
		
		BigDecimal result = NumberExtensions.operator_multiply(x, y);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result, new BigDecimal(0));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_divide() {
		Number x = 12;
		Number y = 4;
		
		BigDecimal result = NumberExtensions.operator_divide(x, y);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result,new BigDecimal(12).divide(new BigDecimal(4), 8, RoundingMode.HALF_UP));
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_divideNullLeft() {
		Number x = null;
		Number y = 4;
		
		BigDecimal result = NumberExtensions.operator_divide(x, y);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result,new BigDecimal(0).divide(new BigDecimal(4), 8, RoundingMode.HALF_UP));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_divide(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test(expected = ArithmeticException.class)
	public void testOperator_divideNullRight() {
		Number x = 12;
		Number y = null;
		
		NumberExtensions.operator_divide(x, y);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_equalsNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_equalsNullNumber() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_equals(x, y);
		Assert.assertFalse(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_equalsNumberNull() {
		Number x = 123;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_equals(x, y);
		Assert.assertFalse(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_equals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_equalsNullrNull() {
		Number x = null;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_equals(x, y);
		Assert.assertTrue(resutl);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_notEqualsNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_notEqualsNullNumber() {
		Number x = 123;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_notEquals(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_notEquals(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_notEqualsNumberNull() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_notEquals(x, y);
		Assert.assertTrue(resutl);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessThanNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessThanNullNumber() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_lessThan(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessThanNumberNull() {
		Number x = 12;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_lessThan(x, y);
		Assert.assertFalse(resutl);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterThanNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterThanNullNumber() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_greaterThan(x, y);
		Assert.assertFalse(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterThanNumberNull() {
		Number x = 123;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_greaterThan(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessEqualsThanNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessEqualsThanNullNumber() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
		Assert.assertTrue(resutl);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessEqualsThanNumberNull() {
		Number x = 12;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
		Assert.assertFalse(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessEqualsThanNullNull() {
		Number x = null;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_lessEqualsThan(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterEqualsThanNumberNumber() {
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
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterEqualsThanNullNumber() {
		Number x = null;
		Number y = 123;
		
		boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
		Assert.assertFalse(resutl);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterEqualsThanNumberNull() {
		Number x = 12;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterEqualsThan(java.lang.Number, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterEqualsThanNullNull() {
		Number x = null;
		Number y = null;
		
		boolean resutl = NumberExtensions.operator_greaterEqualsThan(x, y);
		Assert.assertTrue(resutl);
	}
	
	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_equals(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_equalsTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 10;
		
		boolean result = NumberExtensions.operator_equals((Type)type, x);
		
		Assert.assertTrue(result);
		
		x = 1;
		
		result = NumberExtensions.operator_equals((Type)type, x);
		
		Assert.assertFalse(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_notEquals(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_notEqualsTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 10;
		
		boolean result = NumberExtensions.operator_notEquals((Type)type, x);
		
		Assert.assertFalse(result);
		
		x = 1;
		
		result = NumberExtensions.operator_notEquals((Type)type, x);
		
		Assert.assertTrue(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterThan(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterThanTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 123;
		
		boolean result = NumberExtensions.operator_greaterThan((Type)type, x);
		
		Assert.assertFalse(result);
		
		x = 2;
		
		result = NumberExtensions.operator_greaterThan((Type)type, x);
		
		Assert.assertTrue(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_greaterEqualsThan(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_greaterEqualsThanTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 123;
		
		boolean result = NumberExtensions.operator_greaterEqualsThan((Type)type, x);
		
		Assert.assertFalse(result);
		
		x = 2;
		
		result = NumberExtensions.operator_greaterEqualsThan((Type)type, x);
		
		Assert.assertTrue(result);
		
		x = 10;
		
		result = NumberExtensions.operator_greaterEqualsThan((Type)type, x);
		
		Assert.assertTrue(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessThan(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessThanTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 123;
		
		boolean result = NumberExtensions.operator_lessThan((Type)type, x);
		
		Assert.assertTrue(result);
		
		x = 2;
		
		result = NumberExtensions.operator_lessThan((Type)type, x);
		
		Assert.assertFalse(result);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.smarthome.model.script.lib.NumberExtensions#operator_lessEqualsThan(org.eclipse.smarthome.core.types.Type, java.lang.Number)}
	 * .
	 */
	@Test
	public void testOperator_lessEqualsThanTypeNumber() {
		DecimalType type = new DecimalType(10);
		Number x = 123;
		
		boolean result = NumberExtensions.operator_lessEqualsThan((Type)type, x);
		
		Assert.assertTrue(result);
		
		x = 2;
		
		result = NumberExtensions.operator_lessEqualsThan((Type)type, x);
		
		Assert.assertFalse(result);
		
		x = 10;
		
		result = NumberExtensions.operator_lessEqualsThan((Type)type, x);
		
		Assert.assertTrue(result);
	}

}
