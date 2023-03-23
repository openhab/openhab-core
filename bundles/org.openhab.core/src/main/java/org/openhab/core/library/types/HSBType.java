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
        List<String> constituents = Arrays.stream(value.split(",")).map(in -> in.trim()).collect(Collectors.toList());
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
     * Create HSB from RGB
     *
     * @param r red 0-255
     * @param g green 0-255
     * @param b blue 0-255
     */
    public static HSBType fromRGB(int r, int g, int b) {
        float tmpHue, tmpSaturation, tmpBrightness;
        int max = (r > g) ? r : g;
        if (b > max) {
            max = b;
        }
        int min = (r < g) ? r : g;
        if (b < min) {
            min = b;
        }
        tmpBrightness = max / 2.55f;
        tmpSaturation = (max != 0 ? ((float) (max - min)) / ((float) max) : 0) * 100;
        if (tmpSaturation == 0) {
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

        return new HSBType(new DecimalType((int) tmpHue), new PercentType((int) tmpSaturation),
                new PercentType((int) tmpBrightness));
    }

    /**
     * @deprecated Use {@link ColorUtil#xyToHsv(double[])} or {@link ColorUtil#xyToHsv(double[], ColorUtil.Gamut)}
     *             instead
     *
     *             Returns a HSBType object representing the provided xy color values in CIE XY color model.
     *             Conversion from CIE XY color model to sRGB using D65 reference white
     *             Returned color is set to full brightness
     *
     * @param x, y color information 0.0 - 1.0
     * @return new HSBType object representing the given CIE XY color, full brightness
     *
     */
    @Deprecated
    public static HSBType fromXY(float x, float y) {
        return ColorUtil.xyToHsv(new double[] { x, y });
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

    public PercentType getRed() {
        return toRGB()[0];
    }

    public PercentType getGreen() {
        return toRGB()[1];
    }

    public PercentType getBlue() {
        return toRGB()[2];
    }

    /**
     * Returns the RGB value representing the color in the default sRGB
     * color model.
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
     *
     * @return the RGB value of the color in the default sRGB color model
     */
    public int getRGB() {
        PercentType[] rgb = toRGB();
        return ((0xFF) << 24) | ((convertPercentToByte(rgb[0]) & 0xFF) << 16)
                | ((convertPercentToByte(rgb[1]) & 0xFF) << 8) | ((convertPercentToByte(rgb[2]) & 0xFF) << 0);
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

    public PercentType[] toRGB() {
        PercentType red = null;
        PercentType green = null;
        PercentType blue = null;

        BigDecimal h = hue.divide(BIG_DECIMAL_HUNDRED, 10, RoundingMode.HALF_UP);
        BigDecimal s = saturation.divide(BIG_DECIMAL_HUNDRED);

        int hInt = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP).intValue();
        BigDecimal f = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP)
                .remainder(BigDecimal.ONE);
        PercentType a = new PercentType(value.multiply(BigDecimal.ONE.subtract(s)));
        PercentType b = new PercentType(value.multiply(BigDecimal.ONE.subtract(s.multiply(f))));
        PercentType c = new PercentType(
                value.multiply(BigDecimal.ONE.subtract((BigDecimal.ONE.subtract(f)).multiply(s))));

        switch (hInt) {
            case 0:
            case 6:
                red = getBrightness();
                green = c;
                blue = a;
                break;
            case 1:
                red = b;
                green = getBrightness();
                blue = a;
                break;
            case 2:
                red = a;
                green = getBrightness();
                blue = c;
                break;
            case 3:
                red = a;
                green = b;
                blue = getBrightness();
                break;
            case 4:
                red = c;
                green = a;
                blue = getBrightness();
                break;
            case 5:
                red = getBrightness();
                green = a;
                blue = b;
                break;
            default:
                throw new IllegalArgumentException("Could not convert to RGB.");
        }
        return new PercentType[] { red, green, blue };
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
        return percent.value.multiply(BigDecimal.valueOf(255)).divide(BIG_DECIMAL_HUNDRED, 2, RoundingMode.HALF_UP)
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
}
