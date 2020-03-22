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
package org.openhab.core.library.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;

import org.junit.Test;
import org.openhab.core.library.dimension.ArealDensity;
import org.openhab.core.library.dimension.Density;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.types.QuantityType;

import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.Units;

/**
 * Test for the framework defined {@link SmartHomeUnits}.
 *
 * @author Henning Treu - Initial contribution
 */
public class SmartHomeUnitsTest {

    private static final double DEFAULT_ERROR = 0.0000000000000001d;

    @Test
    public void testInHg2PascalConversion() {
        Quantity<Pressure> inHg = Quantities.getQuantity(BigDecimal.ONE, ImperialUnits.INCH_OF_MERCURY);

        assertThat(inHg.to(SIUnits.PASCAL), is(Quantities.getQuantity(new BigDecimal("3386.388"), SIUnits.PASCAL)));
        assertThat(inHg.to(MetricPrefix.HECTO(SIUnits.PASCAL)),
                is(Quantities.getQuantity(new BigDecimal("33.86388"), MetricPrefix.HECTO(SIUnits.PASCAL))));
    }

    @Test
    public void testInHgUnitSymbol() {
        assertThat(ImperialUnits.INCH_OF_MERCURY.getSymbol(), is("inHg"));
        assertThat(ImperialUnits.INCH_OF_MERCURY.toString(), is("inHg"));
    }

    @Test
    public void testPascal2inHgConversion() {
        Quantity<Pressure> pascal = Quantities.getQuantity(new BigDecimal("3386.388"), SIUnits.PASCAL);

        assertThat(pascal.to(ImperialUnits.INCH_OF_MERCURY),
                is(Quantities.getQuantity(BigDecimal.ONE, ImperialUnits.INCH_OF_MERCURY)));
    }

    @Test
    public void testmmHg2PascalConversion() {
        Quantity<Pressure> mmHg = Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.MILLIMETRE_OF_MERCURY);

