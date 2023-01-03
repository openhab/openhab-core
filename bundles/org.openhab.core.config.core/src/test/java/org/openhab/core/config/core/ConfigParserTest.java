/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The {@link ConfigParserTest} contains tests for the {@link ConfigParser}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ConfigParserTest {

    private static final List<TestParameter<?>> TEST_PARAMETERS = List.of( //
            // float/Float
            new TestParameter<>("7.5", float.class, 7.5f), //
            new TestParameter<>("-7.5", Float.class, -7.5f), //
            new TestParameter<>(-7.5, float.class, -7.5f), //
            new TestParameter<>(7.5, Float.class, 7.5f), //
            // double/Double
            new TestParameter<>("7.5", double.class, 7.5), //
            new TestParameter<>("-7.5", Double.class, -7.5), //
            new TestParameter<>(-7.5, double.class, -7.5), //
            new TestParameter<>(7.5, Double.class, 7.5), //
            // long/Long
            new TestParameter<>("1", long.class, 1L), //
            new TestParameter<>("-1", Long.class, -1L), //
            new TestParameter<>(-1, long.class, -1L), //
            new TestParameter<>(1, Long.class, 1L), //
            // int/Integer
            new TestParameter<>("1", int.class, 1), //
            new TestParameter<>("-1", Integer.class, -1), //
            new TestParameter<>(-1, int.class, -1), //
            new TestParameter<>(1, Integer.class, 1), //
            // short/Short
            new TestParameter<>("1", short.class, (short) 1), //
            new TestParameter<>("-1", Short.class, (short) -1), //
            new TestParameter<>(-1, short.class, (short) -1), //
            new TestParameter<>(1, Short.class, (short) 1), //
            // byte/Byte
            new TestParameter<>("1", byte.class, (byte) 1), //
            new TestParameter<>("-1", Byte.class, (byte) -1), //
            new TestParameter<>(-1, byte.class, (byte) -1), //
            new TestParameter<>(1, Byte.class, (byte) 1), //
            // boolean/Boolean
            new TestParameter<>("true", boolean.class, true), //
            new TestParameter<>("true", Boolean.class, true), //
            new TestParameter<>(false, boolean.class, false), //
            new TestParameter<>(false, Boolean.class, false), //
            // BigDecimal
            new TestParameter<>("7.5", BigDecimal.class, BigDecimal.valueOf(7.5)), //
            new TestParameter<>(BigDecimal.valueOf(-7.5), BigDecimal.class, BigDecimal.valueOf(-7.5)), //
            new TestParameter<>(1, BigDecimal.class, BigDecimal.ONE), //
            // String
            new TestParameter<>("foo", String.class, "foo"), //
            // Enum
            new TestParameter<>("ENUM1", TestEnum.class, TestEnum.ENUM1), //
            // List
            new TestParameter<>("1", List.class, List.of("1")), //
            new TestParameter<>(List.of(1, 2, 3), List.class, List.of(1, 2, 3)),
            // Set
            new TestParameter<>("1", Set.class, Set.of("1")), //
            new TestParameter<>(Set.of(1, 2, 3), Set.class, Set.of(1, 2, 3)), //
            // illegal conversion
            new TestParameter<>(1, Boolean.class, null), //
            // null input
            new TestParameter<>(null, Object.class, null) //
    );

    @SuppressWarnings("unused")
    private static Stream<TestParameter<?>> valueAsTest() {
        return TEST_PARAMETERS.stream();
    }

    @ParameterizedTest
    @MethodSource
    public void valueAsTest(TestParameter<?> parameter) {
        Object result = ConfigParser.valueAs(parameter.input, parameter.type);
        Assertions.assertEquals(parameter.result, result, "Failed equals: " + parameter);
    }

    @Test
    public void valueAsDefaultTest() {
        Object result = ConfigParser.valueAsOrElse(null, String.class, "foo");
        Assertions.assertEquals("foo", result);
    }

    private enum TestEnum {
        ENUM1
    }

    private static class TestParameter<T> {
        public final @Nullable Object input;
        public final Class<T> type;
        public final @Nullable T result;

        public TestParameter(@Nullable Object input, Class<T> type, @Nullable T result) {
            this.input = input;
            this.type = type;
            this.result = result;
        }

        @Override
        public String toString() {
            return "TestParameter{input=" + input + ", type=" + type + ", result=" + result + "}";
        }
    }
}
