/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.core.util.Statistics;

/**
 * This interface is a container for dimension based functions that require {@link QuantityType}s for its calculations.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface QuantityTypeArithmeticGroupFunction extends GroupFunction {

    abstract class DimensionalGroupFunction implements GroupFunction {

        protected final Class<? extends Quantity<?>> dimension;

        public DimensionalGroupFunction(Class<? extends Quantity<?>> dimension) {
            this.dimension = dimension;
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

        protected boolean isSameDimension(@Nullable Item item) {
            if (item instanceof GroupItem groupItem) {
                return isSameDimension(groupItem.getBaseItem());
            }
            return item instanceof NumberItem ni && dimension.equals(ni.getDimension());
        }
    }

    /**
     * This calculates the numeric average over all item states of {@link QuantityType}.
     */
    class Avg extends DimensionalGroupFunction {

        public Avg(Class<? extends Quantity<?>> dimension) {
            super(dimension);
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
                if (isSameDimension(item)) {
                    QuantityType itemState = item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (sum == null) {
                            sum = itemState; // initialise the sum from the first item
                            count++;
                        } else {
                            itemState = itemState.toInvertibleUnit(sum.getUnit());
                            if (itemState != null) {
                                sum = sum.add(itemState);
                                count++;
                            }
                        }
                    }
                }
            }

            if (sum != null && count > 0) {
                BigDecimal result = sum.toBigDecimal().divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);
                return new QuantityType(result, sum.getUnit());
            }

            return UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the numeric median over all item states of {@link QuantityType}.
     */
    class Median extends DimensionalGroupFunction {

        private @Nullable Item baseItem;

        public Median(Class<? extends Quantity<?>> dimension, @Nullable Item baseItem) {
            super(dimension);
            this.baseItem = baseItem;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                List<BigDecimal> values = new ArrayList<>();
                Unit<?> unit = null;
                if (baseItem instanceof NumberItem numberItem) {
                    unit = numberItem.getUnit();
                }
                for (Item item : items) {
                    if (!isSameDimension(item)) {
                        continue;
                    }
                    QuantityType itemState = item.getStateAs(QuantityType.class);
                    if (itemState == null) {
                        continue;
                    }
                    if (unit == null) {
                        unit = itemState.getUnit(); // set it to the first item's unit
                    }
                    if (itemState.toInvertibleUnit(unit) instanceof QuantityType<?> inverted) {
                        values.add(inverted.toBigDecimal());
                    }
                }

                if (!values.isEmpty()) {
                    BigDecimal median = Statistics.median(values);
                    if (median != null && unit != null) {
                        return new QuantityType<>(median, unit);
                    }

                }
            }
            return UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the numeric sum over all item states of {@link QuantityType}.
     */
    class Sum extends DimensionalGroupFunction {

        public Sum(Class<? extends Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> sum = null;
            for (Item item : items) {
                if (isSameDimension(item)) {
                    QuantityType itemState = item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (sum == null) {
                            sum = itemState; // initialise the sum from the first item
                        } else {
                            itemState = itemState.toUnit(sum.getUnit());
                            if (itemState != null) {
                                sum = sum.add(itemState);
                            }
                        }
                    }
                }
            }

            return sum != null ? sum : UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the minimum value of all item states of {@link QuantityType}.
     */
    class Min extends DimensionalGroupFunction {

        public Min(Class<? extends Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> min = null;
            for (Item item : items) {
                if (isSameDimension(item)) {
                    QuantityType itemState = item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (min == null
                                || (min.getUnit().isCompatible(itemState.getUnit()) && min.compareTo(itemState) > 0)) {
                            min = itemState;
                        }
                    }
                }
            }

            return min != null ? min : UnDefType.UNDEF;
        }
    }

    /**
     * This calculates the maximum value of all item states of {@link QuantityType}.
     */
    class Max extends DimensionalGroupFunction {

        public Max(Class<? extends Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items == null || items.isEmpty()) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> max = null;
            for (Item item : items) {
                if (isSameDimension(item)) {
                    QuantityType itemState = item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (max == null
                                || (max.getUnit().isCompatible(itemState.getUnit()) && max.compareTo(itemState) < 0)) {
                            max = itemState;
                        }
                    }
                }
            }

            return max != null ? max : UnDefType.UNDEF;
        }
    }
}
