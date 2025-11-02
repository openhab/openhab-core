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
import java.util.Objects;

import javax.measure.Unit;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.library.unit.CurrencyService;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.dimension.EnergyPrice;
import org.openhab.core.library.dimension.VolumePrice;

import tech.units.indriya.AbstractSystemOfUnits;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.unit.ProductUnit;

/**
 * The {@link CurrencyUnits} defines the UoM system for handling currencies
 *
 * @author Jan N. Klug - Initial contribution
 * @author Christoph Weitkamp - Added price per volume
 */
@NonNullByDefault
public final class CurrencyUnits extends AbstractSystemOfUnits {

    private static final CurrencyUnits INSTANCE = new CurrencyUnits();

    public static final Unit<Currency> BASE_CURRENCY = new CurrencyUnit("DEF", null);
    public static final Unit<EnergyPrice> BASE_ENERGY_PRICE = new ProductUnit<>(
            BASE_CURRENCY.divide(Units.KILOWATT_HOUR));
    public static final Unit<VolumePrice> PRICE_PER_LITRE = new ProductUnit<>(BASE_CURRENCY.divide(Units.LITRE));
    public static final Unit<VolumePrice> PRICE_PER_CUBIC_METRE = new ProductUnit<>(
            BASE_CURRENCY.divide(SIUnits.CUBIC_METRE));
    public static final Unit<VolumePrice> PRICE_PER_GALLON_LIQUID_US = new ProductUnit<>(
            BASE_CURRENCY.divide(ImperialUnits.GALLON_LIQUID_US));

    static {
        addUnit(BASE_CURRENCY);
        INSTANCE.units.add(BASE_ENERGY_PRICE);
        INSTANCE.units.add(PRICE_PER_LITRE);
        INSTANCE.units.add(PRICE_PER_CUBIC_METRE);
        INSTANCE.units.add(PRICE_PER_GALLON_LIQUID_US);
    }

    @Override
    public String getName() {
        return CurrencyUnits.class.getSimpleName();
    }

    public static void addUnit(Unit<Currency> unit) {
        if (!(unit instanceof CurrencyUnit)) {
            throw new IllegalArgumentException("Not an instance of CurrencyUnit");
        }
        INSTANCE.units.add(unit);
        SimpleUnitFormat.getInstance().label(unit, unit.getName());
        String symbol = unit.getSymbol();
        if (symbol != null && !symbol.isBlank()) {
            SimpleUnitFormat.getInstance().alias(unit, symbol);
        }
    }

    public static void removeUnit(Unit<Currency> unit) {
        SimpleUnitFormat.getInstance().removeLabel(unit);
        SimpleUnitFormat.getInstance().removeAliases(unit);
        INSTANCE.units.remove(unit);
    }

    public static SystemOfUnits getInstance() {
        return Objects.requireNonNull(INSTANCE);
    }

    public static Unit<Currency> createCurrency(String symbol, String name) {
        return new CurrencyUnit(symbol, name);
    }

    /**
     * Get the exchange rate for a given currency to the system's base unit
     *
     * @param currency the currency
     * @return the exchange rate
     */
    public static @Nullable BigDecimal getExchangeRate(Unit<Currency> currency) {
        return CurrencyService.FACTOR_FCN.apply(currency);
    }
}
