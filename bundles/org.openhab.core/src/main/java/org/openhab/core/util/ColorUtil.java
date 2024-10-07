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
 * @author Cody Cutrer - Added xyToDuv
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
    private static final BigDecimal BIG_DECIMAL_5 = BigDecimal.valueOf(5);
    private static final BigDecimal BIG_DECIMAL_3 = BigDecimal.valueOf(3);
    private static final BigDecimal BIG_DECIMAL_2 = BigDecimal.valueOf(2);
    private static final BigDecimal BIG_DECIMAL_2_POINT_55 = new BigDecimal("2.55");
    private static final double[] CORM_COEFFICIENTS = { -0.00616793, 0.0893944, -0.5179722, 1.5317403, -2.4243787,
            1.925865, -0.471106 };

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
     * @param rgbw array of three or four {@link PercentType} with the RGB(W) values in the range 0 to 100 percent.
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
     * Calculate the Duv (Delta u,v) metric from a
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> {@code xy} format color.
     *
     * Duv describes the distance of a color point from the black body curve. It's useful for calculating
     * if a color is near to "white", at any color temperature.
     * 
     * @param xy array of double with CIE 1931 x,y in the range 0.0000 to 1.0000
     * @return the calculated Duv metric
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range.
     */
    public static double xyToDuv(double[] xy) throws IllegalArgumentException {
        if (xy.length != 2) {
            throw new IllegalArgumentException("xyToDuv() requires 2 arguments");
        }

        for (int i = 0; i < xy.length; i++) {
            if (xy[i] < 0 || xy[i] > 1) {
                throw new IllegalArgumentException(
                        String.format("xyToDuv() argument %d value '%f' out of range [0..1.0]", i, xy[i]));
            }
        }

        double x = xy[0];
        double y = xy[1];
        double u = 4.0 * x / (-2.0 * x + 12 * y + 3.0);
        double v = 6.0 * y / (-2.0 * x + 12 * y + 3.0);
        double Lfp = Math.sqrt(Math.pow(u - 0.292, 2) + Math.pow(v - 0.24, 2));
        double a = Math.acos((u - 0.292) / Lfp);
        double Lbb = polynomialFit(a, CORM_COEFFICIENTS);
        return Lfp - Lbb;
    }

    /**
     * Get an array of int from an array of PercentType.
     */
    private static int[] getIntArray(PercentType[] percents) {
        int[] ints = new int[percents.length];
        for (int i = 0; i < percents.length; i++) {
            ints[i] = percents[i].toBigDecimal().multiply(BIG_DECIMAL_255)
                    .divide(BIG_DECIMAL_100, 0, RoundingMode.HALF_UP).intValue();
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

    /**
     * Lookup table for converting colour temperatures in Kelvin to a point in the CIE XY colour space.
     * Elements of the array comprise three parts: colour temperature (K), X coordinate, Y coordinate.
     */
    private static final double[][] KELVIN_TO_XY_LOOKUP_TABLE = {
        //@formatter:off
        { 2000, 0.526676280311873, 0.41329727450763 },
        { 2010, 0.52558700949522, 0.413461772751086 },
        { 2020, 0.524501547047308, 0.413618464845305 },
        { 2030, 0.523419902728187, 0.413767442985273 },
        { 2040, 0.522342085989252, 0.413908799143933 },
        { 2050, 0.521268105968449, 0.414042625047528 },
        { 2060, 0.520197971485871, 0.414169012151805 },
        { 2070, 0.519131691039726, 0.414288051619026 },
        { 2080, 0.518069272802678, 0.414399834295808 },
        { 2090, 0.51701072461855, 0.41450445069175 },
        { 2100, 0.515956053999373, 0.414601990958848 },
        { 2110, 0.514905268122789, 0.414692544871688 },
        { 2120, 0.513858373829776, 0.414776201808388 },
        { 2130, 0.512815377622702, 0.414853050732289 },
        { 2140, 0.511776285663697, 0.414923180174373 },
        { 2150, 0.510741103773325, 0.414986678216401 },
        { 2160, 0.509709837429556, 0.415043632474748 },
        { 2170, 0.508682491767023, 0.415094130084932 },
        { 2180, 0.507659071576558, 0.415138257686807 },
        { 2190, 0.506639581304996, 0.415176101410431 },
        { 2200, 0.505624025055233, 0.415207746862562 },
        { 2210, 0.504612406586543, 0.415233279113804 },
        { 2220, 0.50360472931513, 0.415252782686356 },
        { 2230, 0.502600996314909, 0.415266341542373 },
        { 2240, 0.501601210318512, 0.41527403907292 },
        { 2250, 0.500605373718514, 0.415275958087495 },
        { 2260, 0.499613488568853, 0.415272180804119 },
        { 2270, 0.498625556586457, 0.415262788839981 },
        { 2280, 0.497641579153048, 0.415247863202608 },
        { 2290, 0.496661557317139, 0.415227484281571 },
        { 2300, 0.495685491796189, 0.415201731840687 },
        { 2310, 0.494713382978936, 0.415170685010734 },
        { 2320, 0.49374523092787, 0.415134422282633 },
        { 2330, 0.492781035381868, 0.415093021501115 },
        { 2340, 0.491820795758963, 0.415046559858839 },
        { 2350, 0.490864511159246, 0.414995113890958 },
        { 2360, 0.489912180367896, 0.414938759470118 },
        { 2370, 0.488963801858331, 0.414877571801882 },
        { 2380, 0.488019373795462, 0.414811625420557 },
        { 2390, 0.48707889403907, 0.41474099418543 },
        { 2400, 0.486142360147258, 0.414665751277374 },
        { 2410, 0.485209769380024, 0.414585969195845 },
        { 2420, 0.484281118702893, 0.41450171975623 },
        { 2430, 0.483356404790651, 0.414413074087558 },
        { 2440, 0.482435624031141, 0.41432010263054 },
        { 2450, 0.481518772529133, 0.414222875135951 },
        { 2460, 0.480605846110259, 0.414121460663327 },
        { 2470, 0.479696840325004, 0.414015927579967 },
        { 2480, 0.478791750452749, 0.413906343560247 },
        { 2490, 0.477890571505866, 0.413792775585208 },
        { 2500, 0.476993298233858, 0.413675289942435 },
        { 2510, 0.476099925127525, 0.413553952226202 },
        { 2520, 0.475210446423188, 0.413428827337877 },
        { 2530, 0.474324856106916, 0.413299979486579 },
        { 2540, 0.473443147918802, 0.413167472190078 },
        { 2550, 0.472565315357245, 0.413031368275923 },
        { 2560, 0.471691351683258, 0.412891729882807 },
        { 2570, 0.470821249924792, 0.412748618462133 },
        { 2580, 0.469955002881062, 0.412602094779799 },
        { 2590, 0.469092603126886, 0.412452218918178 },
        { 2600, 0.468234043017033, 0.41229905027829 },
        { 2610, 0.46737931469056, 0.412142647582159 },
        { 2620, 0.466528410075157, 0.411983068875347 },
        { 2630, 0.465681320891482, 0.411820371529656 },
        { 2640, 0.464838038657489, 0.411654612245991 },
        { 2650, 0.463998554692746, 0.411485847057383 },
        { 2660, 0.463162860122743, 0.411314131332149 },
        { 2670, 0.462330945883173, 0.411139519777208 },
        { 2680, 0.461502802724214, 0.41096206644152 },
        { 2690, 0.460678421214773, 0.410781824719658 },
        { 2700, 0.459857791746717, 0.410598847355503 },
        { 2710, 0.459040904539081, 0.410413186446054 },
        { 2720, 0.458227749642249, 0.410224893445347 },
        { 2730, 0.457418316942099, 0.410034019168485 },
        { 2740, 0.456612596164132, 0.409840613795755 },
        { 2750, 0.455810576877562, 0.409644726876854 },
        { 2760, 0.455012248499377, 0.409446407335191 },
        { 2770, 0.45421760029836, 0.40924570347228 },
        { 2780, 0.453426621399085, 0.409042662972206 },
        { 2790, 0.452639300785869, 0.408837332906172 },
        { 2800, 0.451855627306685, 0.408629759737106 },
        { 2810, 0.451075589677048, 0.408419989324343 },
        { 2820, 0.450299176483844, 0.408208066928357 },
        { 2830, 0.449526376189136, 0.407994037215556 },
        { 2840, 0.448757177133914, 0.407777944263126 },
        { 2850, 0.447991567541816, 0.407559831563923 },
        { 2860, 0.447229535522791, 0.407339742031414 },
        { 2870, 0.446471069076735, 0.407117718004646 },
        { 2880, 0.445716156097067, 0.406893801253269 },
        { 2890, 0.444964784374273, 0.406668032982576 },
        { 2900, 0.444216941599399, 0.406440453838584 },
        { 2910, 0.443472615367494, 0.406211103913138 },
        { 2920, 0.442731793181017, 0.405980022749035 },
        { 2930, 0.441994462453189, 0.405747249345174 },
        { 2940, 0.441260610511305, 0.405512822161723 },
        { 2950, 0.440530224599989, 0.405276779125295 },
        { 2960, 0.439803291884415, 0.405039157634144 },
        { 2970, 0.439079799453468, 0.404799994563365 },
        { 2980, 0.438359734322871, 0.404559326270101 },
        { 2990, 0.437643083438248, 0.404317188598758 },
        { 3000, 0.436929833678155, 0.404073616886221 },
        { 3010, 0.436219971857052, 0.403828645967064 },
        { 3020, 0.435513484728235, 0.403582310178768 },
        { 3030, 0.434810358986718, 0.403334643366923 },
        { 3040, 0.434110581272061, 0.403085678890438 },
        { 3050, 0.433414138171168, 0.402835449626724 },
        { 3060, 0.432721016221012, 0.402583987976887 },
        { 3070, 0.43203120191134, 0.402331325870893 },
        { 3080, 0.431344681687312, 0.402077494772728 },
        { 3090, 0.4306614419521, 0.401822525685539 },
        { 3100, 0.429981469069442, 0.401566449156763 },
        { 3110, 0.429304749366147, 0.401309295283229 },
        { 3120, 0.428631269134558, 0.401051093716253 },
        { 3130, 0.427961014634963, 0.400791873666696 },
        { 3140, 0.427293972097967, 0.400531663910017 },
        { 3150, 0.426630127726814, 0.400270492791288 },
        { 3160, 0.425969467699671, 0.400008388230194 },
        { 3170, 0.425311978171859, 0.399745377726 },
        { 3180, 0.424657645278049, 0.399481488362498 },
        { 3190, 0.424006455134406, 0.399216746812922 },
        { 3200, 0.423358393840696, 0.398951179344833 },
        { 3210, 0.422713447482352, 0.39868481182498 },
        { 3220, 0.422071602132489, 0.398417669724122 },
        { 3230, 0.421432843853882, 0.39814977812183 },
        { 3240, 0.420797158700909, 0.397881161711245 },
        { 3250, 0.42016453272144, 0.39761184480381 },
        { 3260, 0.419534951958697, 0.397341851333969 },
        { 3270, 0.418908402453069, 0.397071204863828 },
        { 3280, 0.418284870243884, 0.396799928587787 },
        { 3290, 0.41766434137115, 0.396528045337128 },
        { 3300, 0.417046801877253, 0.396255577584578 },
        { 3310, 0.416432237808611, 0.395982547448827 },
        { 3320, 0.415820635217303, 0.395708976699012 },
        { 3330, 0.415211980162654, 0.395434886759171 },
        { 3340, 0.414606258712775, 0.395160298712644 },
        { 3350, 0.414003456946084, 0.394885233306455 },
        { 3360, 0.41340356095278, 0.394609710955639 },
        { 3370, 0.41280655683628, 0.394333751747546 },
        { 3380, 0.412212430714629, 0.39405737544609 },
        { 3390, 0.411621168721873, 0.393780601495975 },
        { 3400, 0.411032757009395, 0.393503449026874 },
        { 3410, 0.410447181747219, 0.393225936857565 },
        { 3420, 0.409864429125286, 0.392948083500039 },
        { 3430, 0.409284485354692, 0.392669907163558 },
        { 3440, 0.408707336668894, 0.39239142575868 },
        { 3450, 0.408132969324891, 0.39211265690124 },
        { 3460, 0.407561369604368, 0.391833617916295 },
        { 3470, 0.406992523814813, 0.391554325842027 },
        { 3480, 0.406426418290599, 0.391274797433607 },
        { 3490, 0.405863039394048, 0.390995049167019 },
        { 3500, 0.405302373516452, 0.390715097242847 },
        { 3510, 0.404744407079075, 0.390434957590016 },
        { 3520, 0.404189126534126, 0.390154645869498 },
        { 3530, 0.403636518365702, 0.389874177477982 },
        { 3540, 0.403086569090702, 0.389593567551495 },
        { 3550, 0.402539265259722, 0.389312830968992 },
        { 3560, 0.401994593457919, 0.389031982355903 },
        { 3570, 0.401452540305847, 0.388751036087639 },
        { 3580, 0.400913092460274, 0.388470006293066 },
        { 3590, 0.400376236614968, 0.388188906857933 },
        { 3600, 0.399841959501467, 0.387907751428262 },
        { 3610, 0.399310247889814, 0.387626553413704 },
        { 3620, 0.398781088589278, 0.387345325990855 },
        { 3630, 0.398254468449049, 0.387064082106531 },
        { 3640, 0.397730374358909, 0.386782834481003 },
        { 3650, 0.397208793249882, 0.386501595611205 },
        { 3660, 0.396689712094866, 0.386220377773894 },
        { 3670, 0.396173117909235, 0.385939193028774 },
        { 3680, 0.395658997751428, 0.385658053221589 },
        { 3690, 0.395147338723516, 0.385376969987172 },
        { 3700, 0.394638127971748, 0.38509595475246 },
        { 3710, 0.394131352687072, 0.384815018739475 },
        { 3720, 0.393627000105649, 0.384534172968266 },
        { 3730, 0.393125057509339, 0.384253428259816 },
        { 3740, 0.392625512226168, 0.383972795238911 },
        { 3750, 0.392128351630781, 0.383692284336979 },
        { 3760, 0.391633563144876, 0.383411905794889 },
        { 3770, 0.391141134237623, 0.383131669665717 },
        { 3780, 0.390651052426057, 0.382851585817475 },
        { 3790, 0.390163305275465, 0.382571663935814 },
        { 3800, 0.389677880399752, 0.382291913526683 },
        { 3810, 0.389194765461792, 0.382012343918955 },
        { 3820, 0.388713948173757, 0.38173296426703 },
        { 3830, 0.388235416297444, 0.381453783553394 },
        { 3840, 0.387759157644574, 0.38117481059115 },
        { 3850, 0.387285160077084, 0.380896054026513 },
        { 3860, 0.386813411507402, 0.380617522341278 },
        { 3870, 0.386343899898705, 0.380339223855255 },
        { 3880, 0.385876613265173, 0.380061166728665 },
        { 3890, 0.385411539672215, 0.379783358964516 },
        { 3900, 0.384948667236696, 0.37950580841094 },
        { 3910, 0.384487984127144, 0.3792285227635 },
        { 3920, 0.384029478563943, 0.378951509567474 },
        { 3930, 0.383573138819519, 0.378674776220096 },
        { 3940, 0.383118953218507, 0.37839832997278 },
        { 3950, 0.382666910137919, 0.378122177933306 },
        { 3960, 0.382216998007281, 0.377846327067982 },
        { 3970, 0.38176920530878, 0.377570784203772 },
        { 3980, 0.381323520577382, 0.377295556030402 },
        { 3990, 0.380879932400953, 0.377020649102431 },
        { 4000, 0.380438429420364, 0.376746069841299 },
        { 4010, 0.379999000329579, 0.376471824537343 },
        { 4020, 0.379561633875749, 0.376197919351794 },
        { 4030, 0.37912631885928, 0.375924360318735 },
        { 4040, 0.378693044133903, 0.37565115334704 },
        { 4050, 0.378261798606731, 0.375378304222286 },
        { 4060, 0.377832571238301, 0.37510581860864 },
        { 4070, 0.377405351042622, 0.374833702050712 },
        { 4080, 0.376980127087195, 0.374561959975396 },
        { 4090, 0.376556888493043, 0.37429059769367 },
        { 4100, 0.376135624434723, 0.374019620402386 },
        { 4110, 0.37571632414033, 0.373749033186026 },
        { 4120, 0.375298976891495, 0.373478841018437 },
        { 4130, 0.37488357202338, 0.373209048764539 },
        { 4140, 0.374470098924658, 0.372939661182012 },
        { 4150, 0.374058547037489, 0.372670682922962 },
        { 4160, 0.373648905857491, 0.372402118535558 },
        { 4170, 0.373241164933703, 0.372133972465647 },
        { 4180, 0.372835313868541, 0.371866249058352 },
        { 4190, 0.372431342317747, 0.371598952559641 },
        { 4200, 0.372029239990332, 0.371332087117875 },
        { 4210, 0.371628996648516, 0.371065656785341 },
        { 4220, 0.371230602107658, 0.370799665519753 },
        { 4230, 0.37083404623618, 0.370534117185738 },
        { 4240, 0.370439318955492, 0.370269015556301 },
        { 4250, 0.370046410239904, 0.370004364314267 },
        { 4260, 0.369655310116535, 0.369740167053704 },
        { 4270, 0.369266008665223, 0.369476427281325 },
        { 4280, 0.368878496018416, 0.369213148417869 },
        { 4290, 0.368492762361075, 0.368950333799466 },
        { 4300, 0.368108797930561, 0.368687986678976 },
        { 4310, 0.36772659301652, 0.36842611022732 },
        { 4320, 0.367346137960767, 0.368164707534776 },
        { 4330, 0.36696742315716, 0.36790378161227 },
        { 4340, 0.366590439051477, 0.367643335392644 },
        { 4350, 0.366215176141282, 0.367383371731904 },
        { 4360, 0.36584162497579, 0.36712389341045 },
        { 4370, 0.365469776155733, 0.366864903134292 },
        { 4380, 0.365099620333213, 0.366606403536243 },
        { 4390, 0.364731148211558, 0.366348397177103 },
        { 4400, 0.364364350545175, 0.366090886546814 },
        { 4410, 0.363999218139393, 0.365833874065608 },
        { 4420, 0.363635741850315, 0.365577362085137 },
        { 4430, 0.363273912584652, 0.365321352889581 },
        { 4440, 0.362913721299567, 0.365065848696745 },
        { 4450, 0.362555159002508, 0.36481085165914 },
        { 4460, 0.362198216751042, 0.364556363865043 },
        { 4470, 0.361842885652686, 0.36430238733955 },
        { 4480, 0.361489156864733, 0.364048924045607 },
        { 4490, 0.36113702159408, 0.363795975885023 },
        { 4500, 0.360786471097048, 0.363543544699483 },
        { 4510, 0.360437496679207, 0.363291632271526 },
        { 4520, 0.36009008969519, 0.363040240325526 },
        { 4530, 0.359744241548512, 0.362789370528647 },
        { 4540, 0.359399943691386, 0.362539024491787 },
        { 4550, 0.35905718762453, 0.362289203770517 },
        { 4560, 0.358715964896985, 0.36203990986599 },
        { 4570, 0.35837626710592, 0.361791144225849 },
        { 4580, 0.358038085896437, 0.361542908245116 },
        { 4590, 0.357701412961382, 0.361295203267073 },
        { 4600, 0.357366240041146, 0.361048030584121 },
        { 4610, 0.357032558923467, 0.360801391438638 },
        { 4620, 0.356700361443233, 0.360555287023811 },
        { 4630, 0.356369639482281, 0.360309718484469 },
        { 4640, 0.356040384969197, 0.360064686917893 },
        { 4650, 0.355712589879109, 0.359820193374621 },
        { 4660, 0.35538624623349, 0.359576238859237 },
        { 4670, 0.355061346099949, 0.359332824331149 },
        { 4680, 0.354737881592023, 0.359089950705356 },
        { 4690, 0.354415844868975, 0.358847618853204 },
        { 4700, 0.354095228135586, 0.358605829603133 },
        { 4710, 0.353776023641942, 0.358364583741404 },
        { 4720, 0.353458223683228, 0.358123882012824 },
        { 4730, 0.353141820599519, 0.357883725121459 },
        { 4740, 0.352826806775565, 0.357644113731333 },
        { 4750, 0.352513174640586, 0.357405048467119 },
        { 4760, 0.35220091666805, 0.357166529914816 },
        { 4770, 0.351890025375474, 0.356928558622422 },
        { 4780, 0.351580493324198, 0.356691135100592 },
        { 4790, 0.351272313119179, 0.356454259823288 },
        { 4800, 0.350965477408777, 0.356217933228418 },
        { 4810, 0.350659978884535, 0.355982155718466 },
        { 4820, 0.350355810280973, 0.355746927661114 },
        { 4830, 0.350052964375366, 0.355512249389855 },
        { 4840, 0.349751433987532, 0.355278121204588 },
        { 4850, 0.349451211979616, 0.355044543372218 },
        { 4860, 0.349152291255876, 0.35481151612724 },
        { 4870, 0.348854664762465, 0.354579039672309 },
        { 4880, 0.348558325487217, 0.35434711417881 },
        { 4890, 0.348263266459433, 0.354115739787415 },
        { 4900, 0.347969480749661, 0.353884916608634 },
        { 4910, 0.347676961469483, 0.353654644723354 },
        { 4920, 0.3473857017713, 0.353424924183371 },
        { 4930, 0.347095694848114, 0.353195755011921 },
        { 4940, 0.346806933933317, 0.352967137204187 },
        { 4950, 0.346519412300469, 0.352739070727817 },
        { 4960, 0.346233123263089, 0.352511555523418 },
        { 4970, 0.345948060174438, 0.352284591505058 },
        { 4980, 0.345664216427301, 0.352058178560741 },
        { 4990, 0.345381585453779, 0.351832316552895 },
        { 5000, 0.345100160725069, 0.35160700531884 },
        { 5010, 0.344819935751253, 0.35138224467125 },
        { 5020, 0.344540904081086, 0.351158034398612 },
        { 5030, 0.344263059301778, 0.350934374265679 },
        { 5040, 0.343986395038783, 0.350711264013907 },
        { 5050, 0.343710904955591, 0.350488703361895 },
        { 5060, 0.34343658275351, 0.350266692005812 },
        { 5070, 0.343163422171456, 0.350045229619827 },
        { 5080, 0.342891416985746, 0.349824315856515 },
        { 5090, 0.34262056100988, 0.349603950347275 },
        { 5100, 0.342350848094336, 0.349384132702732 },
        { 5110, 0.342082272126361, 0.349164862513132 },
        { 5120, 0.341814827029758, 0.348946139348737 },
        { 5130, 0.34154850676468, 0.348727962760208 },
        { 5140, 0.341283305327422, 0.348510332278986 },
        { 5150, 0.341019216750211, 0.348293247417664 },
        { 5160, 0.340756235101003, 0.348076707670355 },
        { 5170, 0.340494354483275, 0.347860712513058 },
        { 5180, 0.340233569035817, 0.347645261404009 },
        { 5190, 0.339973872932533, 0.347430353784033 },
        { 5200, 0.339715260382228, 0.347215989076895 },
        { 5210, 0.339457725628414, 0.347002166689633 },
        { 5220, 0.339201262949099, 0.346788886012898 },
        { 5230, 0.33894586665659, 0.346576146421281 },
        { 5240, 0.338691531097289, 0.346363947273638 },
        { 5250, 0.338438250651491, 0.346152287913411 },
        { 5260, 0.338186019733187, 0.345941167668941 },
        { 5270, 0.337934832789862, 0.34573058585378 },
        { 5280, 0.337684684302299, 0.345520541766991 },
        { 5290, 0.337435568784377, 0.345311034693453 },
        { 5300, 0.337187480782876, 0.345102063904155 },
        { 5310, 0.336940414877284, 0.344893628656486 },
        { 5320, 0.336694365679595, 0.344685728194521 },
        { 5330, 0.336449327834117, 0.344478361749302 },
        { 5340, 0.336205296017281, 0.34427152853912 },
        { 5350, 0.335962264937442, 0.344065227769781 },
        { 5360, 0.335720229334691, 0.34385945863488 },
        { 5370, 0.335479183980662, 0.343654220316063 },
        { 5380, 0.335239123678341, 0.343449511983289 },
        { 5390, 0.335000043261875, 0.343245332795082 },
        { 5400, 0.334761937596386, 0.343041681898789 },
        { 5410, 0.334524801577778, 0.342838558430825 },
        { 5420, 0.334288630132555, 0.342635961516917 },
        { 5430, 0.33405341821763, 0.342433890272344 },
        { 5440, 0.33381916082014, 0.342232343802175 },
        { 5450, 0.333585852957265, 0.342031321201502 },
        { 5460, 0.333353489676037, 0.341830821555668 },
        { 5470, 0.333122066053165, 0.341630843940493 },
        { 5480, 0.332891577194843, 0.341431387422495 },
        { 5490, 0.33266201823658, 0.341232451059109 },
        { 5500, 0.332433384343009, 0.341034033898904 },
        { 5510, 0.332205670707715, 0.34083613498179 },
        { 5520, 0.331978872553048, 0.34063875333923 },
        { 5530, 0.331752985129957, 0.340441887994442 },
        { 5540, 0.3315280037178, 0.340245537962605 },
        { 5550, 0.331303923624177, 0.340049702251049 },
        { 5560, 0.331080740184751, 0.339854379859459 },
        { 5570, 0.330858448763073, 0.339659569780063 },
        { 5580, 0.330637044750413, 0.339465270997818 },
        { 5590, 0.330416523565582, 0.339271482490602 },
        { 5600, 0.330196880654761, 0.339078203229392 },
        { 5610, 0.329978111491335, 0.338885432178446 },
        { 5620, 0.329760211575718, 0.338693168295479 },
        { 5630, 0.329543176435188, 0.338501410531837 },
        { 5640, 0.329327001623714, 0.338310157832671 },
        { 5650, 0.329111682721794, 0.338119409137101 },
        { 5660, 0.328897215336285, 0.337929163378386 },
        { 5670, 0.32868359510024, 0.337739419484084 },
        { 5680, 0.328470817672741, 0.337550176376214 },
        { 5690, 0.328258878738739, 0.337361432971415 },
        { 5700, 0.328047774008889, 0.337173188181098 },
        { 5710, 0.327837499219387, 0.3369854409116 },
        { 5720, 0.32762805013181, 0.336798190064338 },
        { 5730, 0.32741942253296, 0.33661143453595 },
        { 5740, 0.327211612234699, 0.336425173218445 },
        { 5750, 0.327004615073793, 0.336239404999346 },
        { 5760, 0.326798426911756, 0.336054128761828 },
        { 5770, 0.326593043634692, 0.335869343384858 },
        { 5780, 0.326388461153141, 0.33568504774333 },
        { 5790, 0.326184675401921, 0.3355012407082 },
        { 5800, 0.325981682339978, 0.335317921146619 },
        { 5810, 0.325779477950234, 0.335135087922056 },
        { 5820, 0.325578058239428, 0.334952739894432 },
        { 5830, 0.325377419237973, 0.334770875920243 },
        { 5840, 0.325177556999802, 0.334589494852681 },
        { 5850, 0.324978467602219, 0.334408595541759 },
        { 5860, 0.324780147145748, 0.334228176834424 },
        { 5870, 0.324582591753991, 0.33404823757468 },
        { 5880, 0.324385797573479, 0.333868776603699 },
        { 5890, 0.324189760773522, 0.333689792759937 },
        { 5900, 0.323994477546069, 0.333511284879243 },
        { 5910, 0.323799944105563, 0.333333251794967 },
        { 5920, 0.323606156688797, 0.33315569233807 },
        { 5930, 0.32341311155477, 0.332978605337232 },
        { 5940, 0.323220804984549, 0.332801989618949 },
        { 5950, 0.323029233281125, 0.33262584400764 },
        { 5960, 0.322838392769274, 0.332450167325745 },
        { 5970, 0.322648279795422, 0.332274958393826 },
        { 5980, 0.322458890727498, 0.332100216030663 },
        { 5990, 0.322270221954808, 0.331925939053347 },
        { 6000, 0.322082269887888, 0.331752126277376 },
        { 6010, 0.321895030958374, 0.331578776516748 },
        { 6020, 0.321708501618868, 0.331405888584047 },
        { 6030, 0.321522678342801, 0.331233461290538 },
        { 6040, 0.3213375576243, 0.331061493446248 },
        { 6050, 0.32115313597806, 0.330889983860056 },
        { 6060, 0.320969409939206, 0.330718931339777 },
        { 6070, 0.320786376063166, 0.330548334692243 },
        { 6080, 0.320604030925542, 0.330378192723386 },
        { 6090, 0.32042237112198, 0.330208504238316 },
        { 6100, 0.32024139326804, 0.330039268041404 },
        { 6110, 0.320061093999071, 0.329870482936352 },
        { 6120, 0.319881469970083, 0.329702147726276 },
        { 6130, 0.319702517855622, 0.329534261213777 },
        { 6140, 0.319524234349642, 0.329366822201013 },
        { 6150, 0.319346616165388, 0.329199829489774 },
        { 6160, 0.319169660035263, 0.32903328188155 },
        { 6170, 0.318993362710712, 0.328867178177603 },
        { 6180, 0.3188177209621, 0.328701517179031 },
        { 6190, 0.318642731578585, 0.32853629768684 },
        { 6200, 0.318468391368004, 0.328371518502004 },
        { 6210, 0.318294697156752, 0.328207178425535 },
        { 6220, 0.31812164578966, 0.328043276258542 },
        { 6230, 0.31794923412988, 0.327879810802294 },
        { 6240, 0.317777459058767, 0.327716780858286 },
        { 6250, 0.317606317475763, 0.32755418522829 },
        { 6260, 0.317435806298278, 0.32739202271442 },
        { 6270, 0.317265922461579, 0.327230292119189 },
        { 6280, 0.317096662918675, 0.327068992245565 },
        { 6290, 0.316928024640201, 0.326908121897025 },
        { 6300, 0.316760004614306, 0.326747679877612 },
        { 6310, 0.316592599846543, 0.326587664991987 },
        { 6320, 0.316425807359756, 0.326428076045483 },
        { 6330, 0.316259624193968, 0.326268911844154 },
        { 6340, 0.316094047406274, 0.326110171194829 },
        { 6350, 0.315929074070729, 0.325951852905159 },
        { 6360, 0.315764701278241, 0.325793955783666 },
        { 6370, 0.315600926136464, 0.325636478639792 },
        { 6380, 0.315437745769688, 0.325479420283945 },
        { 6390, 0.315275157318735, 0.325322779527545 },
        { 6400, 0.315113157940854, 0.325166555183068 },
        { 6410, 0.314951744809611, 0.325010746064094 },
        { 6420, 0.314790915114793, 0.324855350985344 },
        { 6430, 0.314630666062295, 0.324700368762729 },
        { 6440, 0.314470994874025, 0.324545798213387 },
        { 6450, 0.314311898787797, 0.324391638155726 },
        { 6460, 0.314153375057231, 0.324237887409465 },
        { 6470, 0.31399542095165, 0.324084544795669 },
        { 6480, 0.313838033755984, 0.323931609136792 },
        { 6490, 0.313681210770667, 0.323779079256713 },
        { 6500, 0.313524949311538, 0.323626953980771 },
        { 6510, 0.313369246709745, 0.323475232135805 },
        { 6520, 0.313214100311644, 0.323323912550187 },
        { 6530, 0.313059507478705, 0.323172994053855 },
        { 6540, 0.312905465587415, 0.323022475478352 },
        //@formatter:on
    };

    /**
     * Search the 'KELVIN_TO_XY_LOOKUP_TABLE' for the XY entry closest to the given colour temperature.
     * Uses a recursive 'QuickSearch' algorithm.
     * 
     * @param kelvin the colour temperature in K to find
     * @param min the first index in the lookup table
     * @param max the last index in the lookup table
     * @return an array with the found CIE colour XY values
     * @throws IndexOutOfBoundsException if the colour temperature is out of range 'min' .. 'max'
     */
    private static double[] kelvinToXY(double kelvin, int min, int max) throws IndexOutOfBoundsException {
        if (min < 0 || max < min || max >= KELVIN_TO_XY_LOOKUP_TABLE.length) {
            throw new IndexOutOfBoundsException("kelvinToXY() 'min' or 'max' index out of bounds");
        }
        int mid = (min + max) / 2;
        double delta = kelvin - KELVIN_TO_XY_LOOKUP_TABLE[mid][0];
        if (delta >= 10) {
            return kelvinToXY(kelvin, mid + 1, max);
        }
        if (delta <= -10) {
            return kelvinToXY(kelvin, min, mid - 1);
        }
        return new double[] { KELVIN_TO_XY_LOOKUP_TABLE[mid][1], KELVIN_TO_XY_LOOKUP_TABLE[mid][2] };
    }

    /**
     * Convert a colour temperature in Kelvin to a point in the CIE XY colour space.
     * Uses a lookup table as described <a href=
     * "https://www.waveformlighting.com/tech/calculate-cie-1931-xy-coordinates-from-cct">here</a>.
     * 
     * @param kelvin the colour temperature in K to be converted
     * @return an array with the found CIE colour XY values
     * @throws IndexOutOfBoundsException if the colour temperature is out of range 2000K .. 6500K
     */
    public static double[] kelvinToXY(double kelvin) throws IndexOutOfBoundsException {
        int indexMax = KELVIN_TO_XY_LOOKUP_TABLE.length - 1;
        double kelvinMin = KELVIN_TO_XY_LOOKUP_TABLE[0][0];
        double kelvinMax = KELVIN_TO_XY_LOOKUP_TABLE[indexMax][0];
        if (kelvin < kelvinMin || kelvin > kelvinMax) {
            throw new IndexOutOfBoundsException(
                    String.format("kelvinToXY() %.0f K out of range %.0f K .. %.0f K", kelvin, kelvinMin, kelvinMax));
        }
        return kelvinToXY(kelvin, 0, indexMax);
    }

    /**
     * Convert a point in the CIE XY colour space to a colour temperature in Kelvin.
     * Uses McCamy's approximation as described <a href=
     * "https://www.waveformlighting.com/tech/calculate-color-temperature-cct-from-cie-1931-xy-coordinates">here</a>.
     * 
     * @param xy an array with the CIE colour XY values to be converted
     * @return the colour temperature in K
     * @throws IndexOutOfBoundsException if the wrong number of arguments is provided
     */
    public static double xyToKelvin(double[] xy) {
        if (xy.length != 2) {
            throw new IllegalArgumentException("xyToKelvin() requires 2 arguments");
        }
        double n = (xy[0] - 0.3320) / (0.1858 - xy[1]);
        return (437 * Math.pow(n, 3)) + (3601 * Math.pow(n, 2)) + (6861 * n) + 5517;
    }

    /**
     * Calculates a polynomial regression.
     *
     * This calculates the equation K[4]*x^0 + K[3]*x^1 + K[2]*x^2 + K[1]*x^3 + K[0]*x^4
     *
     * @param x The independent variable distributed through each term of the polynomial
     * @param coefficients The coefficients of the polynomial. Note that the terms are in
     *            order from largest exponent to smallest exponent, which is the reverse order
     *            of the usual way of writing it in academic papers
     * @return the result of substituting x into the regression polynomial
     */
    private static double polynomialFit(double x, double[] coefficients) {
        double result = 0.0;
        double xAccumulator = 1.0;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            result += coefficients[i] * xAccumulator;
            xAccumulator *= x;
        }
        return result;
    }
}
