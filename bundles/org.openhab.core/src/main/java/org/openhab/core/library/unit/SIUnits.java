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

import javax.measure.Unit;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;

import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.Units;

/**
 * Delegate SI units to {@link Units} to hide this dependency from the rest of openHAB.
 * See members of {@link Units} for a detailed description.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public final class SIUnits extends CustomUnits {

    public static final String MEASUREMENT_SYSTEM_NAME = "SI";

    private static final SIUnits INSTANCE = new SIUnits();

    public static final Unit<Temperature> CELSIUS = addUnit(Units.CELSIUS);
    public static final Unit<Speed> KILOMETRE_PER_HOUR = addUnit(Units.KILOMETRE_PER_HOUR);
    public static final Unit<Length> METRE = addUnit(Units.METRE);
    public static final Unit<Mass> KILOGRAM = addUnit(Units.KILOGRAM);
    public static final Unit<Mass> GRAM = addUnit(Units.GRAM);
    public static final Unit<Area> SQUARE_METRE = addUnit(Units.SQUARE_METRE);
    public static final Unit<Volume> CUBIC_METRE = addUnit(Units.CUBIC_METRE);
    public static final Unit<Pressure> PASCAL = addUnit(Units.PASCAL);

    static {
        // Override the default unit symbol ℃ to better support TTS and UIs:
        SimpleUnitFormat.getInstance().label(CELSIUS, "°C");
    }

    private SIUnits() {
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
