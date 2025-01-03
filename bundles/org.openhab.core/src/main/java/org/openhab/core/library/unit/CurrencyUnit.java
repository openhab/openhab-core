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

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Objects;

import javax.measure.Dimension;
import javax.measure.Prefix;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.library.unit.CurrencyConverter;
import org.openhab.core.internal.library.unit.CurrencyService;
import org.openhab.core.library.dimension.Currency;

import tech.units.indriya.AbstractUnit;
import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.MultiplyConverter;
import tech.units.indriya.function.RationalNumber;
import tech.units.indriya.unit.UnitDimension;

/**
 * The {@link CurrencyUnit} is a UoM compatible unit for currencies.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND })
public final class CurrencyUnit extends AbstractUnit<Currency> {

    @Serial
    private static final long serialVersionUID = -1L;
    private static final Dimension DIMENSION = UnitDimension.parse('$');
    private String name;
    private @Nullable String symbol;

    /**
     * Create a new <code>Currency</code>
     *
     * @param name 3-letter ISO-Code
     * @param symbol an (optional) symbol
     * @throws IllegalArgumentException if name is not valid
     */
    public CurrencyUnit(String name, @Nullable String symbol) throws IllegalArgumentException {
        if (name.length() != 3) {
            throw new IllegalArgumentException("Only three characters allowed for currency name");
        }
        this.symbol = symbol;
        this.name = name;
    }

    @Override
    public UnitConverter getSystemConverter() {
        return internalGetConverterTo(getSystemUnit());
    }

    @Override
    protected Unit<Currency> toSystemUnit() {
        return BASE_CURRENCY;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public @NonNullByDefault({}) Map<? extends Unit<?>, Integer> getBaseUnits() {
        return Map.of();
    }

    @Override
    public Dimension getDimension() {
        return DIMENSION;
    }

    @Override
    public void setName(@NonNullByDefault({}) String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getSymbol() {
        return symbol;
    }

    @Override
    public void setSymbol(@Nullable String s) {
        this.symbol = s;
    }

    @Override
    public Unit<Currency> shift(double offset) {
        return shift(RationalNumber.of(offset));
    }

    @Override
    public Unit<Currency> multiply(double multiplier) {
        return multiply(RationalNumber.of(multiplier));
    }

    @Override
    public Unit<Currency> divide(double divisor) {
        return divide(RationalNumber.of(divisor));
    }

    private UnitConverter internalGetConverterTo(Unit<Currency> that) throws UnconvertibleException {
        if (this.equals(that)) {
            return AbstractConverter.IDENTITY;
        }
        if (BASE_CURRENCY.equals(this)) {
            BigDecimal factor = CurrencyService.FACTOR_FCN.apply(that);
            if (factor != null) {
                return new CurrencyConverter(factor);
            }
        } else if (BASE_CURRENCY.equals(that)) {
            BigDecimal factor = CurrencyService.FACTOR_FCN.apply(this);
            if (factor != null) {
                return new CurrencyConverter(factor).inverse();
            }
        } else {
            BigDecimal f1 = CurrencyService.FACTOR_FCN.apply(this);
            BigDecimal f2 = CurrencyService.FACTOR_FCN.apply(that);

            if (f1 != null && f2 != null) {
                return new CurrencyConverter(f2.divide(f1, MathContext.DECIMAL128));
            }
        }
        throw new UnconvertibleException(
                "Could not get factor for converting " + this.getName() + " to " + that.getName());
    }

    @Override
    public Unit<?> pow(int n) {
        if (n > 0) {
            return this.multiply(this.pow(n - 1));
        } else if (n == 0) {
            return ONE;
        } else {
            // n < 0
            return ONE.divide(this.pow(-n));
        }
    }

    @Override
    public Unit<Currency> prefix(@NonNullByDefault({}) Prefix prefix) {
        return this.transform(MultiplyConverter.ofPrefix(prefix));
    }

    @Override
    public int compareTo(Unit<Currency> that) {
        int nameCompare = getName().compareTo(that.getName());
        if (nameCompare != 0) {
            return nameCompare;
        }
        String thatSymbol = that.getSymbol();
        if (symbol != null && thatSymbol != null) {
            return symbol.compareTo(thatSymbol);
        } else if (symbol != null) {
            return 1;
        } else if (thatSymbol != null) {
            return -1;
        }

        return 0;
    }

    @Override
    public boolean isEquivalentTo(@NonNullByDefault({}) Unit<Currency> that) {
        return this.getConverterTo(that).isIdentity();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof CurrencyUnit that) {
            return (name.equals(that.name) && Objects.equals(symbol, that.symbol));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, symbol);
    }
}
