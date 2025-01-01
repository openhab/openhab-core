/**
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
package org.openhab.core.io.rest.internal.resources.beans;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.types.util.UnitUtils;

/**
 * This is a java bean that is used to define UoM information for the REST interface.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UoMInfoBean {

    public final UoMInfo uomInfo;

    public static class UoMInfo {
        public final List<DimensionInfo> dimensions;

        public static class DimensionInfo {
            public final String dimension;
            public final String systemUnit;

            public DimensionInfo(String dimension, String systemUnit) {
                this.dimension = dimension;
                this.systemUnit = systemUnit;
            }
        }

        @SuppressWarnings({ "unchecked" })
        public UoMInfo(UnitProvider unitProvider) {
            dimensions = unitProvider.getAllDimensions().stream().map(dimension -> {
                Unit<?> unit = unitProvider.getUnit((Class<? extends Quantity>) dimension);
                String dimensionName = Objects.requireNonNull(UnitUtils.getDimensionName(unit));
                return new DimensionInfo(dimensionName, unit.toString());
            }).sorted(Comparator.comparing(a -> a.dimension)).toList();
        }
    }

    public UoMInfoBean(UnitProvider unitProvider) {
        uomInfo = new UoMInfo(unitProvider);
    }
}
