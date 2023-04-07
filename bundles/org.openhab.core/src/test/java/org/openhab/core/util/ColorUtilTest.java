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
package org.openhab.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;

/**
 * The {@link ColorUtilTest} is a test class for the color conversion
 *
 * @author Jan N. Klug - Initial contribution
 * @author Holger Friedrich - Parameterized tests for RGB and HSB conversion
 */
@NonNullByDefault
public class ColorUtilTest {

    @ParameterizedTest
    @MethodSource("colors")
    public void inversionTest(HSBType hsb) {
        HSBType hsb2 = ColorUtil.xyToHsb(ColorUtil.hsbToXY(hsb));

        double deltaHue = Math.abs(hsb.getHue().doubleValue() - hsb2.getHue().doubleValue());
        // if deltaHue > 180, the "other direction" is shorter
        deltaHue = deltaHue > 180.0 ? Math.abs(deltaHue - 360) : deltaHue;
        double deltaSat = Math.abs(hsb.getSaturation().doubleValue() - hsb2.getSaturation().doubleValue());
        double deltaBri = Math.abs(hsb.getBrightness().doubleValue() - hsb2.getBrightness().doubleValue());

        assertThat(deltaHue, is(lessThan(5.0)));
        assertThat(deltaSat, is(lessThanOrEqualTo(1.0)));
        assertThat(deltaBri, is(lessThanOrEqualTo(1.0)));
    }

    @ParameterizedTest
    @MethodSource("invalids")
    public void invalidXyValues(double[] xy) {
        assertThrows(IllegalArgumentException.class, () -> ColorUtil.xyToHsb(xy));
    }

    @Test
    public void testConversionToXY() {
        HSBType hsb = new HSBType("220,90,50");
        PercentType[] xy = hsb.toXY();
        assertEquals(14.65, xy[0].doubleValue(), 0.01);
        assertEquals(11.56, xy[1].doubleValue(), 0.01);
    }

