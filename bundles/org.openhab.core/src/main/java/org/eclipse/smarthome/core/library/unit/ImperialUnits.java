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
import javax.measure.quantity.Length;
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
 * @author Henning Treu - initial contribution
 *
 */
@NonNullByDefault
public class ImperialUnits extends SmartHomeUnits {

    private static final ImperialUnits INSTANCE = new ImperialUnits();

    private ImperialUnits() {
        // avoid external instantiation
    }

    @Override
    public String getName() {
        return ImperialUnits.class.getSimpleName();
    }

    /**
     * Returns the unique instance of this class.
     *
     * @return the Units instance.
     */
    public static SystemOfUnits getInstance() {
        return INSTANCE;
    }

    /** Additionally defined units to be used in ESH **/

    public static final Unit<Pressure> INCH_OF_MERCURY = addUnit(new TransformedUnit<>("inHg", Units.PASCAL,
            new RationalConverter(BigInteger.valueOf(3386388), BigInteger.valueOf(1000))));

    public static final Unit<Temperature> FAHRENHEIT = addUnit(new TransformedUnit<>("°F", Units.KELVIN,
            new RationalConverter(BigInteger.valueOf(5), BigInteger.valueOf(9)).concatenate(new AddConverter(459.67))));

    public static final Unit<Speed> MILES_PER_HOUR = addUnit(
            new TransformedUnit<>("mph", Units.KILOMETRE_PER_HOUR, new RationalConverter(1609344l, 1000000l)));

    /** Length **/
    public static final Unit<Length> INCH = addUnit(
            new TransformedUnit<>("in", Units.METRE, new RationalConverter(254l, 10000l)));

    public static final Unit<Length> FOOT = addUnit(
            new TransformedUnit<>("ft", ImperialUnits.INCH, new MultiplyConverter(12.0)));
    
    public static final Unit<Length> YARD = addUnit(
            new TransformedUnit<>("yd", ImperialUnits.FOOT, new MultiplyConverter(3.0)));
    
    public static final Unit<Length> CHAIN = addUnit(
            new TransformedUnit<>("ch", ImperialUnits.YARD, new MultiplyConverter(22.0)));
    
    public static final Unit<Length> FURLONG = addUnit(
            new TransformedUnit<>("fur", ImperialUnits.CHAIN, new MultiplyConverter(10.0)));

    public static final Unit<Length> MILE = addUnit(
            new TransformedUnit<>("mi", ImperialUnits.FURLONG, new MultiplyConverter(8.0)));
    
    public static final Unit<Length> LEAGUE = addUnit(
            new TransformedUnit<>("lea", ImperialUnits.MILE, new MultiplyConverter(3.0)));
    
    public static final Unit<Length> SQUARE_FOOT = addUnit(new ProductUnit<>(FOOT.multiply(FOOT)));
    public static final Unit<Length> CUBIC_FOOT = addUnit(new ProductUnit<>(SQUARE_FOOT.multiply(FOOT)));
    

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
