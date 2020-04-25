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
package org.openhab.core.i18n;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provides {@link Unit}s and the current {@link SystemOfUnits}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface UnitProvider {

    /**
     * Retrieves the default {@link Unit} for the given {@link Quantity} according to the current
     * {@link SystemOfUnits}.
     *
     * @param dimension The {@link Quantity}, called dimension here, defines the base unit for the retrieved unit. E.g.
     *            call {@code getUnit(javax.measure.quantity.Temperature.class)} to retrieve the temperature unit
     *            according to the current {@link SystemOfUnits}.
     * @return The {@link Unit} matching the given {@link Quantity}, {@code null} otherwise.
     */
    <T extends Quantity<T>> @Nullable Unit<T> getUnit(@Nullable Class<T> dimension);

    /**
     * Returns the {@link SystemOfUnits} which is currently set, must not be null.
     *
     * @return the {@link SystemOfUnits} which is currently set, must not be null.
     */
    SystemOfUnits getMeasurementSystem();
}
