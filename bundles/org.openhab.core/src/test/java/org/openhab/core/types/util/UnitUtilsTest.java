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
package org.openhab.core.types.util;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.*;
import static org.openhab.core.library.unit.MetricPrefix.*;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Power;
import javax.measure.quantity.Temperature;

import org.junit.Test;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;

/**
 * @author Henning Treu - Initial contribution
 */
public class UnitUtilsTest {

    @Test
    public void forBaseUnitsOfDifferentSystemsShouldBeTrue() {
        assertTrue(UnitUtils.isDifferentMeasurementSystem(SIUnits.CELSIUS, ImperialUnits.FAHRENHEIT));
        assertTrue(UnitUtils.isDifferentMeasurementSystem(ImperialUnits.MILES_PER_HOUR, SIUnits.KILOMETRE_PER_HOUR));
    }

    @Test
    public void forBaseUnitsOfSameSystemShouldBeFalse() {
        assertFalse(UnitUtils.isDifferentMeasurementSystem(CENTI(SIUnits.METRE), SIUnits.METRE));
        assertFalse(UnitUtils.isDifferentMeasurementSystem(SIUnits.METRE, MILLI(SIUnits.METRE)));
        assertFalse(UnitUtils.isDifferentMeasurementSystem(CENTI(SIUnits.METRE), MILLI(SIUnits.METRE)));
        assertFalse(UnitUtils.isDifferentMeasurementSystem(ImperialUnits.MILE, ImperialUnits.INCH));
        assertFalse(UnitUtils.isDifferentMeasurementSystem(HECTO(SIUnits.PASCAL), SIUnits.PASCAL));
    }

    @Test
    public void forDerivedUnitsOfDifferentSystemsShouldBeTrue() {
        assertTrue(UnitUtils.isDifferentMeasurementSystem(CENTI(SIUnits.METRE), ImperialUnits.INCH));
        assertTrue(UnitUtils.isDifferentMeasurementSystem(ImperialUnits.MILE, KILO(SIUnits.METRE)));
    }

    @Test
    public void whenValidDimensionIsGivenShouldCreateQuantityClass() {
        Class<? extends Quantity<?>> temperature = UnitUtils.parseDimension("Temperature");
        assertNotNull(temperature);
        assertTrue(Temperature.class.isAssignableFrom(temperature));

        Class<? extends Quantity<?>> intensity = UnitUtils.parseDimension("Intensity");
        assertNotNull(intensity);
        assertTrue(Intensity.class.isAssignableFrom(intensity));
    }

    @Test
    public void testConversionOfUnit() {
        assertThat(SmartHomeUnits.DECIBEL_MILLIWATTS.getConverterTo(SmartHomeUnits.WATT).convert(50),
                closeTo(100, 0.001));
        assertThat(SmartHomeUnits.WATT.getConverterTo(SmartHomeUnits.DECIBEL_MILLIWATTS).convert(0.1),
                closeTo(20, 0.0001));
        assertThat(
                SmartHomeUnits.METRE_PER_SQUARE_SECOND.getConverterTo(SmartHomeUnits.STANDARD_GRAVITY).convert(9.8065),
                closeTo(1.0, 0.0001));
    }

    @Test
    public void shouldParseUnitFromPattern() {
        assertThat(UnitUtils.parseUnit("%.2f °F"), is(ImperialUnits.FAHRENHEIT));
        assertThat(UnitUtils.parseUnit("%.2f °C"), is(SIUnits.CELSIUS));
        assertThat(UnitUtils.parseUnit("myLabel km"), is(KILO(SIUnits.METRE)));
        assertThat(UnitUtils.parseUnit("%.2f %%"), is(SmartHomeUnits.PERCENT));
        assertThat(UnitUtils.parseUnit("myLabel %unit%"), is(nullValue()));
        assertThat(UnitUtils.parseUnit("%.2f kvarh"), is(SmartHomeUnits.KILOVAR_HOUR));
    }

    @Test
    public void testParsePureUnit() {
        assertThat(UnitUtils.parseUnit("DU"), is(SmartHomeUnits.DOBSON_UNIT));
        assertThat(UnitUtils.parseUnit("°F"), is(ImperialUnits.FAHRENHEIT));
        assertThat(UnitUtils.parseUnit("m"), is(SIUnits.METRE));
        assertThat(UnitUtils.parseUnit("%"), is(SmartHomeUnits.PERCENT));
    }

    @Test
    public void testGetDimensionName() {
        assertThat(UnitUtils.getDimensionName(SIUnits.CELSIUS), is(Temperature.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(SmartHomeUnits.KILOWATT_HOUR), is(Energy.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(SmartHomeUnits.WATT), is(Power.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(MetricPrefix.MEGA(SmartHomeUnits.KILOWATT_HOUR)),
                is(Energy.class.getSimpleName()));

        Unit<?> unit = UnitUtils.parseUnit("°F");
        assertNotNull(unit);
        assertThat(UnitUtils.getDimensionName(unit), is(Temperature.class.getSimpleName()));
        unit = UnitUtils.parseUnit("m");
        assertNotNull(unit);
        assertThat(UnitUtils.getDimensionName(unit), is(Length.class.getSimpleName()));
    }
}
