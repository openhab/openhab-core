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
package org.openhab.core.library.types;

import java.util.Collection;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.library.unit.Units;

/**
 * The {@link TestMirekProvider} implements a {@link UnitProvider} for testing purposes
 * that only returns {@link Units.MIRED}
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class TestMirekProvider implements UnitProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Quantity<T>> Unit<T> getUnit(Class<T> dimension) {
        return (Unit<T>) Units.MIRED;
    }

    @Override
    public SystemOfUnits getMeasurementSystem() {
        return Units.getInstance();
    }

    @Override
    public Collection<Class<? extends Quantity<?>>> getAllDimensions() {
        return Set.of();
    }
}
