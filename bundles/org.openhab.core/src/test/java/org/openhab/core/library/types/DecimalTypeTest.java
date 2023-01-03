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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.IllegalFormatConversionException;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.core.library.unit.Units;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
@NonNullByDefault
public class DecimalTypeTest {

    /**
     * Locales having a different decimal and grouping separators to test string parsing and generation.
     */
    static Stream<Locale> locales() {
        return Stream.of(
                // ٫٬ (Arabic, Egypt)
                Locale.forLanguageTag("ar-EG"),
                // ,. (German, Germany)
                Locale.forLanguageTag("de-DE"),
                // ., (English, United States)
                Locale.forLanguageTag("en-US"));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testKnownInvalidConstructors(Locale locale) {
        Locale.setDefault(locale);

        assertThrows(NumberFormatException.class, () -> new DecimalType("123 Hello World"));

        assertThrows(NumberFormatException.class, () -> new DecimalType(""));
        assertThrows(NumberFormatException.class, () -> new DecimalType("."));
        assertThrows(NumberFormatException.class, () -> new DecimalType("1 2"));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123..56"));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123abc56"));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123.123,56"));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123٬123٫56"));

        assertThrows(NumberFormatException.class, () -> new DecimalType("", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType(".", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType("1 2", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123..56", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123abc56", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123.123,56", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123٬123٫56", Locale.ENGLISH));

        assertThrows(NumberFormatException.class, () -> new DecimalType("", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType(",", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType("1 2", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123,,56", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123abc56", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123,123.56", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new DecimalType("123٬123٫56", Locale.GERMAN));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-2,000.5", "-2.5", "-2", "-0", "0", "2", "2.5", "2,000.5", "-10E3", "-10E-3", "-0E-22",
            "-0E0", "-0E-0", "0E0", "0E-22", "10E-3", "10E3" })
    public void testValidConstructors(String value) throws Exception {
        new DecimalType(value);
        DecimalType.valueOf(value);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testLocalizedStringConstruction(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);

        // Construction for each locale should always return the same result regardless of the current default locale
        Stream.of(Locale.ENGLISH, Locale.GERMAN).forEach(locale -> {
            char ds = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            char gs = DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();

            assertEquals(new DecimalType("123"), new DecimalType("123", locale));
            assertEquals(new DecimalType("123.56"), new DecimalType(String.format("123%s56", ds), locale));
            assertEquals(new DecimalType("123123123"), new DecimalType("123123123", locale));
            assertEquals(new DecimalType("123123123"), new DecimalType(String.format("123%s123%s123", gs, gs), locale));
            assertEquals(new DecimalType("123123123.56"), new DecimalType(String.format("123123123%s56", ds), locale));
            assertEquals(new DecimalType("123123123.56"),
                    new DecimalType(String.format("123%s123%s123%s56", gs, gs, ds), locale));
        });
    }

    @Test
    public void testEquals() {
        DecimalType dt1 = new DecimalType("142.8");
        DecimalType dt2 = new DecimalType("142.8");
        DecimalType dt3 = new DecimalType("99.7");
        PercentType pt = new PercentType("99.7");

        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertTrue(dt1.equals(dt2));
        assertFalse(dt1.equals(dt3));
        assertTrue(dt3.equals(pt));
        assertFalse(dt1.equals(pt));
    }

    @Test
    public void testIntFormat() {
        DecimalType dt;

        // Basic test with an integer value.
        dt = new DecimalType("87");
        assertEquals("87", dt.format("%d"));

        // Again an integer value, but this time an "advanced" pattern.
        dt = new DecimalType("87");
        assertEquals(" 87", dt.format("%3d"));

        // Again an integer value, but this time an "advanced" pattern.
        dt = new DecimalType("87");
        assertEquals("0x57", dt.format("%#x"));

        // A float value cannot be converted into hex.
        assertThrows(IllegalFormatConversionException.class, () -> new DecimalType("87.5").format("%x"));

        // An integer (with different representation) with int conversion.
        dt = new DecimalType("11.0");
        assertEquals("11", dt.format("%d"));
    }

    @Test
    public void testFloatFormat() {
        DecimalType dt;

        // We know that DecimalType calls "String.format()" without a locale. So
        // we have to do the same thing here in order to get the right decimal
        // separator.
        final char sep = (new DecimalFormatSymbols().getDecimalSeparator());

        // A float value with float conversion.
        dt = new DecimalType("11.123");
        assertEquals("11" + sep + "1", dt.format("%.1f")); // "11.1"

        // An integer value with float conversion. This has to work.
        dt = new DecimalType("11");
        assertEquals("11" + sep + "0", dt.format("%.1f")); // "11.0"

        // An integer value with float conversion. This has to work.
        dt = new DecimalType("11.0");
        assertEquals("11" + sep + "0", dt.format("%.1f")); // "11.0"
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new DecimalType("100.0").as(OnOffType.class));
        assertEquals(OnOffType.ON, new DecimalType("1.0").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new DecimalType("0.0").as(OnOffType.class));
    }

    @Test
    public void testConversionToOpenCloseType() {
        assertEquals(OpenClosedType.OPEN, new DecimalType("1.0").as(OpenClosedType.class));
        assertEquals(OpenClosedType.CLOSED, new DecimalType("0.0").as(OpenClosedType.class));
        assertNull(new DecimalType("0.5").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToUpDownType() {
        assertEquals(UpDownType.UP, new DecimalType("0.0").as(UpDownType.class));
        assertEquals(UpDownType.DOWN, new DecimalType("1.0").as(UpDownType.class));
        assertNull(new DecimalType("0.5").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToHSBType() {
        assertEquals(new HSBType("0,0,0"), new DecimalType("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new DecimalType("1.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new DecimalType("0.5").as(HSBType.class));
    }

    @Test
    public void testConversionToDateTimeType() {
        assertEquals(new DateTimeType("2014-03-30T10:58:47+0000"),
                new DecimalType("1396177127").as(DateTimeType.class));
        assertEquals(new DateTimeType("1969-12-31T23:59:59+0000"), new DecimalType("-1").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:00+0000"), DecimalType.ZERO.as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1.0").as(DateTimeType.class));
        assertEquals(new DateTimeType("1970-01-01T00:00:01+0000"), new DecimalType("1.5").as(DateTimeType.class));
        assertEquals(new DateTimeType("1969-12-31T23:59:59+0000"), new DecimalType("-1.0").as(DateTimeType.class));
    }

    @Test
    public void testConversionToPercentType() {
        assertEquals(new PercentType(70), new DecimalType("0.7").as(PercentType.class));
    }

    @Test
    public void testConversionToPointType() {
        // should not be possible => null
        assertNull(new DecimalType("0.23").as(PointType.class));
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> testNumberConstructor() {
        return Stream.of(arguments((byte) 42, "42", 42f, 42d), arguments((short) 42, "42", 42f, 42d),
                arguments(42, "42", 42f, 42d), arguments(42L, "42", 42f, 42d),
                arguments(new BigInteger("42"), "42", 42f, 42d), arguments(new PercentType(42), "42", 42f, 42d),
                arguments(new HSBType(DecimalType.ZERO, PercentType.ZERO, new PercentType(42)), "42", 42f, 42d),
                // 4.2 is an example of a float value, which cannot converted to a double value directly:
                // (float) 4.2 ==> (double) 4.199999809265137
                arguments(4.2f, "4.2", 4.2f, 4.2d), arguments(4.2d, "4.2", 4.2f, 4.2d),
                arguments(new BigDecimal("4.2"), "4.2", 4.2f, 4.2d),
                arguments(new DecimalType("4.2"), "4.2", 4.2f, 4.2d),
                arguments(new QuantityType<>(4.2f, Units.WATT), "4.2", 4.2f, 4.2d),
                arguments(new QuantityType<>(4.2d, Units.WATT), "4.2", 4.2f, 4.2d));
    }

    @ParameterizedTest
    @MethodSource
    void testNumberConstructor(Number val, String strVal, float floatVal, double doubleVal) {
        assertEquals(new DecimalType(strVal), new DecimalType(val));
        assertEquals(doubleVal, new DecimalType(val).doubleValue());
        assertEquals(floatVal, new DecimalType(val).floatValue());
        assertEquals(new BigDecimal(strVal), new DecimalType(val).toBigDecimal());
        assertEquals(strVal, new DecimalType(val).toString());
    }
}
