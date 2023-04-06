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

import java.math.BigDecimal;
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
 */
@NonNullByDefault
public class ColorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorUtil.class);
    protected static final BigDecimal BIG_DECIMAL_HUNDRED = BigDecimal.valueOf(100);
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
     * See also: {@link hsbToRgbPercent(HSBType)}, {@link hsbTosRGB(HSBType)}
     */
    public static int[] hsbToRgb(HSBType hsb) {
        final PercentType[] rgbPercent = hsbToRgbPercent(hsb);
        return new int[] { convertColorPercentToByte(rgbPercent[0]), convertColorPercentToByte(rgbPercent[1]),
                convertColorPercentToByte(rgbPercent[2]) };
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a>.
     *
     * This function not round the components to integer values. Please consider consider
     * using {@link hsbToRgb(HSBType)} whenever integer values are required.
     *
     * See also: {@link hsbToRgb(HSBType)}, {@link hsbTosRgb(HSBType)}
     */
    public static PercentType[] hsbToRgbPercent(HSBType hsb) {
        PercentType red = null;
        PercentType green = null;
        PercentType blue = null;

        final BigDecimal h = hsb.getHue().toBigDecimal().divide(BIG_DECIMAL_HUNDRED, 10, RoundingMode.HALF_UP);
        final BigDecimal s = hsb.getSaturation().toBigDecimal().divide(BIG_DECIMAL_HUNDRED);

        int hInt = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 0, RoundingMode.DOWN).intValue();
        final BigDecimal f = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP)
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
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}
     * to the RGB value representing the color in the default
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> color model.
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
     *
     * See also: {@link hsbToRgb(HSBType)}, {@link hsbToRgbPercent(HSBType)}
     *
     * @return the RGB value of the color in the default sRGB color model
     */
    public static int hsbTosRgb(HSBType hsb) {
        final int[] rgb = hsbToRgb(hsb);
        return (0xFF << 24) | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | ((rgb[2] & 0xFF) << 0);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developerportal</a>.
     *
     * @param hsbType a {@link HSBType} value
     * @return double array with the closest matching CIE 1931 color, x, y between 0.0000 and 1.0000.
     */
    public static double[] hsbToXY(HSBType hsbType) {
        return hsbToXY(hsbType, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param hsbType a {@link HSBType} value
     * @param gamut the gamut supported by the light.
     * @return double array with the closest matching CIE 1931 color, x, y, Y between 0.0000 and 1.0000.
     */
    public static double[] hsbToXY(HSBType hsbType, Gamut gamut) {
        PercentType[] rgb = hsbToRgbPercent(hsbType);
        double r = inverseCompand(rgb[0].doubleValue() / PercentType.HUNDRED.doubleValue());
        double g = inverseCompand(rgb[1].doubleValue() / PercentType.HUNDRED.doubleValue());
        double b = inverseCompand(rgb[2].doubleValue() / PercentType.HUNDRED.doubleValue());

        double X = r * 0.664511 + g * 0.154324 + b * 0.162028;
        double Y = r * 0.283881 + g * 0.668433 + b * 0.047685;
        double Z = r * 0.000088 + g * 0.072310 + b * 0.986039;

        double sum = X + Y + Z;
        Point p = sum == 0.0 ? new Point() : new Point(X / sum, Y / sum);
        Point q = gamut.closest(p);

        double[] xyY = new double[] { ((int) (q.x * 10000.0)) / 10000.0, ((int) (q.y * 10000.0)) / 10000.0,
                ((int) (Y * 10000.0)) / 10000.0 };

        LOGGER.trace("HSB: {} - RGB: {} - XYZ: {} {} {} - xyY: {}", hsbType, hsbType.toRGB(), X, Y, Z, xyY);

        return xyY;
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> color format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * Note: Conversion result is rounded and HSBType is created with integer valued components.
     *
     * @param rgb int array of length 3, all values are constrained to 0-255
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType rgbToHsb(int[] rgb) throws IllegalArgumentException {
        if (rgb.length != 3 || !inByteRange(rgb[0]) || !inByteRange(rgb[1]) || !inByteRange(rgb[2])) {
            throw new IllegalArgumentException("RGB array only allows values between 0 and 255");
        }
        final int r = rgb[0];
        final int g = rgb[1];
        final int b = rgb[2];

        int max = (r > g) ? r : g;
        if (b > max) {
            max = b;
        }
        int min = (r < g) ? r : g;
        if (b < min) {
            min = b;
        }
        float tmpHue;
        final float tmpBrightness = max / 2.55f;
        final float tmpSaturation = (max != 0 ? ((float) (max - min)) / ((float) max) : 0) * 100.0f;
        // smallest possible saturation: 0 (max=0 or max-min=0), other value closest to 0 is 100/255 (max=255, min=254)
        // -> avoid float comparision to 0
        // if (tmpSaturation == 0) {
        if (max == 0 || (max - min) == 0) {
            tmpHue = 0;
        } else {
            float red = ((float) (max - r)) / ((float) (max - min));
            float green = ((float) (max - g)) / ((float) (max - min));
            float blue = ((float) (max - b)) / ((float) (max - min));
            if (r == max) {
                tmpHue = blue - green;
            } else if (g == max) {
                tmpHue = 2.0f + red - blue;
            } else {
                tmpHue = 4.0f + green - red;
            }
            tmpHue = tmpHue / 6.0f * 360;
            if (tmpHue < 0) {
                tmpHue = tmpHue + 360.0f;
            }
        }

        // adding 0.5 and casting to int approximates rounding
        return new HSBType(new DecimalType((int) (tmpHue + .5f)), new PercentType((int) (tmpSaturation + .5f)),
                new PercentType((int) (tmpBrightness + .5f)));
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xy the CIE 1931 xy color, x,y between 0.0000 and 1.0000.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType xyToHsb(double[] xy) throws IllegalArgumentException {
        return xyToHsb(xy, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format to
     * <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSV</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xy the CIE 1931 xy color, x,y[,Y] between 0.0000 and 1.0000. <code>Y</code> value is optional.
     * @param gamut the gamut supported by the light.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType xyToHsb(double[] xy, Gamut gamut) throws IllegalArgumentException {
        if (xy.length < 2 || xy.length > 3 || !inRange(xy[0]) || !inRange(xy[1])
                || (xy.length == 3 && !inRange(xy[2]))) {
            throw new IllegalArgumentException("xy array only allowes two or three values between 0.0 and 1.0.");
        }
        Point p = gamut.closest(new Point(xy[0], xy[1]));
        double x = p.x;
        double y = p.y == 0.0 ? 0.000001 : p.y;
        double z = 1.0 - x - y;
        double Y = (xy.length == 3) ? xy[2] : 1.0;
        double X = (Y / y) * x;
        double Z = (Y / y) * z;
        double r = X * 1.656492 + Y * -0.354851 + Z * -0.255038;
        double g = X * -0.707196 + Y * 1.655397 + Z * 0.036152;
        double b = X * 0.051713 + Y * -0.121364 + Z * 1.011530;

        // Correction for negative values is missing from Philips' documentation.
        double min = Math.min(r, Math.min(g, b));
        if (min < 0.0) {
            r -= min;
            g -= min;
            b -= min;
        }

        // rescale
        double max = Math.max(r, Math.max(g, b));
        if (max > 1.0) {
            r /= max;
            g /= max;
            b /= max;
        }

        r = compand(r);
        g = compand(g);
        b = compand(b);

        // rescale
        max = Math.max(r, Math.max(g, b));
        if (max > 1.0) {
            r /= max;
            g /= max;
            b /= max;
        }

        HSBType hsb = rgbToHsb(
                new int[] { (int) Math.round(255.0 * r), (int) Math.round(255.0 * g), (int) Math.round(255.0 * b) });
        LOGGER.trace("xy: {} - XYZ: {} {} {} - RGB: {} {} {} - HSB: {} ", xy, X, Y, Z, r, g, b, hsb);

        return hsb;
    }

    /**
     * Gamma correction (inverse sRGB companding)
     *
     * @param value the value to process
     * @return the processed value
     */
    private static double inverseCompand(double value) {
        return value > 0.04045 ? Math.pow((value + 0.055) / (1.0 + 0.055), 2.4) : value / 12.92;
    }

    /**
     * Inverse Gamma correction (sRGB companding)
     *
     * @param value the value to process
     * @return the processed value
     */
    public static double compand(double value) {
        return value <= 0.0031308 ? 12.92 * value : (1.0 + 0.055) * Math.pow(value, (1.0 / 2.4)) - 0.055;
    }

    private static class Point {
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

    public record Gamut(double[] r, double[] g, double[] b) {
        /**
         * Color <a href="https://en.wikipedia.org/wiki/Gamut">gamut</a>
         *
         * @param r double array with `xy` coordinates for red, x, y between 0.0000 and 1.0000.
         * @param g double array with `xy` coordinates for green, x, y between 0.0000 and 1.0000.
         * @param b double array with `xy` coordinates for blue, x, y between 0.0000 and 1.0000.
         */
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

    private static boolean inByteRange(int val) {
        return val >= 0 && val <= 255;
    }

    private static boolean inRange(double val) {
        return val >= 0.0 && val <= 1.0;
    }

    private static int convertColorPercentToByte(PercentType percent) {
        return percent.toBigDecimal().multiply(BigDecimal.valueOf(255))
                .divide(BIG_DECIMAL_HUNDRED, 0, RoundingMode.HALF_UP).intValue();
    }
}
