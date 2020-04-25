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

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The metric prefixes used to derive units by specific powers of 10. This delegates to the enum instances of
 * {@link tec.uom.se.unit.MetricPrefix}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class MetricPrefix {

    public static <T extends Quantity<T>> Unit<T> YOTTA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.YOTTA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ZETTA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.ZETTA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> EXA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.EXA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> PETA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.PETA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> TERA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.TERA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> GIGA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.GIGA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MEGA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.MEGA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> KILO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.KILO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> HECTO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.HECTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> DEKA(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.DEKA(unit);
    }

    public static <T extends Quantity<T>> Unit<T> DECI(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.DECI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> CENTI(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.CENTI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MILLI(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.MILLI(unit);
    }

    public static <T extends Quantity<T>> Unit<T> MICRO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.MICRO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> NANO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.NANO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> PICO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.PICO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> FEMTO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.FEMTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ATTO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.ATTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> ZEPTO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.ZEPTO(unit);
    }

    public static <T extends Quantity<T>> Unit<T> YOCTO(Unit<T> unit) {
        return tec.uom.se.unit.MetricPrefix.YOCTO(unit);
    }
}
