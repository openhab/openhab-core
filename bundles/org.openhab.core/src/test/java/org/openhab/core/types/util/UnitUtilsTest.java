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
package org.openhab.core.types.util;

import static org.eclipse.jdt.annotation.Checks.requireNonNull;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.library.unit.MetricPrefix.*;

import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Power;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.dimension.Intensity;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;

/**
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
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
        Class<? extends Quantity<?>> temperature = requireNonNull(UnitUtils.parseDimension("Temperature"));
        assertTrue(Temperature.class.isAssignableFrom(temperature));

        Class<? extends Quantity<?>> intensity = requireNonNull(UnitUtils.parseDimension("Intensity"));
        assertTrue(Intensity.class.isAssignableFrom(intensity));
    }

    @Test
    public void testConversionOfUnit() {
        assertThat(Units.DECIBEL_MILLIWATTS.getConverterTo(Units.WATT).convert(50), closeTo(100, 0.001));
        assertThat(Units.WATT.getConverterTo(Units.DECIBEL_MILLIWATTS).convert(0.1), closeTo(20, 0.0001));
        assertThat(Units.METRE_PER_SQUARE_SECOND.getConverterTo(Units.STANDARD_GRAVITY).convert(9.8065),
                closeTo(1.0, 0.0001));
    }

    @Test
    public void shouldParseUnitFromPattern() {
        assertThat(UnitUtils.parseUnit("%.2f °F"), is(ImperialUnits.FAHRENHEIT));
        assertThat(UnitUtils.parseUnit("%.2f °C"), is(SIUnits.CELSIUS));
        assertThat(UnitUtils.parseUnit("myLabel km"), is(KILO(SIUnits.METRE)));
        assertThat(UnitUtils.parseUnit("%.2f %%"), is(Units.PERCENT));
        assertThat(UnitUtils.parseUnit("myLabel %unit%"), is(nullValue()));
        assertThat(UnitUtils.parseUnit("%.2f kvarh"), is(Units.KILOVAR_HOUR));
    }

    @Test
    public void testParsePureUnit() {
        assertThat(UnitUtils.parseUnit("DU"), is(Units.DOBSON_UNIT));
        assertThat(UnitUtils.parseUnit("°F"), is(ImperialUnits.FAHRENHEIT));
        assertThat(UnitUtils.parseUnit("m"), is(SIUnits.METRE));
        assertThat(UnitUtils.parseUnit("%"), is(Units.PERCENT));
    }

    @Test
    public void testParseUnknownUnit() {
        assertNull(UnitUtils.parseUnit("123 Hello World"));
        assertNull(UnitUtils.parseUnit("Lux"));
    }

    @Test
    public void testGetDimensionNameWithDimension() {
        assertThat(UnitUtils.getDimensionName(SIUnits.CELSIUS), is(Temperature.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.DEGREE_ANGLE), is(Angle.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.KILOWATT_HOUR), is(Energy.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.WATT), is(Power.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(MetricPrefix.MEGA(Units.KILOWATT_HOUR)),
                is(Energy.class.getSimpleName()));

        Unit<?> unit = Objects.requireNonNull(UnitUtils.parseUnit("°F"));
        assertThat(UnitUtils.getDimensionName(unit), is(Temperature.class.getSimpleName()));

        unit = Objects.requireNonNull(UnitUtils.parseUnit("m"));
        assertThat(UnitUtils.getDimensionName(unit), is(Length.class.getSimpleName()));
    }

    @Test
    public void testGetDimensionNameWithoutDimension() {
        assertThat(UnitUtils.getDimensionName(Units.DECIBEL), is(Dimensionless.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.ONE), is(Dimensionless.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.PARTS_PER_MILLION), is(Dimensionless.class.getSimpleName()));
        assertThat(UnitUtils.getDimensionName(Units.PERCENT), is(Dimensionless.class.getSimpleName()));
    }
}
