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
package org.openhab.core.library.unit;

import java.math.BigInteger;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Acceleration;
import javax.measure.quantity.AmountOfSubstance;
import javax.measure.quantity.Angle;
import javax.measure.quantity.CatalyticActivity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.ElectricCapacitance;
import javax.measure.quantity.ElectricCharge;
import javax.measure.quantity.ElectricConductance;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricInductance;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.ElectricResistance;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Force;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Illuminance;
import javax.measure.quantity.LuminousFlux;
import javax.measure.quantity.LuminousIntensity;
import javax.measure.quantity.MagneticFlux;
import javax.measure.quantity.MagneticFluxDensity;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.RadiationDoseAbsorbed;
import javax.measure.quantity.RadiationDoseEffective;
import javax.measure.quantity.Radioactivity;
import javax.measure.quantity.SolidAngle;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import javax.measure.quantity.Volume;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.dimension.ArealDensity;
import org.openhab.core.library.dimension.DataAmount;
import org.openhab.core.library.dimension.DataTransferRate;
import org.openhab.core.library.dimension.Density;
import org.openhab.core.library.dimension.ElectricConductivity;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.dimension.VolumetricFlowRate;

import si.uom.NonSI;
import tech.units.indriya.AbstractUnit;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.function.ExpConverter;
import tech.units.indriya.function.LogConverter;
import tech.units.indriya.function.MultiplyConverter;
import tech.units.indriya.unit.AlternateUnit;
import tech.units.indriya.unit.BaseUnit;
import tech.units.indriya.unit.ProductUnit;
import tech.units.indriya.unit.TransformedUnit;
import tech.units.indriya.unit.UnitDimension;

