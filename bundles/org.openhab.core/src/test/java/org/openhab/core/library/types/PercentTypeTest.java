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

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.core.library.unit.Units;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class PercentTypeTest {

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

        assertThrows(NumberFormatException.class, () -> new PercentType("123 Hello World"));

        assertThrows(NumberFormatException.class, () -> new PercentType(""));
        assertThrows(NumberFormatException.class, () -> new PercentType("."));
        assertThrows(NumberFormatException.class, () -> new PercentType("1 2"));
        assertThrows(NumberFormatException.class, () -> new PercentType("1..56"));
        assertThrows(NumberFormatException.class, () -> new PercentType("1abc"));

        assertThrows(NumberFormatException.class, () -> new PercentType("", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new PercentType(".", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new PercentType("1 2", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new PercentType("1..56", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new PercentType("1abc", Locale.ENGLISH));

        assertThrows(NumberFormatException.class, () -> new PercentType("", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new PercentType(",", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new PercentType("1 2", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new PercentType("1,,56", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new PercentType("1abc", Locale.GERMAN));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "0.000", "0.001", "2", "2.5", "0E0", "0E-22", "10E-3", "1E2" })
    public void testValidConstructors(String value) {
        new PercentType(value);
        PercentType.valueOf(value);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testLocalizedStringConstruction(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);

        // Construction for each locale should always return the same result regardless of the current default locale
        Stream.of(Locale.ENGLISH, Locale.GERMAN).forEach(locale -> {
            char ds = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            char gs = DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();

            assertEquals(new PercentType("0"), new PercentType("0", locale));
            assertEquals(new PercentType("0.000"), new PercentType(String.format("0%s000", ds), locale));
            assertEquals(new PercentType("0.001"), new PercentType(String.format("0%s001", ds), locale));
            assertEquals(new PercentType("1.56E-10"), new PercentType(String.format("1%s56E-10", ds), locale));
            assertEquals(new PercentType("1E0"), new PercentType("1E0", locale));
            assertEquals(new PercentType("1"), new PercentType("1", locale));
            assertEquals(new PercentType("12"), new PercentType("12", locale));
            assertEquals(new PercentType("12.56"), new PercentType(String.format("12%s56", ds), locale));
            assertEquals(new PercentType("100"), new PercentType(String.format("100", gs, gs), locale));
        });
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void negativeNumber(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);

        assertThrows(IllegalArgumentException.class, () -> new PercentType(-3));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("-0.003"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("-0.1E1"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("-1,000"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("-0,1E1", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("-1.000", Locale.GERMAN));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void moreThan100(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);

        assertThrows(IllegalArgumentException.class, () -> new PercentType(101));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("100.2"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("1.1E2"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("1,000"));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("1.000", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new PercentType("1,1E2", Locale.GERMAN));
    }

    @Test
    public void doubleValue() {
        PercentType pt = new PercentType("0.0001");
        assertEquals("0.0001", pt.toString());
    }

    @Test
    public void intValue() {
        PercentType pt = new PercentType(100);
        assertEquals("100", pt.toString());
    }

    @Test
    public void testEquals() {
        PercentType pt1 = new PercentType(Integer.valueOf(100));
        PercentType pt2 = new PercentType("100.0");
        PercentType pt3 = new PercentType(0);
        PercentType pt4 = new PercentType(0);

        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertTrue(pt1.equals(pt2));
        assertTrue(pt3.equals(pt4));
        assertFalse(pt3.equals(pt1));
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new PercentType("100.0").as(OnOffType.class));
        assertEquals(OnOffType.ON, new PercentType("1.0").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new PercentType("0.0").as(OnOffType.class));
    }

    @Test
    public void testConversionToDecimalType() {
        assertEquals(new DecimalType("1.0"), new PercentType("100.0").as(DecimalType.class));
        assertEquals(new DecimalType("0.01"), new PercentType("1.0").as(DecimalType.class));
        assertEquals(DecimalType.ZERO, new PercentType("0.0").as(DecimalType.class));
    }

    @Test
    public void testConversionToQuantityType() {
        assertEquals(new QuantityType<>("100 %"), PercentType.HUNDRED.as(QuantityType.class));
        assertEquals(new QuantityType<>("1 one"),
                ((QuantityType<?>) PercentType.HUNDRED.as(QuantityType.class)).toUnit(Units.ONE));
    }

    @Test
    public void testConversionToOpenCloseType() {
        assertEquals(OpenClosedType.OPEN, new PercentType("100.0").as(OpenClosedType.class));
        assertEquals(OpenClosedType.CLOSED, new PercentType("0.0").as(OpenClosedType.class));
        assertNull(new PercentType("50.0").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToUpDownType() {
        assertEquals(UpDownType.UP, new PercentType("0.0").as(UpDownType.class));
        assertEquals(UpDownType.DOWN, new PercentType("100.0").as(UpDownType.class));
        assertNull(new PercentType("50.0").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToHSBType() {
        assertEquals(new HSBType("0,0,0"), new PercentType("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new PercentType("100.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new PercentType("50.0").as(HSBType.class));
    }
}
