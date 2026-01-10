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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.unit.CurrencyProvider;
import org.openhab.core.library.unit.CurrencyUnit;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * The {@link FixedCurrencyProvider} is an implementation of {@link CurrencyProvider} that provides only a single
 * (configurable) currency.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = CurrencyProvider.class, configurationPid = CurrencyService.CONFIGURATION_PID)
@NonNullByDefault
public class FixedCurrencyProvider implements CurrencyProvider {
    public static final String CONFIG_OPTION_BASE_CURRENCY = "fixedBaseCurrency";
    private String currencyCode = "DEF";

    @Activate
    public FixedCurrencyProvider(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    public void modified(Map<String, Object> config) {
        String code = (String) config.get(CONFIG_OPTION_BASE_CURRENCY);
        currencyCode = Objects.requireNonNullElse(code, "DEF");
    }

    @Override
    public Unit<Currency> getBaseCurrency() {
        String symbol = null;
        try {
            symbol = java.util.Currency.getInstance(currencyCode).getSymbol();
        } catch (IllegalArgumentException ignored) {
        }

        return new CurrencyUnit(currencyCode, symbol);
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
