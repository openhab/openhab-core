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

import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;

import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.function.AddConverter;
import tec.uom.se.function.MultiplyConverter;
import tec.uom.se.function.RationalConverter;
import tec.uom.se.unit.ProductUnit;
import tec.uom.se.unit.TransformedUnit;
import tec.uom.se.unit.Units;

/**
 * Imperial units used for the United States and Liberia.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public final class ImperialUnits extends CustomUnits {

    private static final ImperialUnits INSTANCE = new ImperialUnits();

    /** Additionally defined units to be used in openHAB **/
    public static final Unit<Pressure> INCH_OF_MERCURY = addUnit(
            new TransformedUnit<>("inHg", Units.PASCAL, new RationalConverter(3386388, 1000)));

    public static final Unit<Temperature> FAHRENHEIT = addUnit(new TransformedUnit<>("°F", Units.KELVIN,
            new RationalConverter(BigInteger.valueOf(5), BigInteger.valueOf(9)).concatenate(new AddConverter(459.67))));

    public static final Unit<Speed> MILES_PER_HOUR = addUnit(
            new TransformedUnit<>("mph", Units.KILOMETRE_PER_HOUR, new RationalConverter(1609344, 1000000)));

    /** Length **/
    public static final Unit<Length> INCH = addUnit(
            new TransformedUnit<>("in", Units.METRE, new RationalConverter(254, 10000)));

    public static final Unit<Length> FOOT = addUnit(new TransformedUnit<>("ft", INCH, new MultiplyConverter(12.0)));

    public static final Unit<Length> YARD = addUnit(new TransformedUnit<>("yd", FOOT, new MultiplyConverter(3.0)));

    public static final Unit<Length> CHAIN = addUnit(new TransformedUnit<>("ch", YARD, new MultiplyConverter(22.0)));

    public static final Unit<Length> FURLONG = addUnit(
            new TransformedUnit<>("fur", CHAIN, new MultiplyConverter(10.0)));

    public static final Unit<Length> MILE = addUnit(new TransformedUnit<>("mi", FURLONG, new MultiplyConverter(8.0)));

    public static final Unit<Length> LEAGUE = addUnit(new TransformedUnit<>("lea", MILE, new MultiplyConverter(3.0)));

    public static final Unit<Length> SQUARE_FOOT = addUnit(new ProductUnit<>(FOOT.multiply(FOOT)));
    public static final Unit<Length> CUBIC_FOOT = addUnit(new ProductUnit<>(SQUARE_FOOT.multiply(FOOT)));

    /**
     * A unit of length equal to <code>1852.0 m</code> (standard name <code>nmi</code>).
     */
    public static final Unit<Length> NAUTICAL_MILE = addUnit(
            new TransformedUnit<>("nmi", Units.METRE, new MultiplyConverter(1852.0)));

    //////////
    // Mass //
    //////////

    /**
     * A unit of mass equal to <code>453.59237 grams</code> (avoirdupois pound, standard name <code>lb</code>).
     */
    public static final Unit<Mass> POUND = addUnit(
            new TransformedUnit<>("lb", Units.KILOGRAM, new RationalConverter(45359237, 100000000)));

    /**
     * A unit of mass equal to <code>1 / 16 {@link #POUND}</code> (standard name <code>oz</code>).
     */
    public static final Unit<Mass> OUNCE = addUnit(new TransformedUnit<>("oz", POUND, new RationalConverter(1, 16)));

    /**
     * A unit of mass equal to <code>2000 {@link #POUND}</code> (short ton, standard name <code>ton</code>).
     */
    public static final Unit<Mass> TON = addUnit(new TransformedUnit<>("ton_us", POUND, new MultiplyConverter(2000.0)));

    //////////////
    // Pressure //
    //////////////
    public static final Unit<Pressure> POUND_FORCE_PER_SQUARE_INCH = addUnit(new TransformedUnit<Pressure>("psi",
            MetricPrefix.KILO(Units.PASCAL), new RationalConverter(6894757, 1000000)));
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
        SimpleUnitFormat.getInstance().label(NAUTICAL_MILE, NAUTICAL_MILE.getSymbol());
        SimpleUnitFormat.getInstance().label(LEAGUE, LEAGUE.getSymbol());
        SimpleUnitFormat.getInstance().label(POUND, POUND.getSymbol());
        SimpleUnitFormat.getInstance().label(OUNCE, OUNCE.getSymbol());
        SimpleUnitFormat.getInstance().label(TON, TON.getSymbol());
        SimpleUnitFormat.getInstance().label(POUND_FORCE_PER_SQUARE_INCH, POUND_FORCE_PER_SQUARE_INCH.getSymbol());
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
}
