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

import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.format.SimpleUnitFormat;

/**
 * The {@link CurrencyService} allows to register and switch {@link CurrencyProvider}s and provides exchange rates
 * for currencies
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = CurrencyService.class, immediate = true, configurationPid = CurrencyService.CONFIGURATION_PID, property = {
        Constants.SERVICE_PID + "=org.openhab.units", //
        "service.config.label=Unit Settings", //
        "service.config.category=system", //
        "service.config.description.uri=system:units" })
@NonNullByDefault
public class CurrencyService {
    public static final String CONFIGURATION_PID = "org.openhab.units";
    public static final String CONFIG_OPTION_CURRENCY_PROVIDER = "currencyProvider";
    private final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    public static Function<Unit<Currency>, @Nullable BigDecimal> FACTOR_FCN = unit -> null;

    private final Map<String, CurrencyProvider> currencyProviders = new ConcurrentHashMap<>();

    private CurrencyProvider enabledCurrencyProvider = DefaultCurrencyProvider.getInstance();
    private String configuredCurrencyProvider = DefaultCurrencyProvider.getInstance().getName();

    @Activate
    public CurrencyService(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    public void modified(Map<String, Object> config) {
        String configOption = (String) config.get(CONFIG_OPTION_CURRENCY_PROVIDER);
        configuredCurrencyProvider = Objects.requireNonNullElse(configOption,
                DefaultCurrencyProvider.getInstance().getName());
        CurrencyProvider currencyProvider = currencyProviders.getOrDefault(configuredCurrencyProvider,
                DefaultCurrencyProvider.getInstance());
        enableProvider(currencyProvider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.put(currencyProvider.getName(), currencyProvider);
        if (configuredCurrencyProvider.equals(currencyProvider.getName())) {
            enableProvider(currencyProvider);
        }
    }

    public void removeCurrencyProvider(CurrencyProvider currencyProvider) {
        if (currencyProvider.equals(enabledCurrencyProvider)) {
            logger.warn("The currently activated currency provider is being removed. Enabling default.");
            enableProvider(DefaultCurrencyProvider.getInstance());
        }
        currencyProviders.remove(currencyProvider.getName());
    }

    private synchronized void enableProvider(CurrencyProvider currencyProvider) {
        SimpleUnitFormat unitFormatter = SimpleUnitFormat.getInstance();
        // remove units from old provider
        enabledCurrencyProvider.getAdditionalCurrencies().forEach(CurrencyUnits::removeUnit);
        unitFormatter.removeLabel(enabledCurrencyProvider.getBaseCurrency());

        // add new units
        FACTOR_FCN = currencyProvider.getExchangeRateFunction();
        Unit<Currency> baseCurrency = currencyProvider.getBaseCurrency();
        ((CurrencyUnit) BASE_CURRENCY).setSymbol(baseCurrency.getSymbol());
        ((CurrencyUnit) BASE_CURRENCY).setName(baseCurrency.getName());
        unitFormatter.label(BASE_CURRENCY, baseCurrency.getName());
        if (baseCurrency.getSymbol() != null) {
            unitFormatter.alias(BASE_CURRENCY, baseCurrency.getSymbol());
        }

        currencyProvider.getAdditionalCurrencies().forEach(CurrencyUnits::addUnit);

        this.enabledCurrencyProvider = currencyProvider;
    }

    private static class DefaultCurrencyProvider implements CurrencyProvider {
        private static final CurrencyProvider INSTANCE = new DefaultCurrencyProvider();

        @Override
        public Unit<Currency> getBaseCurrency() {
            return new CurrencyUnit("DEF", null);
        }

        @Override
        public Collection<Unit<Currency>> getAdditionalCurrencies() {
            return Set.of();
        }

        @Override
        public Function<Unit<Currency>, @Nullable BigDecimal> getExchangeRateFunction() {
            return unit -> null;
        }

        public static CurrencyProvider getInstance() {
            return INSTANCE;
        }
    }
}
