/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.persistence.filter;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersistenceIncludeFilter} is a filter that allows only specific values to pass
 * <p />
 * The filter returns {@code false} if the string representation of the item's state is not in the given list
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceIncludeFilter extends PersistenceFilter {
    private final Logger logger = LoggerFactory.getLogger(PersistenceIncludeFilter.class);

    private final BigDecimal lower;
    private final BigDecimal upper;
    private final String unit;
    private final boolean inverted;

    public PersistenceIncludeFilter(String name, BigDecimal lower, BigDecimal upper, @Nullable String unit,
            @Nullable Boolean inverted) {
        super(name);
        this.lower = lower;
        this.upper = upper;
        this.unit = (unit == null) ? "" : unit;
        this.inverted = inverted != null && inverted;
    }

    public BigDecimal getLower() {
        return lower;
    }

    public BigDecimal getUpper() {
        return upper;
    }

    public String getUnit() {
        return unit;
    }

    public boolean getInverted() {
        return inverted;
    }

    @Override
    public boolean apply(Item item) {
        State state = item.getState();
        BigDecimal compareValue = null;
        if (state instanceof DecimalType decimalType) {
            compareValue = decimalType.toBigDecimal();
        } else if (state instanceof QuantityType<?> quantityType) {
            if (!unit.isBlank()) {
                QuantityType<?> convertedQuantity = quantityType.toUnit(unit);
                if (convertedQuantity != null) {
                    compareValue = convertedQuantity.toBigDecimal();
                }
            }
        }
        if (compareValue == null) {
            logger.warn("Cannot compare {} to range {}{} - {}{} ", state, lower, unit, upper, unit);
            return true;
        }

        if (inverted) {
            return compareValue.compareTo(lower) <= 0 || compareValue.compareTo(upper) >= 0;
        } else {
            return compareValue.compareTo(lower) >= 0 && compareValue.compareTo(upper) <= 0;
        }
    }

    @Override
    public void persisted(Item item) {
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, lower=%s, upper=%s, unit=%s, inverted=%b]", getClass().getSimpleName(),
                getName(), lower, upper, unit, inverted);
    }
}
