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
package org.openhab.core.library.unit;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.dimension.Currency;

/**
 * The {@link CurrencyProvider} can be implemented by services that supply currencies and their exchange rates
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface CurrencyProvider {

    /**
     * Get the name of this {@link CurrencyProvider}
     *
     * @return the name, defaults to the class name
     */
    default String getName() {
        return getClass().getName();
    }

    /**
     * Get the base currency from this provider
     * <p />
     * This currency is used as base for calculating exchange rates.
     *
     * @return the base currency of this provider
     */
    Unit<Currency> getBaseCurrency();

    /**
     * Get all additional currency that are supported by this provider
     * <p />
     * The collection does NOT include the base currency.
     *
     * @return a {@link Collection} of {@link Unit<Currency>}s
     */
    Collection<Unit<Currency>> getAdditionalCurrencies();

    /**
     * Get a {@link Function} that supplies exchanges rates for currencies supported by this provider
     * <p />
     * This needs to be dynamic because in most cases exchange rates are not constant over time.
     *
     * @return the function
     */
    Function<Unit<Currency>, @Nullable BigDecimal> getExchangeRateFunction();
}
