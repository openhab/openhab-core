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
package org.openhab.core.config.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.internal.normalization.Normalizer;
import org.openhab.core.config.core.internal.normalization.NormalizerFactory;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.LoggerFactory;

/**
 * The configuration admin service provides us with a map of key->values. Values can be any
 * primitive type like String, int, double as well as the object variants of the number types
 * like Integer, Double etc. You find normalization utility methods in here to convert all
 * number Types to BigDecimal and all Collections<type> to List<type>. This works on a best-effort
 * strategy (e.g. "3" will always end up being a BigDecimal, never a String),
 * except if a {@link ConfigDescriptionParameter} is given. In the latter case,
 * a conversion according to the type given in the description is performed.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Thomas Höfer - Minor changes for type normalization based on config description
 */
public class ConfigUtil {

    private static final String DEFAULT_LIST_DELIMITER = ",";

    /**
     * Maps the provided (default) value of the given {@link ConfigDescriptionParameter} to the corresponding Java type.
     *
     * In case the provided (default) value is supposed to be a number and cannot be converted into the target type
     * correctly, this method will return <code>null</code> while logging a warning.
     *
     * @param parameter the {@link ConfigDescriptionParameter} which default value should be normalized (must not be
     *            null)
     * @return the given value as the corresponding Java type or <code>null</code> if the value could not be converted
     */
    public static @Nullable Object getDefaultValueAsCorrectType(ConfigDescriptionParameter parameter) {
        return getDefaultValueAsCorrectType(parameter.getName(), parameter.getType(), parameter.getDefault());
    }

    static @Nullable Object getDefaultValueAsCorrectType(String parameterName, Type parameterType,
            String defaultValue) {
        try {
            switch (parameterType) {
                case TEXT:
                    return defaultValue;
                case BOOLEAN:
                    return Boolean.parseBoolean(defaultValue);
                case INTEGER:
                    BigDecimal value = new BigDecimal(defaultValue);
                    if (getNumberOfDecimalPlaces(value) > 0) {
                        LoggerFactory.getLogger(ConfigUtil.class).warn(
                                "Default value for parameter '{}' of type 'INTEGER' seems not to be an integer value: {}",
                                parameterName, defaultValue);
                        return value.setScale(0, BigDecimal.ROUND_DOWN);
                    }
                    return value;
                case DECIMAL:
                    return new BigDecimal(defaultValue);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            LoggerFactory.getLogger(ConfigUtil.class).warn(
                    "Could not parse default value '{}' as type '{}' for parameter '{}': {}", defaultValue,
                    parameterType, parameterName, e.getMessage(), e);
            return null;
        }
    }

    static int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
        return Math.max(0, bigDecimal.stripTrailingZeros().scale());
    }

