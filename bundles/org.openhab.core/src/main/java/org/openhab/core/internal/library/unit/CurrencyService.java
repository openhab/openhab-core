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
package org.openhab.core.internal.library.unit;

import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.unit.CurrencyProvider;
import org.openhab.core.library.unit.CurrencyUnit;
import org.openhab.core.library.unit.CurrencyUnits;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import tech.units.indriya.format.SimpleUnitFormat;

/**
 * The {@link CurrencyService} is allows to register and switch {@link CurrencyProvider}s and provides exchange rates
 * for currencies
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component
@NonNullByDefault
public class CurrencyService {

    public static Function<Unit<Currency>, @Nullable BigDecimal> FACTOR_FCN = unit -> null;

    private final Set<CurrencyProvider> currencyProviders = new CopyOnWriteArraySet<>();

    @Activate
    public CurrencyService(
            @Reference(target = "(" + Constants.SERVICE_PID + "=org.openhab.i18n)") CurrencyProvider currencyProvider) {
        currencyProviders.add(currencyProvider);
        enableProvider(currencyProvider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.add(currencyProvider);
    }

    public void removeCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.remove(currencyProvider);
    }

    private synchronized void enableProvider(CurrencyProvider currencyProvider) {
        FACTOR_FCN = currencyProvider.getExchangeRateFunction();
        ((CurrencyUnit) BASE_CURRENCY).setSymbol(currencyProvider.getBaseCurrency().getSymbol());
        ((CurrencyUnit) BASE_CURRENCY).setName(currencyProvider.getBaseCurrency().getName());
        SimpleUnitFormat.getInstance().label(BASE_CURRENCY, currencyProvider.getBaseCurrency().getSymbol());
        currencyProvider.getCurrencies().forEach(CurrencyUnits::addUnit);
    }

    /**
     * Get the exchange rate for a given currency to the system's base unit
     *
     * @param currency the currency
     * @return the exchange rate
     */
    public static @Nullable BigDecimal getExchangeRate(Unit<Currency> currency) {
        return FACTOR_FCN.apply(currency);
    }
}
