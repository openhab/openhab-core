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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class to convert action {@link Input}s to {@link ConfigDescriptionParameter}s.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ActionInputsToConfigDescriptionParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionInputsToConfigDescriptionParameters.class);

    public static final Pattern QUANTITY_TYPE_PATTERN = Pattern
            .compile("([a-z0-9]+\\.)*QuantityType<([a-z0-9]+\\.)*(?<dimension>[A-Z][a-zA-Z0-9]*)>");

    /**
     * Maps a list of {@link Input}s to a list of {@link ConfigDescriptionParameter}s.
     *
     * @param inputs the list of inputs to map to config description parameters
     * @param unitProvider the unit provider to be used to get the default unit for a dimension, can be null
     * @return the list of config description parameters or null if an input parameter has an unsupported type
     */
    public static @Nullable List<ConfigDescriptionParameter> map(List<Input> inputs,
            @Nullable UnitProvider unitProvider) {
        List<ConfigDescriptionParameter> configDescriptionParameters = new ArrayList<>();

        for (Input input : inputs) {
            ConfigDescriptionParameter parameter = ActionInputsToConfigDescriptionParameters.map(input, unitProvider);
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
     * <br>
     * When adding new input types, remember to update {@link SerialisedInputsToActionInputs} as well!
     *
     * @param unitProvider the unit provider to be used to get the default unit for a dimension, can be null
     * @param input the input to map to a config description parameter
     * @return the config description parameter or null if the input parameter has an unsupported type
     */
    public static @Nullable ConfigDescriptionParameter map(Input input, @Nullable UnitProvider unitProvider) {
        boolean supported = true;
        ConfigDescriptionParameter.Type parameterType = ConfigDescriptionParameter.Type.TEXT;
        String defaultValue = null;
        Unit<?> unit = null;
        boolean required = false;
        String context = null;
        Matcher matcher = QUANTITY_TYPE_PATTERN.matcher(input.getType());
        if (matcher.matches() && unitProvider != null) {
            parameterType = ConfigDescriptionParameter.Type.DECIMAL;
            try {
                unit = getDefaultUnit(matcher.group("dimension"), unitProvider);
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
            LOGGER.debug("Input parameter '{}' with type {} cannot be converted into a config description parameter!",
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
            builder = builder.withUnit(unit.getSymbol(), false);
        }
        return builder.build();
    }

    public static Unit<?> getDefaultUnit(String dimensionName, UnitProvider unitProvider)
            throws IllegalArgumentException {
        Class<? extends Quantity<?>> dimension = UnitUtils.parseDimension(dimensionName);
        if (dimension == null) {
            throw new IllegalArgumentException("Unknwon dimension " + dimensionName);
        }
        return unitProvider.getUnit((Class<? extends Quantity>) dimension);
    }
}