    // test RGB -> HSB -> RGB conversion for different values, including the ones known to cause rounding error
    @ParameterizedTest
    @ArgumentsSource(RgbValueProvider.class)
    public void testConversionRgbToHsbToRgb(int[] rgb, int maxSquaredSum) {
        HSBType hsb = ColorUtil.rgbToHsb(rgb);
        Assertions.assertNotNull(hsb);

        final int[] convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(rgb, convertedRgb, maxSquaredSum);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionHsbToRgb(int[] hsb, int[] rgb) {
        final String hsbString = hsb[0] + ", " + hsb[1] + ", " + hsb[2];
        final HSBType hsbType = new HSBType(hsbString);

        final int[] converted = ColorUtil.hsbToRgb(hsbType);
        assertRgbEquals(rgb, converted, 0);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionRgbToRgb(int[] hsb, int[] rgb) {
        final HSBType hsbType = ColorUtil.rgbToHsb(rgb);

        final int[] rgbConverted = ColorUtil.hsbToRgb(hsbType);
        assertRgbEquals(rgb, rgbConverted, 0);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionRgbToHsb(int[] hsb, int[] rgb) {
        HSBType hsbType = ColorUtil.rgbToHsb(rgb);

        final String expected = hsb[0] + ", " + hsb[1] + ", " + hsb[2];

        // compare in HSB space, threshold 1% difference
        assertTrue(hsbType.closeTo(new HSBType(expected), 0.01));
    }

    /* Providers for parameterized tests */

    private static Stream<Arguments> colors() {
        return Stream.of(HSBType.BLACK, HSBType.BLUE, HSBType.GREEN, HSBType.RED, HSBType.WHITE,
                ColorUtil.rgbToHsb(new int[] { 127, 94, 19 })).map(Arguments::of);
    }

    private static Stream<Arguments> invalids() {
        return Stream.of(new double[] { 0.0 }, new double[] { -1.0, 0.5 }, new double[] { 1.5, 0.5 },
                new double[] { 0.5, -1.0 }, new double[] { 0.5, 1.5 }, new double[] { 0.5, 0.5, -1.0 },
                new double[] { 0.5, 0.5, 1.5 }, new double[] { 0.0, 1.0, 0.0, 1.0 }).map(Arguments::of);
    }

    /*
     * return a stream of well known HSB - RGB pairs
     */
    static class HsbRgbProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(@Nullable ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }),
                    Arguments.of(new int[] { 0, 0, 100 }, new int[] { 255, 255, 255 }),
                    Arguments.of(new int[] { 0, 100, 100 }, new int[] { 255, 0, 0 }),
                    Arguments.of(new int[] { 120, 100, 100 }, new int[] { 0, 255, 0 }),
                    Arguments.of(new int[] { 240, 100, 100 }, new int[] { 0, 0, 255 }),
                    Arguments.of(new int[] { 60, 100, 100 }, new int[] { 255, 255, 0 }),
                    Arguments.of(new int[] { 180, 100, 100 }, new int[] { 0, 255, 255 }),
                    Arguments.of(new int[] { 300, 100, 100 }, new int[] { 255, 0, 255 }),
                    Arguments.of(new int[] { 0, 0, 75 }, new int[] { 191, 191, 191 }),
                    Arguments.of(new int[] { 0, 0, 50 }, new int[] { 128, 128, 128 }),
                    Arguments.of(new int[] { 0, 100, 50 }, new int[] { 128, 0, 0 }),
                    Arguments.of(new int[] { 60, 100, 50 }, new int[] { 128, 128, 0 }),
                    Arguments.of(new int[] { 120, 100, 50 }, new int[] { 0, 128, 0 }),
                    Arguments.of(new int[] { 300, 100, 50 }, new int[] { 128, 0, 128 }),
                    Arguments.of(new int[] { 180, 100, 50 }, new int[] { 0, 128, 128 }),
                    Arguments.of(new int[] { 240, 100, 50 }, new int[] { 0, 0, 128 }));
        }
    }

    /*
     * Return a stream RGB values together with allowed deviation (sum of squared differences).
     * Differences in conversion are due to rounding errors as HSBType is created with integer numbers.
     */

    static class RgbValueProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(@Nullable ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(new int[] { 0, 0, 0 }, 0), Arguments.of(new int[] { 255, 255, 255 }, 0),
                    Arguments.of(new int[] { 255, 0, 0 }, 0), Arguments.of(new int[] { 0, 255, 0 }, 0),
                    Arguments.of(new int[] { 0, 0, 255 }, 0), Arguments.of(new int[] { 255, 255, 0 }, 0),
                    Arguments.of(new int[] { 255, 0, 255 }, 0), Arguments.of(new int[] { 0, 255, 255 }, 0),
                    Arguments.of(new int[] { 191, 191, 191 }, 0), Arguments.of(new int[] { 128, 128, 128 }, 0),
                    Arguments.of(new int[] { 128, 0, 0 }, 0), Arguments.of(new int[] { 128, 128, 0 }, 0),
                    Arguments.of(new int[] { 0, 128, 0 }, 0), Arguments.of(new int[] { 128, 0, 128 }, 0),
                    Arguments.of(new int[] { 0, 128, 128 }, 0), Arguments.of(new int[] { 0, 0, 128 }, 0),
                    Arguments.of(new int[] { 0, 132, 255 }, 0), Arguments.of(new int[] { 1, 131, 254 }, 3),
                    Arguments.of(new int[] { 2, 130, 253 }, 6), Arguments.of(new int[] { 3, 129, 252 }, 4),
                    Arguments.of(new int[] { 4, 128, 251 }, 3), Arguments.of(new int[] { 5, 127, 250 }, 0));
        }
    }

    /* Helper functions */

    /**
     * Helper method for checking if expected and actual RGB color parameters (int[3], 0..255) lie within a given
     * percentage of each other. This method is required in order to eliminate integer rounding artifacts in JUnit tests
     * when comparing RGB values. Asserts that the color parameters of expected and actual do not have a squared sum
     * of differences which exceeds maxSquaredSum.
     *
     * When the test fails, both colors are printed.
     *
     * @param expected an HSBType containing the expected color.
     * @param actual an HSBType containing the actual color.
     * @param maxSquaredSum the maximum allowed squared sum of differences.
     */
    private void assertRgbEquals(final int[] expected, final int[] actual, int maxSquaredSum) {
        int squaredSum = 0;
        if (expected[0] != actual[0] || expected[1] != actual[1] || expected[2] != actual[2]) {
            // only proceed if both RGB colors are not idential
            for (int i = 0; i < 3; i++) {
                int diff = expected[i] - actual[i];
                squaredSum = squaredSum + diff * diff;
            }
            if (squaredSum > maxSquaredSum) {
                // deviation too high, just prepare readable string compare and let it fail
                final String expectedS = expected[0] + ", " + expected[1] + ", " + expected[2];
                final String actualS = actual[0] + ", " + actual[1] + ", " + actual[2];
                assertEquals(expectedS, actualS);
            }
        }
    }
}
