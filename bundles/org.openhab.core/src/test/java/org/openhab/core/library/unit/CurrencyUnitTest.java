/**
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
package org.openhab.core.library.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.measure.Unit;
import javax.measure.quantity.Energy;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.internal.library.unit.CurrencyService;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.dimension.EnergyPrice;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.util.UnitUtils;

/**
 * The {@link CurrencyUnitTest} contains tests for the currency units
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CurrencyUnitTest {

    @SuppressWarnings("unused")
    private final CurrencyService currencyService = new CurrencyService(
            Map.of("currencyProvider", "org.openhab.core.library.unit.CurrencyUnitTest$TestCurrencyProvider"));

    @BeforeEach
    public void setup() {
        currencyService.addCurrencyProvider(new TestCurrencyProvider());
    }

    @AfterEach
    public void tearDown() {
        currencyService.removeCurrencyProvider(new TestCurrencyProvider());
    }

    @Test
    public void testSimpleConversion() {
        QuantityType<Currency> amount = new QuantityType<>("7.45 DKK");
        QuantityType<Currency> convertedAmount = amount.toUnit("€");

        assertThat(convertedAmount, is(notNullValue()));
        assertThat(convertedAmount.getUnit(), is(TestCurrencyProvider.EUR));
        assertThat(convertedAmount.doubleValue(), closeTo(1, 1e-4));
    }

    @Test
    public void testInverseConversion() {
        QuantityType<Currency> amount = new QuantityType<>("1.00 €");
        QuantityType<Currency> convertedAmount = amount.toUnit("DKK");

        assertThat(convertedAmount, is(notNullValue()));
        assertThat(convertedAmount.getUnit(), is(TestCurrencyProvider.DKK));
        assertThat(convertedAmount.doubleValue(), closeTo(7.45, 1e-4));
    }

    @Test
    public void testComplexConversion() {
        QuantityType<Currency> amount = new QuantityType<>("13 DKK");
        QuantityType<Currency> convertedAmount = amount.toUnit("$");

        assertThat(convertedAmount, is(notNullValue()));
        assertThat(convertedAmount.getUnit(), is(TestCurrencyProvider.USD));
        assertThat(convertedAmount.doubleValue(), closeTo(1.8845, 1e-4));
    }

    @Test
    public void testPriceCalculation() {
        QuantityType<EnergyPrice> unitPrice = new QuantityType<>("0.25 €/kWh");
        QuantityType<Energy> amount = new QuantityType<>("5 kWh");
        QuantityType<?> price = amount.multiply(unitPrice);

        assertThat(price, is(notNullValue()));
        assertThat(price.getUnit(), is(TestCurrencyProvider.EUR));
        assertThat(price.doubleValue(), closeTo(1.25, 1E-4));
    }

    @Test
    public void testEnergyPriceConversion() {
        QuantityType<EnergyPrice> price = new QuantityType<>("0.25 EUR/kWh");
        QuantityType<EnergyPrice> convertedPrice = price.toUnit("DKK/kWh");

        assertThat(convertedPrice, is(notNullValue()));
        assertThat(convertedPrice.getUnit(), is(UnitUtils.parseUnit("DKK/kWh")));
        assertThat(convertedPrice.doubleValue(), closeTo(1.8625, 1e-4));
    }

    private static class TestCurrencyProvider implements CurrencyProvider {
        public static final Unit<Currency> EUR = new CurrencyUnit("EUR", "€");
        public static final Unit<Currency> DKK = new CurrencyUnit("DKK", null);
        public static final Unit<Currency> USD = new CurrencyUnit("USD", "$");

        private static final Map<Unit<Currency>, BigDecimal> FACTORS = Map.of( //
                DKK, new BigDecimal("7.45"), //
                USD, new BigDecimal("1.08"));

        @Override
        public Unit<Currency> getBaseCurrency() {
            return EUR;
        }

        @Override
        public Collection<Unit<Currency>> getAdditionalCurrencies() {
            return List.of(DKK, USD);
        }

        @Override
        public Function<Unit<Currency>, @Nullable BigDecimal> getExchangeRateFunction() {
            return FACTORS::get;
        }
    }
}
