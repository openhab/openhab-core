/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.library.unit;

import java.math.BigInteger;

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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.dimension.ArealDensity;
import org.eclipse.smarthome.core.library.dimension.Density;
import org.eclipse.smarthome.core.library.dimension.Intensity;
import org.eclipse.smarthome.core.library.dimension.VolumetricFlowRate;

import tec.uom.se.AbstractSystemOfUnits;
import tec.uom.se.AbstractUnit;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.function.LogConverter;
import tec.uom.se.function.PiMultiplierConverter;
import tec.uom.se.function.RationalConverter;
import tec.uom.se.unit.AlternateUnit;
import tec.uom.se.unit.ProductUnit;
import tec.uom.se.unit.TransformedUnit;
import tec.uom.se.unit.Units;

/**
 * Delegate common units to {@link Units} to hide this dependency from the rest of ESH.
 * See members of {@link Units} for a detailed description.
 * Also: Define common additional units used in {@link SIUnits} and {@link ImperialUnits}.
 *
 * @author Henning Treu - initial contribution
 *
 */
@NonNullByDefault
public class SmartHomeUnits extends AbstractSystemOfUnits {

    private static final SmartHomeUnits INSTANCE = new SmartHomeUnits();

    protected SmartHomeUnits() {
        // avoid external instantiation
    }

    @Override
    public String getName() {
        return SmartHomeUnits.class.getSimpleName();
    }

    public static final Unit<Angle> DEGREE_ANGLE = addUnit(new TransformedUnit<>(Units.RADIAN,
            new PiMultiplierConverter().concatenate(new RationalConverter(1, 180))));

    /**
     * See https://en.wikipedia.org/wiki/Irradiance
     */
    public static final Unit<Intensity> IRRADIANCE = addUnit(
            new AlternateUnit<Intensity>(Units.WATT.divide(Units.SQUARE_METRE), "W/m2"));
    public static final Unit<Intensity> MICROWATT_PER_SQUARE_CENTIMETRE = addUnit(
            new TransformedUnit<>(IRRADIANCE, new RationalConverter(BigInteger.valueOf(100), BigInteger.valueOf(1))));

    public static final Unit<Dimensionless> ONE = addUnit(AbstractUnit.ONE);

