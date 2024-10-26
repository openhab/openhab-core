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
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an utility class to convert the {@link Input}s of a {@link ActionType} into a list of
 * {@link ConfigDescriptionParameter}s and to convert serialised inputs to the Java types required by the
 * {@link Input}s of a {@link ActionType}.
 *
 * @author Laurent Garnier and Florian Hotze - Initial contribution
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

    /**
     * Maps a list of {@link Input}s to a list of {@link ConfigDescriptionParameter}s.
     *
     * @param inputs the list of inputs to map to config description parameters
     * @return the list of config description parameters
     * @throws IllegalArgumentException if at least one of the input parameters has an unsupported type
     */
    public List<ConfigDescriptionParameter> mapActionInputsToConfigDescriptionParameters(List<Input> inputs)
            throws IllegalArgumentException {
        List<ConfigDescriptionParameter> configDescriptionParameters = new ArrayList<>();

        for (Input input : inputs) {
            configDescriptionParameters.add(mapActionInputToConfigDescriptionParameter(input));
        }

        return configDescriptionParameters;
    }

    /**
     * Maps an {@link Input} to a {@link ConfigDescriptionParameter}.
     *
     * @param input the input to map to a config description parameter
     * @return the config description parameter
     * @throws IllegalArgumentException if the input parameter has an unsupported type
     */
    public ConfigDescriptionParameter mapActionInputToConfigDescriptionParameter(Input input)
            throws IllegalArgumentException {
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
                throw new IllegalArgumentException("Input parameter '" + input.getName() + "' with type "
                        + input.getType() + "cannot be converted into a config description parameter!", e);
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
                    throw new IllegalArgumentException("Input parameter '" + input.getName() + "' with type "
                            + input.getType() + "cannot be converted into a config description parameter!");
            }
        }

        ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder
                .create(input.getName(), parameterType).withLabel(input.getLabel())
                .withDescription(input.getDescription()).withReadOnly(false)
                .withRequired(required || input.isRequired()).withContext(context);
        if (input.getDefaultValue() != null && !input.getDefaultValue().isEmpty()) {
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
            Object value = arguments.get(input.getName());
            if (value != null) {
                try {
                    newArguments.put(input.getName(), mapSerializedInputToActionInput(input, value));
                } catch (IllegalArgumentException e) {
                    logger.warn("{} Input parameter is ignored.", e.getMessage());
                }
            }
        }
        return newArguments;
    }

    /**
     * Maps a serialised input to the Java type required by the given {@link Input}.
     *
     * @param input the input whose type to consider
     * @param argument the serialised argument
     * @return the mapped argument
     * @throws IllegalArgumentException if the mapping failed
     */
    public Object mapSerializedInputToActionInput(Input input, Object argument) throws IllegalArgumentException {
        try {
            Matcher matcher = QUANTITY_TYPE_PATTERN.matcher(input.getType());
            if (argument instanceof Double valueDouble) {
                // When an integer value is provided as input value, the value type in the Map is Double.
                // We have to convert Double type into the target type.
                if (matcher.matches()) {
                    return new QuantityType<>(valueDouble, getDefaultUnit(matcher.group("dimension")));
                } else {
                    return switch (input.getType()) {
                        case "byte", "java.lang.Byte" -> Byte.valueOf(valueDouble.byteValue());
                        case "short", "java.lang.Short" -> Short.valueOf(valueDouble.shortValue());
                        case "int", "java.lang.Integer" -> Integer.valueOf(valueDouble.intValue());
                        case "long", "java.lang.Long" -> Long.valueOf(valueDouble.longValue());
                        case "float", "java.lang.Float" -> Float.valueOf(valueDouble.floatValue());
                        case "org.openhab.core.library.types.DecimalType" -> new DecimalType(valueDouble);
                        default -> argument;
                    };
                }
            } else if (argument instanceof String valueString) {
                // String value is accepted to instantiate few target types
                if (matcher.matches()) {
                    // The string can contain either a simple decimal value without unit or a decimal value with
                    // unit
                    try {
                        BigDecimal bigDecimal = new BigDecimal(valueString);
                        return new QuantityType<>(bigDecimal, getDefaultUnit(matcher.group("dimension")));
                    } catch (NumberFormatException e) {
                        return new QuantityType<>(valueString);
                    }
                } else {
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
                            // Accepted format is: 2007-12-03T10:15:30
                            LocalDateTime.parse(valueString);
                        case "java.util.Date" ->
                            // Accepted format is: 2007-12-03T10:15:30
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(valueString);
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
                }
            }
            return argument;
        } catch (IllegalArgumentException | DateTimeParseException | ParseException e) {
            throw new IllegalArgumentException("Input parameter '" + input.getName() + "': converting value '"
                    + argument.toString() + "' into type " + input.getType() + " failed!");
        }
    }

    private Unit<?> getDefaultUnit(String dimensionName) throws IllegalArgumentException {
        Class<? extends Quantity<?>> dimension = UnitUtils.parseDimension(dimensionName);
        if (dimension == null) {
            throw new IllegalArgumentException("Unknown dimension " + dimensionName);
        }
        return unitProvider.getUnit((Class<? extends Quantity>) dimension);
    }
}