        assertThat(mmHg.to(SIUnits.PASCAL), is(Quantities.getQuantity(new BigDecimal("133.322368"), SIUnits.PASCAL)));
        assertThat(mmHg.to(MetricPrefix.HECTO(SIUnits.PASCAL)),
                is(Quantities.getQuantity(new BigDecimal("1.33322368"), MetricPrefix.HECTO(SIUnits.PASCAL))));
    }

    @Test
    public void testMmHgUnitSymbol() {
        assertThat(SmartHomeUnits.MILLIMETRE_OF_MERCURY.getSymbol(), is("mmHg"));
        assertThat(SmartHomeUnits.MILLIMETRE_OF_MERCURY.toString(), is("mmHg"));
    }

    @Test
    public void testPascal2mmHgConversion() {
        Quantity<Pressure> pascal = Quantities.getQuantity(new BigDecimal("133.322368"), SIUnits.PASCAL);

        assertThat(pascal.to(SmartHomeUnits.MILLIMETRE_OF_MERCURY),
                is(Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.MILLIMETRE_OF_MERCURY)));
    }

    @Test
    public void testHectoPascal2Pascal() {
        Quantity<Pressure> pascal = Quantities.getQuantity(BigDecimal.valueOf(100), SIUnits.PASCAL);

        assertThat(pascal.to(MetricPrefix.HECTO(SIUnits.PASCAL)),
                is(Quantities.getQuantity(BigDecimal.ONE, MetricPrefix.HECTO(SIUnits.PASCAL))));
    }

    @Test
    public void testHpaUnitSymbol() {
        assertThat(MetricPrefix.HECTO(SIUnits.PASCAL).toString(), is("hPa"));
    }

    @Test
    public void testKelvin2Fahrenheit() {
        Quantity<Temperature> kelvin = Quantities.getQuantity(BigDecimal.ZERO, SmartHomeUnits.KELVIN);

        assertThat(kelvin.to(ImperialUnits.FAHRENHEIT),
                is(Quantities.getQuantity(new BigDecimal("-459.67"), ImperialUnits.FAHRENHEIT)));
    }

    @Test
    public void testKelvin2Fahrenheit2() {
        Quantity<Temperature> kelvin = Quantities.getQuantity(new BigDecimal("300"), SmartHomeUnits.KELVIN);

        assertThat(kelvin.to(ImperialUnits.FAHRENHEIT),
                is(Quantities.getQuantity(new BigDecimal("80.33"), ImperialUnits.FAHRENHEIT)));
    }

    @Test
    public void testFahrenheit2Kelvin() {
        Quantity<Temperature> fahrenheit = Quantities.getQuantity(new BigDecimal("100"), ImperialUnits.FAHRENHEIT);

        Quantity<Temperature> kelvin = fahrenheit.to(SmartHomeUnits.KELVIN);
        assertThat(kelvin.getUnit(), is(SmartHomeUnits.KELVIN));
        assertThat(kelvin.getValue().doubleValue(), is(closeTo(310.92777777777777778d, DEFAULT_ERROR)));
    }

    @Test
    public void testCelsiusSpecialChar() {
        QuantityType<Temperature> celsius = new QuantityType<>("20 ℃");
        assertThat(celsius, is(new QuantityType<>("20 °C")));
        assertThat(celsius.toFullString(), is("20 °C"));

        assertThat(celsius.getUnit().toString(), is("°C"));
    }

    @Test
    public void testKmh2Mih() {
        Quantity<Speed> kmh = Quantities.getQuantity(BigDecimal.TEN, SIUnits.KILOMETRE_PER_HOUR);

        Quantity<Speed> mph = kmh.to(ImperialUnits.MILES_PER_HOUR);
        assertThat(mph.getUnit(), is(ImperialUnits.MILES_PER_HOUR));
        assertThat(mph.getValue().doubleValue(), is(closeTo(6.21371192237333935d, DEFAULT_ERROR)));
    }

    @Test
    public void testKmh2Knot() {
        Quantity<Speed> kmh = Quantities.getQuantity(new BigDecimal("1.852"), SIUnits.KILOMETRE_PER_HOUR);

        Quantity<Speed> knot = kmh.to(SmartHomeUnits.KNOT);
        assertThat(knot.getUnit(), is(SmartHomeUnits.KNOT));
        assertThat(knot.getValue().doubleValue(), is(closeTo(1.000, DEFAULT_ERROR)));
    }

    @Test
    public void testKnot2Kmh() {
        Quantity<Speed> knot = Quantities.getQuantity(BigDecimal.TEN, SmartHomeUnits.KNOT);

        Quantity<Speed> kmh = knot.to(SIUnits.KILOMETRE_PER_HOUR);
        assertThat(kmh.getUnit(), is(SIUnits.KILOMETRE_PER_HOUR));
        assertThat(kmh.getValue().doubleValue(), is(closeTo(18.52, DEFAULT_ERROR)));
    }

    @Test
    public void testKnotUnitSymbol() {
        assertThat(SmartHomeUnits.KNOT.getSymbol(), is("kn"));
        assertThat(SmartHomeUnits.KNOT.toString(), is("kn"));
    }

    @Test
    public void testVarUnitSymbol() {
        assertThat(SmartHomeUnits.VAR.getSymbol(), is("var"));
        assertThat(SmartHomeUnits.VAR.toString(), is("var"));
        assertThat(SmartHomeUnits.VAR_HOUR.toString(), is("varh"));
    }

    @Test
    public void testKVarUnitSymbol() {
        assertThat(SmartHomeUnits.KILOVAR.toString(), is("kvar"));
        assertThat(SmartHomeUnits.KILOVAR_HOUR.toString(), is("kvarh"));

        Quantity<Power> kvar = Quantities.getQuantity(BigDecimal.TEN, MetricPrefix.KILO(SmartHomeUnits.VAR));
        assertThat(kvar.getUnit().toString(), is("kvar"));

        Quantity<Energy> kvarh = Quantities.getQuantity(BigDecimal.TEN, MetricPrefix.KILO(SmartHomeUnits.VAR_HOUR));
        assertThat(kvarh.getUnit().toString(), is("kvarh"));
    }

    @Test
    public void testKVarHFromString() {
        assertThat(QuantityType.valueOf("2.60 kvarh").getUnit().toString(), is("kvarh"));
    }

    @Test
    public void testVoltAmpereUnitSymbol() {
        assertThat(SmartHomeUnits.VOLT_AMPERE.toString(), is("VA"));
        assertThat(SmartHomeUnits.VOLT_AMPERE.getSymbol(), is("VA"));
        assertThat(SmartHomeUnits.VOLT_AMPERE_HOUR.toString(), is("VAh"));

        Quantity<Power> kVA = Quantities.getQuantity(BigDecimal.TEN, MetricPrefix.KILO(SmartHomeUnits.VOLT_AMPERE));
        assertThat(kVA.getUnit().toString(), is("kVA"));

        Quantity<Energy> kVAh = Quantities.getQuantity(BigDecimal.TEN,
                MetricPrefix.KILO(SmartHomeUnits.VOLT_AMPERE_HOUR));
        assertThat(kVAh.getUnit().toString(), is("kVAh"));
    }

    @Test
    public void testVoltAmpereHFromString() {
        assertThat(QuantityType.valueOf("2.60 VAh").getUnit().toString(), is("VAh"));
    }

    @Test
    public void testCm2In() {
        Quantity<Length> cm = Quantities.getQuantity(BigDecimal.TEN, MetricPrefix.CENTI(SIUnits.METRE));

        Quantity<Length> in = cm.to(ImperialUnits.INCH);
        assertThat(in.getUnit(), is(ImperialUnits.INCH));
        assertThat(in.getValue().doubleValue(), is(closeTo(3.93700787401574803d, DEFAULT_ERROR)));
    }

    @Test
    public void testM2Ft() {
        Quantity<Length> cm = Quantities.getQuantity(new BigDecimal("30"), MetricPrefix.CENTI(SIUnits.METRE));

        Quantity<Length> foot = cm.to(ImperialUnits.FOOT);
        assertThat(foot.getUnit(), is(ImperialUnits.FOOT));
        assertThat(foot.getValue().doubleValue(), is(closeTo(0.9842519685039369d, DEFAULT_ERROR)));
    }

    @Test
    public void testM2Yd() {
        Quantity<Length> m = Quantities.getQuantity(BigDecimal.ONE, SIUnits.METRE);

        Quantity<Length> yard = m.to(ImperialUnits.YARD);
        assertThat(yard.getUnit(), is(ImperialUnits.YARD));
        assertThat(yard.getValue().doubleValue(), is(closeTo(1.0936132983377076d, DEFAULT_ERROR)));
    }

    @Test
    public void testM2Ml() {
        Quantity<Length> km = Quantities.getQuantity(BigDecimal.TEN, MetricPrefix.KILO(SIUnits.METRE));

        Quantity<Length> mile = km.to(ImperialUnits.MILE);
        assertThat(mile.getUnit(), is(ImperialUnits.MILE));
        assertThat(mile.getValue().doubleValue(), is(closeTo(6.2137119223733395d, DEFAULT_ERROR)));
    }

    @Test
    public void testFahrenheitUnitSymbol() {
        assertThat(ImperialUnits.FAHRENHEIT.getSymbol(), is("°F"));
        assertThat(ImperialUnits.FAHRENHEIT.toString(), is("°F"));
    }

    @Test
    public void testInchUnitSymbol() {
        assertThat(ImperialUnits.INCH.getSymbol(), is("in"));
        assertThat(ImperialUnits.INCH.toString(), is("in"));
    }

    @Test
    public void testMileUnitSymbol() {
        assertThat(ImperialUnits.MILE.getSymbol(), is("mi"));
        assertThat(ImperialUnits.MILE.toString(), is("mi"));
    }

    @Test
    public void testOneUnitSymbol() {
        assertThat(SmartHomeUnits.ONE.getSymbol(), is(""));

        Quantity<Dimensionless> one1 = Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.ONE);
        Quantity<Dimensionless> one2 = Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.ONE);

        assertThat(one1.add(one2).toString(), is("2 one"));
    }

    @Test
    public void testPpm() {
        QuantityType<Dimensionless> ppm = new QuantityType<>("500 ppm");
        assertEquals("0.05 %", ppm.toUnit(Units.PERCENT).toString());
    }

    @Test
    public void testDb() {
        QuantityType<Dimensionless> ratio = new QuantityType<>("100");
        assertEquals("20.0 dB", ratio.toUnit("dB").toString());
    }

    @Test
    public void testDobsonUnits() {
        // https://en.wikipedia.org/wiki/Dobson_unit
        QuantityType<ArealDensity> oneDU = new QuantityType<>("1 DU");
        QuantityType<ArealDensity> mmolpsq = oneDU.toUnit(MetricPrefix.MILLI(Units.MOLE).multiply(Units.METRE.pow(-2)));
        assertThat(mmolpsq.doubleValue(), is(closeTo(0.4462d, DEFAULT_ERROR)));
        assertThat(mmolpsq.toUnit(SmartHomeUnits.DOBSON_UNIT).doubleValue(), is(closeTo(1, DEFAULT_ERROR)));
    }

    @Test
    public void testBar2Pascal() {
        Quantity<Pressure> bar = Quantities.getQuantity(BigDecimal.valueOf(1), SmartHomeUnits.BAR);
        assertThat(bar.to(SIUnits.PASCAL), is(Quantities.getQuantity(100000, SIUnits.PASCAL)));
    }

    @Test
    public void testMicrogramPerCubicMeter2KilogramPerCubicMeter() {
        Quantity<Density> oneKgM3 = Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.KILOGRAM_PER_CUBICMETRE);
        Quantity<Density> converted = oneKgM3.to(SmartHomeUnits.MICROGRAM_PER_CUBICMETRE);
        assertThat(converted.getValue().doubleValue(), is(closeTo(1000000000, DEFAULT_ERROR)));
    }

    @Test
    public void testMicrogramPerCubicMeterUnitSymbol() {
        assertThat(SmartHomeUnits.MICROGRAM_PER_CUBICMETRE.toString(), is("µg/m³"));
    }

    @Test
    public void testMicrogramPerCubicMeterFromString() {
        assertThat(QuantityType.valueOf("2.60 µg/m³").getUnit().toString(), is("µg/m³"));
    }

    @Test
    public void testMicrowattPerSquareCentimetre2KilogramPerSquareCentiMetre() {
        Quantity<Intensity> oneMwCm2 = Quantities.getQuantity(BigDecimal.ONE, SmartHomeUnits.IRRADIANCE);
        Quantity<Intensity> converted = oneMwCm2.to(SmartHomeUnits.MICROWATT_PER_SQUARE_CENTIMETRE);
        assertThat(converted.getValue().doubleValue(), is(100d));
    }

    @Test
    public void testMicrowattPerSquareCentimetreUnitSymbol() {
        assertThat(SmartHomeUnits.MICROWATT_PER_SQUARE_CENTIMETRE.toString(), is("µW/cm²"));
    }

    @Test
    public void testMicrowattPerSquareCentimetreFromString() {
        assertThat(QuantityType.valueOf("2.60 µW/cm²").getUnit().toString(), is("µW/cm²"));
    }

    @Test
    public void testElectricCharge() {
        QuantityType<?> oneAh = QuantityType.valueOf("3600 C");
        QuantityType<?> converted = oneAh.toUnit(SmartHomeUnits.AMPERE_HOUR);
        QuantityType<?> converted2 = oneAh.toUnit(SmartHomeUnits.MILLIAMPERE_HOUR);
        assertThat(converted.doubleValue(), is(closeTo(1.00, DEFAULT_ERROR)));
        assertEquals("1000 mAh", converted2.toString());
    }

    @Test
    public void testConductivity() {
        QuantityType<?> oneSM = QuantityType.valueOf("1 S/m");
        QuantityType<?> converted = oneSM.toUnit("µS/cm");
        assertEquals("10000 µS/cm", converted.toString());
    }

}
