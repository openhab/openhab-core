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
package org.openhab.core.model.core.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class provides mathematical helper functions that are required by
 * code of this bundle.
 * <p>
 * This utility class contains static methods for calculating the greatest
 * common divisor (GCD) and least common multiple (LCM) of integers.
 * <p>
 * This class cannot be instantiated.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class MathUtils {

    private MathUtils() {
        // prevent instantiation
    }

    /**
     * Calculates the greatest common divisor (GCD) of two integers using
     * the Euclidean algorithm.
     * <p>
     * The GCD is always a non-negative number. If both arguments are zero,
     * the result is zero. If one argument is zero, the result is the absolute
     * value of the other argument.
     *
     * @param m first number
     * @param n second number
     * @return the GCD of m and n, always non-negative
     */
    public static int gcd(int m, int n) {
        if (n == 0) {
            return Math.abs(m);
        }
        if (m == 0) {
            return Math.abs(n);
        }
        // Use absolute values to ensure non-negative result
        return gcdRecursive(Math.abs(n), Math.abs(m) % Math.abs(n));
    }

    /**
     * Internal recursive helper method for calculating GCD.
     * Assumes both parameters are non-negative.
     *
     * @param m first number (must be non-negative)
     * @param n second number (must be non-negative)
     * @return the GCD of m and n
     */
    private static int gcdRecursive(int m, int n) {
        if (n == 0) {
            return m;
        }
        return gcdRecursive(n, m % n);
    }

    /**
     * Calculates the least common multiple (LCM) of two integers.
     * <p>
     * The LCM is calculated using the formula: LCM(a, b) = (a / GCD(a, b)) * b
     * This formula prevents arithmetic overflow that could occur with
     * (a * b) / GCD(a, b).
     * <p>
     * If either argument is zero, the result is zero. If both arguments
     * are zero, the result is zero.
     *
     * @param m first number
     * @param n second number
     * @return the LCM of m and n, or 0 if either argument is zero
     */
    public static int lcm(int m, int n) {
        if (m == 0 || n == 0) {
            return 0;
        }
        // Use formula (m / gcd) * n to prevent overflow
        int gcd = gcd(m, n);
        return (m / gcd) * n;
    }

    /**
     * Calculates the greatest common divisor (GCD) of multiple integers.
     * <p>
     * The GCD is calculated iteratively by finding the GCD of pairs of
     * numbers. The result is always non-negative.
     *
     * @param numbers an array of integers, must not be null or empty
     * @return the GCD of all numbers in the array, always non-negative
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static int gcd(Integer[] numbers) {
        if (numbers == null) {
            throw new IllegalArgumentException("Array of numbers cannot be null");
        }
        if (numbers.length == 0) {
            throw new IllegalArgumentException("Array of numbers cannot be empty");
        }
        int result = Math.abs(numbers[0]);
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] == null) {
                throw new IllegalArgumentException("Array elements cannot be null");
            }
            result = gcd(result, numbers[i]);
        }
        return result;
    }

    /**
     * Calculates the least common multiple (LCM) of multiple integers.
     * <p>
     * The LCM is calculated iteratively by finding the LCM of pairs of
     * numbers. If any number in the array is zero, the result is zero.
     *
     * @param numbers an array of integers, must not be null or empty
     * @return the LCM of all numbers in the array, or 0 if any number is zero
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static int lcm(Integer[] numbers) {
        if (numbers == null) {
            throw new IllegalArgumentException("Array of numbers cannot be null");
        }
        if (numbers.length == 0) {
            throw new IllegalArgumentException("Array of numbers cannot be empty");
        }
        int result = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] == null) {
                throw new IllegalArgumentException("Array elements cannot be null");
            }
            result = lcm(result, numbers[i]);
            // Early exit if result becomes zero (since 0 is LCM if any number is 0)
            if (result == 0) {
                return 0;
            }
        }
        return result;
    }
}
