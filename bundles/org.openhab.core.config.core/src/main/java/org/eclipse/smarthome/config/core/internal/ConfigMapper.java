/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.core.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map an OSGi configuration map {@code Map<String, Object>} to an individual configuration bean.
 *
 * @author David Graeff - initial contribution
 *
 */
public class ConfigMapper {
    private static final transient Logger logger = LoggerFactory.getLogger(ConfigMapper.class);

    /**
     * Use this method to automatically map a configuration collection to a Configuration holder object. A common
     * use-case would be within a service. Usage example:
     *
     * <pre>
     * {@code
     *   public void modified(Map<String, Object> properties) {
     *     YourConfig config = ConfigMapper.as(properties, YourConfig.class);
     *   }
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
    public static <T> @Nullable T as(Map<String, Object> properties, Class<T> configurationClass) {
        T configuration = null;
        try {
            configuration = configurationClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            return null;
        }

        List<Field> fields = getAllFields(configurationClass);
        for (Field field : fields) {
            // Don't try to write to final fields and ignore transient fields
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            String fieldName = field.getName();
            String configKey = fieldName;
            Class<?> type = field.getType();

            Object value = properties.get(configKey);

            // Consider RequiredField annotations
            if (value == null) {
                logger.trace("Skipping field '{}', because config has no entry for {}", fieldName, configKey);
                continue;
            }

            // Allows to have List<int>, List<Double>, List<String> etc
            if (value instanceof Collection) {
                Collection<?> c = (Collection<?>) value;
                Class<?> innerClass = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];
                final List<Object> lst = new ArrayList<>(c.size());
                for (final Object it : c) {
                    final Object normalized = objectConvert(it, innerClass);
                    lst.add(normalized);
                }
                value = lst;
            }

            try {
                value = objectConvert(value, type);
                logger.trace("Setting value ({}) {} to field '{}' in configuration class {}", type.getSimpleName(),
                        value, fieldName, configurationClass.getName());
                FieldUtils.writeField(configuration, fieldName, value, true);

            } catch (Exception ex) {
                logger.warn("Could not set field value for field '{}': {}", fieldName, ex.getMessage(), ex);
            }
        }

        return configuration;
    }

    /**
     * Return fields of the given class as well as all super classes.
     *
     * @param clazz The class
     * @return A list of Field objects
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();

        Class<?> currentClass = clazz;
        while (currentClass != null) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    private static Object objectConvert(Object value, Class<?> type) {
        Object result = value;
        // Handle the conversion case of BigDecimal to Float,Double,Long,Integer and the respective
        // primitive types
        String typeName = type.getSimpleName();
        if (value instanceof BigDecimal && !type.equals(BigDecimal.class)) {
            BigDecimal bdValue = (BigDecimal) value;
            if (type.equals(Float.class) || typeName.equals("float")) {
                result = bdValue.floatValue();
            } else if (type.equals(Double.class) || typeName.equals("double")) {
                result = bdValue.doubleValue();
            } else if (type.equals(Long.class) || typeName.equals("long")) {
                result = bdValue.longValue();
            } else if (type.equals(Integer.class) || typeName.equals("int")) {
                result = bdValue.intValue();
            }
        } else
        // Handle the conversion case of String to Float,Double,Long,Integer,BigDecimal,Boolean and the respective
        // primitive types
        if (value instanceof String && !type.equals(String.class)) {
            String bdValue = (String) value;
            if (type.equals(Float.class) || typeName.equals("float")) {
                result = Float.valueOf(bdValue);
            } else if (type.equals(Double.class) || typeName.equals("double")) {
                result = Double.valueOf(bdValue);
            } else if (type.equals(Long.class) || typeName.equals("long")) {
                result = Long.valueOf(bdValue);
            } else if (type.equals(BigDecimal.class)) {
                result = new BigDecimal(bdValue);
            } else if (type.equals(Integer.class) || typeName.equals("int")) {
                result = Integer.valueOf(bdValue);
            } else if (type.equals(Boolean.class) || typeName.equals("boolean")) {
                result = Boolean.valueOf(bdValue);
            } else if (type.isEnum()) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                @SuppressWarnings({ "unchecked" })
                final Enum<?> enumvalue = Enum.valueOf(enumType, value.toString());
                result = enumvalue;
            } else if (Collection.class.isAssignableFrom(type)) {
                result = Collections.singletonList(value);
            }
        }
        return result;
    }

}
