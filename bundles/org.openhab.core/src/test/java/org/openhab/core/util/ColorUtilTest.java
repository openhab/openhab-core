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
import static org.hamcrest.Matchers.*;
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
import org.openhab.core.util.ColorUtil.Gamut;

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
        // hue is meaningless when saturation is zero
        if (hsb.getSaturation().doubleValue() > 0) {
            assertThat(deltaHue, is(lessThan(5.0)));
        }
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
    public void testConversionRgbToHsbToRgb(int[] rgb) {
        HSBType hsb = ColorUtil.rgbToHsb(rgb);
        Assertions.assertNotNull(hsb);

        final int[] convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(rgb, convertedRgb);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionHsbToRgb(int[] hsb, int[] rgb) {
        final String hsbString = hsb[0] + ", " + hsb[1] + ", " + hsb[2];
        final HSBType hsbType = new HSBType(hsbString);

        final int[] converted = ColorUtil.hsbToRgb(hsbType);
        assertRgbEquals(rgb, converted);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionRgbToRgb(int[] hsb, int[] rgb) {
        final HSBType hsbType = ColorUtil.rgbToHsb(rgb);

        final int[] rgbConverted = ColorUtil.hsbToRgb(hsbType);
        assertRgbEquals(rgb, rgbConverted);
    }

    @ParameterizedTest
    @ArgumentsSource(HsbRgbProvider.class)
    public void testConversionRgbToHsb(int[] hsb, int[] rgb) {
        HSBType hsbType = ColorUtil.rgbToHsb(rgb);

        final String expected = hsb[0] + ", " + hsb[1] + ", " + hsb[2];

        // compare in HSB space, threshold 1% difference
        assertTrue(hsbType.closeTo(new HSBType(expected), 0.01));
    }

    private void xyToXY(double[] xy, Gamut gamut) {
        assertTrue(xy.length > 1);
        HSBType hsb = ColorUtil.xyToHsb(xy, gamut);
        double[] xy2 = ColorUtil.hsbToXY(hsb, gamut);
        assertTrue(xy2.length > 1);
        for (int i = 0; i < xy.length; i++) {
            assertEquals(xy[i], xy2[i], 0.02);
        }
    }

    /**
     * Test XY -> RGB -> HSB - RGB - XY round trips.
     * Use ColorUtil fine precision methods for conversions.
     * Test on Hue standard Gamuts 'A', 'B', and 'C'.
     */
    @Test
    public void testXyHsbRoundTrips() {
        Gamut[] gamuts = new Gamut[] {
                new Gamut(new double[] { 0.704, 0.296 }, new double[] { 0.2151, 0.7106 }, new double[] { 0.138, 0.08 }),
                new Gamut(new double[] { 0.675, 0.322 }, new double[] { 0.409, 0.518 }, new double[] { 0.167, 0.04 }),
                new Gamut(new double[] { 0.6915, 0.3038 }, new double[] { 0.17, 0.7 }, new double[] { 0.1532, 0.0475 }) //
        };
        for (Gamut g : gamuts) {
            xyToXY(g.r(), g);
            xyToXY(g.g(), g);
            xyToXY(g.b(), g);
            xyToXY(new double[] { (g.r()[0] + g.g()[0]) / 2f, (g.r()[1] + g.g()[1]) / 2f }, g);
            xyToXY(new double[] { (g.g()[0] + g.b()[0]) / 2f, (g.g()[1] + g.b()[1]) / 2f }, g);
            xyToXY(new double[] { (g.b()[0] + g.r()[0]) / 2f, (g.b()[1] + g.r()[1]) / 2f }, g);
            xyToXY(new double[] { (g.r()[0] + g.g()[0] + g.b()[0]) / 3f, (g.r()[1] + g.g()[1] + g.b()[1]) / 3f }, g);
            xyToXY(ColorUtil.hsbToXY(HSBType.WHITE), g);
        }
    }

    /* Providers for parameterized tests */

    private static Stream<Arguments> colors() {
        return Stream
                .of(HSBType.BLACK, HSBType.BLUE, HSBType.GREEN, HSBType.RED, HSBType.WHITE,
                        ColorUtil.rgbToHsb(new int[] { 127, 94, 19 }), new HSBType("0,0.1,0"), new HSBType("0,0.1,100"))
                .map(Arguments::of);
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
     * Return a stream RGB values.
     */
    static class RgbValueProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(@Nullable ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(new int[] { 0, 0, 0 }), Arguments.of(new int[] { 255, 255, 255 }),
                    Arguments.of(new int[] { 255, 0, 0 }), Arguments.of(new int[] { 0, 255, 0 }),
                    Arguments.of(new int[] { 0, 0, 255 }), Arguments.of(new int[] { 255, 255, 0 }),
                    Arguments.of(new int[] { 255, 0, 255 }), Arguments.of(new int[] { 0, 255, 255 }),
                    Arguments.of(new int[] { 191, 191, 191 }), Arguments.of(new int[] { 128, 128, 128 }),
                    Arguments.of(new int[] { 128, 0, 0 }), Arguments.of(new int[] { 128, 128, 0 }),
                    Arguments.of(new int[] { 0, 128, 0 }), Arguments.of(new int[] { 128, 0, 128 }),
                    Arguments.of(new int[] { 0, 128, 128 }), Arguments.of(new int[] { 0, 0, 128 }),
                    Arguments.of(new int[] { 0, 132, 255 }), Arguments.of(new int[] { 1, 131, 254 }),
                    Arguments.of(new int[] { 2, 130, 253 }), Arguments.of(new int[] { 3, 129, 252 }),
                    Arguments.of(new int[] { 4, 128, 251 }), Arguments.of(new int[] { 5, 127, 250 }));
        }
    }

    /* Helper functions */

    /**
     * Helper method for checking if expected and actual RGB color parameters (int[3], 0..255) match.
     *
     * When the test fails, both colors are printed.
     *
     * @param expected an HSBType containing the expected color.
     * @param actual an HSBType containing the actual color.
     */
    private void assertRgbEquals(final int[] expected, final int[] actual) {
        if (expected[0] != actual[0] || expected[1] != actual[1] || expected[2] != actual[2]) {
            // only proceed if both RGB colors are not idential,
            // just prepare readable string compare and let it fail
            final String expectedS = expected[0] + ", " + expected[1] + ", " + expected[2];
            final String actualS = actual[0] + ", " + actual[1] + ", " + actual[2];
            assertEquals(expectedS, actualS);
        }
    }
}
