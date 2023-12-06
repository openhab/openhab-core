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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.library.unit.MetricPrefix.CENTI;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Stream;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.dimension.DataAmount;
import org.openhab.core.library.dimension.DataTransferRate;
import org.openhab.core.library.dimension.Density;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.unit.BinaryPrefix;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.util.UnitUtils;

import tech.units.indriya.unit.UnitDimension;

/**
 * @author Gaël L'hopital - Initial contribution
 */
@SuppressWarnings("null")
@NonNullByDefault
public class QuantityTypeTest {

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

    @Test
    public void testDimensionless() {
        // Dimensionless value that works
        new QuantityType<>("57%");

        QuantityType<Dimensionless> dt0 = new QuantityType<>("12");
        assertEquals(UnitDimension.NONE, dt0.getUnit().getDimension());
        dt0 = new QuantityType<>("2rad");
        assertEquals(UnitDimension.NONE, dt0.getUnit().getDimension());
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testKnownInvalidConstructors(Locale locale) {
        Locale.setDefault(locale);

        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123 Hello World"));

        assertThrows(NumberFormatException.class, () -> new QuantityType<>("abc"));
        assertThrows(NumberFormatException.class, () -> new QuantityType<>("°C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>(". °C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("1 2 °C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123..56 °C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123abc56 °C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123.123,56 °C"));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123٬123٫56 °C"));

        assertThrows(NumberFormatException.class, () -> new QuantityType<>("abc", Locale.ENGLISH));
        assertThrows(NumberFormatException.class, () -> new QuantityType<>("°C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>(". °C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("1 2 °C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123..56 °C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123abc56 °C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123.123,56 °C", Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123٬123٫56 °C", Locale.ENGLISH));

        assertThrows(NumberFormatException.class, () -> new QuantityType<>("abc", Locale.GERMAN));
        assertThrows(NumberFormatException.class, () -> new QuantityType<>("°C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>(", °C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("1 2 °C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123,,56 °C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123abc56 °C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123,123.56 °C", Locale.GERMAN));
        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("123٬123٫56 °C", Locale.GERMAN));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testValidConstructors(Locale locale) {
        Locale.setDefault(locale);

        // Testing various quantities in order to ensure split and parsing is working as expected
        new QuantityType<>("-2,000.5°C");
        new QuantityType<>("-2.5°C");
        new QuantityType<>("-2°C");
        new QuantityType<>("-0°");
        new QuantityType<>("0°");
        new QuantityType<>("2°");
        new QuantityType<>("2°C");
        new QuantityType<>("2.5°C");
        new QuantityType<>("2,000.5°C");
        new QuantityType<>("10 dBm");
        new QuantityType<>("3 µs");
        new QuantityType<>("3km/h");
        new QuantityType<>("1084 hPa");
        new QuantityType<>("-10E3");
        new QuantityType<>("-10E-3");
        new QuantityType<>("-0E-22 m");
        new QuantityType<>("-0E0");
        new QuantityType<>("-0E-0 m");
        new QuantityType<>("0E0");
        new QuantityType<>("0E-22 m");
        new QuantityType<>("10E-3");
        new QuantityType<>("10E3");
        new QuantityType<>("1 /s");
        new QuantityType<>("1/s");
        new QuantityType<>("1Ω·m");
        new QuantityType<>("1 Ω·m");
        new QuantityType<>("1.5E2Ω·m");
        QuantityType.valueOf("2m");
    }

    @Test
    public void testLowerCaseExponents() {
        assertEquals(QuantityType.valueOf("10e3"), QuantityType.valueOf("10E3"));
        assertEquals(QuantityType.valueOf("1.1e3 W"), QuantityType.valueOf("1.1E3 W"));
        assertEquals(QuantityType.valueOf("1.1e3 m"), QuantityType.valueOf("1.1E3 m"));
        assertEquals(QuantityType.valueOf("1.1e3m"), QuantityType.valueOf("1.1E3 m"));
        assertEquals(QuantityType.valueOf("1.1e3m³"), QuantityType.valueOf("1.1E3 m³"));
        assertEquals(QuantityType.valueOf("1.1e3m·cm"), QuantityType.valueOf("1.1E3 m·cm"));
        assertEquals(QuantityType.valueOf("1.1e3 \u03BCm"), QuantityType.valueOf("1.1E3 µm"));
        assertEquals(QuantityType.valueOf("1.1e3\u03BCm"), QuantityType.valueOf("1.1E3 µm"));
        assertEquals(QuantityType.valueOf("1.1e3 \u00B5m"), QuantityType.valueOf("1.1E3 µm"));
        assertEquals(QuantityType.valueOf("1.1e3\u00B5m"), QuantityType.valueOf("1.1E3 µm"));
        assertEquals(QuantityType.valueOf("1.1e3Ω·m"), QuantityType.valueOf("1.1E3 Ω·m"));
        assertEquals(QuantityType.valueOf("1.1e3 Ω·m"), QuantityType.valueOf("1.1E3 Ω·m"));
        assertEquals(QuantityType.valueOf("1.1e3/s"), QuantityType.valueOf("1.1E3 /s"));
        assertEquals(QuantityType.valueOf("1.1e3 /s"), QuantityType.valueOf("1.1E3 /s"));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testLocalizedStringConstruction(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);

        // Construction for each locale should always return the same result regardless of the current default locale
        Stream.of(Locale.ENGLISH, Locale.GERMAN).forEach(locale -> {
            char ds = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            char gs = DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();

            // Without unit
            assertEquals(new QuantityType<>("123"), new QuantityType<>("123", locale));
            assertEquals(new QuantityType<>("123.56"), new QuantityType<>(String.format("123%s56", ds), locale));
            assertEquals(new QuantityType<>("123123123"), new QuantityType<>("123123123", locale));
            assertEquals(new QuantityType<>("123123123"),
                    new QuantityType<>(String.format("123%s123%s123", gs, gs), locale));
            assertEquals(new QuantityType<>("123123123.56"),
                    new QuantityType<>(String.format("123123123%s56", ds), locale));
            assertEquals(new QuantityType<>("123123123.56"),
                    new QuantityType<>(String.format("123%s123%s123%s56", gs, gs, ds), locale));

            // With unit
            assertEquals(new QuantityType<>("123 °C"), new QuantityType<>("123 °C", locale));
            assertEquals(new QuantityType<>("123.56 °C"), new QuantityType<>(String.format("123%s56 °C", ds), locale));
            assertEquals(new QuantityType<>("123123123 °C"), new QuantityType<>("123123123 °C", locale));
            assertEquals(new QuantityType<>("123123123 °C"),
                    new QuantityType<>(String.format("123%s123%s123 °C", gs, gs), locale));
            assertEquals(new QuantityType<>("123123123.56 °C"),
                    new QuantityType<>(String.format("123123123%s56 °C", ds), locale));
            assertEquals(new QuantityType<>("123123123.56 °C"),
                    new QuantityType<>(String.format("123%s123%s123%s56 °C", gs, gs, ds), locale));
        });
    }

    @Test
    public void testReflectiveInstantiation() throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        QuantityType.class.getDeclaredConstructor().newInstance();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUnits() {
        QuantityType<Length> dt2 = new QuantityType<>("2 m");
        // Check that the unit has correctly been identified
        assertEquals(dt2.getDimension(), UnitDimension.LENGTH);
        assertEquals(dt2.getUnit(), SIUnits.METRE);
        assertEquals("2 m", dt2.toString());

        QuantityType<Length> dt1 = new QuantityType<>("2.1cm");
        // Check that the unit has correctly been identified
        assertEquals(dt1.getDimension(), UnitDimension.LENGTH);
        assertEquals(dt1.getUnit(), CENTI(SIUnits.METRE));
        assertEquals("2.1 cm", dt1.toString());

        assertEquals(dt1.intValue(), dt2.intValue());

        QuantityType<Length> dt3 = new QuantityType<>("200cm");
        assertEquals(dt3.compareTo(dt2), 0);
        assertTrue(dt3.equals(dt2));

        QuantityType dt4 = new QuantityType<>("2kg");
        assertEquals("2 kg", dt4.toString());
        // check that beside the fact that we've got the same value, we don't have the same unit
        assertFalse(dt2.equals(dt4));

        assertThrows(IllegalArgumentException.class, () -> dt2.compareTo(dt4));
    }

    @Test
    public void testFormats() {
        QuantityType<Time> seconds = new QuantityType<>(80, Units.SECOND);
        QuantityType<Time> millis = seconds.toUnit(MetricPrefix.MILLI(Units.SECOND));
        QuantityType<Time> minutes = seconds.toUnit(Units.MINUTE);

        char ds = new DecimalFormatSymbols().getDecimalSeparator();

        assertThat(seconds.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("80" + ds + "0 s"));
        assertThat(millis.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("80000" + ds + "0 ms"));
        assertThat(minutes.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("1" + ds + "3 min"));

        assertThat(seconds.format("%.1f"), is("80" + ds + "0"));
        assertThat(minutes.format("%.1f"), is("1" + ds + "3"));

        assertThat(seconds.format("%1$tH:%1$tM:%1$tS"), is("00:01:20"));
        assertThat(millis.format("%1$tHh %1$tMm %1$tSs"), is("00h 01m 20s"));
    }

    @Test
    public void testConverters() {
        QuantityType<?> dt2 = QuantityType.valueOf("2 m");
        QuantityType<?> dt3 = new QuantityType<>("200 cm");

        assertEquals(dt2.toUnit(SIUnits.METRE), dt3.toUnit(SIUnits.METRE));
        assertEquals(dt2.toUnit(MetricPrefix.CENTI(SIUnits.METRE)), dt3.toUnit(MetricPrefix.CENTI(SIUnits.METRE)));

        dt3 = dt2.toUnit("cm");
        assertTrue(dt2.equals(dt3));

        QuantityType<?> tempInC = new QuantityType<>("22 °C");
        QuantityType<?> tempInK = tempInC.toUnit(Units.KELVIN);
        assertTrue(tempInC.equals(tempInK));
        tempInK = tempInC.toUnit("K");
        assertTrue(tempInC.equals(tempInK));
    }

    @Test
    public void testConvertionOnSameUnit() {
        QuantityType<?> dt2 = QuantityType.valueOf("2 m");
        QuantityType<?> dt3 = dt2.toUnit("m");
        assertTrue("m".equalsIgnoreCase(dt3.getUnit().toString()));
    }

    @Test
    public void testConvertionFromDimensionless() {
        QuantityType<?> dt2 = QuantityType.valueOf("2");
        QuantityType<?> dt3 = dt2.toUnit("m");
        // Inconvertible units
        assertTrue(dt3 == null);
    }

    @Test
    public void testConversionToOnOffType() {
        assertEquals(OnOffType.ON, new QuantityType<>("1").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new QuantityType<>("0").as(OnOffType.class));

        assertEquals(OnOffType.ON, new QuantityType<>("1 %").as(OnOffType.class));
        assertEquals(OnOffType.ON, new QuantityType<>("100 %").as(OnOffType.class));
        assertEquals(OnOffType.OFF, new QuantityType<>("0 %").as(OnOffType.class));
    }

    @Test
    public void testConversionToOpenCloseType() {
        assertEquals(OpenClosedType.OPEN, new QuantityType<>("1.0").as(OpenClosedType.class));
        assertEquals(OpenClosedType.CLOSED, new QuantityType<>("0.0").as(OpenClosedType.class));
        assertNull(new QuantityType<>("0.5").as(OpenClosedType.class));
    }

    @Test
    public void testConversionToUpDownType() {
        assertEquals(UpDownType.UP, new QuantityType<>("0.0").as(UpDownType.class));
        assertEquals(UpDownType.DOWN, new QuantityType<>("1.0").as(UpDownType.class));
        assertNull(new QuantityType<>("0.5").as(OpenClosedType.class));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testConversionToHSBType(Locale locale) {
        Locale.setDefault(locale);

        assertEquals(new HSBType("0,0,0"), new QuantityType<>("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new QuantityType<>("1.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new QuantityType<>("0.5").as(HSBType.class));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testConversionToPercentType(Locale locale) {
        Locale.setDefault(locale);

        assertEquals(PercentType.HUNDRED, new QuantityType<>("100 %").as(PercentType.class));
        assertEquals(PercentType.ZERO, new QuantityType<>("0 %").as(PercentType.class));

        // Test QuantityType (different ways to refer to 10%) conversion to PercentType
        assertEquals(new PercentType(BigDecimal.valueOf(10)), new QuantityType<>("10 %").as(PercentType.class));
        assertEquals(new PercentType(BigDecimal.valueOf(10)), new QuantityType<>("0.1").as(PercentType.class));
        assertEquals(new PercentType(BigDecimal.valueOf(10)), new QuantityType<>("100 %/10").as(PercentType.class));
        assertEquals(new PercentType(BigDecimal.valueOf(10)), new QuantityType<>("100000 ppm").as(PercentType.class));

        // Known caveat: bare unit, different dimension. Still gets converted to %
        assertEquals(new PercentType(BigDecimal.valueOf(10)),
                new QuantityType<>(0.1, Units.RADIAN).as(PercentType.class));

        // incompatible units
        assertEquals(null, new QuantityType<>("0.5 m").as(PercentType.class));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void toFullStringShouldParseToEqualState(Locale locale) {
        Locale.setDefault(locale);

        QuantityType<Temperature> temp = new QuantityType<>("20 °C");

        assertThat(temp.toFullString(), is("20 °C"));
        assertThat(QuantityType.valueOf(temp.toFullString()), is(temp));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testAdd(Locale locale) {
        Locale.setDefault(locale);

        QuantityType<?> result = new QuantityType<>("20 m").add(new QuantityType<>("20cm"));
        assertThat(result, is(new QuantityType<>("20.20 m")));

        assertThat(new QuantityType<>("65 °F").add(new QuantityType<>("1 °F")), is(new QuantityType<>("66 °F")));
        assertThat(new QuantityType<>("65 °F").add(new QuantityType<>("2 °F")), is(new QuantityType<>("67 °F")));
        assertThat(new QuantityType<>("1 °F").add(new QuantityType<>("65 °F")), is(new QuantityType<>("66 °F")));
        assertThat(new QuantityType<>("2 °F").add(new QuantityType<>("65 °F")), is(new QuantityType<>("67 °F")));

        result = new QuantityType<>("65 °F").add(new QuantityType<>("5 °C")).toUnit("°F");
        assertThat(result.doubleValue(), is(closeTo(74d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());

        // test associativity of add
        QuantityType<Temperature> tempResult = new QuantityType<Temperature>("1 °F")
                .add(new QuantityType<Temperature>("2 °F")).add(new QuantityType<Temperature>("3 °F"));
        assertThat(tempResult, is(new QuantityType<Temperature>("1 °F")
                .add(new QuantityType<Temperature>("2 °F").add(new QuantityType<Temperature>("3 °F")))));
        assertThat(tempResult, is(new QuantityType<Temperature>("6 °F")));

        assertThat(new QuantityType<>("65 kWh").add(new QuantityType<>("1 kWh")), is(new QuantityType<>("66 kWh")));
        assertThat(new QuantityType<>("65 kJ").add(new QuantityType<>("1 kJ")), is(new QuantityType<>("66 kJ")));
        assertThat(new QuantityType<>("65 kWh").add(new QuantityType<>("1 kJ")), is(new QuantityType<>("234001 kJ")));
    }

    @Test
    public void testNegate() {
        assertThat(new QuantityType<>("20 °C").negate(), is(new QuantityType<>("-20 °C")));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testSubtract(Locale locale) {
        Locale.setDefault(locale);

        QuantityType<?> result = new QuantityType<>("20 m").subtract(new QuantityType<>("20cm"));
        assertThat(result, is(new QuantityType<>("19.80 m")));

        assertThat(new QuantityType<>("65 °F").subtract(new QuantityType<>("1 °F")), is(new QuantityType<>("64 °F")));
        assertThat(new QuantityType<>("65 °F").subtract(new QuantityType<>("2 °F")), is(new QuantityType<>("63 °F")));
        assertThat(new QuantityType<>("1 °F").subtract(new QuantityType<>("65 °F")), is(new QuantityType<>("-64 °F")));
        assertThat(new QuantityType<>("2 °F").subtract(new QuantityType<>("65 °F")), is(new QuantityType<>("-63 °F")));

        assertThat(new QuantityType<>("65 kWh").subtract(new QuantityType<>("1 kWh")),
                is(new QuantityType<>("64 kWh")));
        assertThat(new QuantityType<>("65 kJ").subtract(new QuantityType<>("1 kJ")), is(new QuantityType<>("64 kJ")));
        assertThat(new QuantityType<>("65 kWh").subtract(new QuantityType<>("1 kJ")),
                is(new QuantityType<>("233999 kJ")));
    }

    @Test
    public void testMultiplyNumber() {
        assertThat(new QuantityType<>("2 m").multiply(BigDecimal.valueOf(2)), is(new QuantityType<>("4 m")));

        assertThat(new QuantityType<>("65 °F").multiply(BigDecimal.valueOf(1)).toUnit("°F").doubleValue(),
                is(closeTo(65d, 0.0000000000000001d)));
        assertThat(new QuantityType<>("65 °F").multiply(BigDecimal.valueOf(2)).toUnit("°F").doubleValue(),
                is(closeTo(589.67d, 0.0000000000000001d)));
    }

    @Test
    public void testMultiplyQuantityType() {
        QuantityType<?> result;

        assertThat(new QuantityType<>("2 m").multiply(new QuantityType<>("4 cm")), is(new QuantityType<>("8 m·cm")));

        // Make sure the original unit is preserved when multiplying with dimensionless, so add associativity is
        // guaranteed
        result = new QuantityType<>("65 °F").multiply(QuantityType.valueOf(1, Units.ONE));
        assertThat(result.doubleValue(), is(closeTo(65d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());
        result = new QuantityType<>("65 °F").multiply(QuantityType.valueOf(2, Units.ONE));
        assertThat(result.doubleValue(), is(closeTo(589.67d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());
        result = QuantityType.valueOf(1, Units.ONE).multiply(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(65d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());
        result = QuantityType.valueOf(2, Units.ONE).multiply(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(589.67d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());

        result = new QuantityType<>("65 °F").multiply(new QuantityType<>("1 °F"));
        assertThat(result.doubleValue(), is(closeTo(74598.68175925925925925925925925927d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.multiply(Units.KELVIN), result.getUnit());
        result = new QuantityType<>("65 °F").multiply(new QuantityType<>("2 °F"));
        assertThat(result.doubleValue(), is(closeTo(74760.6169444444444444444444444444d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.multiply(Units.KELVIN), result.getUnit());
        result = new QuantityType<>("1 °F").multiply(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(74598.68175925925925925925925925927d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.multiply(Units.KELVIN), result.getUnit());
        result = new QuantityType<>("2 °F").multiply(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(74760.6169444444444444444444444444d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.multiply(Units.KELVIN), result.getUnit());

        assertThat(new QuantityType<>("65 kWh").multiply(QuantityType.valueOf(1, Units.ONE)),
                is(new QuantityType<>("65 kWh")));
        assertThat(new QuantityType<>("65 kJ").multiply(QuantityType.valueOf(1, Units.ONE)),
                is(new QuantityType<>("65 kJ")));
        assertThat(new QuantityType<>("65 kWh").multiply(new QuantityType<>("1 kWh")),
                is(new QuantityType<>(65, Units.KILOWATT_HOUR.multiply(Units.KILOWATT_HOUR))));
        assertThat(new QuantityType<>("65 kJ").multiply(new QuantityType<>("1 kJ")),
                is(new QuantityType<>(65, MetricPrefix.KILO(Units.JOULE).multiply(MetricPrefix.KILO(Units.JOULE)))));
        assertThat(new QuantityType<>("65 kWh").multiply(new QuantityType<>("1 kJ")),
                is(new QuantityType<>(65, Units.KILOWATT_HOUR.multiply(MetricPrefix.KILO(Units.JOULE)))));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testDivideNumber(Locale locale) {
        Locale.setDefault(locale);

        assertThat(new QuantityType<>("4 m").divide(BigDecimal.valueOf(2)), is(new QuantityType<>("2 m")));

        assertThat(new QuantityType<>("65 °F").divide(BigDecimal.valueOf(1)).toUnit("°F").doubleValue(),
                is(closeTo(65d, 0.0000000000000001d)));
        assertThat(new QuantityType<>("65 °F").divide(BigDecimal.valueOf(2)).toUnit("°F").doubleValue(),
                is(closeTo(-197.335d, 0.0000000000000001d)));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testDivideQuantityType(Locale locale) {
        Locale.setDefault(locale);

        QuantityType<?> result;

        assertThat(new QuantityType<>("4 m").divide(new QuantityType<>("2 cm")), is(new QuantityType<>("2 m/cm")));

        // Make sure the original unit is preserved when dividing with dimensionless, so add associativity is guaranteed
        result = new QuantityType<>("65 °F").divide(QuantityType.valueOf(1, Units.ONE));
        assertThat(result.doubleValue(), is(closeTo(65d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());
        result = new QuantityType<>("65 °F").divide(QuantityType.valueOf(2, Units.ONE));
        assertThat(result.doubleValue(), is(closeTo(-197.335d, 0.0000000000000001d)));
        assertEquals(ImperialUnits.FAHRENHEIT, result.getUnit());
        result = QuantityType.valueOf(1, Units.ONE).divide(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(0.003430727886099834181485505174681228d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.inverse(), result.getUnit());
        result = QuantityType.valueOf(2, Units.ONE).divide(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(), is(closeTo(0.006861455772199668362971010349362456d, 0.0000000000000001d)));
        assertEquals(Units.KELVIN.inverse(), result.getUnit());

        result = new QuantityType<>("65 °F").divide(new QuantityType<>("1 °F"));
        assertThat(result.doubleValue(),
                is(closeTo(1.138928083009529598193934920876115122114246640762367855514793670089202480d,
                        0.0000000000000001d)));
        assertEquals(Units.ONE, result.getUnit());
        result = new QuantityType<>("65 °F").divide(new QuantityType<>("2 °F"));
        assertThat(result.doubleValue(),
                is(closeTo(1.136461108584053544739749171486126553533555353390950245846600385556783676d,
                        0.0000000000000001d)));
        assertEquals(Units.ONE, result.getUnit());
        result = new QuantityType<>("1 °F").divide(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(),
                is(closeTo(0.878018564049783673547182038233556349552596235093804994885674169795613456d,
                        0.0000000000000001d)));
        assertEquals(Units.ONE, result.getUnit());
        result = new QuantityType<>("2 °F").divide(new QuantityType<>("65 °F"));
        assertThat(result.doubleValue(),
                is(closeTo(0.879924523986505803648007318886157031927295252253797625173918844225890256d,
                        0.0000000000000001d)));
        assertEquals(Units.ONE, result.getUnit());

        assertThat(new QuantityType<>("65 kWh").divide(QuantityType.valueOf(1, Units.ONE)),
                is(new QuantityType<>("65 kWh")));
        assertThat(new QuantityType<>("65 kJ").divide(QuantityType.valueOf(1, Units.ONE)),
                is(new QuantityType<>("65 kJ")));
        assertThat(new QuantityType<>("65 kWh").divide(new QuantityType<>("1 kWh")),
                is(new QuantityType<>(65, Units.ONE)));
        assertThat(new QuantityType<>("65 kJ").divide(new QuantityType<>("1 kJ")),
                is(new QuantityType<>(65, Units.ONE)));
        assertThat(new QuantityType<>("65 kWh").divide(new QuantityType<>("1 kJ")).toUnit(Units.ONE),
                is(new QuantityType<>(234000, Units.ONE)));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testDivideZero(Locale locale) {
        Locale.setDefault(locale);

        assertThrows(IllegalArgumentException.class, () -> new QuantityType<>("4 m").divide(QuantityType.ZERO));
    }

    @Test
    public void testExponentials() {
        QuantityType<Length> exponential = new QuantityType<>("10E-2 m");
        assertEquals(new QuantityType<>("10 cm"), exponential);

        exponential = new QuantityType<>("10E3 m");
        assertEquals(new QuantityType<>("10 km"), exponential);
    }

    @Test
    public void testDensity() {
        QuantityType<Density> density = new QuantityType<>("19816 kg/m³");
        assertEquals(19816, density.doubleValue(), 1E-5);

        density = density.toUnit("g/cm³");
        assertEquals("19.816 g/cm³", density.toString());
    }

    @Test
    public void testIntensity() {
        QuantityType<Intensity> density = new QuantityType<>("10 W/m²");
        assertEquals(10, density.doubleValue(), 1E-5);
        assertEquals(Units.IRRADIANCE.toString(), density.getUnit().toString());

        density = density.toUnit("W/cm²");
        assertEquals("0.001 W/cm²", density.toString());

        density = new QuantityType<>(2, Units.IRRADIANCE);
        assertEquals(2, density.doubleValue(), 1E-5);
        assertEquals("2 W/m²", density.toString());

        density = new QuantityType<>("3 W/m^2");
        assertEquals(3, density.doubleValue(), 1E-5);
        assertEquals("3 W/m²", density.toString());
    }

    @Test
    public void testEnergyUnits() {
        QuantityType<Energy> energy = new QuantityType<>("28800 J");
        assertEquals("0.008 kWh", energy.toUnit("kWh").toString());
        assertEquals("28800 Ws", energy.toUnit("Ws").toString());
    }

    @Test
    public void testPressureUnits() {
        QuantityType<Pressure> pressure = new QuantityType<>("1013 mbar");
        assertEquals("1.013 bar", pressure.toUnit("bar").toString());
        assertEquals("101300 Pa", pressure.toUnit("Pa").toString());
    }

    @Test
    public void testMWh() {
        QuantityType<Energy> energy = new QuantityType<>("1 MWh");
        assertEquals("1000000 Wh", energy.toUnit("Wh").toString());
    }

    @Test
    public void testRainfallRate() {
        QuantityType<Speed> rate = new QuantityType<>("3 mm/h");
        assertEquals("0.1181102362204724409448818897637795 in/h", rate.toUnit("in/h").toString());
    }

    @Test
    public void testDataAmount() {
        QuantityType<DataAmount> amount = new QuantityType<>("8 bit");
        QuantityType<DataAmount> octet = amount.toUnit(Units.BYTE);
        assertEquals(1, octet.byteValue());
        QuantityType<DataAmount> bytes = new QuantityType<>("1 B");
        assertEquals("1 B", bytes.toString());
        QuantityType<DataAmount> bits = bytes.toUnit(Units.BIT);
        assertEquals(8, bits.byteValue());
        bytes = new QuantityType<>("1 MB");
        assertEquals("1 MB", bytes.toString());
        bytes = new QuantityType<>(1, MetricPrefix.MEGA(Units.BYTE));
        assertEquals("1 MB", bytes.toString());
        bytes = new QuantityType<>("1 GiB");
        assertEquals("1 GiB", bytes.toString());
        bytes = new QuantityType<>(1, BinaryPrefix.GIBI(Units.BYTE));
        assertEquals("1 GiB", bytes.toString());
        QuantityType<DataAmount> bigAmount = new QuantityType<>("1 kio");
        QuantityType<DataAmount> octets = bigAmount.toUnit(Units.OCTET);
        assertEquals(1024, octets.intValue());
        QuantityType<DataAmount> hugeAmount = new QuantityType<>("1024Gio");
        QuantityType<DataAmount> lotOfOctets = hugeAmount.toUnit(Units.OCTET);
        assertEquals("1099511627776 o", lotOfOctets.format("%d o"));
    }

    @Test
    public void testDataTransferRate() {
        QuantityType<DataTransferRate> speed = new QuantityType<>("1024 bit/s");
        QuantityType<DataTransferRate> octet = speed.toUnit(Units.OCTET.divide(Units.SECOND));
        assertEquals(128, octet.intValue());
        QuantityType<DataTransferRate> gsm2G = new QuantityType<>("115 Mbit/s");
        QuantityType<DataTransferRate> octets = gsm2G.toUnit(MetricPrefix.KILO(Units.OCTET).divide(Units.SECOND));
        assertEquals(14375, octets.intValue());
    }

    @Test
    public void testMireds() {
        QuantityType<Temperature> colorTemp = new QuantityType<>("2700 K");
        QuantityType<?> mireds = colorTemp.toInvertibleUnit(Units.MIRED);
        assertEquals(370, mireds.intValue());
        assertThat(colorTemp.equals(mireds), is(true));
        assertThat(mireds.equals(colorTemp), is(true));
        QuantityType<?> andBack = mireds.toInvertibleUnit(Units.KELVIN);
        assertEquals(2700, andBack.intValue());
    }

    @Test
    public void testRelativeConversion() {
        QuantityType<Temperature> c = new QuantityType<>("1 °C");
        QuantityType<Temperature> f = c.toUnitRelative(ImperialUnits.FAHRENHEIT);
        assertEquals(1.8, f.doubleValue());
    }
}
