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
package org.eclipse.smarthome.model.script.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.types.Type;
import org.junit.Test;

/**
 *
 * @author Henning Treu - initial contribution
 *
 */
public class NumberExtensionsTest {

    private static final DecimalType DECIMAL1 = new DecimalType(1);
    private static final DecimalType DECIMAL2 = new DecimalType(2);

    private static final QuantityType<Temperature> Q_CELSIUS_1 = new QuantityType<Temperature>("1 °C");
    private static final QuantityType<Temperature> Q_CELSIUS_2 = new QuantityType<Temperature>("2 °C");

    private static final QuantityType<Length> Q_LENGTH_1m = new QuantityType<Length>("1 m");
    private static final QuantityType<Length> Q_LENGTH_2cm = new QuantityType<Length>("2 cm");

    private static final QuantityType<Dimensionless> Q_ONE_1 = new QuantityType<>(1, SmartHomeUnits.ONE);
    private static final QuantityType<Dimensionless> Q_ONE_2 = new QuantityType<>(2, SmartHomeUnits.ONE);

    @Test
    public void operatorPlus_Number_Number() {
        assertThat(NumberExtensions.operator_plus(DECIMAL1, DECIMAL2), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void operatorPlus_Number_Quantity_ONE() {
        assertThat(NumberExtensions.operator_plus(Q_ONE_1, DECIMAL2), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void operatorPlus_Quantity_Quantity() {
        assertThat(NumberExtensions.operator_plus(Q_CELSIUS_1, Q_CELSIUS_2), is(QuantityType.valueOf("3 °C")));
    }

    @Test
    public void operatorMinus_Number() {
        assertThat(NumberExtensions.operator_minus(DECIMAL1), is(BigDecimal.valueOf(-1)));
    }

    @Test
    public void operatorMinus_Quantity() {
        assertThat(NumberExtensions.operator_minus(Q_CELSIUS_1), is(QuantityType.valueOf("-1 °C")));
    }

    @Test
    public void operatorMinus_Number_Number() {
        assertThat(NumberExtensions.operator_minus(DECIMAL2, DECIMAL1), is(BigDecimal.valueOf(1)));
    }

    @Test
    public void operatorMinus_Number_Quantity_ONE() {
        assertThat(NumberExtensions.operator_minus(Q_ONE_2, DECIMAL1), is(BigDecimal.valueOf(1)));
    }

    @Test
    public void operatorMinus_Quantity_Quantity() {
        assertThat(NumberExtensions.operator_minus(Q_LENGTH_1m, Q_LENGTH_2cm), is(QuantityType.valueOf("0.98 m")));
    }

    @Test
    public void operatorMultiply_Number_Quantity() {
        assertThat(NumberExtensions.operator_multiply(DECIMAL2, Q_LENGTH_2cm), is(QuantityType.valueOf("4 cm")));
    }

    @Test
    public void operatorMultiply_Quantity_Quantity() {
        assertThat(NumberExtensions.operator_multiply(Q_LENGTH_1m, Q_LENGTH_2cm), is(QuantityType.valueOf("2 m·cm")));
    }

    @Test
    public void operatorDivide_Quantity_Number() {
        assertThat(NumberExtensions.operator_divide(Q_LENGTH_1m, DECIMAL2), is(QuantityType.valueOf("0.5 m")));
    }

    @Test
    public void operatorDivide_Quantity_Quantity() {
        assertThat(NumberExtensions.operator_divide(Q_LENGTH_1m, Q_LENGTH_2cm), is(QuantityType.valueOf("0.5 m/cm")));
    }

    @Test
    public void operatorDivide_Numer_Quantity() {
        assertThat(NumberExtensions.operator_divide(DECIMAL1, Q_LENGTH_2cm), is(QuantityType.valueOf("0.5 one/cm")));
    }

    @Test
    public void operatorEquals_Numer_Quantity() {
        assertFalse(NumberExtensions.operator_equals((Number) DECIMAL1, Q_LENGTH_2cm));
    }

    @Test
    public void operatorEquals_Quantity_Number() {
        assertFalse(NumberExtensions.operator_equals((Number) Q_LENGTH_2cm, DECIMAL1));
    }

    @Test
    public void operatorEquals_Quantity_Quantity_False() {
        assertFalse(NumberExtensions.operator_equals(Q_LENGTH_1m, Q_LENGTH_2cm));
    }

    @Test
    public void operatorEquals_Quantity_Quantity_True() {
        assertTrue(NumberExtensions.operator_equals(Q_LENGTH_1m, new QuantityType<Length>("100 cm")));
    }

    @Test
    public void operatorLessThan_Number_Quantity() {
        assertFalse(NumberExtensions.operator_lessThan((Number) Q_LENGTH_1m, Q_LENGTH_1m));
    }

    @Test
    public void operatorLessThan_Type_Quantity() {
        assertFalse(NumberExtensions.operator_lessThan((Type) Q_LENGTH_1m, Q_LENGTH_1m));
    }

    @Test
    public void operatorLessThan_Quantity_Quantity_False() {
        assertFalse(NumberExtensions.operator_lessThan(Q_LENGTH_1m, Q_LENGTH_2cm));
    }

    @Test
    public void operatorLessThan_Quantity_Quantity_True() {
        assertTrue(NumberExtensions.operator_lessThan(Q_LENGTH_2cm, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterThan_Number_Quantity() {
        assertFalse(NumberExtensions.operator_greaterThan((Number) Q_LENGTH_1m, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterThan_Type_Quantity() {
        assertFalse(NumberExtensions.operator_greaterThan((Type) Q_LENGTH_1m, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterThan_Quantity_Quantity_False() {
        assertFalse(NumberExtensions.operator_greaterThan(Q_LENGTH_2cm, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterThan_Quantity_Quantity_True() {
        assertTrue(NumberExtensions.operator_greaterThan(Q_LENGTH_1m, Q_LENGTH_2cm));
    }

    @Test
    public void operatorLessEqualsThan_Number_Quantity() {
        assertFalse(
                NumberExtensions.operator_lessEqualsThan(BigDecimal.valueOf(100), new QuantityType<Length>("100 cm")));
    }

    @Test
    public void operatorLessEqualsThan_Type_Quantity() {
        assertTrue(NumberExtensions.operator_lessEqualsThan((Type) Q_LENGTH_1m, Q_LENGTH_1m));
    }

    @Test
    public void operatorLessEqualsThan_Quantity_Quantity_False() {
        assertFalse(NumberExtensions.operator_lessEqualsThan(Q_LENGTH_1m, Q_LENGTH_2cm));
    }

    @Test
    public void operatorLessEqualsThan_Quantity_Quantity_True() {
        assertTrue(NumberExtensions.operator_lessEqualsThan(Q_LENGTH_2cm, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterEqualsThan_Number_Quantity() {
        assertFalse(
                NumberExtensions.operator_greaterEqualsThan(BigDecimal.valueOf(1), new QuantityType<Length>("1 km")));
    }

    @Test
    public void operatorGreaterEqualsThan_Type_Quantity() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan((Type) Q_LENGTH_1m, new QuantityType<Length>("100 cm")));
    }

    @Test
    public void operatorGreaterEqualsThan_Quantity_Quantity_False() {
        assertFalse(NumberExtensions.operator_greaterEqualsThan(Q_LENGTH_2cm, Q_LENGTH_1m));
    }

    @Test
    public void operatorGreaterEqualsThan_Quantity_Quantity_True() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan(Q_LENGTH_1m, Q_LENGTH_2cm));
    }

    @Test
    public void operatorEquals_Quantity_ONE_Number() {
        assertTrue(NumberExtensions.operator_equals(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_equals((Number) DECIMAL1, Q_ONE_1));
    }

    @Test
    public void operatorLessThan_Quantity_ONE_Number() {
        assertTrue(NumberExtensions.operator_lessThan(Q_ONE_1, DECIMAL2));
        assertTrue(NumberExtensions.operator_lessThan((Number) DECIMAL1, Q_ONE_2));
    }

    @Test
    public void operatorLessEqualsThan_Quantity_ONE_Number() {
        assertTrue(NumberExtensions.operator_lessEqualsThan(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_lessEqualsThan((Number) DECIMAL1, Q_ONE_1));
    }

    @Test
    public void operatorGreaterThan_Quantity_ONE_Number() {
        assertTrue(NumberExtensions.operator_greaterThan(Q_ONE_2, DECIMAL1));
        assertTrue(NumberExtensions.operator_greaterThan((Number) DECIMAL2, Q_ONE_1));
    }

    @Test
    public void operatorGreaterEqualsThan_Quantity_ONE_Number() {
        assertTrue(NumberExtensions.operator_greaterEqualsThan(Q_ONE_1, DECIMAL1));
        assertTrue(NumberExtensions.operator_greaterEqualsThan((Number) DECIMAL1, Q_ONE_1));
    }

}