    /**
     * Applies the default values from a give {@link ConfigDescription} to the given {@link Configuration}.
     *
     * @param configuration the {@link Configuration} where the default values should be added (must not be null)
     * @param configDescription the {@link ConfigDescription} where the default values are located (may be null, but
     *            method won't have any effect then)
     */
    public static void applyDefaultConfiguration(Configuration configuration,
            @Nullable ConfigDescription configDescription) {
        if (configDescription != null) {
            for (ConfigDescriptionParameter parameter : configDescription.getParameters()) {
                String defaultValue = parameter.getDefault();
                if (defaultValue != null && configuration.get(parameter.getName()) == null) {
                    if (parameter.isMultiple()) {
                        if (defaultValue.contains(DEFAULT_LIST_DELIMITER)) {
                            List<Object> values = Arrays.asList(defaultValue.split(DEFAULT_LIST_DELIMITER)).stream()
                                    .map(v -> v.trim()).filter(v -> !v.isEmpty())
                                    .map(v -> ConfigUtil.getDefaultValueAsCorrectType(parameter.getName(),
                                            parameter.getType(), v))
                                    .filter(v -> v != null).collect(Collectors.toList());
                            Integer multipleLimit = parameter.getMultipleLimit();
                            if (multipleLimit != null && values.size() > multipleLimit.intValue()) {
                                LoggerFactory.getLogger(ConfigUtil.class).warn(
                                        "Number of default values ({}) for parameter '{}' is greater than multiple limit ({})",
                                        values.size(), parameter.getName(), multipleLimit);
                            }
                            configuration.put(parameter.getName(), values);
                        } else {
                            Object value = ConfigUtil.getDefaultValueAsCorrectType(parameter);
                            if (value != null) {
                                configuration.put(parameter.getName(), Arrays.asList(value));
                            }
                        }
                    } else {
                        Object value = ConfigUtil.getDefaultValueAsCorrectType(parameter);
                        if (value != null) {
                            configuration.put(parameter.getName(), value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Normalizes the types to the ones allowed for configurations.
     *
     * @param configuration the configuration that needs to be normalized
     * @return normalized configuration
     */
    public static Map<String, Object> normalizeTypes(Map<String, Object> configuration) {
        Map<String, Object> convertedConfiguration = new HashMap<>(configuration.size());
        for (Entry<String, Object> parameter : configuration.entrySet()) {
            String name = parameter.getKey();
            Object value = parameter.getValue();
            if (!isOSGiConfigParameter(name)) {
                convertedConfiguration.put(name, normalizeType(value, null));
            }
        }
        return convertedConfiguration;
    }

    /**
     * Normalizes the type of the parameter to the one allowed for configurations.
     *
     * @param value the value to return as normalized type
     * @param configDescriptionParameter the parameter that needs to be normalized
     * @return corresponding value as a valid type
     * @throws IllegalArgumentException if a invalid type has been given
     */
    public static Object normalizeType(Object value, @Nullable ConfigDescriptionParameter configDescriptionParameter) {
        if (configDescriptionParameter != null) {
            Normalizer normalizer = NormalizerFactory.getNormalizer(configDescriptionParameter);
            return normalizer.normalize(value);
        } else if (value == null || value instanceof Boolean || value instanceof String
                || value instanceof BigDecimal) {
            return value;
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else if (value instanceof Collection) {
            return normalizeCollection((Collection<?>) value);
        }
        throw new IllegalArgumentException(
                "Invalid type '{" + value.getClass().getCanonicalName() + "}' of configuration value!");
    }

    /**
     * Normalizes the given configuration according to the given config descriptions.
     *
     * By doing so, it tries to convert types on a best-effort basis. The result will contain
     * BigDecimals, Strings and Booleans wherever a conversion of similar types was possible.
     *
     * However, it does not check for general correctness of types. This can be done using the
     * {@link ConfigDescriptionValidator}.
     *
     * If multiple config descriptions are given and a parameter is described several times, then the first one (lower
     * index in the list) wins.
     *
     * @param configuration the configuration to be normalized (can be null)
     * @param configDescriptions the configuration descriptions that should be applied (must not be null or empty).
     * @return the normalized configuration or null if given configuration was null
     * @throws IllegalArgumentExcetpion if given config description is null
     */
    public static Map<String, Object> normalizeTypes(Map<String, Object> configuration,
            List<ConfigDescription> configDescriptions) {
        if (configDescriptions == null || configDescriptions.isEmpty()) {
            throw new IllegalArgumentException("Config description must not be null.");
        }

        if (configuration == null) {
            return null;
        }

        Map<String, Object> convertedConfiguration = new HashMap<>();

        Map<String, ConfigDescriptionParameter> configParams = new HashMap<>();
        for (int i = configDescriptions.size() - 1; i >= 0; i--) {
            configParams.putAll(configDescriptions.get(i).toParametersMap());
        }
        for (Entry<String, ?> parameter : configuration.entrySet()) {
            String name = parameter.getKey();
            Object value = parameter.getValue();
            if (!isOSGiConfigParameter(name)) {
                ConfigDescriptionParameter configDescriptionParameter = configParams.get(name);
                convertedConfiguration.put(name, normalizeType(value, configDescriptionParameter));
            }
        }
        return convertedConfiguration;
    }

    /**
     * Normalizes the type of the parameter to the one allowed for configurations.
     *
     * The conversion is performed 'best-effort' (e.g. "3" will always end up being a BigDecimal, never a String).
     * Use {@link #normalizeType(Object, ConfigDescriptionParameter)} to make sure your field type ends up as intended.
     *
     * @param value the value to return as normalized type
     * @return corresponding value as a valid type
     */
    public static Object normalizeType(Object value) {
        return normalizeType(value, null);
    }

    /**
     * Normalizes a collection.
     *
     * @param collection the collection that entries should be normalized
     * @return a collection that contains the normalized entries
     * @throws IllegalArgumentException if the type of the normalized values differ or an invalid type has been given
     */
    private static Collection<Object> normalizeCollection(Collection<?> collection) throws IllegalArgumentException {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Object> lst = new ArrayList<>(collection.size());
            for (final Object it : collection) {
                final Object normalized = normalizeType(it, null);
                lst.add(normalized);
                if (normalized.getClass() != lst.get(0).getClass()) {
                    throw new IllegalArgumentException(
                            "Invalid configuration property. Heterogeneous collection value!");
                }
            }
            return lst;
        }
    }

    /**
     * We do not want to handle or try to normalize OSGi provided configuration parameters
     *
     * @param name The configuration parameter name
     */
    private static boolean isOSGiConfigParameter(String name) {
        return Constants.OBJECTCLASS.equals(name) || ComponentConstants.COMPONENT_NAME.equals(name)
                || ComponentConstants.COMPONENT_ID.equals(name);
    }
}
