/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.math.BigInteger;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;

import tec.uom.lib.common.function.SymbolSupplier;
import tec.uom.lib.common.function.UnitConverterSupplier;
import tec.uom.se.function.RationalConverter;

/**
 * The binary prefixes used to derive units by specific powers of 2.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public enum BinaryPrefix implements SymbolSupplier, UnitConverterSupplier {
    YOBI("Yi", new RationalConverter(BigInteger.valueOf(2).pow(80), BigInteger.ONE)),
    ZEBI("Zi", new RationalConverter(BigInteger.valueOf(2).pow(70), BigInteger.ONE)),
    EXBI("Ei", new RationalConverter(BigInteger.valueOf(2).pow(60), BigInteger.ONE)),
    PEBI("Pi", new RationalConverter(BigInteger.valueOf(2).pow(50), BigInteger.ONE)),
    TEBI("Ti", new RationalConverter(BigInteger.valueOf(2).pow(40), BigInteger.ONE)),
    GIBI("Gi", new RationalConverter(BigInteger.valueOf(2).pow(30), BigInteger.ONE)),
    MEBI("Mi", new RationalConverter(BigInteger.valueOf(2).pow(20), BigInteger.ONE)),
    KIBI("Ki", new RationalConverter(BigInteger.valueOf(2).pow(10), BigInteger.ONE));

    /**
     * The symbol of this prefix, as returned by {@link #getSymbol}.
     *
     * @see #getSymbol()
     */
    private final String symbol;

    /**
     * The <code>UnitConverter</code> of this prefix, as returned by {@link #getConverter}.
     *
     * @see #getConverter()
     * @see {@link UnitConverter}
     */
    private final UnitConverter converter;

    /**
     * Creates a new prefix.
     *
     * @param symbol the symbol of this prefix.
     * @param converter the associated unit converter.
     */
    BinaryPrefix(String symbol, RationalConverter converter) {
        this.symbol = symbol;
        this.converter = converter;
    }

    /**
     * Returns the symbol of this prefix.
     *
     * @return this prefix symbol, not {@code null}.
     */
    @Override
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the corresponding unit converter.
     *
     * @return the unit converter.
     */
    @Override
    public UnitConverter getConverter() {
        return converter;
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>80</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e80)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> YOBI(Unit<Q> unit) {
        return unit.transform(YOBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>70</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e70)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> ZEBI(Unit<Q> unit) {
        return unit.transform(ZEBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>60</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e60)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> EXBI(Unit<Q> unit) {
        return unit.transform(EXBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>50</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e50)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> PEBI(Unit<Q> unit) {
        return unit.transform(PEBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>40</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e40)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> TEBI(Unit<Q> unit) {
        return unit.transform(TEBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>30</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e30)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> GIBI(Unit<Q> unit) {
        return unit.transform(GIBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>20</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e20)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> MEBI(Unit<Q> unit) {
        return unit.transform(MEBI.getConverter());
    }

    /**
     * Returns the specified unit multiplied by the factor <code>2<sup>10</sup></code>
     *
     * @param <Q> The type of the quantity measured by the unit.
     * @param unit any unit.
     * @return <code>unit.times(2e10)</code>.
     */
    public static <Q extends Quantity<Q>> Unit<Q> KIBI(Unit<Q> unit) {
        return unit.transform(KIBI.getConverter());
    }

}
