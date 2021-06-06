/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import javax.measure.Unit;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.dimension.VolumetricFlowRate;

import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.function.AddConverter;
import tech.units.indriya.function.MultiplyConverter;
import tech.units.indriya.unit.ProductUnit;
import tech.units.indriya.unit.TransformedUnit;
import tech.units.indriya.unit.Units;

/**
 * Imperial units used for the United States and Liberia.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public final class ImperialUnits extends CustomUnits {

    public static final String MEASUREMENT_SYSTEM_NAME = "US";

    private static final ImperialUnits INSTANCE = new ImperialUnits();

    /** Additionally defined units to be used in openHAB **/
    public static final Unit<Pressure> INCH_OF_MERCURY = addUnit(new TransformedUnit<>("inHg", Units.PASCAL,
            MultiplyConverter.ofRational(BigInteger.valueOf(3386388), BigInteger.valueOf(1000))));

    public static final Unit<Temperature> FAHRENHEIT = addUnit(
            new TransformedUnit<>("°F", Units.KELVIN, MultiplyConverter
                    .ofRational(BigInteger.valueOf(5), BigInteger.valueOf(9)).concatenate(new AddConverter(459.67))));

    public static final Unit<Speed> MILES_PER_HOUR = addUnit(new TransformedUnit<>("mph", Units.KILOMETRE_PER_HOUR,
            MultiplyConverter.ofRational(BigInteger.valueOf(1609344), BigInteger.valueOf(1000000))));

    /** Length **/
    public static final Unit<Length> INCH = addUnit(new TransformedUnit<>("in", Units.METRE,
            MultiplyConverter.ofRational(BigInteger.valueOf(254), BigInteger.valueOf(10000))));

    public static final Unit<Length> FOOT = addUnit(new TransformedUnit<>("ft", INCH, MultiplyConverter.of(12.0)));

    public static final Unit<Length> YARD = addUnit(new TransformedUnit<>("yd", FOOT, MultiplyConverter.of(3.0)));

    public static final Unit<Length> CHAIN = addUnit(new TransformedUnit<>("ch", YARD, MultiplyConverter.of(22.0)));

    public static final Unit<Length> FURLONG = addUnit(new TransformedUnit<>("fur", CHAIN, MultiplyConverter.of(10.0)));

    public static final Unit<Length> MILE = addUnit(new TransformedUnit<>("mi", FURLONG, MultiplyConverter.of(8.0)));

    public static final Unit<Length> LEAGUE = addUnit(new TransformedUnit<>("lea", MILE, MultiplyConverter.of(3.0)));

    /** Area **/
    public static final Unit<Area> SQUARE_INCH = addUnit(new ProductUnit<>(INCH.multiply(INCH)));

    public static final Unit<Area> SQUARE_FOOT = addUnit(new ProductUnit<>(FOOT.multiply(FOOT)));

    /** Volume **/
    public static final Unit<Volume> CUBIC_INCH = addUnit(new ProductUnit<>(SQUARE_INCH.multiply(INCH)));

    public static final Unit<Volume> CUBIC_FOOT = addUnit(new ProductUnit<>(SQUARE_FOOT.multiply(FOOT)));

    public static final Unit<Volume> GALLON_LIQUID_US = addUnit(
            new TransformedUnit<>("gal", CUBIC_INCH, MultiplyConverter.of(231.0)));

    public static final Unit<VolumetricFlowRate> GALLON_PER_MINUTE = addUnit(
            new ProductUnit<VolumetricFlowRate>(GALLON_LIQUID_US.divide(tech.units.indriya.unit.Units.MINUTE)));

    /**
     * Add unit symbols for imperial units.
     */
    static {
        SimpleUnitFormat.getInstance().label(INCH_OF_MERCURY, INCH_OF_MERCURY.getSymbol());
        SimpleUnitFormat.getInstance().label(FAHRENHEIT, FAHRENHEIT.getSymbol());
        SimpleUnitFormat.getInstance().label(MILES_PER_HOUR, MILES_PER_HOUR.getSymbol());
        SimpleUnitFormat.getInstance().label(INCH, INCH.getSymbol());
        SimpleUnitFormat.getInstance().label(FOOT, FOOT.getSymbol());
        SimpleUnitFormat.getInstance().label(YARD, YARD.getSymbol());
        SimpleUnitFormat.getInstance().label(CHAIN, CHAIN.getSymbol());
        SimpleUnitFormat.getInstance().label(FURLONG, FURLONG.getSymbol());
        SimpleUnitFormat.getInstance().label(MILE, MILE.getSymbol());
        SimpleUnitFormat.getInstance().label(LEAGUE, LEAGUE.getSymbol());
        SimpleUnitFormat.getInstance().label(GALLON_LIQUID_US, GALLON_LIQUID_US.getSymbol());
        SimpleUnitFormat.getInstance().label(GALLON_PER_MINUTE, "gal/min");
    }

    private ImperialUnits() {
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

    @Override
    public String getName() {
        return MEASUREMENT_SYSTEM_NAME;
    }
}
