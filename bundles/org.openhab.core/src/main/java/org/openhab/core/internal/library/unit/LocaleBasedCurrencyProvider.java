/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.internal.library.unit;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.unit.CurrencyProvider;
import org.openhab.core.library.unit.CurrencyUnit;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link LocaleBasedCurrencyProvider} is an implementation of {@link CurrencyProvider} that provides the base
 * currency based on the configured locale
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = CurrencyProvider.class, property = { Constants.SERVICE_PID + "=org.openhab.localebasedcurrency" })
@NonNullByDefault
public class LocaleBasedCurrencyProvider implements CurrencyProvider {

    private final LocaleProvider localeProvider;

    @Activate
    public LocaleBasedCurrencyProvider(@Reference LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    @Override
    public Unit<Currency> getBaseCurrency() {
        String currencyCode = java.util.Currency.getInstance(localeProvider.getLocale()).getCurrencyCode();
        if (currencyCode != null) {
            // either the currency was set or determined from the locale
            String symbol = java.util.Currency.getInstance(currencyCode).getSymbol();
            return new CurrencyUnit(currencyCode, symbol);
        } else {
            return new CurrencyUnit("DEF", null);
        }
    }

    @Override
    public Collection<Unit<Currency>> getAdditionalCurrencies() {
        return Set.of();
    }

    @Override
    public Function<Unit<Currency>, @Nullable BigDecimal> getExchangeRateFunction() {
        return unit -> null;
    }
}
