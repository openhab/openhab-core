/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import static org.junit.Assert.*;
import static org.openhab.core.library.unit.MetricPrefix.CENTI;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;

import org.junit.Test;
import org.openhab.core.library.dimension.DataAmount;
import org.openhab.core.library.dimension.DataTransferRate;
import org.openhab.core.library.dimension.Density;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;
import org.openhab.core.types.util.UnitUtils;

import tec.uom.se.quantity.QuantityDimension;
import tec.uom.se.unit.Units;

/**
 * @author Gaël L'hopital - Initial contribution
 */
@SuppressWarnings("null")
public class QuantityTypeTest {

    // we need to get the decimal separator of the default locale for our tests
    private static final char SEP = new DecimalFormatSymbols().getDecimalSeparator();

    @Test
    public void testDimensionless() {
        // Dimensionless value that works
        new QuantityType<>("57%");

        QuantityType<Dimensionless> dt0 = new QuantityType<>("12");
        assertTrue(dt0.getUnit().getDimension() == QuantityDimension.NONE);
        dt0 = new QuantityType<>("2rad");
        assertTrue(dt0.getUnit().getDimension() == QuantityDimension.NONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKnownInvalidConstructors() throws Exception {
        new QuantityType<>("123 Hello World");
    }

    @Test
    public void testValidConstructors() throws Exception {
        // Testing various quantities in order to ensure split and parsing is working
        // as expected
        new QuantityType<>("2°");
        new QuantityType<>("2°C");
        new QuantityType<>("3 µs");
        new QuantityType<>("3km/h");
        new QuantityType<>("1084 hPa");
        new QuantityType<>("0E-22 m");
        new QuantityType<>("10E-3");
        new QuantityType<>("10E+3");
        new QuantityType<>("10E3");
        QuantityType.valueOf("2m");
    }

    @Test
    public void testReflectiveInstantiation() throws InstantiationException, IllegalAccessException {
        QuantityType.class.newInstance();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUnits() {
        QuantityType<Length> dt2 = new QuantityType<>("2 m");
        // Check that the unit has correctly been identified
        assertEquals(dt2.getDimension(), QuantityDimension.LENGTH);
        assertEquals(dt2.getUnit(), SIUnits.METRE);
        assertEquals("2 m", dt2.toString());

        QuantityType<Length> dt1 = new QuantityType<>("2.1cm");
        // Check that the unit has correctly been identified
        assertEquals(dt1.getDimension(), QuantityDimension.LENGTH);
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
        try {
            dt2.compareTo(dt4);
            fail();
        } catch (Exception e) {
            // That's what we expect.
        }
    }

    @Test
    public void testFormats() {
        QuantityType<Time> seconds = new QuantityType<>(80, SmartHomeUnits.SECOND);
        QuantityType<Time> millis = seconds.toUnit(MetricPrefix.MILLI(SmartHomeUnits.SECOND));
        QuantityType<Time> minutes = seconds.toUnit(SmartHomeUnits.MINUTE);

        assertThat(seconds.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("80" + SEP + "0 s"));
        assertThat(millis.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("80000" + SEP + "0 ms"));
        assertThat(minutes.format("%.1f " + UnitUtils.UNIT_PLACEHOLDER), is("1" + SEP + "3 min"));

        assertThat(seconds.format("%.1f"), is("80" + SEP + "0"));
        assertThat(minutes.format("%.1f"), is("1" + SEP + "3"));

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
        QuantityType<?> tempInK = tempInC.toUnit(SmartHomeUnits.KELVIN);
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

    @Test
    public void testConversionToHSBType() {
        assertEquals(new HSBType("0,0,0"), new QuantityType<>("0.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,100"), new QuantityType<>("1.0").as(HSBType.class));
        assertEquals(new HSBType("0,0,50"), new QuantityType<>("0.5").as(HSBType.class));
    }

    @Test
    public void testConversionToPercentType() {
        assertEquals(PercentType.HUNDRED, new QuantityType<>("100 %").as(PercentType.class));
        assertEquals(PercentType.ZERO, new QuantityType<>("0 %").as(PercentType.class));
    }

    @Test
    public void toFullStringShouldParseToEqualState() {
        QuantityType<Temperature> temp = new QuantityType<>("20 °C");

        assertThat(temp.toFullString(), is("20 °C"));
        assertThat(QuantityType.valueOf(temp.toFullString()), is(temp));
    }

    public void testAdd() {
        QuantityType<?> result = new QuantityType<>("20 m").add(new QuantityType<>("20cm"));
        assertThat(result, is(new QuantityType<>("20.20 m")));
    }

    @Test
    public void testNegate() {
        assertThat(new QuantityType<>("20 °C").negate(), is(new QuantityType<>("-20 °C")));
    }

    @Test
    public void testSubtract() {
        QuantityType<?> result = new QuantityType<>("20 m").subtract(new QuantityType<>("20cm"));
        assertThat(result, is(new QuantityType<>("19.80 m")));
    }

    @Test
    public void testMultiplyNumber() {
        assertThat(new QuantityType<>("2 m").multiply(BigDecimal.valueOf(2)), is(new QuantityType<>("4 m")));
    }

    @Test
    public void testMultiplyQuantityType() {
        assertThat(new QuantityType<>("2 m").multiply(new QuantityType<>("4 cm")), is(new QuantityType<>("8 m·cm")));
    }

    @Test
    public void testDivideNumber() {
        assertThat(new QuantityType<>("4 m").divide(BigDecimal.valueOf(2)), is(new QuantityType<>("2 m")));
    }

    @Test
    public void testDivideQuantityType() {
        assertThat(new QuantityType<>("4 m").divide(new QuantityType<>("2 cm")), is(new QuantityType<>("2 m/cm")));
    }

    @Test(expected = ArithmeticException.class)
    public void testDivideZero() {
        new QuantityType<>("4 m").divide(QuantityType.ZERO);
    }

    @Test
    public void testExponentials() {
        QuantityType<Length> exponential = new QuantityType<>("10E-2 m");
        assertEquals(exponential, new QuantityType<>("10 cm"));

        exponential = new QuantityType<>("10E+3 m");
        assertEquals(exponential, new QuantityType<>("10 km"));

        exponential = new QuantityType<>("10E3 m");
        assertEquals(exponential, new QuantityType<>("10 km"));
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
        assertEquals(SmartHomeUnits.IRRADIANCE.toString(), density.getUnit().toString());

        density = density.toUnit("W/cm²");
        assertEquals("0.001 W/cm²", density.toString());

        density = new QuantityType<>(2, SmartHomeUnits.IRRADIANCE);
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
        QuantityType<DataAmount> octet = amount.toUnit(SmartHomeUnits.BYTE);
        assertEquals(1, octet.byteValue());
        QuantityType<DataAmount> bigAmount = new QuantityType<>("1 Kio");
        QuantityType<DataAmount> octets = bigAmount.toUnit(SmartHomeUnits.OCTET);
        assertEquals(1024, octets.intValue());
        QuantityType<DataAmount> hugeAmount = new QuantityType<>("1024Gio");
        QuantityType<DataAmount> lotOfOctets = hugeAmount.toUnit(SmartHomeUnits.OCTET);
        assertEquals("1099511627776 o", lotOfOctets.toString());
    }

    @Test
    public void testDataTransferRate() {
        QuantityType<DataTransferRate> speed = new QuantityType<>("1024 bit/s");
        QuantityType<DataTransferRate> octet = speed.toUnit(SmartHomeUnits.OCTET.divide(Units.SECOND));
        assertEquals(128, octet.intValue());
        QuantityType<DataTransferRate> gsm2G = new QuantityType<>("115 Mbit/s");
        QuantityType<DataTransferRate> octets = gsm2G
                .toUnit(MetricPrefix.KILO(SmartHomeUnits.OCTET).divide(Units.SECOND));
        assertEquals(14375, octets.intValue());
    }
}
