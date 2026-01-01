package org.openhab.core.model.core.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MathUtils}.
 *
 * These tests validate correct behavior for normal cases, edge cases
 * (zero and negative values), and defined invalid inputs such as empty
 * arrays or null elements, while respecting the {@code @NonNullByDefault}
 * contract of the production code.
 */
class MathUtilsTest {

    /* ---------- gcd(int, int) ---------- */

    @Test
    void testGcdWithPositiveNumbers() {
        assertEquals(6, MathUtils.gcd(54, 24));
        assertEquals(1, MathUtils.gcd(17, 13));
    }

    @Test
    void testGcdWithZeroValues() {
        assertEquals(5, MathUtils.gcd(5, 0));
        assertEquals(5, MathUtils.gcd(0, 5));
        assertEquals(0, MathUtils.gcd(0, 0));
    }

    @Test
    void testGcdWithNegativeValues() {
        assertEquals(4, MathUtils.gcd(-8, 12));
        assertEquals(4, MathUtils.gcd(8, -12));
        assertEquals(4, MathUtils.gcd(-8, -12));
    }

    /* ---------- lcm(int, int) ---------- */

    @Test
    void testLcmWithPositiveNumbers() {
        assertEquals(216, MathUtils.lcm(54, 24));
        assertEquals(221, MathUtils.lcm(17, 13));
    }

    @Test
    void testLcmWithZeroValues() {
        assertEquals(0, MathUtils.lcm(5, 0));
        assertEquals(0, MathUtils.lcm(0, 5));
        assertEquals(0, MathUtils.lcm(0, 0));
    }

    @Test
    void testLcmWithNegativeValues() {
        assertEquals(-24, MathUtils.lcm(-6, 8));
        assertEquals(-24, MathUtils.lcm(6, -8));
        assertEquals(24, MathUtils.lcm(-6, -8));
    }

    /* ---------- gcd(Integer[]) ---------- */

    @Test
    void testGcdArrayNormalCase() {
        Integer[] values = { 48, 16, 8 };
        assertEquals(8, MathUtils.gcd(values));
    }

    @Test
    void testGcdArrayWithNegativeValues() {
        Integer[] values = { -48, 16, -8 };
        assertEquals(8, MathUtils.gcd(values));
    }

    @Test
    void testGcdArraySingleElement() {
        Integer[] values = { -10 };
        assertEquals(10, MathUtils.gcd(values));
    }

    @Test
    void testGcdArrayEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.gcd(new Integer[0]));
    }

    @Test
    void testGcdArrayWithNullElement() {
        Integer[] values = { 8, null, 4 };
        assertThrows(IllegalArgumentException.class, () -> MathUtils.gcd(values));
    }

    /* ---------- lcm(Integer[]) ---------- */

    @Test
    void testLcmArrayNormalCase() {
        Integer[] values = { 6, 8, 12 };
        assertEquals(24, MathUtils.lcm(values));
    }

    @Test
    void testLcmArrayWithZeroValue() {
        Integer[] values = { 6, 0, 12 };
        assertEquals(0, MathUtils.lcm(values));
    }

    @Test
    void testLcmArrayWithNegativeValues() {
        Integer[] values = { -6, 8 };
        assertEquals(-24, MathUtils.lcm(values));
    }

    @Test
    void testLcmArraySingleElement() {
        Integer[] values = { 7 };
        assertEquals(7, MathUtils.lcm(values));
    }

    @Test
    void testLcmArrayEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.lcm(new Integer[0]));
    }

    @Test
    void testLcmArrayWithNullElement() {
        Integer[] values = { 6, null, 12 };
        assertThrows(IllegalArgumentException.class, () -> MathUtils.lcm(values));
    }
}
