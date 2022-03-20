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

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.openhab.core.types.util.UnitUtils;

/**
 * The {@link PersistenceThresholdFilterTest} contains tests for {@link PersistenceThresholdFilter}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceThresholdFilterTest {
    private static final String ITEM_NAME_1 = "itemName1";
    private static final String ITEM_NAME_2 = "itemName2";

    @Test
    public void differentItemSameValue() {
        filterTest(ITEM_NAME_2, DecimalType.ZERO, DecimalType.ZERO, "", true);
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    public void filterTest(State state1, State state2, String unit, boolean expected) {
        filterTest(ITEM_NAME_1, state1, state2, unit, expected);
    }

    private static Stream<Arguments> argumentProvider() {
        return Stream.of(//
                // same item, same value -> false
                Arguments.of(DecimalType.ZERO, DecimalType.ZERO, "", false),
                // plain decimal, below threshold, absolute
                Arguments.of(DecimalType.ZERO, DecimalType.valueOf("5"), "", false),
                // plain decimal, above threshold, absolute
                Arguments.of(DecimalType.ZERO, DecimalType.valueOf("15"), "", true),
                // plain decimal, below threshold, relative
                Arguments.of(DecimalType.valueOf("10.0"), DecimalType.valueOf("9.5"), "%", false),
                // plain decimal, above threshold, relative
                Arguments.of(DecimalType.valueOf("10.0"), DecimalType.valueOf("11.5"), "%", true),
                // quantity type, below threshold, relative
                Arguments.of(new QuantityType<>("15 A"), new QuantityType<>("14000 mA"), "%", false),
                // quantity type, above threshold, relative
                Arguments.of(new QuantityType<>("2000 mbar"), new QuantityType<>("2.6 bar"), "%", true),
                // quantity type, below threshold, absolute, no unit
                Arguments.of(new QuantityType<>("100 K"), new QuantityType<>("105 K"), "", false),
                // quantity type, above threshold, absolute, no unit
                Arguments.of(new QuantityType<>("20 V"), new QuantityType<>("9000 mV"), "", true),
                // quantity type, below threshold, absolute, with unit
                Arguments.of(new QuantityType<>("10 m"), new QuantityType<>("10.002 m"), "mm", false),
                // quantity type, above threshold, absolute, with unit
                Arguments.of(new QuantityType<>("-10 °C"), new QuantityType<>("5 °C"), "K", true));
    }

    private void filterTest(String item2name, State state1, State state2, String unit, boolean expected) {
        String itemType = "Number";
        if (state1 instanceof QuantityType<?> q) {
            itemType += ":" + UnitUtils.getDimensionName(q.getUnit());
        }

        NumberItem item1 = new NumberItem(itemType, PersistenceThresholdFilterTest.ITEM_NAME_1);
        NumberItem item2 = new NumberItem(itemType, item2name);

        item1.setState(state1);
        item2.setState(state2);

        PersistenceFilter filter = new PersistenceThresholdFilter("test", BigDecimal.TEN, unit);

        assertThat(filter.apply(item1), is(true));
        filter.persisted(item1);
        assertThat(filter.apply(item2), is(expected));
    }
}
