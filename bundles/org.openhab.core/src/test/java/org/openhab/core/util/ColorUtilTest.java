/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.util.ColorUtil.Gamut;
import org.openhab.core.util.ColorUtil.Point;

/**
 * The {@link ColorUtilTest} is a test class for the color conversion
 *
 * @author Jan N. Klug - Initial contribution
 * @author Holger Friedrich - Parameterized tests for RGB and HSB conversion
 * @author Andrew Fiddian-Green - Added tests to detect prior bugs and accuracy limitations
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

    @Test
    public void hsbToRgbwTest() {
        HSBType hsb = HSBType.WHITE;
        PercentType[] rgbw = ColorUtil.hsbToRgbwPercent(hsb);
        assertEquals(0.0, rgbw[0].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[1].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[2].doubleValue(), 0.01);
        assertEquals(100.0, rgbw[3].doubleValue(), 0.01);

        hsb = HSBType.BLACK;
        rgbw = ColorUtil.hsbToRgbwPercent(hsb);
        assertEquals(0.0, rgbw[0].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[1].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[2].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[3].doubleValue(), 0.01);

        hsb = HSBType.RED;
        rgbw = ColorUtil.hsbToRgbwPercent(hsb);
        assertEquals(100.0, rgbw[0].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[1].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[2].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[3].doubleValue(), 0.01);

        hsb = HSBType.GREEN;
        rgbw = ColorUtil.hsbToRgbwPercent(hsb);
        assertEquals(0.0, rgbw[0].doubleValue(), 0.01);
        assertEquals(100.0, rgbw[1].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[2].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[3].doubleValue(), 0.01);

        hsb = HSBType.BLUE;
        rgbw = ColorUtil.hsbToRgbwPercent(hsb);
        assertEquals(0.0, rgbw[0].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[1].doubleValue(), 0.01);
        assertEquals(100.0, rgbw[2].doubleValue(), 0.01);
        assertEquals(0.0, rgbw[3].doubleValue(), 0.01);
    }

    @Test
    public void rgbwToHsbTest() {
        // Test Red
        HSBType hsb = ColorUtil.rgbToHsb(new int[] { 255, 0, 0, 0 });
        int[] convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 0, 0 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(
                new PercentType[] { new PercentType(100), new PercentType(0), new PercentType(0), new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 0, 0 }, convertedRgb);

        // Test Green
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 255, 0, 0 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 255, 0 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(
                new PercentType[] { new PercentType(0), new PercentType(100), new PercentType(0), new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 255, 0 }, convertedRgb);

        // Test Blue
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 0, 255, 0 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 255 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(
                new PercentType[] { new PercentType(0), new PercentType(0), new PercentType(100), new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 255 }, convertedRgb);

        // Test White
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 0, 0, 255 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(
                new PercentType[] { new PercentType(0), new PercentType(0), new PercentType(0), new PercentType(100) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);

        // Test Black
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 0, 0, 0 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 0 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(
                new PercentType[] { new PercentType(0), new PercentType(0), new PercentType(0), new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 0 }, convertedRgb);

        // Test Over-Drive Red
        hsb = ColorUtil.rgbToHsb(new int[] { 255, 0, 0, 255 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 0, 0 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(100), new PercentType(0), new PercentType(0),
                new PercentType(100) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 0, 0 }, convertedRgb);

        // Test Over-Drive Green
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 255, 0, 255 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 255, 0 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(0), new PercentType(100), new PercentType(0),
                new PercentType(100) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 255, 0 }, convertedRgb);

        // Test Over-Drive Blue
        hsb = ColorUtil.rgbToHsb(new int[] { 0, 0, 255, 255 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 255 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(0), new PercentType(0), new PercentType(100),
                new PercentType(100) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 0, 0, 255 }, convertedRgb);

        // Test White - Alternate B
        hsb = ColorUtil.rgbToHsb(new int[] { 255, 255, 255, 0 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(100), new PercentType(100), new PercentType(100),
                new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);

        // Test Over-Drive White
        hsb = ColorUtil.rgbToHsb(new int[] { 255, 255, 255, 255 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(100), new PercentType(100), new PercentType(100),
                new PercentType(100) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 255, 255 }, convertedRgb);

        // Test Unsaturated Orange-Yellow
        hsb = ColorUtil.rgbToHsb(new int[] { 255, 191, 128, 0 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 191, 128 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(100), new PercentType(75), new PercentType(50),
                new PercentType(0) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 191, 128 }, convertedRgb);

        // Test Unsaturated Orange-Yellow - With White
        hsb = ColorUtil.rgbToHsb(new int[] { 155, 91, 28, 100 });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 191, 128 }, convertedRgb);
        hsb = ColorUtil.rgbToHsb(new PercentType[] { new PercentType(61), new PercentType(36), new PercentType(11),
                new PercentType(39) });
        convertedRgb = ColorUtil.hsbToRgb(hsb);
        assertRgbEquals(new int[] { 255, 191, 128 }, convertedRgb);
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

    @Test
    public void testXyToDuv() {
        // Black
        assertEquals(-0.0017d, ColorUtil.xyToDuv(new double[] { 0.3227d, 0.3290d }), 0.0001);
        // 2700K
        assertEquals(0.0000d, ColorUtil.xyToDuv(new double[] { 0.4599d, 0.4106d }), 0.0001);
        // 3000K
        assertEquals(0.0000d, ColorUtil.xyToDuv(new double[] { 0.4369d, 0.4041d }), 0.0001);
        // Red
        assertEquals(0.2727d, ColorUtil.xyToDuv(new double[] { 0.6987d, 0.2974d }), 0.0001);
        // Yellow
        assertEquals(0.0387d, ColorUtil.xyToDuv(new double[] { 0.4442d, 0.5166d }), 0.0001);
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
    @ParameterizedTest
    @MethodSource("gamuts")
    public void testXyHsbRoundTrips(Gamut g) {
        xyToXY(g.r(), g);
        xyToXY(g.g(), g);
        xyToXY(g.b(), g);
        xyToXY(new double[] { (g.r()[0] + g.g()[0]) / 2f, (g.r()[1] + g.g()[1]) / 2f }, g);
        xyToXY(new double[] { (g.g()[0] + g.b()[0]) / 2f, (g.g()[1] + g.b()[1]) / 2f }, g);
        xyToXY(new double[] { (g.b()[0] + g.r()[0]) / 2f, (g.b()[1] + g.r()[1]) / 2f }, g);
        xyToXY(new double[] { (g.r()[0] + g.g()[0] + g.b()[0]) / 3f, (g.r()[1] + g.g()[1] + g.b()[1]) / 3f }, g);
        xyToXY(ColorUtil.hsbToXY(HSBType.WHITE), g);
    }

    /* Providers for parameterized tests */

    private static Stream<Arguments> gamuts() {
        return Stream.of(
                new Gamut(new double[] { 0.704, 0.296 }, new double[] { 0.2151, 0.7106 }, new double[] { 0.138, 0.08 }),
                new Gamut(new double[] { 0.675, 0.322 }, new double[] { 0.409, 0.518 }, new double[] { 0.167, 0.04 }),
                new Gamut(new double[] { 0.6915, 0.3038 }, new double[] { 0.17, 0.7 }, new double[] { 0.1532, 0.0475 }))
                .map(Arguments::of);
    }

    private static Stream<Arguments> colors() {
        return Stream
                .of(HSBType.BLACK, HSBType.BLUE, HSBType.GREEN, HSBType.RED, HSBType.WHITE,
                        ColorUtil.rgbToHsb(new int[] { 127, 94, 19 }), new HSBType("0,0.1,0"), new HSBType("0,0.1,100"))
                .map(Arguments::of);
    }

    private static Stream<Arguments> invalids() {
        return Stream.of(new double[] { 0.0 }, new double[] { -1.0, 0.5 }, new double[] { 1.5, 0.5 },
                new double[] { 0.5, -1.0 }, new double[] { 0.5, 1.5 }, new double[] { 0.5, 0.5, -1.0 },
                new double[] { 0.5, 0.5, 1.5 }).map(Arguments::of);
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

    /*
     * Return an extended stream of HSB values.
     */
    private static Stream<Arguments> allHSB() {
        List<Arguments> result = new ArrayList<>();
        final double step = 5.0;
        for (double h = 0; h < 360; h = h + step) {
            for (double s = 0; s <= 100; s = s + step) {
                for (double b = 0; b <= 100; b = b + step) {
                    result.add(Arguments.of(new double[] { h, s, b }));
                }
            }
        }
        return result.stream();
    }

    /*
     * Return a extended stream of RGB values.
     */
    private static Stream<Arguments> allRGB() {
        List<Arguments> result = new ArrayList<>();
        final double step = 5.0;
        for (double r = 0; r <= 100; r = r + step) {
            for (double g = 0; g <= 100; g = g + step) {
                for (double b = 0; b <= 100; b = b + step) {
                    result.add(Arguments.of(new double[] { r, g, b }));
                }
            }
        }
        return result.stream();
    }

    /*
     * Return an extended stream of XY values within Gamut C overall limits.
     */
    private static Stream<Arguments> allXY() {
        List<Arguments> result = new ArrayList<>();
        final double step = 0.01;
        for (double x = 0.1532; x <= 0.6915; x = x + step) {
            for (double y = 0.0475; y <= 0.7; y = y + step) {
                result.add(Arguments.of(new double[] { x, y }));
            }
        }
        return result.stream();
    }

    /*
     * Return an extended stream of RGBW values.
     */
    private static Stream<Arguments> allRGBW() {
        List<Arguments> result = new ArrayList<>();
        final double step = 5.0;
        for (double r = 0; r <= 100; r = r + step) {
            for (double g = 0; g <= 100; g = g + step) {
                for (double b = 0; b <= 100; b = b + step) {
                    for (double w = 0; w <= 100; w = w + step) {
                        result.add(Arguments.of(new double[] { r, g, b, w }));
                    }
                }
            }
        }
        return result.stream();
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

    /**
     * Test round trips HSB => xyY => HSB
     */
    @ParameterizedTest
    @MethodSource("allHSB")
    public void hsbToXY2xyToHsb(double[] hsb) {
        HSBType hsb1 = new HSBType(new DecimalType(hsb[0]), new PercentType(new BigDecimal(hsb[1])),
                new PercentType(new BigDecimal(hsb[2])));
        double[] xyY = new double[3];
        HSBType hsb2 = HSBType.BLACK;
        try {
            xyY = ColorUtil.hsbToXY(hsb1);
            hsb2 = ColorUtil.xyToHsb(xyY);

            // HSB assertions are meaningless if B is zero, or xy was forced into gamut
            if (hsb[2] == 0 || xyY.length > 3) {
                return;
            }

            // assert that S values are within 0.01%
            assertEquals(hsb1.getSaturation().doubleValue(), hsb2.getSaturation().doubleValue(), 0.01);

            // assert that B values are within 0.01%
            assertEquals(hsb1.getBrightness().doubleValue(), hsb2.getBrightness().doubleValue(), 0.01);

            // H assertions are meaningless if S is zero
            if (hsb[1] == 0) {
                return;
            }

            // assert that H values are within 0.05 degrees
            double h1 = hsb1.getHue().doubleValue();
            h1 = h1 >= 180.0 ? 360.0 - h1 : h1;
            double h2 = hsb2.getHue().doubleValue();
            h2 = h2 >= 180.0 ? 360.0 - h2 : h2;
            assertEquals(h1, h2, 0.05);
        } catch (AssertionError e) {
            throw new AssertionError(
                    String.format("HSB1:[%.6f,%.6f,%.6f] - xyY:[%.6f,%.6f,%.6f] - HSB2:[%.6f,%.6f,%.6f] - %s",
                            hsb1.getHue().doubleValue(), hsb1.getSaturation().doubleValue(),
                            hsb1.getBrightness().doubleValue(), xyY[0], xyY[1], xyY[2], hsb2.getHue().doubleValue(),
                            hsb2.getSaturation().doubleValue(), hsb2.getBrightness().doubleValue(), e.getMessage()));
        }
    }

    /**
     * Test round trips xyY => HSB => xyY
     */
    @ParameterizedTest
    @MethodSource("allXY")
    public void xyToHsb2hsbToXY(double[] xy) {
        Gamut gamutC = new Gamut(new double[] { 0.6915, 0.3038 }, new double[] { 0.17, 0.7 },
                new double[] { 0.1532, 0.0475 });
        HSBType hsb = HSBType.BLACK;
        double[] xy2 = new double[2];
        try {
            Point p = gamutC.closest(new Point(xy[0], xy[1]));

            // XY assertions are meaningless if if xy was forced into gamut
            if (!(p.x == xy[0] && p.y == xy[1])) {
                return;
            }

            double[] xy1 = new double[] { p.x, p.y };
            hsb = ColorUtil.xyToHsb(xy1, gamutC);
            xy2 = ColorUtil.hsbToXY(hsb, gamutC);

            // assert that x and y values are within 0.01%
            assertEquals(xy1[0], xy2[0], 0.01);
            assertEquals(xy1[1], xy2[1], 0.01);
        } catch (AssertionError e) {
            throw new AssertionError(
                    String.format("xy1:[%.6f,%.6f] - HSB:[%.6f,%.6f,%.6f] - xyY2:[%.6f,%.6f,%.6f] - %s", xy[0], xy[1],
                            hsb.getHue().doubleValue(), hsb.getSaturation().doubleValue(),
                            hsb.getBrightness().doubleValue(), xy2[0], xy2[1], xy2[2], e.getMessage()));
        }
    }

    /**
     * Test round trips HSB => RGB => HSB
     */
    @ParameterizedTest
    @MethodSource("allHSB")
    public void hsbToRgb2rgbToHsb(double[] hsb) {
        HSBType hsb1 = new HSBType(new DecimalType(hsb[0]), new PercentType(new BigDecimal(hsb[1])),
                new PercentType(new BigDecimal(hsb[2])));
        PercentType[] rgb = new PercentType[3];
        HSBType hsb2 = HSBType.BLACK;
        try {
            rgb = ColorUtil.hsbToRgbPercent(hsb1);
            hsb2 = ColorUtil.rgbToHsb(rgb);

            // HSB assertions are meaningless if B is zero
            if (hsb[2] == 0) {
                return;
            }

            assertEquals(hsb1.getSaturation().doubleValue(), hsb2.getSaturation().doubleValue(), 0.01);
            assertEquals(hsb1.getBrightness().doubleValue(), hsb2.getBrightness().doubleValue(), 0.01);

            // H assertions are meaningless if S is zero
            if (hsb[1] == 0) {
                return;
            }

            assertEquals(hsb1.getHue().doubleValue(), hsb2.getHue().doubleValue(), 0.05);
        } catch (AssertionError e) {
            throw new AssertionError(
                    String.format("HSB1:[%.6f,%.6f,%.6f] - RGB:[%.6f,%.6f,%.6f] - HSB2:[%.6f,%.6f,%.6f] - %s",
                            hsb1.getHue().doubleValue() / 100, hsb1.getSaturation().doubleValue() / 100,
                            hsb1.getBrightness().doubleValue() / 100, rgb[0].doubleValue() / 100,
                            rgb[1].doubleValue() / 100, rgb[2].doubleValue() / 100, hsb2.getHue().doubleValue() / 100,
                            hsb2.getSaturation().doubleValue() / 100, hsb2.getBrightness().doubleValue() / 100,
                            e.getMessage()));
        }
    }

    /**
     * Test round trips RGB => HSB => RGB
     */
    @ParameterizedTest
    @MethodSource("allRGB")
    public void rgbToHsb2hsbToRgb(double[] rgb) {
        PercentType[] rgb1 = new PercentType[] { new PercentType(new BigDecimal(rgb[0])),
                new PercentType(new BigDecimal(rgb[1])), new PercentType(new BigDecimal(rgb[2])) };
        HSBType hsb = HSBType.BLACK;
        PercentType[] rgb2 = new PercentType[3];
        try {
            hsb = ColorUtil.rgbToHsb(rgb1);
            rgb2 = ColorUtil.hsbToRgbPercent(hsb);

            // RGB assertions are meaningless if B or S are zero
            if (hsb.getBrightness().doubleValue() == 0 || hsb.getSaturation().doubleValue() == 0) {
                return;
            }

            for (int i = 0; i < rgb1.length; i++) {
                assertEquals(rgb1[i].doubleValue(), rgb2[i].doubleValue(), 0.05);
            }
        } catch (AssertionError e) {
            throw new AssertionError(
                    String.format("RGB1:[%.6f,%.6f,%.6f] - HSB:[%.6f,%.6f,%.6f] - RGB2:[%.6f,%.6f,%.6f] - %s",
                            rgb1[0].doubleValue() / 100, rgb1[1].doubleValue() / 100, rgb1[2].doubleValue() / 100,
                            hsb.getHue().doubleValue() / 100, hsb.getSaturation().doubleValue() / 100,
                            hsb.getBrightness().doubleValue() / 100, rgb2[0].doubleValue() / 100,
                            rgb2[1].doubleValue() / 100, rgb2[2].doubleValue() / 100, e.getMessage()));
        }
    }

    /**
     * Test round trips RGBW => HSB => RGBW
     */
    @ParameterizedTest
    @MethodSource("allRGBW")
    public void rgbwToHsb2hsbToRgbw(double[] rgbw) {
        PercentType[] rgbw1 = new PercentType[] { new PercentType(new BigDecimal(rgbw[0])),
                new PercentType(new BigDecimal(rgbw[1])), new PercentType(new BigDecimal(rgbw[2])),
                new PercentType(new BigDecimal(rgbw[3])) };
        HSBType hsb = HSBType.BLACK;
        PercentType[] rgbw2 = new PercentType[4];
        try {
            hsb = ColorUtil.rgbToHsb(rgbw1);
            rgbw2 = ColorUtil.hsbToRgbwPercent(hsb);

            // RGB assertions are meaningless if B or S are zero
            if (hsb.getBrightness().doubleValue() == 0 || hsb.getSaturation().doubleValue() == 0) {
                return;
            }

            // RGB assertions are meaningless if W exceeds max head-room
            if (rgbw[3] > 100 - Math.max(rgbw[0], Math.max(rgbw[1], rgbw[2]))) {
                return;
            }

            for (int i = 0; i < 3; i++) {
                assertEquals(rgbw1[i].doubleValue() + rgbw1[3].doubleValue(),
                        rgbw2[i].doubleValue() + rgbw2[3].doubleValue(), 0.05);
            }
        } catch (AssertionError e) {
            throw new AssertionError(
                    String.format("RGB1:[%.6f,%.6f,%.6f,%.6f] - HSB:[%.6f,%.6f,%.6f] - RGB2:[%.6f,%.6f,%.6f,%.6f] - %s",
                            rgbw1[0].doubleValue() / 100, rgbw1[1].doubleValue() / 100, rgbw1[2].doubleValue() / 100,
                            rgbw1[3].doubleValue() / 100, hsb.getHue().doubleValue() / 100,
                            hsb.getSaturation().doubleValue() / 100, hsb.getBrightness().doubleValue() / 100,
                            rgbw2[0].doubleValue() / 100, rgbw2[1].doubleValue() / 100, rgbw2[2].doubleValue() / 100,
                            rgbw2[3].doubleValue() / 100, e.getMessage()));
        }
    }

    /**
     * Test conversion between colour temperature in Kelvin and points on the colour
     * temperature locus in the CIE XY colour space
     */
    @Test
    void testKelvinXyConversion() {
        // test minimum and maximum limits 500..153 Mirek i.e. 2000..6536 Kelvin
        assertThrows(IndexOutOfBoundsException.class, () -> ColorUtil.kelvinToXY(1000000 / 501));
        assertDoesNotThrow(() -> ColorUtil.kelvinToXY(1000000 / 500));
        assertDoesNotThrow(() -> ColorUtil.kelvinToXY(1000000 / 153));
        assertThrows(IndexOutOfBoundsException.class, () -> ColorUtil.kelvinToXY(1000000 / 152));

        // test round trips K => XY => K
        for (double kelvin = 2000; kelvin <= 6536; kelvin += 5) {
            assertEquals(kelvin, ColorUtil.xyToKelvin(ColorUtil.kelvinToXY(kelvin)), 15);
        }
    }
}
