/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.library.types;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.internal.i18n.TestUnitProvider;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.service.component.ComponentContext;

/**
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class QuantityTypeArithmeticGroupFunctionTest {

    private @Mock @NonNullByDefault({}) ComponentContext componentContext;
    private final UnitProvider unitProvider = new TestUnitProvider();

    /**
     * Locales having a different decimal and grouping separators to test string parsing and generation.
     */
    static Stream<Locale> locales() {
        return Stream.of(
                // ٫٬ (Arabic, Egypt)
                Locale.forLanguageTag("ar-EG"),
                // ,. (German, Germany)
                Locale.forLanguageTag("de-DE"),
                // ., (English, United States)
                Locale.forLanguageTag("en-US"));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testSumFunctionQuantityType(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("89 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("122.41 °C")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("234.95 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testSumFunctionQuantityTypeDifferentUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("192.2 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("395.56 K")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("234.95 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testSumFunctionQuantityTypeIncompatibleUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>(); // we need an ordered set to guarantee the Unit of the first entry
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Sum(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testAvgFunctionQuantityType(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("200 °C"), state);

        items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("19.5 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, new QuantityType<>("19.5 °C")));

        state = function.calculate(items);

        assertEquals(new QuantityType<>("19.5 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testAvgFunctionQuantityTypeDifferentUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("55.33333333333333333333333333333334 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testAvgFunctionQuantityTypeIncompatibleUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Avg(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    static Stream<Arguments> medianTestSource() {
        return Stream.of( //
                arguments( //
                        List.of(new QuantityType("100 °C"), UnDefType.NULL, new QuantityType("200 °C"), UnDefType.UNDEF,
                                new QuantityType("300 °C"), new QuantityType("400 °C")), //
                        new QuantityType("250 °C")), //
                // mixed units. 200 °C = 392 °F
                arguments( //
                        List.of(new QuantityType("100 °C"), UnDefType.NULL, new QuantityType("392 °F"), UnDefType.UNDEF,
                                new QuantityType("300 °C"), new QuantityType("400 °C")), //
                        new QuantityType("250 °C")), //
                arguments( //
                        List.of(new QuantityType("100 °C"), UnDefType.NULL, new QuantityType("200 °C"), UnDefType.UNDEF,
                                new QuantityType("300 °C")), //
                        new QuantityType("200 °C")), //
                arguments( //
                        List.of(new QuantityType("100 °C"), UnDefType.NULL, new QuantityType("200 °C")), //
                        new QuantityType("150 °C")), //
                arguments( //
                        List.of(new QuantityType("100 °C"), UnDefType.NULL), //
                        new QuantityType("100 °C")), //
                arguments( //
                        List.of(), //
                        UnDefType.UNDEF) //
        );
    }

    @ParameterizedTest
    @MethodSource("medianTestSource")
    public void testMedianFunctionQuantityType(List<State> states, State expected) {
        AtomicInteger index = new AtomicInteger(1);
        Set<Item> items = states.stream()
                .map(state -> createNumberItem("TestItem" + index.getAndIncrement(), Temperature.class, state))
                .collect(Collectors.toSet());

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Median(Temperature.class, null);
        State state = function.calculate(items);

        assertEquals(state.getClass(), expected.getClass());
        if (expected instanceof QuantityType expectedQuantityType) {
            QuantityType stateQuantityType = ((QuantityType) state).toInvertibleUnit(expectedQuantityType.getUnit());
            assertThat(stateQuantityType.doubleValue(), is(closeTo(expectedQuantityType.doubleValue(), 0.01d)));
        }
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMaxFunctionQuantityType(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("300 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMaxFunctionQuantityTypeDifferentUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("100 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMaxFunctionQuantityTypeIncompatibleUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Max(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMinFunctionQuantityType(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("200 °C")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("300 °C")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("100 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMaxFunctionQuantityTypeOnDimensionless(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Dimensionless.class, new QuantityType<>("48 %")));
        items.add(createNumberItem("TestItem2", Dimensionless.class, new QuantityType<>("36 %")));
        items.add(createNumberItem("TestItem3", Dimensionless.class, new QuantityType<>("0 %")));
        items.add(createNumberItem("TestItem4", Dimensionless.class, new QuantityType<>("48 %")));
        items.add(createNumberItem("TestItem5", Dimensionless.class, new QuantityType<>("0 %")));
        items.add(createNumberItem("TestItem6", Dimensionless.class, new QuantityType<>("0 %")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Max(Dimensionless.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("48 %"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMinFunctionQuantityTypeDifferentUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("100 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Temperature.class, new QuantityType<>("113 °F")));
        items.add(createNumberItem("TestItem4", Temperature.class, UnDefType.UNDEF));
        items.add(createNumberItem("TestItem5", Temperature.class, new QuantityType<>("294.15 K")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("294.15 K"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testMinFunctionQuantityTypeIncompatibleUnits(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Temperature.class, new QuantityType<>("23.54 °C")));
        items.add(createNumberItem("TestItem2", Temperature.class, UnDefType.NULL));
        items.add(createNumberItem("TestItem3", Pressure.class, new QuantityType<>("192.2 hPa")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Min(Temperature.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("23.54 °C"), state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testSumFunctionQuantityTypeWithGroups(Locale locale) {
        Locale.setDefault(locale);

        Set<Item> items = new LinkedHashSet<>();
        items.add(createNumberItem("TestItem1", Power.class, new QuantityType<>("5 W")));
        items.add(createGroupItem("TestGroup1", Power.class, new QuantityType<>("5 W")));

        GroupFunction function = new QuantityTypeArithmeticGroupFunction.Sum(Power.class);
        State state = function.calculate(items);

        assertEquals(new QuantityType<>("10 W"), state);
    }

    private NumberItem createNumberItem(String name, Class<? extends Quantity<?>> dimension, State state) {
        NumberItem item = new NumberItem(CoreItemFactory.NUMBER + ":" + dimension.getSimpleName(), name, unitProvider);
        item.setState(state);
        return item;
    }

    private GroupItem createGroupItem(String name, Class<? extends Quantity<?>> dimension, State state) {
        GroupItem item = new GroupItem(name,
                new NumberItem(CoreItemFactory.NUMBER + ":" + dimension.getSimpleName(), name, unitProvider));
        item.setState(state);
        return item;
    }
}
