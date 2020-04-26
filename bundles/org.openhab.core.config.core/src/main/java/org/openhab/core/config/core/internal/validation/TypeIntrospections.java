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
package org.openhab.core.config.core.internal.validation;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * The {@link TypeIntrospections} provides a corresponding {@link TypeIntrospection} for each config description
 * parameter type.
 *
 * @author Thomas Höfer - Initial contribution
 */
final class TypeIntrospections {

    private static final Map<Type, TypeIntrospection> INTROSPECTIONS = Collections.unmodifiableMap(Stream
            .of(new SimpleEntry<>(Type.BOOLEAN, new BooleanIntrospection()),
                    new SimpleEntry<>(Type.TEXT, new StringIntrospection()),
                    new SimpleEntry<>(Type.INTEGER, new IntegerIntrospection()),
                    new SimpleEntry<>(Type.DECIMAL, new FloatIntrospection()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

    private TypeIntrospections() {
        super();
    }

    /**
     * Returns the corresponding {@link TypeIntrospection} for the given type.
     *
     * @param type the type for which the {@link TypeIntrospection} is to be returned
     *
     * @return the {@link TypeIntrospection} for the given type
     *
     * @throws IllegalArgumentException if no {@link TypeIntrospection} was found for the given type
     */
    static TypeIntrospection get(Type type) {
        TypeIntrospection typeIntrospection = INTROSPECTIONS.get(type);
        if (typeIntrospection == null) {
            throw new IllegalArgumentException("There is no type introspection for type " + type);
        }
        return typeIntrospection;
    }

    /**
     * The {@link TypeIntrospection} provides operations to introspect the actual value for a configuration description
     * parameter.
     */
    abstract static class TypeIntrospection {

        private final Class<?> clazz;
        private final MessageKey minViolationMessageKey;
        private final MessageKey maxViolationMessageKey;

        private TypeIntrospection(Class<?> clazz) {
            this(clazz, null, null);
        }

        private TypeIntrospection(Class<?> clazz, MessageKey minViolationMessageKey,
                MessageKey maxViolationMessageKey) {
            this.clazz = clazz;
            this.minViolationMessageKey = minViolationMessageKey;
            this.maxViolationMessageKey = maxViolationMessageKey;
        }

        /**
         * Returns true, if the given value is less than the given min attribute, otherwise false.
         *
         * @param value the corresponding value
         * @param min the value of the min attribute
         *
         * @return true, if the given value is less than the given min attribute, otherwise false
         */
        boolean isMinViolated(Object value, BigDecimal min) {
            if (min == null) {
                return false;
            }
            final BigDecimal bd;
            if (isBigDecimalInstance(value)) {
                bd = (BigDecimal) value;
            } else {
                bd = new BigDecimal(value.toString());
            }
            return bd.compareTo(min) < 0;
        }

        /**
         * Returns true, if the given value is greater than the given max attribute, otherwise false.
         *
         * @param value the corresponding value
         * @param max the value of the max attribute
         *
         * @return true, if the given value is greater than the given max attribute, otherwise false
         */
        boolean isMaxViolated(Object value, BigDecimal max) {
            if (max == null) {
                return false;
            }
            final BigDecimal bd;
            if (isBigDecimalInstance(value)) {
                bd = (BigDecimal) value;
            } else {
                bd = new BigDecimal(value.toString());
            }
            return bd.compareTo(max) > 0;
        }

        /**
         * Returns true, if the given value can be assigned to the type of this introspection, otherwise false.
         *
         * @param value the corresponding value
         *
         * @return true, if the given value can be assigned to the type of this introspection, otherwise false
         */
        boolean isAssignable(Object value) {
            return clazz.isAssignableFrom(value.getClass());
        }

        /**
         * Returns true, if the given value is a big decimal, otherwise false.
         *
         * @param value the value to be analyzed
         *
         * @return true, if the given value is a big decimal, otherwise false
         */
        final boolean isBigDecimalInstance(Object value) {
            return value instanceof BigDecimal;
        }

        /**
         * Returns the corresponding {@link MessageKey} for the min attribute violation.
         *
         * @return the corresponding {@link MessageKey} for the min attribute violation
         */
        final MessageKey getMinViolationMessageKey() {
            return minViolationMessageKey;
        }

        /**
         * Returns the corresponding {@link MessageKey} for the max attribute violation.
         *
         * @return the corresponding {@link MessageKey} for the max attribute violation
         */
        final MessageKey getMaxViolationMessageKey() {
            return maxViolationMessageKey;
        }
    }

    private static final class BooleanIntrospection extends TypeIntrospection {

        private BooleanIntrospection() {
            super(Boolean.class);
        }

        @Override
        boolean isMinViolated(Object value, BigDecimal min) {
            throw new UnsupportedOperationException("Min attribute not supported for boolean parameter.");
        }

        @Override
        boolean isMaxViolated(Object value, BigDecimal max) {
            throw new UnsupportedOperationException("Max attribute not supported for boolean parameter.");
        }
    }

    private static final class FloatIntrospection extends TypeIntrospection {

        private FloatIntrospection() {
            super(Float.class, MessageKey.MIN_VALUE_NUMERIC_VIOLATED, MessageKey.MAX_VALUE_NUMERIC_VIOLATED);
        }

        @Override
        boolean isAssignable(Object value) {
            if (!super.isAssignable(value)) {
                return isBigDecimalInstance(value);
            }
            return true;
        }
    }

    private static final class IntegerIntrospection extends TypeIntrospection {

        private IntegerIntrospection() {
            super(Integer.class, MessageKey.MIN_VALUE_NUMERIC_VIOLATED, MessageKey.MAX_VALUE_NUMERIC_VIOLATED);
        }

        @Override
        boolean isAssignable(Object value) {
            if (!super.isAssignable(value)) {
                return isBigDecimalInstance(value);
            }
            return true;
        }
    }

    private static final class StringIntrospection extends TypeIntrospection {

        private StringIntrospection() {
            super(String.class, MessageKey.MIN_VALUE_TXT_VIOLATED, MessageKey.MAX_VALUE_TXT_VIOLATED);
        }

        @Override
        boolean isMinViolated(Object value, BigDecimal min) {
            if (min == null) {
                return false;
            }
            return ((String) value).length() < min.intValueExact();
        }

        @Override
        boolean isMaxViolated(Object value, BigDecimal max) {
            if (max == null) {
                return false;
            }
            return ((String) value).length() > max.intValueExact();
        }
    }
}
