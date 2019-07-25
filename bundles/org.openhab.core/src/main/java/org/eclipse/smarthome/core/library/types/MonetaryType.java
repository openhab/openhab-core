/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.library.types;

import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryContext;
import javax.money.UnknownCurrencyException;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.money.format.MonetaryParseException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.PrimitiveType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MonetaryType} extends {@link Number} to handle monetary amounts
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault()
public class MonetaryType extends Number implements PrimitiveType, State, Command, Comparable<MonetaryType> {

    private final Logger logger = LoggerFactory.getLogger(MonetaryType.class);

    private static final long serialVersionUID = 8828949721938234633L;

    private final MonetaryAmount amount;

    /**
     *
     */
    public MonetaryType() {
        this.amount = Monetary.getDefaultAmountFactory().setCurrency(Monetary.getCurrency("EUR")).setNumber(0).create();
    }

    /**
     *
     */
    public MonetaryType(String value) throws MonetaryParseException {
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(Locale.getDefault());
        this.amount = format.parse(value);
    }

    /**
     *
     */
    public MonetaryType(Number value, CurrencyUnit currency) {
        this.amount = Monetary.getDefaultAmountFactory().setCurrency(currency).setNumber(value).create();
    }

    /**
     *
     */
    public MonetaryType(Number value, String currency) throws UnknownCurrencyException {
        this(value, Monetary.getCurrency(currency));
    }

    /**
     * Private constructor for arithmetic operations.
     */
    private MonetaryType(MonetaryAmount amount) {
        this.amount = amount;
    }

    /**
     *
     */
    public static MonetaryType valueOf(double value, CurrencyUnit currency) {
        return new MonetaryType(value, currency);
    }

    public static MonetaryType valueOf(double value, String currency) {
        return new MonetaryType(value, currency);
    }

    public static MonetaryType valueOf(String value) throws MonetaryParseException {
        return new MonetaryType(value);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (!(object instanceof MonetaryType)) {
            return false;
        }
        return amount.equals(((MonetaryType) object).amount);
    }

    @Override
    public int compareTo(MonetaryType object) {
        return amount.compareTo(object.amount);
    }

    /**
     * Returns the {@link CurrencyUnit} of this {@link MonetaryType}.
     *
     * @return the {@link CurrencyUnit}
     */
    public CurrencyUnit getCurrency() {
        return amount.getCurrency();
    }

    /**
     * Convert this {@link MonetaryType} to a new {@link MonetaryType} using the given target {@link CurrencyUnit}.
     *
     * @param targetCurrency the {@link CurrencyUnit} to which this {@link MonetaryType} will be converted to.
     * @return the new {@link MonetaryType} in the given {@link CurrencyUnit} or {@code null}.
     */
    public @Nullable MonetaryType toCurrency(CurrencyUnit targetCurrency) {
        CurrencyConversion conversion = MonetaryConversions.getConversion(targetCurrency);
        return new MonetaryType(amount.with(conversion));
    }

    /**
     * Converts this {@link MonetaryType} to a new {@link MonetaryType} using the given target currency.
     *
     * @param targetCurrency the currency to which this {@link MonetaryType} will be converted to.
     * @return the new {@link MonetaryType} in the given currency or {@code null}.
     * @throws UnknownCurrencyException
     */
    public @Nullable MonetaryType toCurrency(String targetCurrency) throws UnknownCurrencyException {
        return toCurrency(Monetary.getCurrency(targetCurrency));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime * getCurrency().hashCode();
        hash += prime * amount.getNumber().hashCode();
        return hash;
    }

    @Override
    public String format(String pattern) {
        // TODO
        return "";
    }

    @Override
    public int intValue() {
        return amount.getNumber().intValue();
    }

    @Override
    public long longValue() {
        return amount.getNumber().longValue();
    }

    @Override
    public float floatValue() {
        return amount.getNumber().floatValue();
    }

    @Override
    public double doubleValue() {
        return amount.getNumber().doubleValue();
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return amount.toString();
    }

    // @Override
    // public @Nullable State as(@Nullable Class<U> target) {
    // return UnDefType.UNDEF;
    // }

    /**
     * Returns a {@code MonetaryType} whose value is <code>this + other</code>.
     *
     * @param other value to be added to this {@code MonetaryType}.
     * @return {@code this + other}
     * @throws ArithmeticException if the result exceeds the numeric capabilities of this implementation class, i.e. the
     *             {@link MonetaryContext} cannot be adapted as required.
     */
    public MonetaryType add(MonetaryType other) {
        return new MonetaryType(amount.add(other.amount));
    }

    /**
     * Returns a {@code MonetaryType} whose value is <code>this - other</code>.
     *
     * @param other value to be subtracted from this {@code MonetaryType}.
     * @return {@code this - other}
     * @throws ArithmeticException if the result exceeds the numeric capabilities of this implementation class, i.e. the
     *             {@link MonetaryContext} cannot be adapted as required.
     */
    public MonetaryType subtract(MonetaryType other) {
        return new MonetaryType(amount.subtract(other.amount));
    }

    /**
     * Returns a {@code MonetaryType} whose value is <code>(this &times; multiplicand)</code>.
     *
     * @param multiplicand value to be multiplied by this {@code MonetaryType}. If the multiplicand's scale exceeds
     *            the capabilities of the implementation, it may be rounded implicitly.
     * @return {@code this * multiplicand}
     * @throws ArithmeticException if the result exceeds the numeric capabilities of this implementation class, i.e. the
     *             {@link MonetaryContext} cannot be adapted as required.
     */
    public MonetaryType multiply(Number multiplicand) {
        return new MonetaryType(amount.multiply(multiplicand));
    }

    /**
     * Returns a {@code MonetaryType} whose value is <code>this / divisor</code>; if the exact quotient cannot be
     * represented an {@code ArithmeticException} is thrown.
     *
     * @param divisor value by which this {@code MonetaryType} is to be divided.
     * @return {@code this / divisor}
     * @throws ArithmeticException if the exact quotient does not have a terminating decimal expansion, or if the
     *             result exceeds the numeric capabilities of this implementation class, i.e. the
     *             {@link MonetaryContext} cannot be adapted as required.
     */
    public MonetaryType divide(Number divisor) {
        return new MonetaryType(amount.divide(divisor));
    }
}
