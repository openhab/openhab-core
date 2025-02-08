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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

import tech.units.indriya.AbstractUnit;

/**
 * This interface is a container for dimension based functions that require {@link QuantityType}s for its calculations.
 *
 * @author Henning Treu - Initial contribution
 * @author Andrew Fiddian-Green - Normalise calculations based on the Unit of the GroupItem
 */
@NonNullByDefault
public interface QuantityTypeArithmeticGroupFunction extends GroupFunction {

    abstract class DimensionalGroupFunction implements GroupFunction {

        protected final Unit<?> referenceUnit; // the reference unit for all group member calculations

        public DimensionalGroupFunction(Unit<?> referenceUnit) {
            this.referenceUnit = referenceUnit;
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
         * {@link GroupItem} i.e. 'referenceUnit'. Returns null if the {@link State} is not a {@link QuantityType} or
         * if the {@link QuantityType} could not be converted to 'referenceUnit'.
         *
         * The conversion can be made to both inverted and non-inverted units, so invertible type conversions (e.g.
         * Mirek <=> Kelvin) are supported.
         *
         * @param state the State of any given group member item
         * @return a QuantityType or null
         */
        private @Nullable QuantityType<?> referenceUnitQuantityType(@Nullable State state) {
            return state instanceof QuantityType<?> quantity ? toInvertibleUnit(quantity, referenceUnit) : null;
        }

        /**
         * Convert the given {@link QuantityType} to an equivalent based on the 'systemUnit'. The conversion can be made
         * to both inverted and non-inverted units, so invertible type conversions (e.g. Mirek <=> Kelvin) are
         * supported.
         * <p>
         * Note: we can use {@link QuantityType.toInvertibleUnit()} if OH Core PR #4561 is merged.
         *
         * @param sourceQtyType the {@link QuantityType} to be converted.
         * @param targetUnit the {@link Unit} to convert to.
         *
         * @return a new {@link QuantityType} based on 'systemUnit' or null.
         */
        public @Nullable QuantityType<?> toInvertibleUnit(QuantityType<?> sourceQtyType, Unit<?> targetUnit) {
            Unit<?> sourceSystemUnit = sourceQtyType.getUnit().getSystemUnit();
            if (!targetUnit.equals(sourceSystemUnit) && !targetUnit.isCompatible(AbstractUnit.ONE)
                    && sourceSystemUnit.inverse().isCompatible(targetUnit)) {
                return sourceQtyType.toUnit(sourceSystemUnit) instanceof QuantityType<?> systemQtyType //
                        ? systemQtyType.inverse().toUnit(targetUnit)
                        : null;
            }
            return sourceQtyType.toUnit(targetUnit);
        }

        /**
         * Convert a set of {@link Item} to a respective list of {@link QuantityType}. Exclude any {@link Item}s whose
         * current {@link State} is not a {@link QuantityType}. Convert any remaining {@link QuantityType} to the
         * 'referenceUnit' and exclude any values that did not convert.
         *
         * @param items a list of {@link Item}
         * @return a list of {@link QuantityType} converted to the 'referenceUnit'
         */
        @SuppressWarnings({ "rawtypes" })
        protected List<QuantityType> referenceUnitQuantityTypes(Set<Item> items) {
            return items.stream().map(i -> i.getState()).map(s -> referenceUnitQuantityType(s)).filter(Objects::nonNull)
                    .map(s -> (QuantityType) s).toList();
        }
    }

    /**
     * Calculates the average of a set of item states whose value could be converted to the 'referenceUnit'.
     */
    class Avg extends DimensionalGroupFunction {

        public Avg(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                List<QuantityType> referenceUnitQuantities = referenceUnitQuantityTypes(items);
                if (!referenceUnitQuantities.isEmpty()) {
                    return referenceUnitQuantities.stream()
                            .reduce(new QuantityType<>(0, referenceUnit), QuantityType::add)
                            .divide(BigDecimal.valueOf(referenceUnitQuantities.size()));
                }
            }
            return UnDefType.UNDEF;
        }
    }

    /**
     * Calculates the median of a set of item states whose value could be converted to the 'referenceUnit'.
     */
    class Median extends DimensionalGroupFunction {

        public Median(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                BigDecimal median = Statistics
                        .median(referenceUnitQuantityTypes(items).stream().map(q -> q.toBigDecimal()).toList());
                if (median != null) {
                    return new QuantityType<>(median, referenceUnit);
                }
            }
            return UnDefType.UNDEF;
        }
    }

    /**
     * Calculates the sum of a set of item states whose value could be converted to the 'referenceUnit'.
     *
     * Uses the {@link QuanitityType.add()} method so the result is an incremental sum based on the 'referenceUnit'. As
     * a general rule this class is instantiated with a 'referenceUnit' that is a "system unit" (which are zero based)
     * so such incremental sum is in fact also an absolute sum. However the class COULD be instantiated with a "non-
     * system unit" (e.g. °C, °F) in which case the result would be an incremental sum based on that unit.
     *
     */
    class Sum extends DimensionalGroupFunction {

        public Sum(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                List<QuantityType> referenceUnitQuantities = referenceUnitQuantityTypes(items);
                if (!referenceUnitQuantities.isEmpty()) {
                    return referenceUnitQuantities.stream().reduce(new QuantityType<>(0, referenceUnit),
                            QuantityType::add);
                }
            }
            return UnDefType.UNDEF;
        }
    }

    /**
     * Calculates the minimum of a set of item states whose value could be converted to the 'referenceUnit'.
     */
    class Min extends DimensionalGroupFunction {

        public Min(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                Optional<QuantityType> min = referenceUnitQuantityTypes(items).stream().min(QuantityType::compareTo);
                return min.isPresent() ? min.get() : UnDefType.UNDEF;
            }
            return UnDefType.UNDEF;
        }
    }

    /**
     * Calculates the maximum of a set of item states whose value could be converted to the 'referenceUnit'.
     */
    class Max extends DimensionalGroupFunction {

        public Max(Unit<?> targetUnit) {
            super(targetUnit);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(@Nullable Set<Item> items) {
            if (items != null) {
                Optional<QuantityType> max = referenceUnitQuantityTypes(items).stream().max(QuantityType::compareTo);
                return max.isPresent() ? max.get() : UnDefType.UNDEF;
            }
            return UnDefType.UNDEF;
        }
    }
}
