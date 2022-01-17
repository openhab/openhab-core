/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.util.ConditionBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.State;

/**
 * Basic unit tests for {@link ItemStateConditionHandler}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class ItemStateConditionHandlerTest {

    public static class ParameterSet {
        public final String comparisonState;
        public final State itemState;
        public final boolean expectedResult;

        public ParameterSet(String comparisonState, State itemState, boolean expectedResult) {
            this.comparisonState = comparisonState;
            this.itemState = itemState;
            this.expectedResult = expectedResult;
        }
    }

    public static Collection<Object[]> equalsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("5", new DecimalType(23), false) }, //
                { new ParameterSet("5", new DecimalType(5), true) }, //
                { new ParameterSet("5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("5 °C", new DecimalType(5), false) }, //
                { new ParameterSet("0", new QuantityType<>(), true) }, //
                { new ParameterSet("5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5", new QuantityType<>(5, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT), true) }, //
                { new ParameterSet("32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) } });
    }

    public static Collection<Object[]> greaterThanParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("5", new DecimalType(23), true) }, //
                { new ParameterSet("5", new DecimalType(5), false) }, //
                { new ParameterSet("5 °C", new DecimalType(23), true) }, //
                { new ParameterSet("5 °C", new DecimalType(5), false) }, //
                { new ParameterSet("0", new QuantityType<>(), false) }, //
                { new ParameterSet("5", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5", new QuantityType<>(5, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(5, SIUnits.CELSIUS), false) } });
    }

    public static Collection<Object[]> greaterThanOrEqualsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("5", new DecimalType(23), true) }, //
                { new ParameterSet("5", new DecimalType(5), true) }, //
                { new ParameterSet("5", new DecimalType(4), false) }, //
                { new ParameterSet("5 °C", new DecimalType(23), true) }, //
                { new ParameterSet("5 °C", new DecimalType(5), true) }, //
                { new ParameterSet("5 °C", new DecimalType(4), false) }, //
                { new ParameterSet("0", new QuantityType<>(), true) }, //
                { new ParameterSet("5", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5", new QuantityType<>(4, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(4, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT), true) }, //
                { new ParameterSet("32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) } });
    }

    public static Collection<Object[]> lessThanParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("5", new DecimalType(23), false) }, //
                { new ParameterSet("5", new DecimalType(4), true) }, //
                { new ParameterSet("5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("5 °C", new DecimalType(4), true) }, //
                { new ParameterSet("0", new QuantityType<>(), false) }, //
                { new ParameterSet("5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(4, SIUnits.CELSIUS), true) } });
    }

    public static Collection<Object[]> lessThanOrEqualsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("5", new DecimalType(23), false) }, //
                { new ParameterSet("5", new DecimalType(5), true) }, //
                { new ParameterSet("5", new DecimalType(4), true) }, //
                { new ParameterSet("5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("5 °C", new DecimalType(5), true) }, //
                { new ParameterSet("5 °C", new DecimalType(4), true) }, //
                { new ParameterSet("0", new QuantityType<>(), true) }, //
                { new ParameterSet("5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("5 °C", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT), true) }, //
                { new ParameterSet("32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) } });
    }

    private static final String ITEM_NAME = "myItem";

    private final NumberItem item = new NumberItem(ITEM_NAME);

    private @NonNullByDefault({}) @Mock ItemRegistry mockItemRegistry;

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        when(mockItemRegistry.getItem(ITEM_NAME)).thenReturn(item);
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testEqualsCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler("=", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testNotEqualsCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler("!=", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertFalse(handler.isSatisfied(Map.of()));
        } else {
            assertTrue(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("greaterThanParameters")
    public void testGreaterThanCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler(">", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("greaterThanOrEqualsParameters")
    public void testGreaterThanOrEqualsCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler(">=", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("lessThanParameters")
    public void testLessThanCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler("<", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("lessThanOrEqualsParameters")
    public void testLessThanOrEqualsCondition(ParameterSet parameterSet) {
        ItemStateConditionHandler handler = initItemStateConditionHandler("<=", parameterSet.comparisonState);

        item.setState(parameterSet.itemState);
        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    private ItemStateConditionHandler initItemStateConditionHandler(String operator, String state) {
        Configuration configuration = new Configuration();
        configuration.put(ItemStateConditionHandler.ITEM_NAME, ITEM_NAME);
        configuration.put(ItemStateConditionHandler.OPERATOR, operator);
        configuration.put(ItemStateConditionHandler.STATE, state);
        ConditionBuilder builder = ConditionBuilder.create() //
                .withId("conditionId") //
                .withTypeUID(ItemStateConditionHandler.ITEM_STATE_CONDITION) //
                .withConfiguration(configuration);
        ItemStateConditionHandler handler = new ItemStateConditionHandler(builder.build());
        handler.setItemRegistry(mockItemRegistry);
        return handler;
    }
}
