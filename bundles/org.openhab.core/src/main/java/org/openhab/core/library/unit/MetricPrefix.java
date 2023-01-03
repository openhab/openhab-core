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
package org.openhab.core.library.unit;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The metric prefixes used to derive units by specific powers of 10. This delegates to the enum instances of
 * {@link javax.measure.MetricPrefix}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class MetricPrefix {

    public static <T extends Quantity<T>> Unit<T> YOTTA(Unit<T> unit) {
        return javax.measure.MetricPrefix.YOTTA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ZETTA(Unit<T> unit) {
        return javax.measure.MetricPrefix.ZETTA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> EXA(Unit<T> unit) {
        return javax.measure.MetricPrefix.EXA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> PETA(Unit<T> unit) {
        return javax.measure.MetricPrefix.PETA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> TERA(Unit<T> unit) {
        return javax.measure.MetricPrefix.TERA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> GIGA(Unit<T> unit) {
        return javax.measure.MetricPrefix.GIGA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MEGA(Unit<T> unit) {
        return javax.measure.MetricPrefix.MEGA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> KILO(Unit<T> unit) {
        return javax.measure.MetricPrefix.KILO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> HECTO(Unit<T> unit) {
        return javax.measure.MetricPrefix.HECTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> DEKA(Unit<T> unit) {
        return javax.measure.MetricPrefix.DEKA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> DECI(Unit<T> unit) {
        return javax.measure.MetricPrefix.DECI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> CENTI(Unit<T> unit) {
        return javax.measure.MetricPrefix.CENTI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MILLI(Unit<T> unit) {
        return javax.measure.MetricPrefix.MILLI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MICRO(Unit<T> unit) {
        return javax.measure.MetricPrefix.MICRO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> NANO(Unit<T> unit) {
        return javax.measure.MetricPrefix.NANO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> PICO(Unit<T> unit) {
        return javax.measure.MetricPrefix.PICO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> FEMTO(Unit<T> unit) {
        return javax.measure.MetricPrefix.FEMTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ATTO(Unit<T> unit) {
        return javax.measure.MetricPrefix.ATTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ZEPTO(Unit<T> unit) {
        return javax.measure.MetricPrefix.ZEPTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> YOCTO(Unit<T> unit) {
        return javax.measure.MetricPrefix.YOCTO(unit);
    }
}
