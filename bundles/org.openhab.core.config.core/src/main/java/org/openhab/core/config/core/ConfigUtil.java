/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
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
 * @author Florian Hotze - Add support for environment variable substitution in config values
 */
@NonNullByDefault
public class ConfigUtil {

    private static final Pattern DEFAULT_LIST_SPLITTER = Pattern.compile("(?<!\\\\),");
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{ENV:([^}]+)}");

    private static EnvProvider envProvider = System::getenv;

    /**
     * Setter for envProvider to allow overwriting it in tests.
     * 
     * @param provider the env provider to use for resolving environment variables
     */
    protected static void setEnvProvider(EnvProvider provider) {
        envProvider = provider;
    }

    /**
     * Maps the provided (default) value of the given {@link ConfigDescriptionParameter} to the corresponding Java type.
     *
     * In case the provided (default) value is supposed to be a number and cannot be converted into the target type
     * correctly, this method will return <code>null</code> while logging a warning.
     *
     * @param parameter the {@link ConfigDescriptionParameter} which default value should be normalized (must not be
     *            null)
     * @return the default value as the corresponding Java type, or
     *         a <code>List</code> of the corresponding Java type if the parameter contains multiple values.
     *         Returns <code>null</code> if the value could not be converted.
     */
    public static @Nullable Object getDefaultValueAsCorrectType(ConfigDescriptionParameter parameter) {
        if (parameter.isMultiple()) {
            if (parameter.getDefault() == null) {
                return null;
            }
            List<Object> defaultValues = Stream.of(DEFAULT_LIST_SPLITTER.split(parameter.getDefault())) //
                    .map(value -> value.trim().replace("\\,", ",")) //
                    .filter(not(String::isEmpty)) //
                    .map(value -> getDefaultValueAsCorrectType(parameter.getName(), parameter.getType(), value)) //
                    .filter(Objects::nonNull) //
                    .toList();

            Integer multipleLimit = parameter.getMultipleLimit();
            if (multipleLimit != null && defaultValues.size() > multipleLimit.intValue()) {
                LoggerFactory.getLogger(ConfigUtil.class).warn(
                        "Number of default values ({}) for parameter '{}' is greater than multiple limit ({})",
                        defaultValues.size(), parameter.getName(), multipleLimit);
            }
            return defaultValues;
        } else {
            return getDefaultValueAsCorrectType(parameter.getName(), parameter.getType(), parameter.getDefault());
        }
    }

    static @Nullable Object getDefaultValueAsCorrectType(String parameterName, Type parameterType,
            @Nullable String defaultValue) {
        if (defaultValue == null) {
            return null;
        }
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
                        return value.setScale(0, RoundingMode.DOWN);
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
                    Object value = ConfigUtil.getDefaultValueAsCorrectType(parameter);
                    if (value != null) {
                        configuration.put(parameter.getName(), value);
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
    public static Map<String, @Nullable Object> normalizeTypes(Map<String, @Nullable Object> configuration) {
        Map<String, @Nullable Object> convertedConfiguration = new HashMap<>(configuration.size());
        for (Entry<String, @Nullable Object> parameter : configuration.entrySet()) {
            String name = parameter.getKey();
            Object value = parameter.getValue();
            if (!isOSGiConfigParameter(name)) {
                convertedConfiguration.put(name, value == null ? null : normalizeType(value, null));
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
     * @throws IllegalArgumentException if an invalid type has been given
     */
    @Nullable
    public static Object normalizeType(@Nullable Object value,
            @Nullable ConfigDescriptionParameter configDescriptionParameter) {
        if (configDescriptionParameter != null) {
            Normalizer normalizer = NormalizerFactory.getNormalizer(configDescriptionParameter);
            return normalizer.normalize(value);
        } else if (value instanceof Boolean) {
            return NormalizerFactory.getNormalizer(Type.BOOLEAN).normalize(value);
        } else if (value instanceof String) {
            return NormalizerFactory.getNormalizer(Type.TEXT).normalize(value);
        } else if (value instanceof Number) {
            return NormalizerFactory.getNormalizer(Type.DECIMAL).normalize(value);
        } else if (value instanceof Collection collection) {
            return normalizeCollection(collection);
        }
        throw new IllegalArgumentException(
                "Invalid type '{%s}' of configuration value!".formatted(value.getClass().getCanonicalName()));
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
     * @param configuration the configuration to be normalized
     * @param configDescriptions the configuration descriptions that should be applied (must not be empty).
     * @return the normalized configuration or null if given configuration was null
     * @throws IllegalArgumentException if given config description is null
     */
    public static Map<String, @Nullable Object> normalizeTypes(Map<String, @Nullable Object> configuration,
            List<ConfigDescription> configDescriptions) {
        if (configDescriptions.isEmpty()) {
            throw new IllegalArgumentException("Config description must not be empty.");
        }

        Map<String, @Nullable Object> convertedConfiguration = new HashMap<>();

        Map<String, ConfigDescriptionParameter> configParams = new HashMap<>();
        for (int i = configDescriptions.size() - 1; i >= 0; i--) {
            configParams.putAll(configDescriptions.get(i).toParametersMap());
        }
        for (Entry<String, @Nullable Object> parameter : configuration.entrySet()) {
            String name = parameter.getKey();
            @Nullable
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
    public static @Nullable Object normalizeType(@Nullable Object value) {
        return normalizeType(value, null);
    }

    /**
     * Normalizes a collection.
     *
     * @param collection the collection that entries should be normalized
     * @return a collection that contains the normalized entries
     * @throws IllegalArgumentException if the type of the normalized values differ or an invalid type has been given
     */
    private static Collection<Object> normalizeCollection(Collection<@NonNull ?> collection)
            throws IllegalArgumentException {
        if (collection.isEmpty()) {
            return List.of();
        } else {
            final List<Object> lst = new ArrayList<>(collection.size());
            for (final Object it : collection) {
                final Object normalized = normalizeType(it, null);
                if (normalized == null) {
                    continue;
                }

                lst.add(normalized);
                if (normalized.getClass() != lst.getFirst().getClass()) {
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

    /**
     * Checks a string value for the variable patterns and resolves referenced variables.
     *
     * <p>
     * Note: At the moment, only environment variables are supported.
     * If no variable is referenced, the string value is returned as-is.
     * If a referenced variable fails to resolve, a {@link IllegalArgumentException} is thrown.
     *
     * @param value the value to resolve
     * @return the resolved value
     * @throws IllegalArgumentException when a variable fails to resolve
     */
    private static String resolveVariables(String value) throws IllegalArgumentException {
        final Matcher matcher = ENV_PATTERN.matcher(value);

        return matcher.replaceAll(matchResult -> {
            final String envVarName = matchResult.group(1);
            final @Nullable String envVarValue = envProvider.get(envVarName);

            if (envVarValue == null) {
                throw new IllegalArgumentException(
                        "Could not resolve environment variable '{%s}'!".formatted(envVarName));
            }

            // Safely escape the replacement string so '$' and '\' are treated as literals
            return Matcher.quoteReplacement(envVarValue);
        });
    }

    /**
     * Resolves variables in the given value by replacing the variable patterns through the variable values.
     *
     * <p>
     * The following rules are applied:
     * <ol>
     * <li>If the given value is a string, it is checked for variable patterns and referenced variables are
     * resolved.</li>
     * <li>If a variable fails to resolve, a {@link IllegalArgumentException} is thrown.</li>
     * <li>If the value is a collection, this method is called for each element.</li>
     * <li>If the value is neither a string nor a collection, it is returned as-is.</li>
     * </ol>
     *
     * @param value the value to resolve
     * @return the resolved value
     * @throws IllegalArgumentException when a variable fails to resolve
     */
    public static Object resolveVariables(Object value) throws IllegalArgumentException {
        if (value instanceof String stringValue) {
            return resolveVariables(stringValue);
        } else if (value instanceof Collection<?> collectionValue) {
            final List<Object> entry = new ArrayList<>(collectionValue.size());
            for (final Object it : collectionValue) {
                final Object resolved = resolveVariables(it);
                entry.add(resolved);
            }
            return entry;
        }
        return value;
    }

    /**
     * Resolve variables in the given {@link Configuration}.
     *
     * @param configuration the configuration to resolve variables in
     * @return the resolved configuration
     * @throws IllegalArgumentException when a variable fails to resolve
     */
    public static Configuration resolveVariables(Configuration configuration) throws IllegalArgumentException {
        final Map<String, @Nullable Object> rawProperties = configuration.getProperties();
        final Map<String, @Nullable Object> resolvedProperties = new HashMap<>();
        for (final Entry<String, @Nullable Object> entry : rawProperties.entrySet()) {
            final @Nullable Object value = entry.getValue();
            if (value != null) {
                final Object resolved = resolveVariables(value);
                resolvedProperties.put(entry.getKey(), resolved);
            } else {
                resolvedProperties.put(entry.getKey(), null);
            }
        }
        return new Configuration(resolvedProperties);
    }

    /**
     * A provider for environment variables.
     */
    @FunctionalInterface
    protected interface EnvProvider {
        @Nullable
        String get(String name);
    }
}
