/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.automation.util.mapper;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class to convert serialised inputs to the Java types required by the {@link Input}s of a
 * {@link ActionType}.
 *
 * @author Laurent Garnier & Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class SerialisedInputsToActionInputs {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialisedInputsToActionInputs.class);

    /**
     * Maps serialised inputs to the Java types required by the {@link Input}s of the given {@link ActionType}.
     *
     * @param actionType the action type whose inputs to consider
     * @param arguments the serialised arguments
     * @param unitProvider the unit provider to be used to get the default unit for a dimension, can be null
     * @return the mapped arguments
     */
    public static Map<String, Object> map(ActionType actionType, Map<String, Object> arguments,
            @Nullable UnitProvider unitProvider) {
        Map<String, Object> newArguments = new HashMap<>();
        for (Input input : actionType.getInputs()) {
            String name = input.getName();
            Object value = arguments.get(name);
            value = map(actionType, input, value, unitProvider);
            if (value == null) {
                continue;
            }
            newArguments.put(name, value);
        }
        return newArguments;
    }

    /**
     * Maps a serialised input to the Java type required by the given {@link Input}.
     *
     * @param actionType the action type whose inputs to consider
     * @param input the input whose type to consider
     * @param argument the serialised argument
     * @param unitProvider the unit provider to be used to get the default unit for a dimension, can be null
     * @return the mapped argument or null if the input argument was null or mapping failed
     */
    public static @Nullable Object map(ActionType actionType, Input input, @Nullable Object argument,
            @Nullable UnitProvider unitProvider) {
        boolean failed = false;
        if (argument == null) {
            return null;
        }
        Matcher matcher = ActionInputsToConfigDescriptionParameters.QUANTITY_TYPE_PATTERN.matcher(input.getType());
        if (argument instanceof Double valueDouble) {
            // When an integer value is provided as input value, the value type in the Map is Double.
            // We have to convert Double type into the target type.
            if (matcher.matches()) {
                try {
                    if (unitProvider != null) {
                        return new QuantityType<>(valueDouble, ActionInputsToConfigDescriptionParameters
                                .getDefaultUnit(matcher.group("dimension"), unitProvider));
                    }
                } catch (IllegalArgumentException e) {
                    failed = true;
                }
            } else {
                try {
                    return switch (input.getType()) {
                        case "byte", "java.lang.Byte" -> Byte.valueOf(valueDouble.byteValue());
                        case "short", "java.lang.Short" -> Short.valueOf(valueDouble.shortValue());
                        case "int", "java.lang.Integer" -> Integer.valueOf(valueDouble.intValue());
                        case "long", "java.lang.Long" -> Long.valueOf(valueDouble.longValue());
                        case "float", "java.lang.Float" -> Float.valueOf(valueDouble.floatValue());
                        case "org.openhab.core.library.types.DecimalType" -> new DecimalType(valueDouble);
                        default -> argument;
                    };
                } catch (NumberFormatException e) {
                    failed = true;
                }
            }
        } else if (argument instanceof String valueString) {
            // String value is accepted to instantiate few target types
            if (matcher.matches()) {
                try {
                    // The string can contain either a simple decimal value without unit or a decimal value with unit
                    try {
                        BigDecimal bigDecimal = new BigDecimal(valueString);
                        if (unitProvider != null) {
                            return new QuantityType<>(bigDecimal, ActionInputsToConfigDescriptionParameters
                                    .getDefaultUnit(matcher.group("dimension"), unitProvider));
                        }
                    } catch (NumberFormatException e) {
                        return new QuantityType<>(valueString);
                    }
                } catch (IllegalArgumentException e) {
                    failed = true;
                }
            } else {
                try {
                    return switch (input.getType()) {
                        case "boolean", "java.lang.Boolean" -> Boolean.valueOf(valueString.toLowerCase());
                        case "byte", "java.lang.Byte" -> Byte.valueOf(valueString);
                        case "short", "java.lang.Short" -> Short.valueOf(valueString);
                        case "int", "java.lang.Integer" -> Integer.valueOf(valueString);
                        case "long", "java.lang.Long" -> Long.valueOf(valueString);
                        case "float", "java.lang.Float" -> Float.valueOf(valueString);
                        case "double", "java.lang.Double", "java.lang.Number" -> Double.valueOf(valueString);
                        case "org.openhab.core.library.types.DecimalType" -> new DecimalType(valueString);
                        case "java.time.LocalDate" ->
                            // Accepted format is: 2007-12-03
                            LocalDate.parse(valueString);
                        case "java.time.LocalTime" ->
                            // Accepted format is: 10:15:30
                            LocalTime.parse(valueString);
                        case "java.time.LocalDateTime" ->
                            // Accepted format is: 2007-12-03 10:15:30
                            LocalDateTime.parse(valueString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        case "java.util.Date" ->
                            // Accepted format is: 2007-12-03 10:15:30
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(valueString);
                        case "java.time.ZonedDateTime" ->
                            // Accepted format is: 2007-12-03T10:15:30+01:00[Europe/Paris]
                            ZonedDateTime.parse(valueString);
                        case "java.time.Instant" ->
                            // Accepted format is: 2007-12-03T10:15:30.00Z
                            Instant.parse(valueString);
                        case "java.time.Duration" ->
                            // Accepted format is: P2DT17H25M30.5S
                            Duration.parse(valueString);
                        default -> argument;
                    };
                } catch (NumberFormatException | DateTimeParseException | ParseException e) {
                    failed = true;
                }
            }
        }
        if (failed) {
            LOGGER.warn(
                    "Action {} input parameter '{}': converting value '{}' into type {} failed! Input parameter is ignored.",
                    actionType.getUID(), input.getName(), argument, input.getType());
        }
        return failed ? null : argument;
    }
}
