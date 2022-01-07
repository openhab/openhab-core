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
package org.openhab.core.internal.items;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.stream.Stream;

import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;

/**
 * Test the {@link ItemStateConverterImpl}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class ItemStateConverterImplTest {

    private @NonNullByDefault({}) ItemStateConverterImpl itemStateConverter;

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

    @BeforeEach
    public void setup() {
        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Temperature.class)).thenReturn(ImperialUnits.FAHRENHEIT);
        itemStateConverter = new ItemStateConverterImpl(unitProvider);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testNullState(Locale locale) {
        Locale.setDefault(locale);

        State undef = itemStateConverter.convertToAcceptedState(null, null);

        assertThat(undef, is(UnDefType.NULL));
    }

    @ParameterizedTest
    @MethodSource("locales")
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void testNoConversion(Locale locale) {
        Locale.setDefault(locale);

        Item item = new NumberItem("number");
        State originalState = new DecimalType(12.34);
        State state = itemStateConverter.convertToAcceptedState(originalState, item);

        assertTrue(originalState == state);
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void testStateConversion(Locale locale) {
        Locale.setDefault(locale);

        Item item = new NumberItem("number");
        State originalState = new PercentType("42");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new DecimalType("0.42")));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void numberItemWithoutDimensionShouldConvertToDecimalType(Locale locale) {
        Locale.setDefault(locale);

        Item item = new NumberItem("number");
        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new DecimalType("12.34")));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void numberItemWitDimensionShouldConvertToItemStateDescriptionUnit(Locale locale) {
        Locale.setDefault(locale);

        NumberItem item = mock(NumberItem.class);
        StateDescription stateDescription = mock(StateDescription.class);
        when(item.getStateDescription()).thenReturn(stateDescription);
        doReturn(Temperature.class).when(item).getDimension();
        when(stateDescription.getPattern()).thenReturn("%.1f K");

        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new QuantityType<>("285.49 K")));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void numberItemWitDimensionShouldConvertToLocaleBasedUnit(Locale locale) {
        Locale.setDefault(locale);

        NumberItem item = mock(NumberItem.class);
        doReturn(Temperature.class).when(item).getDimension();

        State originalState = new QuantityType<>("12.34 °C");
        State convertedState = itemStateConverter.convertToAcceptedState(originalState, item);

        assertThat(convertedState, is(new QuantityType<>("54.212 °F")));
    }

    @ParameterizedTest
    @MethodSource("locales")
    public void numberItemShouldNotConvertUnitsWhereMeasurmentSystemEquals(Locale locale) {
        Locale.setDefault(locale);

        NumberItem item = mock(NumberItem.class);
        doReturn(Length.class).when(item).getDimension();

        QuantityType<Length> originalState = new QuantityType<>("100 cm");

        @SuppressWarnings("unchecked")
        QuantityType<Length> convertedState = (QuantityType<Length>) itemStateConverter
                .convertToAcceptedState(originalState, item);

        assertThat(convertedState.getUnit(), is(originalState.getUnit()));
    }
}
