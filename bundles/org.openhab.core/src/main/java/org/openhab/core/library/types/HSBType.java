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
package org.openhab.core.library.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;
import org.openhab.core.util.ColorUtil;

/**
 * The HSBType is a complex type with constituents for hue, saturation and
 * brightness and can be used for color items.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Added fromRGB
 * @author Andrew Fiddian-Green - closeTo (copied from binding)
 */
@NonNullByDefault
public class HSBType extends PercentType implements ComplexType, State, Command {

    private static final long serialVersionUID = 322902950356613226L;

    // constants for the constituents
    public static final String KEY_HUE = "h";
    public static final String KEY_SATURATION = "s";
    public static final String KEY_BRIGHTNESS = "b";

    // constants for colors
    public static final HSBType BLACK = new HSBType("0,0,0");
    public static final HSBType WHITE = new HSBType("0,0,100");
    public static final HSBType RED = new HSBType("0,100,100");
    public static final HSBType GREEN = new HSBType("120,100,100");
    public static final HSBType BLUE = new HSBType("240,100,100");

    private static final String UNIT_HSB = "%hsb%";
    private static final String UNIT_RGB = "%rgb%";

    protected BigDecimal hue;
    protected BigDecimal saturation;

    public HSBType() {
        this("0,0,0");
    }

    /**
     * Constructs a HSBType instance with the given values
     *
     * @param h the hue value in the range from 0 <= h < 360
     * @param s the saturation as a percent value
     * @param b the brightness as a percent value
     */
    public HSBType(DecimalType h, PercentType s, PercentType b) {
        this.hue = h.toBigDecimal();
        this.saturation = s.toBigDecimal();
        this.value = b.toBigDecimal();
        validateValue(this.hue, this.saturation, this.value);
    }

    /**
     * Constructs a HSBType instance from a given string.
     * The string has to be in comma-separated format with exactly three segments, which correspond to the hue,
     * saturation and brightness values.
     * where the hue value in the range from 0 <= h < 360 and the saturation and brightness are percent values.
     *
     * @param value a stringified HSBType value in the format "hue,saturation,brightness"
     */
    public HSBType(String value) {
        List<String> constituents = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
        if (constituents.size() == 3) {
            this.hue = new BigDecimal(constituents.get(0));
            this.saturation = new BigDecimal(constituents.get(1));
            this.value = new BigDecimal(constituents.get(2));
            validateValue(this.hue, this.saturation, this.value);
        } else {
            throw new IllegalArgumentException(value + " is not a valid HSBType syntax");
        }
    }