    public static final Unit<ElectricCurrent> AMPERE = addUnit(Units.AMPERE);
    public static final Unit<LuminousIntensity> CANDELA = addUnit(Units.CANDELA);
    public static final Unit<Temperature> KELVIN = addUnit(Units.KELVIN);
    public static final Unit<AmountOfSubstance> MOLE = addUnit(Units.MOLE);
    public static final Unit<Time> SECOND = addUnit(Units.SECOND);
    public static final Unit<Angle> RADIAN = addUnit(Units.RADIAN);
    public static final Unit<SolidAngle> STERADIAN = addUnit(Units.STERADIAN);
    public static final Unit<Frequency> HERTZ = addUnit(Units.HERTZ);
    public static final Unit<Force> NEWTON = addUnit(Units.NEWTON);
    public static final Unit<Energy> JOULE = addUnit(Units.JOULE);
    public static final Unit<Power> WATT = addUnit(Units.WATT);
    public static final Unit<ElectricCharge> COULOMB = addUnit(Units.COULOMB);
    public static final Unit<ElectricPotential> VOLT = addUnit(Units.VOLT);
    public static final Unit<ElectricCapacitance> FARAD = addUnit(Units.FARAD);
    public static final Unit<ElectricResistance> OHM = addUnit(Units.OHM);
    public static final Unit<ElectricConductance> SIEMENS = addUnit(Units.SIEMENS);
    public static final Unit<MagneticFlux> WEBER = addUnit(Units.WEBER);
    public static final Unit<MagneticFluxDensity> TESLA = addUnit(Units.TESLA);
    public static final Unit<ElectricInductance> HENRY = addUnit(Units.HENRY);
    public static final Unit<LuminousFlux> LUMEN = addUnit(Units.LUMEN);
    public static final Unit<Illuminance> LUX = addUnit(Units.LUX);
    public static final Unit<Radioactivity> BECQUEREL = addUnit(Units.BECQUEREL);
    public static final Unit<RadiationDoseAbsorbed> GRAY = addUnit(Units.GRAY);
    public static final Unit<RadiationDoseEffective> SIEVERT = addUnit(Units.SIEVERT);
    public static final Unit<CatalyticActivity> KATAL = addUnit(Units.KATAL);
    public static final Unit<Speed> METRE_PER_SECOND = addUnit(Units.METRE_PER_SECOND);
    public static final Unit<Speed> KNOT = addUnit(new TransformedUnit<>("kn", Units.KILOMETRE_PER_HOUR,
            new RationalConverter(BigInteger.valueOf(1852), BigInteger.valueOf(1000))));
    public static final Unit<Acceleration> METRE_PER_SQUARE_SECOND = addUnit(Units.METRE_PER_SQUARE_SECOND);
    public static final Unit<Dimensionless> PERCENT = addUnit(Units.PERCENT);
    public static final Unit<Dimensionless> PARTS_PER_MILLION = addUnit(
            new TransformedUnit<>(ONE, new RationalConverter(1, 1000000)));
    public static final Unit<Dimensionless> DECIBEL = addUnit(
            ONE.transform(new LogConverter(10).inverse().concatenate(RationalConverter.of(1d, 10d))));
    public static final Unit<Time> MINUTE = addUnit(Units.MINUTE);
    public static final Unit<Time> HOUR = addUnit(Units.HOUR);
    public static final Unit<Time> DAY = addUnit(Units.DAY);
    public static final Unit<Time> WEEK = addUnit(Units.WEEK);
    public static final Unit<Time> YEAR = addUnit(Units.YEAR);
    public static final Unit<Volume> LITRE = addUnit(Units.LITRE);
    public static final Unit<Density> KILOGRAM_PER_CUBICMETRE = addUnit(
            new ProductUnit<Density>(Units.KILOGRAM.divide(Units.METRE.pow(3))));
    public static final Unit<Density> MICROGRAM_PER_CUBICMETRE = addUnit(new TransformedUnit<>(KILOGRAM_PER_CUBICMETRE,
            new RationalConverter(BigInteger.valueOf(1), BigInteger.valueOf(1000000000))));
    public static final Unit<Energy> WATT_SECOND = addUnit(new ProductUnit<Energy>(Units.WATT.multiply(Units.SECOND)));
    public static final Unit<Energy> WATT_HOUR = addUnit(new ProductUnit<Energy>(Units.WATT.multiply(Units.HOUR)));
    public static final Unit<Energy> KILOWATT_HOUR = addUnit(MetricPrefix.KILO(WATT_HOUR));
    public static final Unit<Pressure> MILLIMETRE_OF_MERCURY = addUnit(new TransformedUnit<>("mmHg", Units.PASCAL,
            new RationalConverter(BigInteger.valueOf(133322368), BigInteger.valueOf(1000000))));
    public static final Unit<Pressure> BAR = addUnit(new TransformedUnit<>("bar", Units.PASCAL,
            new RationalConverter(BigInteger.valueOf(100000), BigInteger.valueOf(1))));
    public static final Unit<Pressure> MILLIBAR = addUnit(MetricPrefix.MILLI(BAR));
    public static final Unit<ArealDensity> DOBSON_UNIT = addUnit(
            new ProductUnit<ArealDensity>(MetricPrefix.MILLI(Units.MOLE).multiply(0.4462).divide(Units.METRE.pow(2))));
    public static final Unit<VolumetricFlowRate> LITRE_PER_MINUTE = addUnit(
            new ProductUnit<VolumetricFlowRate>(Units.LITRE.divide(Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_SECOND = addUnit(
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.SECOND)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_MINUTE = addUnit(
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.MINUTE)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_HOUR = addUnit(
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.HOUR)));
    public static final Unit<VolumetricFlowRate> CUBICMETRE_PER_DAY = addUnit(
            new ProductUnit<VolumetricFlowRate>(Units.CUBIC_METRE.divide(Units.DAY)));
    public static final Unit<AmountOfSubstance> DEUTSCHE_HAERTE = addUnit(new TransformedUnit<AmountOfSubstance>("°dH",
            (Unit<AmountOfSubstance>) MetricPrefix.MILLI(Units.MOLE).divide(Units.LITRE),
            RationalConverter.of(5.6, 1)));
    /**
     * Add unit symbols for custom ESH units.
     */
    static {
        SimpleUnitFormat.getInstance().label(PARTS_PER_MILLION, "ppm");
        SimpleUnitFormat.getInstance().label(DECIBEL, "dB");
        SimpleUnitFormat.getInstance().label(IRRADIANCE, "W/m²");
        SimpleUnitFormat.getInstance().label(MICROWATT_PER_SQUARE_CENTIMETRE, "µW/cm²");
        SimpleUnitFormat.getInstance().label(DEGREE_ANGLE, "°");
        SimpleUnitFormat.getInstance().label(MICROGRAM_PER_CUBICMETRE, "µg/m³");
        SimpleUnitFormat.getInstance().label(WATT_SECOND, "Ws");
        SimpleUnitFormat.getInstance().label(WATT_HOUR, "Wh");
        SimpleUnitFormat.getInstance().label(KILOWATT_HOUR, "kWh");
        SimpleUnitFormat.getInstance().label(MILLIMETRE_OF_MERCURY, MILLIMETRE_OF_MERCURY.getSymbol());
        SimpleUnitFormat.getInstance().label(BAR, BAR.getSymbol());
        SimpleUnitFormat.getInstance().label(MILLIBAR, "mbar");
        SimpleUnitFormat.getInstance().label(KNOT, KNOT.getSymbol());
        SimpleUnitFormat.getInstance().label(DOBSON_UNIT, "DU");
        SimpleUnitFormat.getInstance().label(LITRE_PER_MINUTE, "l/min");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_SECOND, "m³/s");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_MINUTE, "m³/min");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_HOUR, "m³/h");
        SimpleUnitFormat.getInstance().label(CUBICMETRE_PER_DAY, "m³/d");
        SimpleUnitFormat.getInstance().label(DEUTSCHE_HAERTE, "°dH");
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
}
