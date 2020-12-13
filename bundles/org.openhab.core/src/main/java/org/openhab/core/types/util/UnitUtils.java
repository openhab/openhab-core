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

import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.library.unit.UnitInitializer;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.MetricPrefix;
import tec.uom.se.unit.TransformedUnit;

/**
 * A utility for parsing dimensions to interface classes of {@link Quantity} and parsing units from format strings.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class UnitUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitUtils.class);

    public static final String UNIT_PLACEHOLDER = "%unit%";
    public static final String UNIT_PERCENT_FORMAT_STRING = "%%";

    private static final String JAVAX_MEASURE_QUANTITY_PREFIX = "javax.measure.quantity.";
    private static final String FRAMEWORK_DIMENSION_PREFIX = "org.openhab.core.library.dimension.";

    private static final Collection<Class<? extends SystemOfUnits>> ALL_SYSTEM_OF_UNITS = Arrays.asList(SIUnits.class,
            ImperialUnits.class, Units.class, tec.uom.se.unit.Units.class);

    static {
        UnitInitializer.init();
    }

    /**
     * Parses a String denoting a dimension (e.g. Length, Temperature, Mass,..) into a {@link Class} instance of an
     * interface from {@link javax.measure.Quantity}. The interfaces reside in {@code javax.measure.quantity} and
     * framework specific interfaces in {@code org.openhab.core.library.dimension}.
     *
     * @param dimension the simple name of an interface from the package {@code javax.measure.quantity} or
     *            {@code org.openhab.core.library.dimension}.
     * @return the {@link Class} instance of the interface or {@code null} if the given dimension is blank.
     * @throws IllegalArgumentException in case no class instance could be parsed from the given dimension.
     */
    public static @Nullable Class<? extends Quantity<?>> parseDimension(String dimension) {
        if (dimension.isBlank()) {
            return null;
        }

        try {
            return dimensionClass(FRAMEWORK_DIMENSION_PREFIX, dimension);
        } catch (ClassNotFoundException e1) {
            try {
                return dimensionClass(JAVAX_MEASURE_QUANTITY_PREFIX, dimension);
            } catch (ClassNotFoundException e2) {
                throw new IllegalArgumentException(
                        "Error creating a dimension Class instance for name '" + dimension + "'.");
            }
        }
    }

    /**
     * The name of the dimension as stated in the ChannelType configuration.
     * e.g.
     * <p>
     * <code> Unit: 'm' -> "Length"</code>
     * <p>
     * <code> Unit: 'kWh' -> "Energy"</code>
     * <p>
     * If the {@link Unit} can not be found in any of the available Measurement systems, it returns <code>null</code>
     *
     * @param unit The {@link Unit} to get the Dimension's name from.
     * @return The Dimension string or null if the unit can not be found in any of the SystemOfUnits.
     */
    public static @Nullable String getDimensionName(Unit<?> unit) {
        for (Class<? extends SystemOfUnits> system : ALL_SYSTEM_OF_UNITS) {
            for (Field field : system.getDeclaredFields()) {
                if (field.getType().isAssignableFrom(Unit.class) && Modifier.isStatic(field.getModifiers())) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        String dimension = ((Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0])
                                .getSimpleName();
                        Unit<?> systemUnit;
                        try {
                            systemUnit = (Unit<?>) field.get(null);
                            if (systemUnit == null) {
                                LOGGER.warn("Unit field points to a null value: {}", field);
                            } else if (systemUnit.isCompatible(unit)) {
                                return dimension;
                            }
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            LOGGER.error("The unit field '{}' seems to be not accessible", field, e);
                        }
                    } else {
                        LOGGER.warn("There is a unit field defined which has no generic type parametrization: {}",
                                field);
                    }
                }
            }
        }
        return null;
    }

    /**
     * A utility method to parse a unit symbol either directly or from a given pattern (like stateDescription or widget
     * label). In the latter case, the unit is expected to be the last part of the pattern separated by " " (e.g. "%.2f
     * °C" for °C).
     *
     * @param stringWithUnit the string to extract the unit symbol from
     * @return the unit symbol extracted from the string or {@code null} if no unit could be parsed
     *
     */
    public static @Nullable Unit<?> parseUnit(@Nullable String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }

        String unitSymbol = pattern;
        int lastBlankIndex = pattern.lastIndexOf(" ");
        if (lastBlankIndex >= 0) {
            unitSymbol = pattern.substring(lastBlankIndex).trim();
        }

        if (!UNIT_PLACEHOLDER.equals(unitSymbol)) {
            if (UNIT_PERCENT_FORMAT_STRING.equals(unitSymbol)) {
                return Units.PERCENT;
            }
            try {
                Quantity<?> quantity = Quantities.getQuantity("1 " + unitSymbol);
                return quantity.getUnit();
            } catch (IllegalArgumentException e) {
                // we expect this exception in case the extracted string does not match any known unit
                LOGGER.debug("Unknown unit from pattern: {}", unitSymbol);
            }
        }

        return null;
    }

    public static boolean isDifferentMeasurementSystem(Unit<? extends Quantity<?>> thisUnit, Unit<?> thatUnit) {
        Set<? extends Unit<?>> siUnits = SIUnits.getInstance().getUnits();
        Set<? extends Unit<?>> usUnits = ImperialUnits.getInstance().getUnits();

        boolean differentSystems = (siUnits.contains(thisUnit) && usUnits.contains(thatUnit)) //
                || (siUnits.contains(thatUnit) && usUnits.contains(thisUnit));

        if (!differentSystems) {
            if (thisUnit instanceof TransformedUnit
                    && isMetricConversion(((TransformedUnit<?>) thisUnit).getConverter())) {
                return isDifferentMeasurementSystem(((TransformedUnit<?>) thisUnit).getParentUnit(), thatUnit);
            }

            if (thatUnit instanceof TransformedUnit
                    && isMetricConversion(((TransformedUnit<?>) thatUnit).getConverter())) {
                return isDifferentMeasurementSystem(thisUnit, ((TransformedUnit<?>) thatUnit).getParentUnit());
            }
        }

        // Compare the unit symbols. For product units (e.g. 1km / 1h) the equality is not given in the Sets above.
        if (!differentSystems) {
            Set<String> siSymbols = siUnits.stream().map(Unit::getSymbol).collect(toSet());
            Set<String> usSymbols = usUnits.stream().map(Unit::getSymbol).collect(toSet());

            differentSystems = (siSymbols.contains(thisUnit.getSymbol()) && usSymbols.contains(thatUnit.getSymbol())) //
                    || (siSymbols.contains(thatUnit.getSymbol()) && usSymbols.contains(thisUnit.getSymbol()));
        }

        return differentSystems;
    }

    private static boolean isMetricConversion(UnitConverter converter) {
        for (MetricPrefix mp : MetricPrefix.values()) {
            if (mp.getConverter().equals(converter)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Quantity<?>> dimensionClass(String prefix, String name)
            throws ClassNotFoundException {
        return (Class<? extends Quantity<?>>) Class.forName(prefix + name);
    }
}