/**
 * Delegate common units to {@link Units} to hide this dependency from the rest of openHAB.
 * See members of {@link Units} for a detailed description.
 * Also: Define common additional units used in {@link SIUnits} and {@link ImperialUnits}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public final class Units extends CustomUnits {

    private static final Units INSTANCE = new Units();

    // Alphabetical ordered by Unit.
    public static final Unit<Acceleration> METRE_PER_SQUARE_SECOND = addUnit(
            tech.units.indriya.unit.Units.METRE_PER_SQUARE_SECOND);
    public static final Unit<Acceleration> STANDARD_GRAVITY = addUnit(METRE_PER_SQUARE_SECOND.multiply(9.80665));
    public static final Unit<AmountOfSubstance> MOLE = addUnit(tech.units.indriya.unit.Units.MOLE);
    public static final Unit<Volume> LITRE = addUnit(tech.units.indriya.unit.Units.LITRE);
    @SuppressWarnings("unchecked")
    public static final Unit<AmountOfSubstance> DEUTSCHE_HAERTE = addUnit(new TransformedUnit<>("°dH",
            (Unit<AmountOfSubstance>) MetricPrefix.MILLI(Units.MOLE).divide(Units.LITRE), MultiplyConverter.of(5.6)));
    public static final Unit<Angle> DEGREE_ANGLE = addUnit(NonSI.DEGREE_ANGLE);
    public static final Unit<Angle> RADIAN = addUnit(tech.units.indriya.unit.Units.RADIAN);
    public static final Unit<ArealDensity> DOBSON_UNIT = addUnit(
            new ProductUnit<ArealDensity>(MetricPrefix.MILLI(tech.units.indriya.unit.Units.MOLE).multiply(0.4462)
                    .divide(tech.units.indriya.unit.Units.SQUARE_METRE)));
    public static final Unit<CatalyticActivity> KATAL = addUnit(tech.units.indriya.unit.Units.KATAL);
    public static final Unit<Density> KILOGRAM_PER_CUBICMETRE = addUnit(new ProductUnit<Density>(
            tech.units.indriya.unit.Units.KILOGRAM.divide(tech.units.indriya.unit.Units.CUBIC_METRE)));
    public static final Unit<Density> MICROGRAM_PER_CUBICMETRE = addUnit(new TransformedUnit<>(KILOGRAM_PER_CUBICMETRE,
            MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(1000000000))));
    public static final Unit<Dimensionless> ONE = addUnit(AbstractUnit.ONE);
    public static final Unit<Dimensionless> PERCENT = addUnit(tech.units.indriya.unit.Units.PERCENT);
    public static final Unit<Dimensionless> PARTS_PER_BILLION = addUnit(
            new TransformedUnit<>(ONE, MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(1000000000))));
    public static final Unit<Dimensionless> PARTS_PER_MILLION = addUnit(
            new TransformedUnit<>(ONE, MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(1000000))));
    public static final Unit<Dimensionless> DECIBEL = addUnit(ONE.transform(
            new LogConverter(10).inverse().concatenate(MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.TEN))));
    public static final Unit<ElectricCurrent> AMPERE = addUnit(tech.units.indriya.unit.Units.AMPERE);
    public static final Unit<ElectricCapacitance> FARAD = addUnit(tech.units.indriya.unit.Units.FARAD);
    public static final Unit<ElectricCharge> COULOMB = addUnit(tech.units.indriya.unit.Units.COULOMB);
    public static final Unit<ElectricCharge> AMPERE_HOUR = addUnit(
            tech.units.indriya.unit.Units.COULOMB.multiply(3600));
    public static final Unit<ElectricCharge> MILLIAMPERE_HOUR = addUnit(MetricPrefix.MILLI(AMPERE_HOUR));
    public static final Unit<ElectricConductance> SIEMENS = addUnit(tech.units.indriya.unit.Units.SIEMENS);
    public static final Unit<ElectricConductivity> SIEMENS_PER_METRE = addUnit(new ProductUnit<ElectricConductivity>(
            tech.units.indriya.unit.Units.SIEMENS.divide(tech.units.indriya.unit.Units.METRE)));
    public static final Unit<ElectricInductance> HENRY = addUnit(tech.units.indriya.unit.Units.HENRY);
    public static final Unit<ElectricPotential> VOLT = addUnit(tech.units.indriya.unit.Units.VOLT);
    public static final Unit<ElectricResistance> OHM = addUnit(tech.units.indriya.unit.Units.OHM);
    public static final Unit<Energy> JOULE = addUnit(tech.units.indriya.unit.Units.JOULE);
    public static final Unit<Energy> WATT_SECOND = addUnit(
            new ProductUnit<>(tech.units.indriya.unit.Units.WATT.multiply(tech.units.indriya.unit.Units.SECOND)));
    public static final Unit<Energy> WATT_HOUR = addUnit(
            new ProductUnit<>(tech.units.indriya.unit.Units.WATT.multiply(tech.units.indriya.unit.Units.HOUR)));
    public static final Unit<Energy> KILOWATT_HOUR = addUnit(MetricPrefix.KILO(WATT_HOUR));
    public static final Unit<Energy> MEGAWATT_HOUR = addUnit(MetricPrefix.MEGA(WATT_HOUR));
    public static final Unit<Power> VAR = addUnit(new AlternateUnit<>(tech.units.indriya.unit.Units.WATT, "var"));
    public static final Unit<Power> KILOVAR = addUnit(MetricPrefix.KILO(VAR));
    public static final Unit<Energy> VAR_HOUR = addUnit(
            new ProductUnit<>(VAR.multiply(tech.units.indriya.unit.Units.HOUR)), Energy.class);
    public static final Unit<Energy> KILOVAR_HOUR = addUnit(MetricPrefix.KILO(VAR_HOUR));
    public static final Unit<Power> VOLT_AMPERE = addUnit(
            new AlternateUnit<>(tech.units.indriya.unit.Units.WATT, "VA"));
    public static final Unit<Power> KILOVOLT_AMPERE = addUnit(MetricPrefix.KILO(VOLT_AMPERE));
    public static final Unit<Energy> VOLT_AMPERE_HOUR = addUnit(
            new ProductUnit<>(VOLT_AMPERE.multiply(tech.units.indriya.unit.Units.HOUR)), Energy.class);
    public static final Unit<Force> NEWTON = addUnit(tech.units.indriya.unit.Units.NEWTON);
    public static final Unit<Frequency> HERTZ = addUnit(tech.units.indriya.unit.Units.HERTZ);
    public static final Unit<Intensity> IRRADIANCE = addUnit(
            new ProductUnit<>(tech.units.indriya.unit.Units.WATT.divide(tech.units.indriya.unit.Units.SQUARE_METRE)));
    public static final Unit<Intensity> MICROWATT_PER_SQUARE_CENTIMETRE = addUnit(
            new TransformedUnit<>(IRRADIANCE, MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(100))));
    public static final Unit<Illuminance> LUX = addUnit(tech.units.indriya.unit.Units.LUX);
    public static final Unit<LuminousFlux> LUMEN = addUnit(tech.units.indriya.unit.Units.LUMEN);
    public static final Unit<LuminousIntensity> CANDELA = addUnit(tech.units.indriya.unit.Units.CANDELA);
    public static final Unit<MagneticFlux> WEBER = addUnit(tech.units.indriya.unit.Units.WEBER);
    public static final Unit<MagneticFluxDensity> TESLA = addUnit(tech.units.indriya.unit.Units.TESLA);
    public static final Unit<Power> WATT = addUnit(tech.units.indriya.unit.Units.WATT);
    public static final Unit<Power> DECIBEL_MILLIWATTS = new TransformedUnit<>("dBm", MetricPrefix.MILLI(WATT),
            new ExpConverter(10.0).concatenate(MultiplyConverter.of(0.1)));
    public static final Unit<Pressure> MILLIMETRE_OF_MERCURY = addUnit(
            new TransformedUnit<>("mmHg", tech.units.indriya.unit.Units.PASCAL,
                    MultiplyConverter.ofRational(BigInteger.valueOf(133322368), BigInteger.valueOf(1000000))));
    public static final Unit<Pressure> BAR = addUnit(new TransformedUnit<>("bar", tech.units.indriya.unit.Units.PASCAL,
            MultiplyConverter.ofRational(BigInteger.valueOf(100000), BigInteger.ONE)));
    public static final Unit<Pressure> MILLIBAR = addUnit(MetricPrefix.MILLI(BAR));
    public static final Unit<Radioactivity> BECQUEREL = addUnit(tech.units.indriya.unit.Units.BECQUEREL);
    public static final Unit<Density> BECQUEREL_PER_CUBIC_METRE = addUnit(new ProductUnit<>(
            tech.units.indriya.unit.Units.BECQUEREL.divide(tech.units.indriya.unit.Units.CUBIC_METRE)));
    public static final Unit<RadiationDoseAbsorbed> GRAY = addUnit(tech.units.indriya.unit.Units.GRAY);
    public static final Unit<RadiationDoseEffective> SIEVERT = addUnit(tech.units.indriya.unit.Units.SIEVERT);
    public static final Unit<Speed> MILLIMETRE_PER_HOUR = addUnit(
            new TransformedUnit<>("mm/h", tech.units.indriya.unit.Units.KILOMETRE_PER_HOUR,
                    MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(1000000))));
    public static final Unit<Speed> INCHES_PER_HOUR = addUnit(new TransformedUnit<>("in/h",
            ImperialUnits.MILES_PER_HOUR, MultiplyConverter.ofRational(BigInteger.ONE, BigInteger.valueOf(63360))));
    public static final Unit<Speed> METRE_PER_SECOND = addUnit(tech.units.indriya.unit.Units.METRE_PER_SECOND);
    public static final Unit<Speed> KNOT = addUnit(
            new TransformedUnit<>("kn", tech.units.indriya.unit.Units.KILOMETRE_PER_HOUR,
                    MultiplyConverter.ofRational(BigInteger.valueOf(1852), BigInteger.valueOf(1000))));
    public static final Unit<SolidAngle> STERADIAN = addUnit(tech.units.indriya.unit.Units.STERADIAN);
    public static final Unit<Temperature> KELVIN = addUnit(tech.units.indriya.unit.Units.KELVIN);
    public static final Unit<?> MIRED = addUnit(MetricPrefix.MEGA(tech.units.indriya.unit.Units.KELVIN).inverse());
    public static final Unit<Time> SECOND = addUnit(tech.units.indriya.unit.Units.SECOND);
    public static final Unit<Time> MINUTE = addUnit(tech.units.indriya.unit.Units.MINUTE);
    public static final Unit<Time> HOUR = addUnit(tech.units.indriya.unit.Units.HOUR);
    public static final Unit<Time> DAY = addUnit(tech.units.indriya.unit.Units.DAY);
    public static final Unit<Time> WEEK = addUnit(tech.units.indriya.unit.Units.WEEK);
    public static final Unit<Time> YEAR = addUnit(tech.units.indriya.unit.Units.YEAR);
    public static final Unit<VolumetricFlowRate> LITRE_PER_MINUTE = addUnit(new ProductUnit<VolumetricFlowRate>(
            tech.units.indriya.unit.Units.LITRE.divide(tech.units.indriya.unit.Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_SECOND = addUnit(new ProductUnit<VolumetricFlowRate>(
            tech.units.indriya.unit.Units.CUBIC_METRE.divide(tech.units.indriya.unit.Units.SECOND)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_MINUTE = addUnit(new ProductUnit<VolumetricFlowRate>(
            tech.units.indriya.unit.Units.CUBIC_METRE.divide(tech.units.indriya.unit.Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_HOUR = addUnit(new ProductUnit<VolumetricFlowRate>(
            tech.units.indriya.unit.Units.CUBIC_METRE.divide(tech.units.indriya.unit.Units.HOUR)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_DAY = addUnit(new ProductUnit<VolumetricFlowRate>(
            tech.units.indriya.unit.Units.CUBIC_METRE.divide(tech.units.indriya.unit.Units.DAY)));
    public static final Unit<DataAmount> BIT = addUnit(new BaseUnit<>("bit", UnitDimension.parse('X')));
    public static final Unit<DataAmount> KILOBIT = addUnit(MetricPrefix.KILO(BIT));
    public static final Unit<DataAmount> MEGABIT = addUnit(MetricPrefix.MEGA(BIT));
    public static final Unit<DataAmount> GIGABIT = addUnit(MetricPrefix.GIGA(BIT));
    public static final Unit<DataAmount> TERABIT = addUnit(MetricPrefix.TERA(BIT));
    public static final Unit<DataAmount> PETABIT = addUnit(MetricPrefix.PETA(BIT));
    public static final Unit<DataAmount> BYTE = addUnit(BIT.multiply(8));
    public static final Unit<DataAmount> OCTET = addUnit(BIT.multiply(8));
    public static final Unit<DataAmount> KILOBYTE = addUnit(MetricPrefix.KILO(BYTE));
    public static final Unit<DataAmount> MEGABYTE = addUnit(MetricPrefix.MEGA(BYTE));
    public static final Unit<DataAmount> GIGABYTE = addUnit(MetricPrefix.GIGA(BYTE));
    public static final Unit<DataAmount> TERABYTE = addUnit(MetricPrefix.TERA(BYTE));
    public static final Unit<DataAmount> PETABYTE = addUnit(MetricPrefix.PETA(BYTE));
    public static final Unit<DataAmount> KIBIBYTE = addUnit(BinaryPrefix.KIBI(BYTE));
    public static final Unit<DataAmount> MEBIBYTE = addUnit(BinaryPrefix.MEBI(BYTE));
    public static final Unit<DataAmount> GIBIBYTE = addUnit(BinaryPrefix.GIBI(BYTE));
    public static final Unit<DataAmount> TEBIBYTE = addUnit(BinaryPrefix.TEBI(BYTE));
    public static final Unit<DataAmount> PEBIBYTE = addUnit(BinaryPrefix.PEBI(BYTE));
    public static final Unit<DataAmount> KIBIOCTET = addUnit(BinaryPrefix.KIBI(OCTET));
    public static final Unit<DataAmount> MEBIOCTET = addUnit(BinaryPrefix.MEBI(OCTET));
    public static final Unit<DataAmount> GIBIOCTET = addUnit(BinaryPrefix.GIBI(OCTET));
    public static final Unit<DataAmount> TEBIOCTET = addUnit(BinaryPrefix.TEBI(OCTET));
    public static final Unit<DataAmount> PEBIOCTET = addUnit(BinaryPrefix.PEBI(OCTET));
    public static final Unit<DataTransferRate> BIT_PER_SECOND = addUnit(
            new ProductUnit<DataTransferRate>(BIT.divide(tech.units.indriya.unit.Units.SECOND)));
    public static final Unit<DataTransferRate> KILOBIT_PER_SECOND = addUnit(MetricPrefix.KILO(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> MEGABIT_PER_SECOND = addUnit(MetricPrefix.MEGA(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> GIGABIT_PER_SECOND = addUnit(MetricPrefix.GIGA(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> TERABIT_PER_SECOND = addUnit(MetricPrefix.TERA(BIT_PER_SECOND));

    /**
     * Add unit symbols for custom openHAB units.
     */
    static {
        // Ordered alphabetical by name
        SimpleUnitFormat.getInstance().label(AMPERE_HOUR, "Ah");
        SimpleUnitFormat.getInstance().label(BAR, BAR.getSymbol());
        SimpleUnitFormat.getInstance().label(BECQUEREL_PER_CUBIC_METRE, "Bq/m³");
        SimpleUnitFormat.getInstance().label(BIT, BIT.getSymbol());
        SimpleUnitFormat.getInstance().label(BIT_PER_SECOND, "bit/s");
        SimpleUnitFormat.getInstance().label(BYTE, "B");
        SimpleUnitFormat.getInstance().alias(BYTE, "o");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_DAY, "m³/d");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_HOUR, "m³/h");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_MINUTE, "m³/min");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_SECOND, "m³/s");
        SimpleUnitFormat.getInstance().label(DECIBEL, "dB");
        SimpleUnitFormat.getInstance().label(DECIBEL_MILLIWATTS, "dBm");
        SimpleUnitFormat.getInstance().label(DEGREE_ANGLE, "°");
        SimpleUnitFormat.getInstance().label(DEUTSCHE_HAERTE, "°dH");
        SimpleUnitFormat.getInstance().label(DOBSON_UNIT, "DU");
        SimpleUnitFormat.getInstance().label(GIGABYTE, "GB");
        SimpleUnitFormat.getInstance().label(GIBIBYTE, "GiB");
        SimpleUnitFormat.getInstance().alias(GIBIBYTE, "Gio");
        SimpleUnitFormat.getInstance().label(GIGABIT, "Gbit");
        SimpleUnitFormat.getInstance().label(GIGABIT_PER_SECOND, "Gbit/s");
        SimpleUnitFormat.getInstance().label(IRRADIANCE, "W/m²");
        SimpleUnitFormat.getInstance().label(KILOBYTE, "kB");
        SimpleUnitFormat.getInstance().label(KIBIBYTE, "kiB");
        SimpleUnitFormat.getInstance().alias(KIBIBYTE, "kio");
        SimpleUnitFormat.getInstance().label(KILOBIT, "kbit");
        SimpleUnitFormat.getInstance().label(KILOBIT_PER_SECOND, "kbit/s");
        SimpleUnitFormat.getInstance().label(KILOVAR, "kvar");
        SimpleUnitFormat.getInstance().label(KILOVAR_HOUR, "kvarh");
        SimpleUnitFormat.getInstance().label(KILOVOLT_AMPERE, "kVA");
        SimpleUnitFormat.getInstance().label(KILOWATT_HOUR, "kWh");
        SimpleUnitFormat.getInstance().label(KNOT, KNOT.getSymbol());
        SimpleUnitFormat.getInstance().label(LITRE_PER_MINUTE, "l/min");
        SimpleUnitFormat.getInstance().label(MEGABYTE, "MB");
        SimpleUnitFormat.getInstance().label(MEBIBYTE, "MiB");
        SimpleUnitFormat.getInstance().alias(MEBIBYTE, "Mio");
        SimpleUnitFormat.getInstance().label(MEGABIT, "Mbit");
        SimpleUnitFormat.getInstance().label(MEGABIT_PER_SECOND, "Mbit/s");
        SimpleUnitFormat.getInstance().label(MEGAWATT_HOUR, "MWh");
        SimpleUnitFormat.getInstance().label(MICROGRAM_PER_CUBICMETRE, "µg/m³");
        SimpleUnitFormat.getInstance().label(MICROWATT_PER_SQUARE_CENTIMETRE, "µW/cm²");
        SimpleUnitFormat.getInstance().label(MILLIAMPERE_HOUR, "mAh");
        SimpleUnitFormat.getInstance().label(MILLIBAR, "mbar");
        SimpleUnitFormat.getInstance().label(MILLIMETRE_OF_MERCURY, MILLIMETRE_OF_MERCURY.getSymbol());
        SimpleUnitFormat.getInstance().label(MIRED, "mired");
        SimpleUnitFormat.getInstance().label(PARTS_PER_BILLION, "ppb");
        SimpleUnitFormat.getInstance().label(PARTS_PER_MILLION, "ppm");
        SimpleUnitFormat.getInstance().label(PETABYTE, "PB");
        SimpleUnitFormat.getInstance().label(PEBIBYTE, "PiB");
        SimpleUnitFormat.getInstance().alias(PEBIBYTE, "Pio");
        SimpleUnitFormat.getInstance().label(PETABIT, "Pbit");
        SimpleUnitFormat.getInstance().label(STANDARD_GRAVITY, "gₙ");
        SimpleUnitFormat.getInstance().label(SIEMENS_PER_METRE, "S/m");
        SimpleUnitFormat.getInstance().label(TERABYTE, "TB");
        SimpleUnitFormat.getInstance().label(TEBIBYTE, "TiB");
        SimpleUnitFormat.getInstance().alias(TEBIBYTE, "Tio");
        SimpleUnitFormat.getInstance().label(TERABIT, "Tbit");
        SimpleUnitFormat.getInstance().label(TERABIT_PER_SECOND, "Tbit/s");
        SimpleUnitFormat.getInstance().label(VAR, "var");
        SimpleUnitFormat.getInstance().label(VAR_HOUR, "varh");
        SimpleUnitFormat.getInstance().label(VOLT_AMPERE, "VA");
        SimpleUnitFormat.getInstance().label(VOLT_AMPERE_HOUR, "VAh");
        SimpleUnitFormat.getInstance().label(WATT_HOUR, "Wh");
        SimpleUnitFormat.getInstance().label(WATT_SECOND, "Ws");
    }

    private Units() {
        // avoid external instantiation
    }

    /**
     * Returns the unique instance of this class.
     *
     * @return the Units instance.
     */
    public static SystemOfUnits getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a new unit not mapped to any specified quantity type.
     *
     * @param unit the unit being added.
     * @return <code>unit</code>.
     */
    private static <U extends Unit<?>> U addUnit(U unit) {
        INSTANCE.units.add(unit);
        return unit;
    }

    /**
     * Adds a new unit and maps it to the specified quantity type.
     *
     * @param unit the unit being added.
     * @param type the quantity type.
     * @return <code>unit</code>.
     */
    private static <U extends AbstractUnit<?>> U addUnit(U unit, Class<? extends Quantity<?>> type) {
        INSTANCE.units.add(unit);
        INSTANCE.quantityToUnit.put(type, unit);
        return unit;
    }
}
