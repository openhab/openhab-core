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

import java.math.BigInteger;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Acceleration;
import javax.measure.quantity.AmountOfSubstance;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Area;
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
import javax.measure.quantity.Length;
import javax.measure.quantity.LuminousFlux;
import javax.measure.quantity.LuminousIntensity;
import javax.measure.quantity.MagneticFlux;
import javax.measure.quantity.MagneticFluxDensity;
import javax.measure.quantity.Mass;
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
import org.openhab.core.library.dimension.AngularAcceleration;
import org.openhab.core.library.dimension.AngularVelocity;
import org.openhab.core.library.dimension.ArealDensity;
import org.openhab.core.library.dimension.DataAmount;
import org.openhab.core.library.dimension.DataTransferRate;
import org.openhab.core.library.dimension.Density;
import org.openhab.core.library.dimension.DynamicViscosity;
import org.openhab.core.library.dimension.ElectricChargeDensity;
import org.openhab.core.library.dimension.ElectricChargeSurfaceDensity;
import org.openhab.core.library.dimension.ElectricConductivity;
import org.openhab.core.library.dimension.ElectricCurrentDensity;
import org.openhab.core.library.dimension.ElectricFieldStrength;
import org.openhab.core.library.dimension.ElectricPermittivity;
import org.openhab.core.library.dimension.Irradiance;
import org.openhab.core.library.dimension.Luminance;
import org.openhab.core.library.dimension.LuminousEfficacy;
import org.openhab.core.library.dimension.LuminousEnergy;
import org.openhab.core.library.dimension.LuminousExposure;
import org.openhab.core.library.dimension.MagneticFieldStrength;
import org.openhab.core.library.dimension.MagneticPermeability;
import org.openhab.core.library.dimension.MassFlowRate;
import org.openhab.core.library.dimension.Radiance;
import org.openhab.core.library.dimension.RadiantIntensity;
import org.openhab.core.library.dimension.RadiationConcentration;
import org.openhab.core.library.dimension.RadiationDoseAbsorptionRate;
import org.openhab.core.library.dimension.RadiationExposure;
import org.openhab.core.library.dimension.VolumetricFlowRate;
import org.openhab.core.library.dimension.WaveNumber;

import tec.uom.se.AbstractUnit;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.function.ExpConverter;
import tec.uom.se.function.LogConverter;
import tec.uom.se.function.MultiplyConverter;
import tec.uom.se.function.PiMultiplierConverter;
import tec.uom.se.function.RationalConverter;
import tec.uom.se.unit.AlternateUnit;
import tec.uom.se.unit.ProductUnit;
import tec.uom.se.unit.TransformedUnit;
import tec.uom.se.unit.Units;

