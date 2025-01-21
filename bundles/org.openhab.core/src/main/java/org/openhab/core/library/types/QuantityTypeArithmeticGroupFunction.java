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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.core.util.Statistics;

/**
 * This interface is a container for dimension based functions that require {@link QuantityType}s for its calculations.
 *
 * @author Henning Treu - Initial contribution
 * @author Andrew Fiddian-Green - Normalise calculations based on the Unit of the GroupItem
 */
@NonNullByDefault
public interface QuantityTypeArithmeticGroupFunction extends GroupFunction {

    abstract class DimensionalGroupFunction implements GroupFunction {

        protected final Unit<?> targetUnit; // the base reference unit for all group calculations

        public DimensionalGroupFunction(Unit<?> targetUnit) {
            this.targetUnit = targetUnit;
        }

        @Override
        public @Nullable <T extends State> T getStateAs(@Nullable Set<Item> items, Class<T> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return stateClass.cast(state);
            } else {
                return null;
            }
        }

        @Override
        public State[] getParameters() {
            return new State[0];
        }

        /**
         * Convert the given item {@link State} to a {@link QuantityType} based on the {@link Unit} of the
         * {@link GroupItem} i.e. the 'targetUnit'. Returns false if the {@link State} is not a {@link QuantityType} or
         * if the {@link QuantityType} could not be converted to the 'targetUnit'. The conversion can be made to both
         * inverted and non-inverted units, so invertible type conversions (e.g. Mirek <=> Kelvin) will succeed.
         *
         * @param state the State of any given group member item
         * @return a QuantityType or null
         */
        protected @Nullable QuantityType<?> normalizedQuantityType(@Nullable State state) {
            return state instanceof QuantityType<?> quantity ? quantity.toInvertibleUnit(targetUnit) : null;
        }
    }

    /**
     * This calculates the numeric average over all item states of {@link QuantityType}.
     */
    class Avg extends DimensionalGroupFunction {

        public Avg(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> sum = null;
            int count = 0;

            for (Item item : items) {
                QuantityType itemState = normalizedQuantityType(item.getStateAs(QuantityType.class));
                if (itemState != null) {
                    if (sum == null) {
                        sum = QuantityType.valueOf(0, targetUnit);
                    }
                    sum = sum.add(itemState);
                    count++;
                }
            }

            if (sum != null && count > 0) {
                BigDecimal result = sum.toBigDecimal().divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);
                return new QuantityType(result, targetUnit);
            }

            return UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the numeric median over all item states of {@link QuantityType}.
     */
    class Median extends DimensionalGroupFunction {

        public Median(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            List<BigDecimal> values = new ArrayList<>();

            for (Item item : items) {
                QuantityType itemState = normalizedQuantityType(item.getStateAs(QuantityType.class));
                if (itemState != null) {
                    values.add(itemState.toBigDecimal());
                }
            }

            if (!values.isEmpty()) {
                BigDecimal median = Statistics.median(values);
                if (median != null) {
                    return new QuantityType<>(median, targetUnit);
                }
            }

            return UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the numeric sum over all item states of {@link QuantityType}.
     */
    class Sum extends DimensionalGroupFunction {

        public Sum(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> sum = null;

            for (Item item : items) {
                QuantityType itemState = normalizedQuantityType(item.getStateAs(QuantityType.class));
                if (itemState != null) {
                    if (sum == null) {
                        sum = QuantityType.valueOf(0, targetUnit);
                    }
                    sum = sum.add(itemState);
                }
            }

            return sum != null ? sum : UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the minimum value of all item states of {@link QuantityType}.
     */
    class Min extends DimensionalGroupFunction {

        public Min(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> min = null;

            for (Item item : items) {
                QuantityType itemState = normalizedQuantityType(item.getStateAs(QuantityType.class));
                if (itemState != null && (min == null || min.compareTo(itemState) > 0)) {
                    min = itemState;
                }
            }

            return min != null ? min : UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the maximum value of all item states of {@link QuantityType}.
     */
    class Max extends DimensionalGroupFunction {

        public Max(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> max = null;

            for (Item item : items) {
                QuantityType itemState = normalizedQuantityType(item.getStateAs(QuantityType.class));
                if (itemState != null && (max == null || max.compareTo(itemState) < 0)) {
                    max = itemState;
                }
            }

            return max != null ? max : UnDefType.UNDEF;
        }
    }
}
