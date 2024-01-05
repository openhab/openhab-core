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
package org.openhab.core.internal.library.unit;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

import javax.measure.UnitConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import tech.units.indriya.function.AbstractConverter;

/**
 * The {@link CurrencyConverter} implements an {@link UnitConverter} for
 * {@link org.openhab.core.library.unit.CurrencyUnit}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CurrencyConverter extends AbstractConverter {

    private final BigDecimal factor;

    public CurrencyConverter(BigDecimal factor) {
        this.factor = factor;
    }

    @Override
    public boolean equals(@Nullable Object cvtr) {
        return cvtr instanceof CurrencyConverter currencyConverter && factor.equals(currencyConverter.factor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(factor);
    }

    @Override
    protected @Nullable String transformationLiteral() {
        return null;
    }

    @Override
    protected AbstractConverter inverseWhenNotIdentity() {
        return new CurrencyConverter(BigDecimal.ONE.divide(factor, MathContext.DECIMAL128));
    }

    @Override
    protected boolean canReduceWith(@Nullable AbstractConverter that) {
        return false;
    }

    @Override
    protected Number convertWhenNotIdentity(@NonNullByDefault({}) Number value) {
        return new BigDecimal(value.toString()).multiply(factor, MathContext.DECIMAL128);
    }

    @Override
    public int compareTo(@Nullable UnitConverter o) {
        return o instanceof CurrencyConverter currencyConverter ? factor.compareTo(currencyConverter.factor) : -1;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public boolean isLinear() {
        return true;
    }
}
