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
package org.openhab.core.persistence.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;

/**
 * The {@link PersistenceIncludeFilterTest} contains tests for {@link PersistenceIncludeFilter}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistenceIncludeFilterTest {
    private static final String ITEM_NAME = "itemName";

    private @NonNullByDefault({}) @Mock GenericItem item;

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void includeFilterTest(State state, BigDecimal lower, BigDecimal upper, String unit, boolean expected) {
        when(item.getState()).thenReturn(state);

        PersistenceIncludeFilter filter = new PersistenceIncludeFilter("filter", lower, upper, unit, false);
        assertThat(filter.apply(item), is(expected));
    }

    @ParameterizedTest
    @MethodSource("notArgumentProvider")
    public void notIncludeFilterTest(State state, BigDecimal lower, BigDecimal upper, String unit, boolean expected) {
        when(item.getState()).thenReturn(state);

        PersistenceIncludeFilter filter = new PersistenceIncludeFilter("filter", lower, upper, unit, true);
        assertThat(filter.apply(item), is(expected));
    }

    private static Stream<Arguments> argumentProvider() {
        return Stream.of(//
                // item state, lower, upper, unit, result
                // QuantityType
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(17), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", true),
                Arguments.of(new QuantityType<>("10 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", false),
                Arguments.of(new QuantityType<>("20 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", false),
                Arguments.of(new QuantityType<>("0 °C"), BigDecimal.valueOf(270), BigDecimal.valueOf(275), "K", true),
                // invalid or missing units
                Arguments.of(new QuantityType<>("5 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("17 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("5 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", true),
                Arguments.of(new QuantityType<>("17 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", true),
                // DecimalType
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", true),
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(17), BigDecimal.valueOf(19), "", true),
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", true),
                Arguments.of(new DecimalType("10"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", false),
                Arguments.of(new DecimalType("20"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", false));
    }

    private static Stream<Arguments> notArgumentProvider() {
        return Stream.of(//
                // item state, lower, upper, unit, result
                // QuantityType
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", false),
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(17), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("17 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", true),
                Arguments.of(new QuantityType<>("10 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", true),
                Arguments.of(new QuantityType<>("20 °C"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "°C", true),
                Arguments.of(new QuantityType<>("0 °C"), BigDecimal.valueOf(270), BigDecimal.valueOf(275), "K", false),
                // invalid or missing units
                Arguments.of(new QuantityType<>("5 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("17 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "°C", true),
                Arguments.of(new QuantityType<>("5 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", true),
                Arguments.of(new QuantityType<>("17 kg"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", true),
                // DecimalType
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(14), BigDecimal.valueOf(19), "", false),
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(17), BigDecimal.valueOf(19), "", true),
                Arguments.of(new DecimalType("17"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", true),
                Arguments.of(new DecimalType("10"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", true),
                Arguments.of(new DecimalType("20"), BigDecimal.valueOf(14), BigDecimal.valueOf(17), "", true));
    }
}