/**
 * Delegate common units to {@link Units} to hide this dependency from the rest of openHAB.
 * See members of {@link Units} for a detailed description.
 * Also: Define common additional units used in {@link SIUnits} and {@link ImperialUnits}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public final class SmartHomeUnits extends CustomUnits {

    private static final SmartHomeUnits INSTANCE = new SmartHomeUnits();

    // Alphabetical ordered by Unit.
    public static final Unit<Acceleration> METRE_PER_SQUARE_SECOND = addUnit(Units.METRE_PER_SQUARE_SECOND);
    public static final Unit<Acceleration> STANDARD_GRAVITY = addUnit("gₙ", METRE_PER_SQUARE_SECOND.multiply(9.80665));
    public static final Unit<Area> BARN = addUnit("b",
            new ProductUnit<Area>(MetricPrefix.FEMTO(Units.METRE).pow(2)).multiply(100));
    public static final Unit<Area> HECTARE = addUnit("ha", Units.SQUARE_METRE, 10000, 1);
    public static final Unit<AmountOfSubstance> MOLE = addUnit(Units.MOLE);
    @SuppressWarnings("unchecked")
    public static final Unit<AmountOfSubstance> DEUTSCHE_HAERTE = addUnit("°dH",
            (Unit<AmountOfSubstance>) MetricPrefix.MILLI(Units.MOLE).divide(Units.LITRE), 56, 10);
    public static final Unit<Angle> RADIAN = addUnit(Units.RADIAN);
    public static final Unit<Angle> REVOLUTION = addUnit("rev", RADIAN,
            new PiMultiplierConverter().concatenate(new RationalConverter(2, 1)));
    public static final Unit<Angle> DEGREE_ANGLE = addUnit("°", REVOLUTION, 1, 360);
    public static final Unit<Angle> MINUTE_ANGLE = addUnit("'", DEGREE_ANGLE, 1, 60);
    public static final Unit<Angle> SECOND_ANGLE = addUnit("\"", MINUTE_ANGLE, 1, 60);
    public static final Unit<AngularVelocity> RADIANS_PER_SECOND = addUnit("rad/s",
            new ProductUnit<AngularVelocity>(RADIAN.divide(Units.SECOND)));
    public static final Unit<AngularVelocity> REVOLUTIONS_PER_MINUTE = addUnit("rpm",
            new ProductUnit<AngularVelocity>(REVOLUTION.divide(Units.MINUTE)));
    public static final Unit<AngularAcceleration> RADIANS_PER_SQUARED_SECOND = addUnit("rad/s²",
            new ProductUnit<AngularAcceleration>(RADIANS_PER_SECOND.divide(Units.SECOND)));
    public static final Unit<ArealDensity> DOBSON_UNIT = addUnit("DU",
            new ProductUnit<ArealDensity>(MetricPrefix.MILLI(Units.MOLE).multiply(0.4462).divide(Units.SQUARE_METRE)));
    public static final Unit<ArealDensity> KILOGRAM_PER_SQUARE_METRE = addUnit("kg/m²",
            new ProductUnit<ArealDensity>(Units.KILOGRAM.divide(Units.SQUARE_METRE)));
    public static final Unit<CatalyticActivity> KATAL = addUnit(Units.KATAL);
    public static final Unit<Density> KILOGRAM_PER_CUBICMETRE = addUnit("kg/m³",
            new ProductUnit<Density>(Units.KILOGRAM.divide(Units.CUBIC_METRE)));
    public static final Unit<Density> MICROGRAM_PER_CUBICMETRE = addUnit("µg/m³", KILOGRAM_PER_CUBICMETRE, 1,
            1000000000);
    public static final Unit<Density> MILLIGRAM_PER_DECILITRE = addUnit("mg/dl", KILOGRAM_PER_CUBICMETRE, 100);
    public static final Unit<Dimensionless> ONE = addUnit(AbstractUnit.ONE);
    public static final Unit<Dimensionless> PERCENT = addUnit("%", Units.PERCENT);
    public static final Unit<Dimensionless> PER_MILLE = addUnit("‰", ONE, 1, 1000);
    public static final Unit<Dimensionless> PARTS_PER_BILLION = addUnit("ppb", ONE, 1, 1000000000);
    public static final Unit<Dimensionless> PARTS_PER_MILLION = addUnit("ppm", ONE, 1, 1000000);
    public static final Unit<Dimensionless> DECIBEL = addUnit("dB", ONE.transform(
            new LogConverter(10).inverse().concatenate(new RationalConverter(BigInteger.ONE, BigInteger.TEN))));
    public static final Unit<DynamicViscosity> PASCAL_SECOND = addUnit("Pa⋅s",
            new ProductUnit<DynamicViscosity>(Units.PASCAL.multiply(Units.SECOND)));
    public static final Unit<ElectricCurrent> AMPERE = addUnit(Units.AMPERE);
    public static final Unit<ElectricCapacitance> FARAD = addUnit(Units.FARAD);
    public static final Unit<ElectricCharge> COULOMB = addUnit(Units.COULOMB);
    public static final Unit<ElectricCharge> AMPERE_HOUR = addUnit("Ah", Units.COULOMB.multiply(3600));
    public static final Unit<ElectricCharge> MILLIAMPERE_HOUR = addUnit("mAh", MetricPrefix.MILLI(AMPERE_HOUR));
    public static final Unit<ElectricChargeSurfaceDensity> COULOMB_PER_SQUARE_METRE = addUnit("C/m²",
            new ProductUnit<ElectricChargeSurfaceDensity>(Units.COULOMB.divide(Units.SQUARE_METRE)));
    public static final Unit<ElectricChargeDensity> COULOMB_PER_CUBIC_METRE = addUnit("C/m³",
            new ProductUnit<ElectricChargeDensity>(Units.COULOMB.divide(Units.CUBIC_METRE)));
    public static final Unit<ElectricConductance> SIEMENS = addUnit(Units.SIEMENS);
    public static final Unit<ElectricConductivity> SIEMENS_PER_METRE = addUnit("S/m",
            new ProductUnit<ElectricConductivity>(Units.SIEMENS.divide(Units.METRE)));
    public static final Unit<ElectricCurrentDensity> AMPERE_PER_SQUARE_METRE = addUnit("A/m²",
            new ProductUnit<ElectricCurrentDensity>(AMPERE.divide(Units.SQUARE_METRE)));
    public static final Unit<ElectricInductance> HENRY = addUnit(Units.HENRY);
    public static final Unit<ElectricPermittivity> FARAD_PER_METRE = addUnit(
            new AlternateUnit<ElectricPermittivity>(FARAD.divide(Units.METRE), "ε"));
    public static final Unit<ElectricPotential> VOLT = addUnit(Units.VOLT);
    public static final Unit<ElectricResistance> OHM = addUnit(Units.OHM);
    public static final Unit<ElectricFieldStrength> VOLT_PER_METRE = addUnit("V/m",
            new ProductUnit<ElectricFieldStrength>(VOLT.divide(Units.METRE)));
    public static final Unit<Energy> JOULE = addUnit(Units.JOULE);
    public static final Unit<Energy> GRAM_CALORIE = addUnit("cal", Units.JOULE, 41858, 10000);
    public static final Unit<Energy> KILOGRAM_CALORIE = addUnit("Cal", MetricPrefix.KILO(GRAM_CALORIE));
    public static final Unit<Energy> WATT_SECOND = addUnit("Ws",
            new ProductUnit<Energy>(Units.WATT.multiply(Units.SECOND)));
    public static final Unit<Energy> WATT_HOUR = addUnit("Wh",
            new ProductUnit<Energy>(Units.WATT.multiply(Units.HOUR)));
    public static final Unit<Energy> KILOWATT_HOUR = addUnit("kWh", MetricPrefix.KILO(WATT_HOUR));
    public static final Unit<Energy> MEGAWATT_HOUR = addUnit("MWh", MetricPrefix.MEGA(WATT_HOUR));
    public static final Unit<Power> VAR = addUnit(new AlternateUnit<>(Units.WATT, "var"));
    public static final Unit<Power> KILOVAR = addUnit("kvar", MetricPrefix.KILO(VAR));
    public static final Unit<Energy> VAR_HOUR = addUnit("varh", new ProductUnit<Energy>(VAR.multiply(Units.HOUR)));
    public static final Unit<Energy> KILOVAR_HOUR = addUnit("kvarh", MetricPrefix.KILO(VAR_HOUR));
    public static final Unit<Power> VOLT_AMPERE = addUnit(new AlternateUnit<>(Units.WATT, "VA"));
    public static final Unit<Energy> VOLT_AMPERE_HOUR = addUnit("VAh",
            new ProductUnit<Energy>(VOLT_AMPERE.multiply(Units.HOUR)));
    public static final Unit<Force> NEWTON = addUnit(Units.NEWTON);
    public static final Unit<Frequency> HERTZ = addUnit(Units.HERTZ);
    public static final Unit<Irradiance> WATT_PER_SQUARE_METRE = addUnit("W/m²",
            new ProductUnit<Irradiance>(Units.WATT.divide(Units.SQUARE_METRE)));
    public static final Unit<Irradiance> MICROWATT_PER_SQUARE_CENTIMETRE = addUnit("µW/cm²", WATT_PER_SQUARE_METRE, 1,
            100);
    public static final Unit<Illuminance> LUX = addUnit(Units.LUX);
    public static final Unit<Length> PARSEC = addUnit("pc", Units.METRE.multiply(30856770e9));
    public static final Unit<Luminance> CANDELA_PER_SQUARE_METRE = addUnit("cd/m²",
            new ProductUnit<Luminance>(Units.CANDELA.divide(Units.SQUARE_METRE)));
    public static final Unit<LuminousEfficacy> LUMEN_PER_WATT = addUnit("lm/W",
            new ProductUnit<LuminousEfficacy>(Units.LUMEN.divide(Units.WATT)));
    public static final Unit<LuminousEnergy> LUMEN_HOUR = addUnit("lm·h",
            new ProductUnit<LuminousEnergy>(Units.LUMEN.multiply(Units.HOUR)));
    public static final Unit<LuminousExposure> LUX_HOUR = addUnit("lx·h",
            new ProductUnit<LuminousExposure>(Units.LUX.multiply(Units.HOUR)));
    public static final Unit<LuminousFlux> LUMEN = addUnit(Units.LUMEN);
    public static final Unit<LuminousIntensity> CANDELA = addUnit(Units.CANDELA);
    public static final Unit<Mass> TONNE = addUnit("t", Units.KILOGRAM, 1000);
    public static final Unit<MassFlowRate> GRAM_PER_SECOND = addUnit("g/s",
            new ProductUnit<MassFlowRate>(Units.GRAM.divide(Units.SECOND)));
    public static final Unit<MagneticFieldStrength> AMPERE_PER_METRE = addUnit("A/m",
            new ProductUnit<MagneticFieldStrength>(AMPERE.divide(Units.METRE)));
    public static final Unit<MagneticFlux> WEBER = addUnit(Units.WEBER);
    public static final Unit<MagneticFluxDensity> TESLA = addUnit(Units.TESLA);
    public static final Unit<MagneticPermeability> NEWTON_PER_SQUARE_AMPERE = addUnit("N/A²",
            new ProductUnit<MagneticPermeability>(NEWTON.divide(AMPERE.pow(2))));
    public static final Unit<Power> WATT = addUnit(Units.WATT);
    public static final Unit<Power> DECIBEL_MILLIWATTS = addUnit("dBm", MetricPrefix.MILLI(WATT),
            new ExpConverter(10.0).concatenate(new MultiplyConverter(0.1)));
    public static final Unit<Pressure> MILLIMETRE_OF_MERCURY = addUnit("mmHg", Units.PASCAL, 133322368, 1000000);
    public static final Unit<Pressure> BAR = addUnit("bar", Units.PASCAL, 100000);
    public static final Unit<Pressure> MILLIBAR = addUnit("mbar", MetricPrefix.MILLI(BAR));
    public static final Unit<RadiantIntensity> WATT_PER_STERADIAN = addUnit("W/sr",
            new ProductUnit<RadiantIntensity>(Units.WATT.divide(Units.STERADIAN)));
    public static final Unit<Radiance> WATT_PER_STERADIAN_PER_SQUARE_METRE = addUnit("W⋅sr−1⋅m−2",
            new ProductUnit<Radiance>(WATT_PER_STERADIAN.divide(Units.SQUARE_METRE)));
    public static final Unit<Radioactivity> BECQUEREL = addUnit(Units.BECQUEREL);
    public static final Unit<RadiationConcentration> BECQUEREL_PER_CUBIC_METRE = addUnit("Bq/m³",
            new ProductUnit<RadiationConcentration>(Units.BECQUEREL.divide(Units.CUBIC_METRE)));
    public static final Unit<RadiationDoseAbsorbed> GRAY = addUnit(Units.GRAY);
    public static final Unit<RadiationDoseAbsorptionRate> GRAY_PER_SECOND = addUnit("Gy/s",
            new ProductUnit<RadiationDoseAbsorptionRate>(Units.GRAY.divide(Units.SECOND)));
    public static final Unit<RadiationDoseEffective> SIEVERT = addUnit(Units.SIEVERT);
    public static final Unit<RadiationExposure> COULOMB_PER_KILOGRAM = addUnit("C/kg",
            new ProductUnit<RadiationExposure>(Units.COULOMB.divide(Units.KILOGRAM)));
    public static final Unit<Speed> KILOMETER_PER_MINUTE = addUnit("km/min", Units.KILOMETRE_PER_HOUR, 60);
    public static final Unit<Speed> MILLIMETRE_PER_HOUR = addUnit("mm/h", Units.KILOMETRE_PER_HOUR, 1, 1000000);
    public static final Unit<Speed> INCHES_PER_HOUR = addUnit("in/h", ImperialUnits.MILES_PER_HOUR, 1, 63360);
    public static final Unit<Speed> METRE_PER_SECOND = addUnit(Units.METRE_PER_SECOND);
    public static final Unit<Speed> KNOT = addUnit("kn", Units.KILOMETRE_PER_HOUR, 1852, 1000);
    public static final Unit<SolidAngle> STERADIAN = addUnit(Units.STERADIAN);
    public static final Unit<Temperature> KELVIN = addUnit(Units.KELVIN);
    public static final Unit<Time> SECOND = addUnit(Units.SECOND);
    public static final Unit<Time> MINUTE = addUnit(Units.MINUTE);
    public static final Unit<Time> HOUR = addUnit(Units.HOUR);
    public static final Unit<Time> DAY = addUnit(Units.DAY);
    public static final Unit<Time> WEEK = addUnit(Units.WEEK);
    public static final Unit<Time> YEAR = addUnit(Units.YEAR);
    public static final Unit<Volume> LITRE = addUnit(Units.LITRE);
    public static final Unit<VolumetricFlowRate> LITRE_PER_SECOND = addUnit("l/s",
            new ProductUnit<VolumetricFlowRate>(Units.LITRE.divide(Units.SECOND)));
    public static final Unit<VolumetricFlowRate> LITRE_PER_MINUTE = addUnit("l/min",
            new ProductUnit<VolumetricFlowRate>(Units.LITRE.divide(Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_SECOND = addUnit("m³/s",
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.SECOND)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_MINUTE = addUnit("m³/min",
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_HOUR = addUnit("m³/h",
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.HOUR)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_DAY = addUnit("m³/d",
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.DAY)));
    public static final Unit<WaveNumber> RECIPROCAL_METRE = addUnit("m⁻¹",
            new ProductUnit<WaveNumber>(Units.METRE.pow(-1)));
    public static final Unit<DataAmount> BIT = addUnit(new AlternateUnit<>(ONE, "bit"));
    public static final Unit<DataAmount> KILOBIT = addUnit("kbit", MetricPrefix.KILO(BIT));
    public static final Unit<DataAmount> MEGABIT = addUnit("Mbit", MetricPrefix.MEGA(BIT));
    public static final Unit<DataAmount> GIGABIT = addUnit("Gbit", MetricPrefix.GIGA(BIT));
    public static final Unit<DataAmount> TERABIT = addUnit("Tbit", MetricPrefix.TERA(BIT));
    public static final Unit<DataAmount> PETABIT = addUnit("Pbit", MetricPrefix.PETA(BIT));
    public static final Unit<DataAmount> OCTET = addUnit("o", BIT.multiply(8));
    public static final Unit<DataAmount> KIBIOCTET = addUnit("Kio", BinaryPrefix.KIBI(OCTET));
    public static final Unit<DataAmount> MEBIOCTET = addUnit("Mio", BinaryPrefix.MEBI(OCTET));
    public static final Unit<DataAmount> GIBIOCTET = addUnit("Gio", BinaryPrefix.GIBI(OCTET));
    public static final Unit<DataAmount> TEBIOCTET = addUnit("Tio", BinaryPrefix.TEBI(OCTET));
    public static final Unit<DataAmount> PEBIOCTET = addUnit("Pio", BinaryPrefix.PEBI(OCTET));
    public static final Unit<DataAmount> BYTE = addUnit("B", BIT.multiply(8));
    public static final Unit<DataAmount> KILOBYTE = addUnit("KB", MetricPrefix.KILO(BYTE));
    public static final Unit<DataAmount> MEGABYTE = addUnit("MB", MetricPrefix.MEGA(BYTE));
    public static final Unit<DataAmount> GIGABYTE = addUnit("GB", MetricPrefix.GIGA(BYTE));
    public static final Unit<DataAmount> TERABYTE = addUnit("TB", MetricPrefix.TERA(BYTE));
    public static final Unit<DataAmount> PETABYTE = addUnit("PB", MetricPrefix.PETA(BYTE));
    public static final Unit<DataAmount> KIBIBYTE = addUnit("KiB", BinaryPrefix.KIBI(BYTE));
    public static final Unit<DataAmount> MEBIBYTE = addUnit("MiB", BinaryPrefix.MEBI(BYTE));
    public static final Unit<DataAmount> GIBIBYTE = addUnit("GiB", BinaryPrefix.GIBI(BYTE));
    public static final Unit<DataAmount> TEBIBYTE = addUnit("TiB", BinaryPrefix.TEBI(BYTE));
    public static final Unit<DataAmount> PEBIBYTE = addUnit("PiB", BinaryPrefix.PEBI(BYTE));
    public static final Unit<DataTransferRate> BIT_PER_SECOND = addUnit(
            new ProductUnit<DataTransferRate>(BIT.divide(Units.SECOND)));
    public static final Unit<DataTransferRate> KILOBIT_PER_SECOND = addUnit("kbit/s",
            MetricPrefix.KILO(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> MEGABIT_PER_SECOND = addUnit("Mbit/s",
            MetricPrefix.MEGA(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> GIGABIT_PER_SECOND = addUnit("Gbit/s",
            MetricPrefix.GIGA(BIT_PER_SECOND));
    public static final Unit<DataTransferRate> TERABIT_PER_SECOND = addUnit("Tbit/s",
            MetricPrefix.TERA(BIT_PER_SECOND));

    /**
     * Add unit symbols for custom openHAB units.
     */
    static {
        INSTANCE.quantityToUnit.put(DynamicViscosity.class, PASCAL_SECOND);
        INSTANCE.quantityToUnit.put(DataAmount.class, BYTE);
        INSTANCE.quantityToUnit.put(Energy.class, WATT_HOUR);
        INSTANCE.quantityToUnit.put(ElectricCharge.class, AMPERE_HOUR);
        INSTANCE.quantityToUnit.put(ElectricChargeDensity.class, COULOMB_PER_CUBIC_METRE);
        INSTANCE.quantityToUnit.put(ElectricChargeSurfaceDensity.class, COULOMB_PER_SQUARE_METRE);
        INSTANCE.quantityToUnit.put(ElectricCurrentDensity.class, AMPERE_PER_SQUARE_METRE);
        INSTANCE.quantityToUnit.put(ElectricPotential.class, VOLT);
        INSTANCE.quantityToUnit.put(Luminance.class, CANDELA_PER_SQUARE_METRE);
        INSTANCE.quantityToUnit.put(LuminousEfficacy.class, LUMEN_PER_WATT);
        INSTANCE.quantityToUnit.put(LuminousEnergy.class, LUMEN_HOUR);
        INSTANCE.quantityToUnit.put(LuminousExposure.class, LUX_HOUR);
        INSTANCE.quantityToUnit.put(RadiationDoseAbsorbed.class, GRAY);
        INSTANCE.quantityToUnit.put(RadiationDoseAbsorptionRate.class, GRAY_PER_SECOND);
        INSTANCE.quantityToUnit.put(RadiantIntensity.class, WATT_PER_STERADIAN);
        INSTANCE.quantityToUnit.put(RadiationExposure.class, COULOMB_PER_KILOGRAM);
        INSTANCE.quantityToUnit.put(Radiance.class, WATT_PER_STERADIAN_PER_SQUARE_METRE);
        INSTANCE.quantityToUnit.put(VolumetricFlowRate.class, LITRE_PER_SECOND);
        INSTANCE.quantityToUnit.put(WaveNumber.class, RECIPROCAL_METRE);
    }

    private SmartHomeUnits() {
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
        if (unit instanceof AlternateUnit) {
            SimpleUnitFormat.getInstance().label(unit, ((AbstractUnit<?>) unit).getSymbol());
        }
        INSTANCE.units.add(unit);
        return unit;
    }

    /**
     * Adds a new unit not mapped to any specified quantity type.
     *
     * @param unit the unit being added.
     * @return <code>unit</code>.
     */
    private static <Q extends Quantity<Q>> Unit<Q> addUnit(String symbol, Unit<Q> unit) {
        SimpleUnitFormat.getInstance().label(unit, symbol);
        return addUnit(unit);
    }

    private static <Q extends Quantity<Q>> Unit<Q> addUnit(String symbol, Unit<Q> unit, UnitConverter converter) {
        return addUnit(symbol, new TransformedUnit<>(symbol, unit, converter));
    }

    private static <Q extends Quantity<Q>> Unit<Q> addUnit(String symbol, Unit<Q> unit, long multiplier) {
        return addUnit(symbol, unit, new RationalConverter(multiplier, 1));
    }

    private static <Q extends Quantity<Q>> Unit<Q> addUnit(String symbol, Unit<Q> unit, long dividend, long divisor) {
        return addUnit(symbol, unit, new RationalConverter(dividend, divisor));
    }
}
