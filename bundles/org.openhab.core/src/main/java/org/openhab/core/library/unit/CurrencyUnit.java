/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;
import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;
import static tech.units.indriya.AbstractUnit.ONE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Objects;

import javax.measure.Dimension;
import javax.measure.IncommensurableException;
import javax.measure.Prefix;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.library.unit.CurrencyConverter;
import org.openhab.core.internal.library.unit.CurrencyService;
import org.openhab.core.library.dimension.Currency;

import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.AddConverter;
import tech.units.indriya.function.Calculus;
import tech.units.indriya.function.MultiplyConverter;
import tech.units.indriya.function.RationalNumber;
import tech.units.indriya.unit.AlternateUnit;
import tech.units.indriya.unit.ProductUnit;
import tech.units.indriya.unit.TransformedUnit;
import tech.units.indriya.unit.UnitDimension;
import tech.uom.lib.common.function.Nameable;
import tech.uom.lib.common.function.PrefixOperator;
import tech.uom.lib.common.function.SymbolSupplier;

/**
 * The {@link CurrencyUnit} is a UoM compatible unit for currencies.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND })
public final class CurrencyUnit implements Unit<Currency>, Comparable<Unit<Currency>>, PrefixOperator<Currency>,
        Nameable, Serializable, SymbolSupplier {

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

    public UnitConverter getSystemConverter() {
        return AbstractConverter.IDENTITY;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Unit<Currency> getSystemUnit() {
        return this;
    }

    @Override
    public boolean isCompatible(@NonNullByDefault({}) Unit<?> that) {
        return DIMENSION.equals(that.getDimension());
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNullByDefault({}) <T extends Quantity<T>> Unit<T> asType(@NonNullByDefault({}) Class<T> type) {
        Dimension typeDimension = UnitDimension.of(type);
        if (typeDimension != null && !typeDimension.equals(this.getDimension())) {
            throw new ClassCastException("The unit: " + this + " is not compatible with quantities of type " + type);
        }
        return (Unit<T>) this;
    }

    @Override
    public @NonNullByDefault({}) Map<? extends Unit<?>, Integer> getBaseUnits() {
        return Map.of();
    }

    @Override
    public Dimension getDimension() {
        return DIMENSION;
    }

    public void setName(String name) {
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

    public void setSymbol(@Nullable String s) {
        this.symbol = s;
    }

    @Override
    public final UnitConverter getConverterTo(@NonNullByDefault({}) Unit<Currency> that) throws UnconvertibleException {
        return internalGetConverterTo(that);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final @NonNullByDefault({}) UnitConverter getConverterToAny(@NonNullByDefault({}) Unit<?> that)
            throws IncommensurableException, UnconvertibleException {
        if (!isCompatible(that)) {
            throw new IncommensurableException(this + " is not compatible with " + that);
        }
        return internalGetConverterTo((Unit<Currency>) that);
    }

    @Override
    public final Unit<Currency> alternate(@NonNullByDefault({}) String newSymbol) {
        return new AlternateUnit<>(this, newSymbol);
    }

    @Override
    public final Unit<Currency> transform(@NonNullByDefault({}) UnitConverter operation) {
        return operation.isIdentity() ? this : new TransformedUnit<>(null, this, this, operation);
    }

    @Override
    public Unit<Currency> shift(@NonNullByDefault({}) Number offset) {
        return Calculus.currentNumberSystem().isZero(offset) ? this : transform(new AddConverter(offset));
    }

    @Override
    public Unit<Currency> multiply(@NonNullByDefault({}) Number factor) {
        return Calculus.currentNumberSystem().isOne(factor) ? this : transform(MultiplyConverter.of(factor));
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
    public final Unit<?> multiply(@NonNullByDefault({}) Unit<?> that) {
        return that.equals(ONE) ? this : ProductUnit.ofProduct(this, that);
    }

    @Override
    public final Unit<?> inverse() {
        return ProductUnit.ofQuotient(ONE, this);
    }

    @Override
    public final Unit<Currency> divide(@NonNullByDefault({}) Number divisor) {
        if (Calculus.currentNumberSystem().isOne(divisor)) {
            return this;
        }
        BigDecimal factor = BigDecimal.ONE.divide(new BigDecimal(divisor.toString()), MathContext.DECIMAL128);
        return transform(MultiplyConverter.of(factor));
    }

    @Override
    public final Unit<?> divide(@NonNullByDefault({}) Unit<?> that) {
        return this.multiply(that.inverse());
    }

    @Override
    public final Unit<?> root(int n) {
        if (n > 0) {
            return ProductUnit.ofRoot(this, n);
        } else if (n == 0) {
            throw new ArithmeticException("Root's order of zero");
        } else {
            // n < 0
            return ONE.divide(this.root(-n));
        }
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
