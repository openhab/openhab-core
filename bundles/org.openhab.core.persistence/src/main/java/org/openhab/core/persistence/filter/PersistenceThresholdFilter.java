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
package org.openhab.core.persistence.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import javax.measure.UnconvertibleException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersistenceThresholdFilter} is a filter to prevent persistence based on a threshold.
 *
 * The filter returns {@code false} if the new value deviates by less than {@link #value}. If unit is "%" is
 * {@code true}, the filter returns {@code false} if the relative deviation is less than {@link #value}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceThresholdFilter extends PersistenceFilter {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Logger logger = LoggerFactory.getLogger(PersistenceThresholdFilter.class);

    private final BigDecimal value;
    private final String unit;

    private final transient Map<String, State> valueCache = new HashMap<>();

    public PersistenceThresholdFilter(String name, BigDecimal value, String unit) {
        super(name);
        this.value = value;
        this.unit = unit;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean apply(Item item) {
        String itemName = item.getName();
        State state = item.getState();
        if (!(state instanceof DecimalType || state instanceof QuantityType)) {
            return true;
        }

        State cachedState = valueCache.get(itemName);

        if (cachedState == null || !state.getClass().equals(cachedState.getClass())) {
            return true;
        }

        if (state instanceof DecimalType) {
            BigDecimal oldState = ((DecimalType) cachedState).toBigDecimal();
            BigDecimal delta = oldState.subtract(((DecimalType) state).toBigDecimal());
            if ("%".equals(unit) && !BigDecimal.ZERO.equals(oldState)) {
                delta = delta.multiply(HUNDRED).divide(oldState, 2, RoundingMode.HALF_UP);
            }
            return delta.abs().compareTo(value) > 0;
        } else {
            try {
                QuantityType oldState = (QuantityType) cachedState;
                QuantityType delta = oldState.subtract((QuantityType) state);
                if ("%".equals(unit)) {
                    if (BigDecimal.ZERO.equals(oldState.toBigDecimal())) {
                        // value is different and old value is 0 -> always above relative threshold
                        return true;
                    } else {
                        // calculate percent
                        delta = delta.multiply(HUNDRED).divide(oldState);
                    }
                } else if (!unit.isBlank()) {
                    // consider unit only if not relative threshold
                    delta = delta.toUnit(unit);
                    if (delta == null) {
                        throw new UnconvertibleException("");
                    }
                }

                return delta.toBigDecimal().abs().compareTo(value) > 0;
            } catch (UnconvertibleException e) {
                logger.warn("Cannot compare {} to {}", cachedState, state);
                return true;
            }
        }
    }

    @Override
    public void persisted(Item item) {
        valueCache.put(item.getName(), item.getState());
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, value=%s, unit=%s]", getClass().getSimpleName(), getName(), value, unit);
    }
}
