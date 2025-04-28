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
package org.openhab.core.internal.library.unit;

import java.io.Serial;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Objects;

import javax.measure.UnitConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.Calculus;

/**
 * The {@link CurrencyConverter} implements an {@link UnitConverter} for
 * {@link org.openhab.core.library.unit.CurrencyUnit}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CurrencyConverter extends AbstractConverter {
    @Serial
    private static final long serialVersionUID = 1L;

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

    /**
     * This is currently necessary because conversion of {@link tech.units.indriya.unit.ProductUnit}s requires a
     * converter that is properly registered. This is currently not possible. We can't use the registered providers,
     * because they only have package-private constructors.
     *
     * {@see https://github.com/unitsofmeasurement/indriya/issues/402}
     */
    static {
        // call to ensure map is initialized
        Map<Class<? extends AbstractConverter>, Integer> unused = (Map<Class<? extends AbstractConverter>, Integer>) Calculus
                .getNormalFormOrder();
        try {
            Field field = Calculus.class.getDeclaredField("normalFormOrder");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends AbstractConverter>, Integer> original = (Map<Class<? extends AbstractConverter>, Integer>) field
                    .get(null);
            Objects.requireNonNull(original).put(CurrencyConverter.class, 1000);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Could not add currency converter", e);
        }

    }
}
