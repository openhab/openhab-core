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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.openhab.core.i18n.TimeZoneProvider;
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

    private final TimeZoneProvider timeZoneProvider;
    private final UnitProvider unitProvider;

    // Primitive types
    private static final String BOOLEAN = "boolean";
    private static final String BYTE = "byte";
    private static final String SHORT = "short";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";

    // Wrapper and specific Java classes
    private static final String JAVA_BOOLEAN = "java.lang.Boolean";
    private static final String JAVA_BYTE = "java.lang.Byte";
    private static final String JAVA_SHORT = "java.lang.Short";
    private static final String JAVA_INTEGER = "java.lang.Integer";
    private static final String JAVA_LONG = "java.lang.Long";
    private static final String JAVA_FLOAT = "java.lang.Float";
    private static final String JAVA_DOUBLE = "java.lang.Double";
    private static final String JAVA_NUMBER = "java.lang.Number";
    private static final String JAVA_DECIMAL_TYPE = "org.openhab.core.library.types.DecimalType";
    private static final String JAVA_STRING = "java.lang.String";
    private static final String JAVA_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_LOCAL_TIME = "java.time.LocalTime";
    private static final String JAVA_LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String JAVA_UTIL_DATE = "java.util.Date";
    private static final String JAVA_ZONED_DATE_TIME = "java.time.ZonedDateTime";
    private static final String JAVA_INSTANT = "java.time.Instant";
    private static final String JAVA_DURATION = "java.time.Duration";

    @Activate
    public ActionInputsHelper(final @Reference TimeZoneProvider timeZoneProvider,
            final @Reference UnitProvider unitProvider) {
        this.timeZoneProvider = timeZoneProvider;
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
        BigDecimal step = null;
        Matcher matcher = QUANTITY_TYPE_PATTERN.matcher(input.getType());
        if (matcher.matches()) {
            parameterType = ConfigDescriptionParameter.Type.DECIMAL;
            step = BigDecimal.ZERO;
            try {
                unit = getDefaultUnit(matcher.group("dimension"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Input parameter '" + input.getName() + "' with type "
                        + input.getType() + "cannot be converted into a config description parameter!", e);
            }
        } else {
            switch (input.getType()) {
                case BOOLEAN:
                    defaultValue = "false";
                    required = true;
                case JAVA_BOOLEAN:
                    parameterType = ConfigDescriptionParameter.Type.BOOLEAN;
                    break;
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                    defaultValue = "0";
                    required = true;
                case JAVA_BYTE:
                case JAVA_SHORT:
                case JAVA_INTEGER:
                case JAVA_LONG:
                    parameterType = ConfigDescriptionParameter.Type.INTEGER;
                    break;
                case FLOAT:
                case DOUBLE:
                    defaultValue = "0";
                    required = true;
                case JAVA_FLOAT:
                case JAVA_DOUBLE:
                case JAVA_NUMBER:
                case JAVA_DECIMAL_TYPE:
                    parameterType = ConfigDescriptionParameter.Type.DECIMAL;
                    step = BigDecimal.ZERO;
                    break;
                case JAVA_STRING:
                    break;
                case JAVA_LOCAL_DATE:
                    context = "date";
                    break;
                case JAVA_LOCAL_TIME:
                    context = "time";
                    step = BigDecimal.ONE;
                    break;
                case JAVA_LOCAL_DATE_TIME:
                case JAVA_UTIL_DATE:
                case JAVA_ZONED_DATE_TIME:
                case JAVA_INSTANT:
                    context = "datetime";
                    step = BigDecimal.ONE;
                    break;
                case JAVA_DURATION:
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
        if (step != null) {
            builder = builder.withStepSize(step);
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
            } else {
                value = getDefaultValueForActionInput(input);
                if (value != null) {
                    newArguments.put(input.getName(), value);
                }
            }
        }
        return newArguments;
    }

    private @Nullable Object getDefaultValueForActionInput(Input input) {
        return switch (input.getType()) {
            case BOOLEAN -> false;
            case BYTE -> (byte) 0;
            case SHORT -> (short) 0;
            case INT -> 0;
            case LONG -> 0L;
            case FLOAT -> 0.0f;
            case DOUBLE -> 0.0d;
            default -> null;
        };
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
                        case BYTE, JAVA_BYTE -> Byte.valueOf(valueDouble.byteValue());
                        case SHORT, JAVA_SHORT -> Short.valueOf(valueDouble.shortValue());
                        case INT, JAVA_INTEGER -> Integer.valueOf(valueDouble.intValue());
                        case LONG, JAVA_LONG -> Long.valueOf(valueDouble.longValue());
                        case FLOAT, JAVA_FLOAT -> Float.valueOf(valueDouble.floatValue());
                        case JAVA_DECIMAL_TYPE -> new DecimalType(valueDouble);
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
                        case BOOLEAN, JAVA_BOOLEAN -> Boolean.valueOf(valueString.toLowerCase());
                        case BYTE, JAVA_BYTE -> Byte.valueOf(valueString);
                        case SHORT, JAVA_SHORT -> Short.valueOf(valueString);
                        case INT, JAVA_INTEGER -> Integer.valueOf(valueString);
                        case LONG, JAVA_LONG -> Long.valueOf(valueString);
                        case FLOAT, JAVA_FLOAT -> Float.valueOf(valueString);
                        case DOUBLE, JAVA_DOUBLE, JAVA_NUMBER -> Double.valueOf(valueString);
                        case JAVA_DECIMAL_TYPE -> new DecimalType(valueString);
                        case JAVA_LOCAL_DATE ->
                            // Accepted format is: 2007-12-03
                            LocalDate.parse(valueString);
                        case JAVA_LOCAL_TIME ->
                            // Accepted format is: 10:15:30
                            LocalTime.parse(valueString);
                        case JAVA_LOCAL_DATE_TIME ->
                            // Accepted format is: 2007-12-03T10:15:30
                            LocalDateTime.parse(valueString);
                        case JAVA_UTIL_DATE ->
                            // Accepted format is: 2007-12-03T10:15:30
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(valueString);
                        case JAVA_ZONED_DATE_TIME ->
                            // Accepted format is: 2007-12-03T10:15:30
                            LocalDateTime.parse(valueString).atZone(timeZoneProvider.getTimeZone());
                        case JAVA_INSTANT ->
                            // Accepted format is: 2007-12-03T10:15:30
                            LocalDateTime.parse(valueString).atZone(timeZoneProvider.getTimeZone()).toInstant();
                        case JAVA_DURATION ->
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
