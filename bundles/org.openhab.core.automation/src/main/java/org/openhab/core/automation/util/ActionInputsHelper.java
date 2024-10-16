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
package org.openhab.core.automation.util;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.util.UnitUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class to convert serialised inputs to the Java types required by the {@link Input}s of a
 * {@link ActionType}.
 *
 * @author Laurent Garnier & Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = ActionInputsHelper.class)
public class ActionInputsHelper {
    private static final Pattern QUANTITY_TYPE_PATTERN = Pattern
            .compile("([a-z0-9]+\\.)*QuantityType<([a-z0-9]+\\.)*(?<dimension>[A-Z][a-zA-Z0-9]*)>");

    private final Logger logger = LoggerFactory.getLogger(ActionInputsHelper.class);

    private final UnitProvider unitProvider;

    @Activate
    public ActionInputsHelper(final @Reference UnitProvider unitProvider) {
        this.unitProvider = unitProvider;
    }

    @Deactivate
    protected void deactivate() {
    }

    /**
     * Maps a list of {@link Input}s to a list of {@link ConfigDescriptionParameter}s.
     *
     * @param inputs the list of inputs to map to config description parameters
     * @return the list of config description parameters or null if an input parameter has an unsupported type
     */
    public @Nullable List<ConfigDescriptionParameter> mapActionInputsToConfigDescriptionParameters(List<Input> inputs) {
        List<ConfigDescriptionParameter> configDescriptionParameters = new ArrayList<>();

        for (Input input : inputs) {
            ConfigDescriptionParameter parameter = mapActionInputToConfigDescriptionParameter(input);
            if (parameter != null) {
                configDescriptionParameters.add(parameter);
            } else {
                configDescriptionParameters = null;
                break;
            }
        }

        return configDescriptionParameters;
    }

    /**
     * Maps an {@link Input} to a {@link ConfigDescriptionParameter}.
     *
     * @param input the input to map to a config description parameter
     * @return the config description parameter or null if the input parameter has an unsupported type
     */
    public @Nullable ConfigDescriptionParameter mapActionInputToConfigDescriptionParameter(Input input) {
        boolean supported = true;
        ConfigDescriptionParameter.Type parameterType = ConfigDescriptionParameter.Type.TEXT;
        String defaultValue = null;
        Unit<?> unit = null;
        boolean required = false;
        String context = null;
        Matcher matcher = QUANTITY_TYPE_PATTERN.matcher(input.getType());
        if (matcher.matches()) {
            parameterType = ConfigDescriptionParameter.Type.DECIMAL;
            try {
                unit = getDefaultUnit(matcher.group("dimension"));
            } catch (IllegalArgumentException e) {
                supported = false;
            }
        } else {
            switch (input.getType()) {
                case "boolean":
                    defaultValue = "false";
                    required = true;
                case "java.lang.Boolean":
                    parameterType = ConfigDescriptionParameter.Type.BOOLEAN;
                    break;
                case "byte":
                case "short":
                case "int":
                case "long":
                    defaultValue = "0";
                    required = true;
                case "java.lang.Byte":
                case "java.lang.Short":
                case "java.lang.Integer":
                case "java.lang.Long":
                    parameterType = ConfigDescriptionParameter.Type.INTEGER;
                    break;
                case "float":
                case "double":
                    defaultValue = "0";
                    required = true;
                case "java.lang.Float":
                case "java.lang.Double":
                case "java.lang.Number":
                case "org.openhab.core.library.types.DecimalType":
                    parameterType = ConfigDescriptionParameter.Type.DECIMAL;
                    break;
                case "java.lang.String":
                    break;
                case "java.time.LocalDate":
                    context = "date";
                    break;
                case "java.time.LocalTime":
                    context = "time";
                    break;
                case "java.time.LocalDateTime":
                case "java.util.Date":
                    context = "datetime";
                    break;
                case "java.time.ZonedDateTime":
                case "java.time.Instant":
                case "java.time.Duration":
                    // There is no available configuration parameter context for these types.
                    // A text parameter is used. The expected value must respect a particular format specific
                    // to each of these types.
                    break;
                default:
                    supported = false;
                    break;
            }
        }
        if (!supported) {
            logger.debug("Input parameter '{}' with type {} cannot be converted into a config description parameter!",
                    input.getName(), input.getType());
            return null;
        }

        ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder
                .create(input.getName(), parameterType).withLabel(input.getLabel())
                .withDescription(input.getDescription()).withReadOnly(false)
                .withRequired(required || input.isRequired()).withContext(context);
        if (!input.getDefaultValue().isEmpty()) {
            builder = builder.withDefault(input.getDefaultValue());
        } else if (defaultValue != null) {
            builder = builder.withDefault(defaultValue);
        }
        if (unit != null) {
            builder = builder.withUnit(unit.getSymbol());
        }
        return builder.build();
    }

    /**
     * Maps serialised inputs to the Java types required by the {@link Input}s of the given {@link ActionType}.
     *
     * @param actionType the action type whose inputs to consider
     * @param arguments the serialised arguments
     * @return the mapped arguments
     */
    public Map<String, Object> mapSerializedInputsToActionInputs(ActionType actionType, Map<String, Object> arguments) {
        Map<String, Object> newArguments = new HashMap<>();
        for (Input input : actionType.getInputs()) {
            String name = input.getName();
            Object value = arguments.get(name);
            value = mapSerializedInputToActionInput(actionType, input, value);
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
     * @return the mapped argument or null if the input argument was null or mapping failed
     */
    public @Nullable Object mapSerializedInputToActionInput(ActionType actionType, Input input,
            @Nullable Object argument) {
        boolean failed = false;
        if (argument == null) {
            return null;
        }
        Matcher matcher = QUANTITY_TYPE_PATTERN.matcher(input.getType());
        if (argument instanceof Double valueDouble) {
            // When an integer value is provided as input value, the value type in the Map is Double.
            // We have to convert Double type into the target type.
            if (matcher.matches()) {
                try {
                    return new QuantityType<>(valueDouble, getDefaultUnit(matcher.group("dimension")));
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
                        return new QuantityType<>(bigDecimal, getDefaultUnit(matcher.group("dimension")));
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
            logger.warn(
                    "Action {} input parameter '{}': converting value '{}' into type {} failed! Input parameter is ignored.",
                    actionType.getUID(), input.getName(), argument, input.getType());
        }
        return failed ? null : argument;
    }

    private Unit<?> getDefaultUnit(String dimensionName) throws IllegalArgumentException {
        Class<? extends Quantity<?>> dimension = UnitUtils.parseDimension(dimensionName);
        if (dimension == null) {
            throw new IllegalArgumentException("Unknown dimension " + dimensionName);
        }
        return unitProvider.getUnit((Class<? extends Quantity>) dimension);
    }
}
