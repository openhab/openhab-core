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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map an OSGi configuration map {@code Map<String, Object>} or type-less value to an individual configuration bean or
 * typed value.
 *
 * @author David Graeff - Initial contribution
 * @author Jan N. Klug - Extended and refactored to an exposed utility class
 *
 */
@NonNullByDefault
public final class ConfigParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParser.class);

    private static final Map<String, Class<?>> WRAPPER_CLASSES_MAP = Map.of(//
            "float", Float.class, //
            "double", Double.class, //
            "long", Long.class, //
            "int", Integer.class, //
            "short", Short.class, //
            "byte", Byte.class, //
            "boolean", Boolean.class);

    private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = Map.of( //
            boolean.class, false, //
            byte.class, (byte) 0, //
            short.class, (short) 0, //
            int.class, 0, //
            long.class, 0L, //
            float.class, 0f, //
            double.class, 0d, //
            char.class, '\0');

    private ConfigParser() {
        // prevent instantiation
    }

    /**
     * Use this method to automatically map a configuration collection to a Configuration holder object. A common
     * use-case would be within a service. Usage example:
     *
     * <pre>
     * {@code
     * public void modified(Map<String, Object> properties) {
     *     YourConfig config = ConfigParser.configurationAs(properties, YourConfig.class);
     * }
     * }
     * </pre>
     *
     *
     * @param properties The configuration map.
     * @param configurationClass The configuration holder class. An instance of this will be created so make sure that
     *            a default constructor is available.
     * @return The configuration holder object. All fields that matched a configuration option are set. If a required
     *         field is not set, null is returned.
     */
    public static <T> @Nullable T configurationAs(Map<String, @Nullable Object> properties,
            Class<T> configurationClass) {
        try {
            if (configurationClass.isRecord()) {
                return constructRecord(properties, configurationClass);
            } else {
                return constructClass(properties, configurationClass);
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            LOGGER.warn("Could not create configuration instance of '{}' with properties {}: {}",
                    configurationClass.getName(), properties, e.getMessage(), e);
            return null;
        }
    }

    private static <T> @Nullable T constructClass(Map<String, @Nullable Object> properties, Class<T> configurationClass)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        T configuration = configurationClass.getConstructor().newInstance();

        for (Field field : getAllFields(configurationClass)) {
            // Don't try to write to final fields and ignore transient fields
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();
            Object rawValue = properties.get(fieldName);
            // Consider RequiredField annotations
            if (rawValue == null) {
                LOGGER.trace("Skipping field '{}', because config has no entry for it", fieldName);
                continue;
            }

            Object value = convertValue(rawValue, field.getType(), field.getGenericType(), fieldName);

            if (value != null) {
                try {
                    LOGGER.trace("Setting value ({}) {} to field '{}' in configuration class {}",
                            field.getType().getSimpleName(), value, fieldName, configurationClass.getName());
                    field.setAccessible(true);
                    field.set(configuration, value);
                } catch (IllegalAccessException e) {
                    LOGGER.warn("Could not set field value for field '{}': {}", fieldName, e.getMessage(), e);
                }
            }
        }

        return configuration;
    }

    private static <T> @Nullable T constructRecord(Map<String, @Nullable Object> properties,
            Class<T> configurationClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        Constructor<?> constructor = configurationClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);

        RecordComponent[] components = configurationClass.getRecordComponents();
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];

            String name = component.getName();
            Object rawValue = properties.get(name);

            Object value = convertValue(rawValue, component.getType(), component.getGenericType(), name);

            if (value == null && component.getType().isPrimitive()) {
                value = defaultValue(component.getType());
            }

            args[i] = value;
        }

        return configurationClass.cast(constructor.newInstance(args));
    }

    private static @Nullable Object convertValue(Object value, Class<?> type, @Nullable Type genericType,
            String fieldName) {
        // Allows to have List<int>, List<Double>, List<String> etc (and the corresponding Set<?>)
        if (value instanceof Collection<?> valueCollection) {
            Collection<Object> collection = List.class.isAssignableFrom(type) ? new ArrayList<>()
                    : Set.class.isAssignableFrom(type) ? new HashSet<>() : null;

            if (collection != null && genericType instanceof ParameterizedType parameterizedType) {
                Type innerType = parameterizedType.getActualTypeArguments()[0];

                if (innerType instanceof Class<?> innerClass) {
                    valueCollection.stream().map(it -> valueAs(it, innerClass)).filter(Objects::nonNull)
                            .forEach(collection::add);
                }
                return collection;
            } else {
                LOGGER.warn("Skipping field '{}', only List and Set is supported as target Collection", fieldName);
                return null;
            }
        }

        Object converted = valueAs(value, type);
        if (converted == null) {
            LOGGER.warn("Could not set value for field '{}' because conversion failed. Check your configuration value.",
                    fieldName);
        }

        return converted;
    }

    /**
     * Return fields of the given class as well as all super classes.
     *
     * @param clazz The class
     * @return A list of Field objects
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> superclazz = clazz; superclazz != null; superclazz = superclazz.getSuperclass()) {
            fields.addAll(Arrays.asList(superclazz.getDeclaredFields()));
        }
        return fields;
    }

    private static Object defaultValue(Class<?> type) {
        Object value = PRIMITIVE_DEFAULTS.get(type);
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }

    /**
     * Convert a value to a given type or return default value
     *
     * @param value input value or String representation of that value
     * @param type desired target class
     * @param defaultValue default value to be used if conversion fails or input value is null
     * @return the converted value or the default value if value is null or conversion fails
     */
    public static <T> T valueAsOrElse(@Nullable Object value, Class<T> type, T defaultValue) {
        return Objects.requireNonNullElse(valueAs(value, type), defaultValue);
    }

    /**
     * Convert a value to a given type
     *
     * @param value input value or String representation of that value
     * @param type desired target class
     * @return the converted value or null if conversion fails or input value is null
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> @Nullable T valueAs(@Nullable Object value, Class<T> type) {
        if (value == null || type.isAssignableFrom(value.getClass())) {
            // exit early if value is null or type is already compatible
            return (T) value;
        }

        // make sure primitives are converted to their respective wrapper class
        Class<?> typeClass = WRAPPER_CLASSES_MAP.getOrDefault(type.getSimpleName(), type);

        Object result = value;
        // Handle the conversion case of Number to Float,Double,Long,Integer,Short,Byte,BigDecimal
        if (value instanceof Number number) {
            if (Float.class.equals(typeClass)) {
                result = number.floatValue();
            } else if (Double.class.equals(typeClass)) {
                result = number.doubleValue();
            } else if (Long.class.equals(typeClass)) {
                result = number.longValue();
            } else if (Integer.class.equals(typeClass)) {
                result = number.intValue();
            } else if (Short.class.equals(typeClass)) {
                result = number.shortValue();
            } else if (Byte.class.equals(typeClass)) {
                result = number.byteValue();
            } else if (BigDecimal.class.equals(typeClass)) {
                result = new BigDecimal(number.toString());
            }
        } else if (value instanceof String strValue && !String.class.equals(typeClass)) {
            // Handle the conversion case of String to Float,Double,Long,Integer,BigDecimal,Boolean
            if (Float.class.equals(typeClass)) {
                result = Float.valueOf(strValue);
            } else if (Double.class.equals(typeClass)) {
                result = Double.valueOf(strValue);
            } else if (Long.class.equals(typeClass)) {
                result = Long.valueOf(strValue);
            } else if (Integer.class.equals(typeClass)) {
                result = Integer.valueOf(strValue);
            } else if (Short.class.equals(typeClass)) {
                result = Short.valueOf(strValue);
            } else if (Byte.class.equals(typeClass)) {
                result = Byte.valueOf(strValue);
            } else if (BigDecimal.class.equals(typeClass)) {
                result = new BigDecimal(strValue);
            } else if (Boolean.class.equals(typeClass)) {
                result = Boolean.valueOf(strValue);
            } else if (type.isEnum()) {
                final Class<? extends Enum> enumType = (Class<? extends Enum>) typeClass;
                try {
                    result = Enum.valueOf(enumType, value.toString());
                } catch (IllegalArgumentException e) {
                    result = null;
                }
            } else if (Set.class.isAssignableFrom(typeClass)) {
                result = Set.of(value);
            } else if (Collection.class.isAssignableFrom(typeClass)) {
                result = List.of(value);
            }
        }

        if (result != null && typeClass.isAssignableFrom(result.getClass())) {
            return (T) result;
        }

        LOGGER.warn("Conversion of value '{}' with type '{}' to '{}' failed. Returning null", value, value.getClass(),
                type);

        return null;
    }
}
