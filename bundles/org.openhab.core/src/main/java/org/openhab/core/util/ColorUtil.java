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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ColorUtil} is responsible for converting HSB to CIE
 *
 * The class is based work from Erik Baauw for the <a href="https://github.com/ebaauw/homebridge-lib">Homebridge</a>
 * project
 *
 * @author Jan N. Klug - Initial contribution
 * @author Holger Friedrich - Transfer RGB color conversion from HSBType, improve RGB conversion, restructuring
 */
@NonNullByDefault
public class ColorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorUtil.class);
    public static final Gamut DEFAULT_GAMUT = new Gamut(new double[] { 0.9961, 0.0001 }, new double[] { 0, 0.9961 },
            new double[] { 0, 0.0001 });

    private ColorUtil() {
        // prevent instantiation
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developerportal</a>.
     *
     * @param hsbType a {@link HSBType} value
     * @return double array with the closest matching CIE 1931 colour, x, y between 0.0000 and 1.0000.
     */
    public static double[] hsbToXY(HSBType hsbType) {
        return hsbToXY(hsbType, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> based {@link HSBType} to
     * <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param hsbType a {@link HSBType} value
     * @param gamut the gamut supported by the light.
     * @return double array with the closest matching CIE 1931 colour, x, y, Y between 0.0000 and 1.0000.
     */
    public static double[] hsbToXY(HSBType hsbType, Gamut gamut) {
        double r = inverseCompand(hsbType.getRed().doubleValue() / PercentType.HUNDRED.doubleValue());
        double g = inverseCompand(hsbType.getGreen().doubleValue() / PercentType.HUNDRED.doubleValue());
        double b = inverseCompand(hsbType.getBlue().doubleValue() / PercentType.HUNDRED.doubleValue());

        double X = r * 0.664511 + g * 0.154324 + b * 0.162028;
        double Y = r * 0.283881 + g * 0.668433 + b * 0.047685;
        double Z = r * 0.000088 + g * 0.072310 + b * 0.986039;

        double sum = X + Y + Z;
        Point p = sum == 0.0 ? new Point() : new Point(X / sum, Y / sum);
        Point q = gamut.closest(p);

        double[] xyY = new double[] { ((int) (q.x * 10000.0)) / 10000.0, ((int) (q.y * 10000.0)) / 10000.0,
                ((int) (Y * 10000.0)) / 10000.0 };

        LOGGER.trace("HSV: {} - RGB: {} - XYZ: {} {} {} - xyY: {}", hsbType, hsbType.toRGB(), X, Y, Z, xyY);

        return xyY;
    }

    /**
     * Transform RGB color format to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> based {@link HSBType}.
     *
     * Note: Conversion result is rounded and HSBType is created with integer valued components.
     *
     * @param rgb int array of length 3, all values are constrained to 0-255
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType rgbToHsv(int[] rgb) throws IllegalArgumentException {
        if (rgb.length != 3 || !inByteRange(rgb[0]) || !inByteRange(rgb[1]) || !inByteRange(rgb[2])) {
            throw new IllegalArgumentException("rgb array only allows values between 0 and 255");
        }
        final int r  = rgb[0];
        final int g  = rgb[1];
        final int b  = rgb[2];

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
        if (max==0 || (max-min)==0) {
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
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xy the CIE 1931 xy colour, x,y between 0.0000 and 1.0000.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType xyToHsv(double[] xy) throws IllegalArgumentException {
        return xyToHsv(xy, DEFAULT_GAMUT);
    }

    /**
     * Transform <a href="https://en.wikipedia.org/wiki/CIE_1931_color_space">CIE 1931</a> `xy` format to
     * <a href="https://en.wikipedia.org/wiki/SRGB">sRGB</a> based {@link HSBType}.
     *
     * See <a href=
     * "https://developers.meethue.com/develop/application-design-guidance/color-conversion-formulas-rgb-to-xy-and-back/">Hue
     * developer portal</a>.
     *
     * @param xy the CIE 1931 xy colour, x,y[,Y] between 0.0000 and 1.0000. <code>Y</code> value is optional.
     * @param gamut the gamut supported by the light.
     * @return the corresponding {@link HSBType}.
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    public static HSBType xyToHsv(double[] xy, Gamut gamut) throws IllegalArgumentException {
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

        HSBType hsb = rgbToHsv(new int[] {(int) Math.round(255.0 * r), (int) Math.round(255.0 * g),
                (int) Math.round(255.0 * b)});
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
}
