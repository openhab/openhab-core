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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.State;

/**
 * The {@link PersistenceEqualsFilterTest} contains tests for {@link PersistenceEqualsFilter}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistenceEqualsFilterTest {
    private static final String ITEM_NAME = "itemName";

    private @NonNullByDefault({}) @Mock GenericItem item;

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void equalsFilterTest(State state, Collection<String> values, boolean expected) {
        when(item.getState()).thenReturn(state);

        PersistenceEqualsFilter filter = new PersistenceEqualsFilter("filter", values, false);
        assertThat(filter.apply(item), is(expected));
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void notEqualsFilterTest(State state, Collection<String> values, boolean expected) {
        when(item.getState()).thenReturn(state);

        PersistenceEqualsFilter filter = new PersistenceEqualsFilter("filter", values, true);
        assertThat(filter.apply(item), is(not(expected)));
    }

    private static Stream<Arguments> argumentProvider() {
        return Stream.of(//
                // item state, values, result
                Arguments.of(new StringType("value1"), List.of("value1", "value2"), true),
                Arguments.of(new StringType("value3"), List.of("value1", "value2"), false),
                Arguments.of(new DecimalType(5), List.of("3", "5", "9"), true),
                Arguments.of(new DecimalType(7), List.of("3", "5", "9"), false),
                Arguments.of(new QuantityType<>(10, SIUnits.CELSIUS), List.of("5 °C", "10 °C", "15 °C"), true),
                Arguments.of(new QuantityType<>(20, SIUnits.CELSIUS), List.of("5 °C", "10 °C", "15 °C"), false),
                Arguments.of(OnOffType.ON, List.of("ON", "UNDEF", "NULL"), true),
                Arguments.of(OnOffType.OFF, List.of("ON", "UNDEF", "NULL"), false));
    }
}
