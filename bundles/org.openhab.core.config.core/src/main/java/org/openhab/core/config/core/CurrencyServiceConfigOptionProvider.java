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
package org.openhab.core.config.core;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.CurrencyProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link CurrencyServiceConfigOptionProvider} is an implementation of {@link ConfigOptionProvider} for the
 * available currency providers.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = ConfigOptionProvider.class)
@NonNullByDefault
public class CurrencyServiceConfigOptionProvider implements ConfigOptionProvider {

    private final List<CurrencyProvider> currencyProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.add(currencyProvider);
    }

    public void removeCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.remove(currencyProvider);
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if ("system:units".equals(uri.toString()) && "currencyProvider".equals(param)) {
            return currencyProviders.stream().map(this::mapProvider).toList();
        }
        return null;
    }

    private ParameterOption mapProvider(CurrencyProvider currencyProvider) {
        String providerName = currencyProvider.getName();
        int lastDot = providerName.lastIndexOf(".");
        String providerDescription = lastDot > -1 ? providerName.substring(lastDot + 1) : providerName;
        return new ParameterOption(providerName, providerDescription);
    }
}
