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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ColorUtil} is responsible for converting different color formats.
 *
 * The implementation of HSB/CIE conversion is based work from Erik Baauw for the
 * <a href="https://github.com/ebaauw/homebridge-lib">Homebridge</a>
 * project.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Holger Friedrich - Transfer RGB color conversion from HSBType, improve RGB conversion, restructuring
 * @author Chris Jackson - Added fromRGB (moved from HSBType)
 * @author Andrew Fiddian-Green - Extensive revamp to fix bugs and improve accuracy
 */
@NonNullByDefault
public class ColorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorUtil.class);
    private static final MathContext COLOR_MATH_CONTEXT = new MathContext(5, RoundingMode.HALF_UP);
    private static final BigDecimal BIG_DECIMAL_360 = BigDecimal.valueOf(360);
    private static final BigDecimal BIG_DECIMAL_255 = BigDecimal.valueOf(255);
    private static final BigDecimal BIG_DECIMAL_240 = BigDecimal.valueOf(240);
    private static final BigDecimal BIG_DECIMAL_120 = BigDecimal.valueOf(120);
    private static final BigDecimal BIG_DECIMAL_100 = BigDecimal.valueOf(100);
    private static final BigDecimal BIG_DECIMAL_60 = BigDecimal.valueOf(60);
    private static final BigDecimal BIG_DECIMAL_50 = BigDecimal.valueOf(50);
    private static final BigDecimal BIG_DECIMAL_5 = BigDecimal.valueOf(5);
    private static final BigDecimal BIG_DECIMAL_3 = BigDecimal.valueOf(3);
    private static final BigDecimal BIG_DECIMAL_2 = BigDecimal.valueOf(2);
    private static final BigDecimal BIG_DECIMAL_2_POINT_55 = new BigDecimal("2.55");

    public static final Gamut DEFAULT_GAMUT = new Gamut(new double[] { 0.9961, 0.0001 }, new double[] { 0, 0.9961 },
            new double[] { 0, 0.0001 });

    private ColorUtil() {
        // prevent instantiation
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a>.
     *
     * This function does rounding to integer valued components. It is the preferred way of doing HSB to RGB conversion.
     *
     * See also: {@link #hsbToRgbPercent(HSBType)}, {@link #hsbToRgbw(HSBType)}, {@link #hsbTosRgb(HSBType)}
     *
     * @param hsb an {@link HSBType} value.
     * @return array of three int with the RGB values in the range 0 to 255.
     */
    public static int[] hsbToRgb(HSBType hsb) {
        return getIntArray(hsbToRgbPercent(hsb));
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a>.
     *
     * This function does rounding to integer valued components. It is the preferred way of doing HSB to RGBW
     * conversion.
     *
     * See also: {@link #hsbToRgbPercent(HSBType)}, {@link #hsbToRgbwPercent(HSBType)}, {@link #hsbTosRgb(HSBType)}
     *
     * @param hsb an {@link HSBType} value.
     * @return array of four int with the RGBW values in the range 0 to 255.
     */
    public static int[] hsbToRgbw(HSBType hsb) {
        return getIntArray(hsbToRgbwPercent(hsb));
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a>.
     *
     * This function does not round the components. For conversion to integer values in the range 0 to 255 use
     * {@link #hsbToRgb(HSBType)}.
     *
     * See also: {@link #hsbToRgb(HSBType)}, {@link #hsbTosRgb(HSBType)}, {@link #hsbToRgbwPercent(HSBType)}
     *
     * @param hsb an {@link HSBType} value.
     * @return array of three {@link PercentType} with the RGB values in the range 0 to 100 percent.
     */
    public static PercentType[] hsbToRgbPercent(HSBType hsb) {
        PercentType red;
        PercentType green;
        PercentType blue;

        final BigDecimal h = hsb.getHue().toBigDecimal().divide(BIG_DECIMAL_100, 10, RoundingMode.HALF_UP);
        final BigDecimal s = hsb.getSaturation().toBigDecimal().divide(BIG_DECIMAL_100);

        int hInt = h.multiply(BIG_DECIMAL_5).divide(BIG_DECIMAL_3, 0, RoundingMode.DOWN).intValue();
        final BigDecimal f = h.multiply(BIG_DECIMAL_5).divide(BIG_DECIMAL_3, 10, RoundingMode.HALF_UP)
                .remainder(BigDecimal.ONE);
        final BigDecimal value = hsb.getBrightness().toBigDecimal();

        PercentType a = new PercentType(value.multiply(BigDecimal.ONE.subtract(s)));
        PercentType b = new PercentType(value.multiply(BigDecimal.ONE.subtract(s.multiply(f))));
        PercentType c = new PercentType(
                value.multiply(BigDecimal.ONE.subtract((BigDecimal.ONE.subtract(f)).multiply(s))));

        switch (hInt) {
            case 0:
            case 6:
                red = hsb.getBrightness();
                green = c;
                blue = a;
                break;
            case 1:
                red = b;
                green = hsb.getBrightness();
                blue = a;
                break;
            case 2:
                red = a;
                green = hsb.getBrightness();
                blue = c;
                break;
            case 3:
                red = a;
                green = b;
                blue = hsb.getBrightness();
                break;
            case 4:
                red = c;
                green = a;
                blue = hsb.getBrightness();
                break;
            case 5:
                red = hsb.getBrightness();
                green = a;
                blue = b;
                break;
            default:
                throw new IllegalArgumentException("Could not convert to RGB.");
        }
        return new PercentType[] { red, green, blue };
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to RGBW.
     *
     * See <a href=
     * "https://stackoverflow.com/questions/40312216/converting-rgb-to-rgbw">Converting RGB to RGBW</a>.
     *
     * This function does not round the components. For conversion to integer values in the range 0 to 255 use
     * {@link #hsbToRgb(HSBType)}.
     *
     * See also: {@link #hsbToRgb(HSBType)}, {@link #hsbTosRgb(HSBType)}, {@link #hsbToRgbPercent(HSBType)}
     *
     * @param hsb an {@link HSBType} value.
     * @return array of four {@link PercentType} with the RGBW values in the range 0 to 100 percent.
     */
    public static PercentType[] hsbToRgbwPercent(HSBType hsb) {
        PercentType[] rgbPercents = hsbToRgbPercent(hsb);

        // convert RGB PercentTypes to RGB doubles
        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            rgb[i] = rgbPercents[i].doubleValue();
        }

        // create RGBW array
        PercentType[] rgbw = new PercentType[4];
        if (Math.max(rgb[0], Math.max(rgb[1], rgb[2])) > 0) {
            double whi = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
            for (int i = 0; i < 3; i++) {
                rgbw[i] = new PercentType(BigDecimal.valueOf(rgb[i] - whi));
            }
            rgbw[3] = new PercentType(BigDecimal.valueOf(whi));
        } else {
            for (int i = 0; i < 4; i++) {
                rgbw[i] = PercentType.ZERO;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}",
                    String.format("RGB:[%.6f,%.6f,%.6f] - RGBW:[%.6f,%.6f,%.6f,%.6f]", rgbPercents[0].doubleValue(),
                            rgbPercents[1].doubleValue(), rgbPercents[2].doubleValue(), rgbw[0].doubleValue(),
                            rgbw[1].doubleValue(), rgbw[2].doubleValue(), rgbw[3].doubleValue()));
        }

        return rgbw;
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}
     * to the RGB value representing the color in the default
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> color model.
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
     *
     * See also: {@link #hsbToRgb(HSBType)}, {@link #hsbToRgbPercent(HSBType)}
     *
     * @param hsb an {@link HSBType} value.
     * @return the RGB value of the color in the default sRGB color model.
     */
    public static int hsbTosRgb(HSBType hsb) {
        final int[] rgb = getIntArray(hsbToRgbPercent(hsb));
        return (0xFF << 24) | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | ((rgb[2] & 0xFF) << 0);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> {@code xy} format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developerportal</a>.
     *
     * @param hsb an {@link HSBType} value.
     * @return array of three double with the closest matching CIE 1931 x,y,Y in the range 0.0000 to 1.0000
     */
    public static double[] hsbToXY(HSBType hsb) {
        return hsbToXY(hsb, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> {@code xy} format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param hsb an {@link HSBType} value.
     * @param gamut the color Gamut supported by the light.
     * @return array of three or four double with the closest matching CIE 1931 x,y,Y in the range 0.0000 to 1.0000 -
     *         plus an optional extra empty element to flag if the xyY result has been forced inside the given Gamut.
     */
    public static double[] hsbToXY(HSBType hsb, Gamut gamut) {
        PercentType[] rgbPercents = hsbToRgbPercent(hsb);

        // convert rgbPercents to doubles
        double r = rgbPercents[0].doubleValue() / 100.0;
        double g = rgbPercents[1].doubleValue() / 100.0;
        double b = rgbPercents[2].doubleValue() / 100.0;

        // prevent divide by zero errors
        if (Math.max(r, Math.max(g, b)) <= 0.0) {
            r = 0.000001;
            g = 0.000001;
            b = 0.000001;
        }

        // apply gamma correction
        r = r > 0.04045 ? Math.pow((r + 0.055) / (1.0 + 0.055), 2.4) : r / 12.92;
        g = g > 0.04045 ? Math.pow((g + 0.055) / (1.0 + 0.055), 2.4) : g / 12.92;
        b = b > 0.04045 ? Math.pow((b + 0.055) / (1.0 + 0.055), 2.4) : b / 12.92;

        // convert RGB to XYZ using 'Wide RGB D65' formula
        double X = r * 0.664511 + g * 0.154324 + b * 0.162028;
        double Y = r * 0.283881 + g * 0.668433 + b * 0.047685;
        double Z = r * 0.000088 + g * 0.072310 + b * 0.986039;

        // convert XYZ to xyz
        double sum = X + Y + Z;
        double x = X / sum;
        double y = Y / sum;
        double z = Y;

        // force xy point to be inside the gamut
        Point xy = gamut.closest(new Point(x, y));
        boolean xyForced = xy.x != x || xy.y != y;

        // create xyY; increment array size to flag if xy was forced
        double[] xyY = new double[xyForced ? 4 : 3];
        xyY[0] = xy.x;
        xyY[1] = xy.y;
        xyY[2] = Y;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}", String.format(
                    "HSB:[%.6f,%.6f,%.6f] - RGB:[%.6f,%.6f,%.6f] - RGB':[%.6f,%.6f,%.6f] - XYZ:[%.6f,%.6f,%.6f] - xyz:[%.6f,%.6f,%.6f] - xyY:[%.6f,%.6f,%.6f] (xyForced:%b)",
                    hsb.getHue().doubleValue(), hsb.getSaturation().doubleValue(), hsb.getBrightness().doubleValue(),
                    rgbPercents[0].doubleValue() / 100.0, rgbPercents[1].doubleValue() / 100.0,
                    rgbPercents[2].doubleValue() / 100.0, r, g, b, X, Y, Z, x, y, z, xyY[0], xyY[1], xyY[2], xyForced));
        }

        return xyY;
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> color format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * @param rgbw array of three or four int with the RGB(W) values in the range 0 to 255.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range.
     */
    public static HSBType rgbToHsb(int[] rgbw) throws IllegalArgumentException {
        if (rgbw.length < 3 || rgbw.length > 4) {
            throw new IllegalArgumentException("rgbToHsb() requires 3 or 4 arguments");
        }

        for (int i = 0; i < rgbw.length; i++) {
            if (rgbw[i] < 0 || rgbw[i] > 255) {
                throw new IllegalArgumentException(
                        String.format("rgbToHsb() argument %d value '%f' out of range [0..255]", i, rgbw[i]));
            }
        }

        PercentType[] rgbwPercents = new PercentType[rgbw.length];
        for (int i = 0; i < rgbw.length; i++) {
            rgbwPercents[i] = new PercentType(
                    new BigDecimal(rgbw[i]).divide(BIG_DECIMAL_2_POINT_55, COLOR_MATH_CONTEXT));
        }

        return rgbToHsb(rgbwPercents);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> color format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * @param rgb array of three or four {@link PercentType} with the RGB(W) values in the range 0 to 100 percent.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range.
     */
    public static HSBType rgbToHsb(PercentType[] rgbw) throws IllegalArgumentException {
        if (rgbw.length < 3 || rgbw.length > 4) {
            throw new IllegalArgumentException("rgbToHsb() requires 3 or 4 arguments");
        }

        BigDecimal r;
        BigDecimal g;
        BigDecimal b;

        if (rgbw.length == 3) {
            // use RGB BigDecimal values as-is
            r = rgbw[0].toBigDecimal();
            g = rgbw[1].toBigDecimal();
            b = rgbw[2].toBigDecimal();
        } else {
            // convert RGBW BigDecimal values to RGB BigDecimal values
            double red = rgbw[0].doubleValue();
            double grn = rgbw[1].doubleValue();
            double blu = rgbw[2].doubleValue();
            double max = Math.max(red, Math.max(grn, blu));
            double whi = Math.min(100 - max, rgbw[3].doubleValue());

            if (max > 0 || whi > 0) {
                r = BigDecimal.valueOf(red + whi);
                g = BigDecimal.valueOf(grn + whi);
                b = BigDecimal.valueOf(blu + whi);
            } else {
                r = BigDecimal.ZERO;
                g = BigDecimal.ZERO;
                b = BigDecimal.ZERO;
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{}", String.format("RGBW:[%.6f,%.6f,%.6f,%.6f] - RGB:[%.6f,%.6f,%.6f]", red, grn, blu,
                        whi, r.doubleValue(), g.doubleValue(), b.doubleValue()));
            }
        }

        BigDecimal max = r.max(g).max(b);
        BigDecimal min = r.min(g).min(b);
        BigDecimal span = max.subtract(min);

        if (max.compareTo(BigDecimal.ZERO) == 0) { // all values are 0, return black
            return new HSBType();
        } else if (span.compareTo(BigDecimal.ZERO) == 0) { // all values are equal, return dimmed white
            return new HSBType(new DecimalType(), new PercentType(), new PercentType(max));
        }

        PercentType saturation = new PercentType(span.divide(max, COLOR_MATH_CONTEXT).multiply(BIG_DECIMAL_100));
        PercentType brightness = new PercentType(max);

        BigDecimal scale = span.divide(BIG_DECIMAL_60, COLOR_MATH_CONTEXT);

        BigDecimal redAngle = max.subtract(r).divide(scale, COLOR_MATH_CONTEXT);
        BigDecimal greenAngle = max.subtract(g).divide(scale, COLOR_MATH_CONTEXT);
        BigDecimal blueAngle = max.subtract(b).divide(scale, COLOR_MATH_CONTEXT);

        BigDecimal hue;
        if (r.compareTo(max) == 0) {
            hue = blueAngle.subtract(greenAngle);
        } else if (g.compareTo(max) == 0) {
            hue = BIG_DECIMAL_120.add(redAngle).subtract(blueAngle);
        } else {
            hue = BIG_DECIMAL_240.add(greenAngle).subtract(redAngle);
        }
        if (hue.compareTo(BigDecimal.ZERO) < 0) {
            hue = hue.add(BIG_DECIMAL_360);
        } else if (hue.compareTo(BIG_DECIMAL_360) >= 0) {
            hue = hue.subtract(BIG_DECIMAL_360);
        }

        return new HSBType(new DecimalType(hue), saturation, brightness);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> {@code xy} format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xy array of double with CIE 1931 x,y[,Y] in the range 0.0000 to 1.0000 <code>Y</code> value is optional.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range.
     */
    public static HSBType xyToHsb(double[] xy) throws IllegalArgumentException {
        return xyToHsb(xy, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> {@code xy} format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xyY array of double with CIE 1931 x,y[,Y] in the range 0.0000 to 1.0000 <code>Y</code> value is optional.
     * @param gamut the color Gamut supported by the light.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType xyToHsb(double[] xyY, Gamut gamut) throws IllegalArgumentException {
        if (xyY.length < 2 || xyY.length > 4) {
            throw new IllegalArgumentException("xyToHsb() requires 2, 3 or 4 arguments");
        }

        for (int i = 0; i < xyY.length; i++) {
            if (xyY[i] < 0 || xyY[i] > 1) {
                throw new IllegalArgumentException(
                        String.format("xyToHsb() argument %d value '%f' out of range [0..1.0]", i, xyY[i]));
            }
        }

        // map xy to the closest point on the gamut
        final Point xy = gamut.closest(new Point(xyY[0], xyY[1]));

        // convert to xyz
        final double x = xy.x;
        final double y = xy.y == 0.0 ? 0.000001 : xy.y;
        final double z = 1.0 - x - y;

        // convert xy(Y) to XYZ
        final double Y = xyY.length == 3 && xyY[2] > 0.0 ? xyY[2] : 1.0;
        final double X = (Y / y) * x;
        final double Z = (Y / y) * z;

        // convert XYZ to RGB using 'Wide RGB D65' formula
        final double[] rgb = new double[] {
            // @formatter:off
                X *  1.656492 + Y * -0.354851 + Z * -0.255038,
                X * -0.707196 + Y *  1.655397 + Z *  0.036152,
                X *  0.051713 + Y * -0.121364 + Z *  1.011530 };
            // @formatter:on

        final double[] rgbPrime = rgb.clone();

        // correction for negative values is missing from Philips' documentation.
        double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        if (min < 0.0) {
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] -= min;
            }
        }

        // rescale
        double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        if (max > 1.0) {
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] /= max;
            }
        }

        // remove gamma correction
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = rgb[i] <= 0.0031308 ? 12.92 * rgb[i] : (1.0 + 0.055) * Math.pow(rgb[i], (1.0 / 2.4)) - 0.055;
        }

        // rescale
        max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        if (max > 1.0) {
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] /= max;
            }
        }

        // convert double[] to PercentType[]
        PercentType[] rgbPercents = new PercentType[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            rgbPercents[i] = new PercentType(new BigDecimal(rgb[i]).multiply(BIG_DECIMAL_100, COLOR_MATH_CONTEXT));
        }

        HSBType hsb = rgbToHsb(rgbPercents);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}", String.format(
                    "xyY:[%.6f,%.6f,%.6f] - xyz:[%.6f,%.6f,%.6f] - XYZ:[%.6f,%.6f,%.6f] - RGB':[%.6f,%.6f,%.6f] - RGB:[%.6f,%.6f,%.6f] - HSB:[%.6f,%.6f,%.6f] (xyForced:%b)",
                    xyY[0], xyY[1], Y, x, y, z, X, Y, Z, rgbPrime[0], rgbPrime[1], rgbPrime[2], rgb[0], rgb[1], rgb[2],
                    hsb.getHue().doubleValue(), hsb.getSaturation().doubleValue(), hsb.getBrightness().doubleValue(),
                    xy.x != xyY[0] || xy.y != xyY[1]));
        }

        return hsb;
    }

    /**
     * Get an array of int from an array of PercentType.
     */
    private static int[] getIntArray(PercentType[] percents) {
        int[] ints = new int[percents.length];
        for (int i = 0; i < percents.length; i++) {
            ints[i] = percents[i].toBigDecimal().multiply(BIG_DECIMAL_2_POINT_55).intValue();
        }
        return ints;
    }

    /**
     * Class for points in the CIE xy color space
     */
    public static class Point {
        public final double x;
        public final double y;

        /**
         * a default point with x/y = 0.0
         */
        public Point() {
            this(0.0, 0.0);
        }

        /**
         * a point with the given values
         *
         * @param x the x-value (between 0.0 and 1.0)
         * @param y the y-value (between 0.0 and 1.0)
         */
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * distance between this point and another point
         *
         * @param other the other point
         * @return distance as double
         */
        public double distance(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        /**
         * return the cross product of this tuple and the other tuple
         *
         * @param other the other point
         * @return the cross product as double
         */
        public double crossProduct(Point other) {
            return this.x * other.y - this.y * other.x;
        }

        /**
         * return point closest to this point on a line between a and b
         *
         * @param a point a
         * @param b point b
         * @return the point closest to this point on a-b
         */
        public Point closest(Point a, Point b) {
            Point ap = new Point(this.x - a.x, this.y - a.y);
            Point ab = new Point(b.x - a.x, b.y - a.y);
            double t = Math.min(1.0, Math.max(0, (ap.x * ab.x + ap.y * ab.y) / (ab.x * ab.x + ab.y * ab.y)));

            return new Point(a.x + t * ab.x, a.y + t * ab.y);
        }
    }

    /**
     * Color <a href="https://en.wikipedia.org/wiki/Gamut">gamut</a>
     *
     * @param r double array with {@code xy} coordinates for red, x, y between 0.0000 and 1.0000.
     * @param g double array with {@code xy} coordinates for green, x, y between 0.0000 and 1.0000.
     * @param b double array with {@code xy} coordinates for blue, x, y between 0.0000 and 1.0000.
     */
    public record Gamut(double[] r, double[] g, double[] b) {

        public Gamut {
        }

        /**
         * return point in color gamut closest to a given point
         *
         * @param p a color point
         * @return the color point closest to {@param p} in this gamut
         */
        public Point closest(Point p) {
            Point r = new Point(this.r[0], this.r[1]);
            Point g = new Point(this.g[0], this.g[1]);
            Point b = new Point(this.b[0], this.b[1]);

            Point v1 = new Point(g.x - r.x, g.y - r.y);
            Point v2 = new Point(b.x - r.x, b.y - r.y);
            Point q = new Point(p.x - r.x, p.y - r.y);
            double v = v1.crossProduct(v2);
            double s = q.crossProduct(v2) / v;
            double t = v1.crossProduct(q) / v;
            if (s >= 0.0 && t >= 0.0 && s + t <= 1.0) {
                return p;
            }

            Point pRG = p.closest(r, g);
            Point pGB = p.closest(g, b);
            Point pBR = p.closest(b, r);
            double dRG = p.distance(pRG);
            double dGB = p.distance(pGB);
            double dBR = p.distance(pBR);

            double min = dRG;
            Point retVal = pRG;
            if (dGB < min) {
                min = dGB;
                retVal = pGB;
            }
            if (dBR < min) {
                retVal = pBR;
            }
            return retVal;
        }
    }
}
