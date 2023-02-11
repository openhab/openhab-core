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
package org.openhab.core.automation.internal.module.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.util.ConditionBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;

/**
 * Basic unit tests for {@link ItemStateConditionHandler}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemStateConditionHandlerTest extends JavaTest {

    public static class ParameterSet {
        public final Item item;
        public final String comparisonState;
        public final State itemState;
        public final boolean expectedResult;

        public ParameterSet(String itemType, String comparisonState, State itemState, boolean expectedResult) {
            switch (itemType) {
                case "Number":
                    item = new NumberItem(ITEM_NAME);
                    ((NumberItem) item).setState(itemState);
                    break;
                case "Number:Temperature":
                    item = new NumberItem("Number:Temperature", ITEM_NAME);
                    ((NumberItem) item).setState(itemState);
                    break;
                case "Dimmer":
                    item = new DimmerItem(ITEM_NAME);
                    ((DimmerItem) item).setState(itemState);
                    break;
                case "DateTime":
                    item = new DateTimeItem(ITEM_NAME);
                    ((DateTimeItem) item).setState(itemState);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            this.comparisonState = comparisonState;
            this.itemState = itemState;
            this.expectedResult = expectedResult;
        }
    }

    public static Collection<Object[]> equalsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("Number", "5", new DecimalType(23), false) }, //
                { new ParameterSet("Number", "5", new DecimalType(5), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new DecimalType(5), false) }, //
                { new ParameterSet("Number:Temperature", "0", new QuantityType<>(), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(5, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT),
                        true) }, //
                { new ParameterSet("Number:Temperature", "32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) } });
    }

    public static Collection<Object[]> greaterThanParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("Number", "5", new DecimalType(23), true) }, //
                { new ParameterSet("Number", "5", new DecimalType(5), false) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(23), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(5), false) }, //
                { new ParameterSet("Number:Temperature", "0", new QuantityType<>(), false) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(5, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(5, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Dimmer", "20", new PercentType(40), true) }, //
                { new ParameterSet("Dimmer", "20", new PercentType(20), false) }, //
                { new ParameterSet("DateTime", "-1H", new DateTimeType(), true) }, //
                { new ParameterSet("DateTime", "1D1M", new DateTimeType(), false) } });
    }

    public static Collection<Object[]> greaterThanOrEqualsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("Number", "5", new DecimalType(23), true) }, //
                { new ParameterSet("Number", "5", new DecimalType(5), true) }, //
                { new ParameterSet("Number", "5", new DecimalType(4), false) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(23), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(5), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(4), false) }, //
                { new ParameterSet("Number:Temperature", "0", new QuantityType<>(), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(4, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(23, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(4, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT),
                        true) }, //
                { new ParameterSet("Number:Temperature", "32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Dimmer", "20", new PercentType(40), true) }, //
                { new ParameterSet("Dimmer", "40", new PercentType(20), false) }, //
                { new ParameterSet("DateTime", "2000-01-01T12:05:00", new DateTimeType(), true) } });
    }

    public static Collection<Object[]> lessThanParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("Number", "5", new DecimalType(23), false) }, //
                { new ParameterSet("Number", "5", new DecimalType(4), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(4), true) }, //
                { new ParameterSet("Number:Temperature", "0", new QuantityType<>(), false) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Dimmer", "40", new PercentType(20), true) }, //
                { new ParameterSet("Dimmer", "20", new PercentType(20), false) }, //
                { new ParameterSet("DateTime", "-1D", new DateTimeType(), false) }, //
                { new ParameterSet("DateTime", "1D5M", new DateTimeType(), true) }, //
                { new ParameterSet("DateTime", "2050-01-01T12:05:00+01:00", new DateTimeType(), true) } });
    }

    public static Collection<Object[]> lessThanOrEqualsParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("Number", "5", new DecimalType(23), false) }, //
                { new ParameterSet("Number", "5", new DecimalType(5), true) }, //
                { new ParameterSet("Number", "5", new DecimalType(4), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(23), false) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(5), true) }, //
                { new ParameterSet("Number", "5 °C", new DecimalType(4), true) }, //
                { new ParameterSet("Number:Temperature", "0", new QuantityType<>(), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(23, SIUnits.CELSIUS), false) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(5, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "5 °C", new QuantityType<>(4, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Number:Temperature", "0 °C", new QuantityType<>(32, ImperialUnits.FAHRENHEIT),
                        true) }, //
                { new ParameterSet("Number:Temperature", "32 °F", new QuantityType<>(0, SIUnits.CELSIUS), true) }, //
                { new ParameterSet("Dimmer", "20", new PercentType(40), false) }, //
                { new ParameterSet("Dimmer", "40", new PercentType(20), true) }, //
                { new ParameterSet("DateTime", "", new DateTimeType(), true) } });
    }

    private static final String ITEM_NAME = "myItem";

    private @NonNullByDefault({}) Item item;

    private @NonNullByDefault({}) @Mock ItemRegistry mockItemRegistry;
    private @NonNullByDefault({}) @Mock BundleContext mockBundleContext;
    private @NonNullByDefault({}) @Mock TimeZoneProvider mockTimeZoneProvider;

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        when(mockItemRegistry.getItem(ITEM_NAME)).thenAnswer(i -> item);
        when(mockItemRegistry.get(ITEM_NAME)).thenAnswer(i -> item);
        when(mockTimeZoneProvider.getTimeZone()).thenReturn(ZoneId.systemDefault());
    }

    public Item getItem() {
        return item;
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testEqualsCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler("=", parameterSet.comparisonState);

        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testNotEqualsCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler("!=", parameterSet.comparisonState);

        if (parameterSet.expectedResult) {
            assertFalse(handler.isSatisfied(Map.of()));
        } else {
            assertTrue(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("greaterThanParameters")
    public void testGreaterThanCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler(">", parameterSet.comparisonState);

        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("greaterThanOrEqualsParameters")
    public void testGreaterThanOrEqualsCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler(">=", parameterSet.comparisonState);

        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("lessThanParameters")
    public void testLessThanCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler("<", parameterSet.comparisonState);

        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()));
        } else {
            assertFalse(handler.isSatisfied(Map.of()));
        }
    }

    @ParameterizedTest
    @MethodSource("lessThanOrEqualsParameters")
    public void testLessThanOrEqualsCondition(ParameterSet parameterSet) {
        item = parameterSet.item;
        ItemStateConditionHandler handler = initItemStateConditionHandler("<=", parameterSet.comparisonState);

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
        return new ItemStateConditionHandler(builder.build(), "", mockBundleContext, mockItemRegistry,
                mockTimeZoneProvider);
    }

    @Test
    public void itemMessagesAreLogged() {
        Configuration configuration = new Configuration();
        configuration.put(ItemStateConditionHandler.ITEM_NAME, ITEM_NAME);
        configuration.put(ItemStateConditionHandler.OPERATOR, "=");
        Condition condition = ConditionBuilder.create() //
                .withId("conditionId") //
                .withTypeUID(ItemStateConditionHandler.ITEM_STATE_CONDITION) //
                .withConfiguration(configuration) //
                .build();

        setupInterceptedLogger(ItemStateConditionHandler.class, LogLevel.INFO);

        // missing on creation
        when(mockItemRegistry.get(ITEM_NAME)).thenReturn(null);
        ItemStateConditionHandler handler = new ItemStateConditionHandler(condition, "foo", mockBundleContext,
                mockItemRegistry, mockTimeZoneProvider);
        assertLogMessage(ItemStateConditionHandler.class, LogLevel.WARN,
                "Item 'myItem' needed for rule 'foo' is missing. Condition 'conditionId' will not work.");

        // added later
        ItemAddedEvent addedEvent = ItemEventFactory.createAddedEvent(new SwitchItem(ITEM_NAME));
        assertTrue(handler.getEventFilter().apply(addedEvent));
        handler.receive(addedEvent);
        assertLogMessage(ItemStateConditionHandler.class, LogLevel.INFO,
                "Item 'myItem' needed for rule 'foo' added. Condition 'conditionId' will now work.");

        // removed later
        ItemRemovedEvent removedEvent = ItemEventFactory.createRemovedEvent(new SwitchItem(ITEM_NAME));
        assertTrue(handler.getEventFilter().apply(removedEvent));
        handler.receive(removedEvent);
        assertLogMessage(ItemStateConditionHandler.class, LogLevel.WARN,
                "Item 'myItem' needed for rule 'foo' removed. Condition 'conditionId' will no longer work.");
    }
}