    private void validateValue(BigDecimal hue, BigDecimal saturation, BigDecimal value) {
        if (BigDecimal.ZERO.compareTo(hue) > 0 || BigDecimal.valueOf(360).compareTo(hue) <= 0) {
            throw new IllegalArgumentException("Hue must be between 0 and 360");
        }
        if (BigDecimal.ZERO.compareTo(saturation) > 0 || BIG_DECIMAL_HUNDRED.compareTo(saturation) < 0) {
            throw new IllegalArgumentException("Saturation must be between 0 and 100");
        }
        if (BigDecimal.ZERO.compareTo(value) > 0 || BIG_DECIMAL_HUNDRED.compareTo(value) < 0) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        }
    }

    public static HSBType valueOf(String value) {
        return new HSBType(value);
    }

    /**
     * Create HSB from RGB.
     *
     * See also {@link ColorUtil#rgbToHsb(int[])}.
     *
     * @param r red 0-255
     * @param g green 0-255
     * @param b blue 0-255
     * @throws IllegalArgumentException when color values exceed allowed range
     */
    public static HSBType fromRGB(int r, int g, int b) throws IllegalArgumentException {
        return ColorUtil.rgbToHsb(new int[] { r, g, b });
    }

    /**
     * @deprecated Use {@link ColorUtil#xyToHsb(double[])} or {@link ColorUtil#xyToHsb(double[], ColorUtil.Gamut)}
     *             instead.
     *
     *             Returns a HSBType object representing the provided xy color values in CIE XY color model.
     *             Conversion from CIE XY color model to sRGB using D65 reference white
     *             Returned color is set to full brightness
     *
     * @param x, y color information 0.0 - 1.0
     * @return new HSBType object representing the given CIE XY color, full brightness
     * @throws IllegalArgumentException when input array has wrong size or exceeds allowed value range
     */
    @Deprecated
    public static HSBType fromXY(float x, float y) throws IllegalArgumentException {
        return ColorUtil.xyToHsb(new double[] { x, y });
    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        TreeMap<String, PrimitiveType> map = new TreeMap<>();
        map.put(KEY_HUE, getHue());
        map.put(KEY_SATURATION, getSaturation());
        map.put(KEY_BRIGHTNESS, getBrightness());
        return map;
    }

    public DecimalType getHue() {
        return new DecimalType(hue);
    }

    public PercentType getSaturation() {
        return new PercentType(saturation);
    }

    public PercentType getBrightness() {
        return new PercentType(value);
    }

    /** @deprecated Use {@link ColorUtil#hsbToRgb(HSBType)} instead */
    @Deprecated
    public PercentType getRed() {
        return toRGB()[0];
    }

    /** @deprecated Use {@link ColorUtil#hsbToRgb(HSBType)} instead */
    @Deprecated
    public PercentType getGreen() {
        return toRGB()[1];
    }

    /** @deprecated Use {@link ColorUtil#hsbToRgb(HSBType)} instead */
    @Deprecated
    public PercentType getBlue() {
        return toRGB()[2];
    }

    /**
     * @deprecated Use {@link ColorUtil#hsbTosRgb(HSBType)} instead.
     *
     *             Returns the RGB value representing the color in the default sRGB
     *             color model.
     *             (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
     *
     * @return the RGB value of the color in the default sRGB color model
     */
    @Deprecated
    public int getRGB() {
        return ColorUtil.hsbTosRgb(this);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return getHue() + "," + getSaturation() + "," + getBrightness();
    }

    @Override
    public String format(String pattern) {
        String formatPattern = pattern;
        String val = getHue() + "," + getSaturation() + "," + getBrightness();
        if (pattern.contains(UNIT_HSB)) {
            formatPattern = pattern.replace(UNIT_HSB, "%s");
        } else if (pattern.contains(UNIT_RGB)) {
            formatPattern = pattern.replace(UNIT_RGB, "%s");
            PercentType[] rgb = toRGB();
            val = convertPercentToByte(rgb[0]) + "," + convertPercentToByte(rgb[1]) + ","
                    + convertPercentToByte(rgb[2]);
        }
        return String.format(formatPattern, val);
    }

    @Override
    public int hashCode() {
        int tmp = 10000 * getHue().hashCode();
        tmp += 100 * getSaturation().hashCode();
        tmp += getBrightness().hashCode();
        return tmp;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HSBType)) {
            return false;
        }
        HSBType other = (HSBType) obj;
        return getHue().equals(other.getHue()) && getSaturation().equals(other.getSaturation())
                && getBrightness().equals(other.getBrightness());
    }

    /* @deprecated Use {@link ColorUtil#hsbToRgb(HSBType)} instead */
    @Deprecated
    public PercentType[] toRGB() {
        return ColorUtil.hsbToRgbPercent(this);
    }

    /**
     * Returns the xyY values representing this object's color in CIE XY color model.
     * Conversion from sRGB to CIE XY using D65 reference white
     * xy pair contains color information
     * Y represents relative luminance
     *
     * @return PercentType[x, y, Y] values in the CIE XY color model
     */
    public PercentType[] toXY() {
        return Arrays.stream(ColorUtil.hsbToXY(this)).mapToObj(d -> new PercentType(new BigDecimal(d * 100.0)))
                .toArray(PercentType[]::new);
    }

    private int convertPercentToByte(PercentType percent) {
        return percent.value.multiply(BigDecimal.valueOf(255)).divide(BIG_DECIMAL_HUNDRED, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    @Override
    public <T extends State> @Nullable T as(@Nullable Class<T> target) {
        if (target == OnOffType.class) {
            // if brightness is not completely off, we consider the state to be on
            return target.cast(PercentType.ZERO.equals(getBrightness()) ? OnOffType.OFF : OnOffType.ON);
        } else if (target == DecimalType.class) {
            return target.cast(
                    new DecimalType(getBrightness().toBigDecimal().divide(BIG_DECIMAL_HUNDRED, 8, RoundingMode.UP)));
        } else if (target == PercentType.class) {
            return target.cast(new PercentType(getBrightness().toBigDecimal()));
        } else {
            return defaultConversion(target);
        }
    }

    /**
     * Helper method for checking if two HSBType colors are close to each other. A maximum deviation is specifid in
     * percent.
     *
     * @param other an HSBType containing the other color.
     * @param maxPercentage the maximum allowed difference in percent (range 0.0..1.0).
     * @throws IllegalArgumentException if percentage is out of range.
     */
    public boolean closeTo(HSBType other, double maxPercentage) throws IllegalArgumentException {
        if (maxPercentage <= 0.0 || maxPercentage > 1.0) {
            throw new IllegalArgumentException("'maxPercentage' out of bounds, allowed range 0..1");
        }
        double[] exp = ColorUtil.hsbToXY(this);
        double[] act = ColorUtil.hsbToXY(other);
        return ((Math.abs(exp[0] - act[0]) < maxPercentage) && (Math.abs(exp[1] - act[1]) < maxPercentage));
    }
}
