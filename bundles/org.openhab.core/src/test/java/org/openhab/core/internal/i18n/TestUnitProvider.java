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
package org.openhab.core.internal.i18n;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.unit.SIUnits;

/**
 * The {@link TestUnitProvider} implements a {@link UnitProvider} for testing purposes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestUnitProvider implements UnitProvider {

    private final Map<Class<? extends Quantity<?>>, Map<SystemOfUnits, Unit<? extends Quantity<?>>>> dimensionMap = I18nProviderImpl
            .getDimensionMap();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Quantity<T>> Unit<T> getUnit(Class<T> dimension) {
        Unit<T> unit = (Unit<T>) dimensionMap.getOrDefault(dimension, Map.of()).get(SIUnits.getInstance());
        assert unit != null;
        return unit;
    }

    @Override
    public SystemOfUnits getMeasurementSystem() {
        return SIUnits.getInstance();
    }

    @Override
    public Collection<Class<? extends Quantity<?>>> getAllDimensions() {
        return Set.of();
    }
}
