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

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.measure.quantity.Dimensionless;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.types.Type;

/**
 * This class contains all kinds of extensions to be used by scripts and not
 * provided by Xbase. These include things like number handling and comparisons.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class NumberExtensions {

    /**
     * It is the definition of Java null pointer for the rules language.
     * Actually its value is 0 (rules variables are number) but we can use
     * the null pointer and throws an NPE when a null value is used.
     * I think this concept should not exist for those who writes the rules.
     */
    public static final BigDecimal NULL_DEFINITION = new BigDecimal(0);

    // Calculation operators for numbers

    public static BigDecimal operator_plus(Number x, Number y) {
        BigDecimal xValue = numberToBigDecimal(x);
        BigDecimal yValue = numberToBigDecimal(y);
        if (xValue == null) {
            return yValue;
        } else if (yValue == null) {
            return xValue;
        } else {
            return xValue.add(yValue);
        }
    }

    public static BigDecimal operator_minus(Number x) {
        BigDecimal xValue = numberToBigDecimal(x);
        if (xValue == null) {
            return xValue;
        } else {
            return xValue.negate();
        }
    }

    public static BigDecimal operator_minus(Number x, Number y) {
        BigDecimal xValue = numberToBigDecimal(x);
        BigDecimal yValue = numberToBigDecimal(y);
        if (xValue == null) {
            return operator_minus(yValue);
        } else if (yValue == null) {
            return xValue;
        } else {
            return xValue.subtract(yValue);
        }
    }

    public static BigDecimal operator_multiply(Number x, Number y) {
        BigDecimal xValue = numberToBigDecimal(x);
        BigDecimal yValue = numberToBigDecimal(y);
        if (xValue == null) {
            return NULL_DEFINITION;
        } else if (yValue == null) {
            return NULL_DEFINITION;
        } else {
            return xValue.multiply(yValue);
        }
    }

    public static BigDecimal operator_divide(Number x, Number y) {
        BigDecimal xValue = numberToBigDecimal(x);
        BigDecimal yValue = numberToBigDecimal(y);
        return xValue.divide(yValue, 8, RoundingMode.HALF_UP);
    }

    // Comparison operations between numbers

    public static boolean operator_equals(Number left, Number right) {
        // in case one of the Number instances is of type QuantityType they are never equal (except for
        // SmartHomeUnit.ONE).
        // for both instances being QuantityTypes the specific method
        // operator_equals(QuantityType<?> left, QuantityType<?> right) is called by the script engine.
        if (oneIsQuantity(left, right)) {
            return false;
        }
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return rightValue == null;
        } else if (rightValue == null) {
            return false;
        } else {
            return leftValue.compareTo(rightValue) == 0;
        }
    }

    public static boolean operator_notEquals(Number left, Number right) {
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return rightValue != null;
        } else if (rightValue == null) {
            return true;
        } else {
            return leftValue.compareTo(rightValue) != 0;
        }
    }

    public static boolean operator_lessThan(Number left, Number right) {
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return true;
        } else if (rightValue == null) {
            return false;
        } else {
            return leftValue.compareTo(rightValue) < 0;
        }
    }

    public static boolean operator_greaterThan(Number left, Number right) {
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return false;
        } else if (rightValue == null) {
            return true;
        } else {
            return leftValue.compareTo(rightValue) > 0;
        }
    }

    public static boolean operator_lessEqualsThan(Number left, Number right) {
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return true;
        } else if (rightValue == null) {
            return false;
        } else {
            return leftValue.compareTo(rightValue) <= 0;
        }
    }

    public static boolean operator_greaterEqualsThan(Number left, Number right) {
        BigDecimal leftValue = numberToBigDecimal(left);
        BigDecimal rightValue = numberToBigDecimal(right);
        if (leftValue == null) {
            return (rightValue != null) ? false : true;
        } else if (rightValue == null) {
            return true;
        } else {
            return leftValue.compareTo(rightValue) >= 0;
        }
    }

    // Comparison operators between ESH types and numbers

    public static boolean operator_equals(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_equals((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) == 0;
        } else {
            return type == x; // both might be null, then we should return true
        }
    }

    public static boolean operator_notEquals(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_notEquals((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) != 0;
        } else {
            return type != x; // both might be null, then we should return
                              // false, otherwise true
        }
    }

    public static boolean operator_greaterThan(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_greaterThan((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) > 0;
        } else {
            return false;
        }
    }

    public static boolean operator_greaterEqualsThan(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_greaterEqualsThan((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) >= 0;
        } else {
            return false;
        }
    }

    public static boolean operator_lessThan(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_lessThan((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) < 0;
        } else {
            return false;
        }
    }

    public static boolean operator_lessEqualsThan(Type type, Number x) {
        if (type instanceof QuantityType && x instanceof QuantityType) {
            return operator_lessEqualsThan((QuantityType<?>) type, (QuantityType<?>) x);
        }
        if (type != null && type instanceof DecimalType && x != null) {
            return ((DecimalType) type).toBigDecimal().compareTo(numberToBigDecimal(x)) <= 0;
        } else {
            return false;
        }
    }

    // QuantityType support

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static QuantityType<?> operator_plus(QuantityType<?> x, QuantityType<?> y) {
        return x == null ? y : y == null ? x : x.add((QuantityType) y);
    }

    public static QuantityType<?> operator_minus(QuantityType<?> x) {
        return x == null ? null : x.negate();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static QuantityType<?> operator_minus(QuantityType<?> x, QuantityType<?> y) {
        return x == null ? operator_minus(y) : y == null ? x : x.subtract((QuantityType) y);
    }

    public static QuantityType<?> operator_multiply(Number x, QuantityType<?> y) {
        BigDecimal xValue = numberToBigDecimal(x);
        if (xValue == null) {
            return QuantityType.ZERO;
        } else if (y == null) {
            return QuantityType.ZERO;
        } else {
            return y.multiply(xValue);
        }
    }

    public static QuantityType<?> operator_multiply(QuantityType<?> x, Number y) {
        return operator_multiply(y, x);
    }

    public static QuantityType<?> operator_multiply(QuantityType<?> x, QuantityType<?> y) {
        return x == null || y == null ? QuantityType.ZERO : x.multiply(y);
    }

    public static QuantityType<?> operator_divide(QuantityType<?> x, Number y) {
        BigDecimal yValue = numberToBigDecimal(y);
        return x.divide(yValue);
    }

    public static QuantityType<?> operator_divide(Number x, QuantityType<?> y) {
        QuantityType<Dimensionless> xQuantity = new QuantityType<>(x, SmartHomeUnits.ONE);
        return operator_divide(xQuantity, y);
    }

    public static QuantityType<?> operator_divide(QuantityType<?> x, QuantityType<?> y) {
        return x.divide(y);
    }

    public static boolean operator_equals(QuantityType<?> left, QuantityType<?> right) {
        return left.equals(right);
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_equals(QuantityType<?> left, Number right) {
        return operator_equals((Number) left, right);
    }

    public static boolean operator_notEquals(QuantityType<?> left, QuantityType<?> right) {
        return !operator_equals(left, right);
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_notEquals(QuantityType<?> left, Number right) {
        return operator_notEquals((Number) left, right);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean operator_lessThan(QuantityType<?> x, QuantityType<?> y) {
        if (x != null && y != null) {
            return x.compareTo((QuantityType) y) < 0;
        } else {
            return false;
        }
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_lessThan(QuantityType<?> x, Number y) {
        return operator_lessThan((Number) x, y);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean operator_lessEqualsThan(QuantityType<?> x, QuantityType<?> y) {
        if (x != null && y != null) {
            return x.compareTo((QuantityType) y) <= 0;
        } else {
            return false;
        }
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_lessEqualsThan(QuantityType<?> x, Number y) {
        return operator_lessEqualsThan((Number) x, y);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean operator_greaterThan(QuantityType<?> x, QuantityType<?> y) {
        if (x != null && y != null) {
            return x.compareTo((QuantityType) y) > 0;
        } else {
            return false;
        }
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_greaterThan(QuantityType<?> x, Number y) {
        return operator_greaterThan((Number) x, y);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean operator_greaterEqualsThan(QuantityType<?> x, QuantityType<?> y) {
        if (x != null && y != null) {
            return x.compareTo((QuantityType) y) >= 0;
        } else {
            return false;
        }
    }

    // support SmartHomeUnit.ONE as Number representation
    public static boolean operator_greaterEqualsThan(QuantityType<?> x, Number y) {
        return operator_greaterEqualsThan((Number) x, y);
    }

    /**
     * Convert the given number into a BigDecimal
     *
     * @param number
     *            the number to convert
     * @return the given number as BigDecimal or null if number is null
     */
    public static BigDecimal numberToBigDecimal(Number number) {
        if (number instanceof QuantityType) {
            QuantityType<?> state = ((QuantityType<?>) number)
                    .toUnit(((QuantityType<?>) number).getUnit().getSystemUnit());
            if (state != null) {
                return state.toBigDecimal();
            }
            return null;
        }
        if (number != null) {
            return new BigDecimal(number.toString());
        } else {
            return null;
        }
    }

    private static boolean oneIsQuantity(Number left, Number right) {
        return (left instanceof QuantityType && !isAbstractUnitOne((QuantityType<?>) left))
                || (right instanceof QuantityType && !isAbstractUnitOne((QuantityType<?>) right));
    }

    private static boolean isAbstractUnitOne(QuantityType<?> left) {
        return left.getUnit().equals(SmartHomeUnits.ONE);
    }

}
