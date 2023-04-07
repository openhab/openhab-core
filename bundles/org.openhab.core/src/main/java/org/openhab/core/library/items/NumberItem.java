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
package org.openhab.core.library.items;

import java.util.List;
import java.util.Locale;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;

/**
 * A NumberItem has a decimal value and is usually used for all kinds
 * of sensors, like temperature, brightness, wind, etc.
 * It can also be used as a counter or as any other thing that can be expressed
 * as a number.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class NumberItem extends GenericItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(DecimalType.class,
            QuantityType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(DecimalType.class,
            QuantityType.class, RefreshType.class);

    @Nullable
    private Class<? extends Quantity<?>> dimension;

    public NumberItem(String name) {
        this(CoreItemFactory.NUMBER, name);
    }

    public NumberItem(String type, String name) {
        super(type, name);

        String itemTypeExtension = ItemUtil.getItemTypeExtension(getType());
        if (itemTypeExtension != null) {
            dimension = UnitUtils.parseDimension(itemTypeExtension);
        }
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    public void send(DecimalType command) {
        internalSend(command);
    }

    public void send(QuantityType<?> command) {
        if (dimension == null) {
            DecimalType strippedCommand = new DecimalType(command.toBigDecimal());
            internalSend(strippedCommand);
        } else {
            internalSend(command);
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        StateDescription stateDescription = super.getStateDescription(locale);
        if (getDimension() == null && stateDescription != null) {
            String pattern = stateDescription.getPattern();
            if (pattern != null && pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                return StateDescriptionFragmentBuilder.create(stateDescription)
                        .withPattern(pattern.replaceAll(UnitUtils.UNIT_PLACEHOLDER, "").trim()).build()
                        .toStateDescription();
            }
        }
        return stateDescription;
    }

    /**
     * Returns the {@link Dimension} associated with this {@link NumberItem}, may be null.
     *
     * @return the {@link Dimension} associated with this {@link NumberItem}, may be null.
     */
    public @Nullable Class<? extends Quantity<?>> getDimension() {
        return dimension;
    }

    @Override
    public void setState(State state) {
        // QuantityType update to a NumberItem without, strip unit
        if (state instanceof QuantityType quantityType && dimension == null) {
            DecimalType plainState = new DecimalType(quantityType.toBigDecimal());
            super.setState(plainState);
            return;
        }

        // DecimalType update for a NumberItem with dimension, convert to QuantityType:
        if (state instanceof DecimalType decimalType && dimension != null) {
            Unit<?> unit = getUnit(dimension, false);
            if (unit != null) {
                super.setState(new QuantityType<>(decimalType.doubleValue(), unit));
                return;
            }
        }

        // QuantityType update, check unit and convert if necessary:
        if (state instanceof QuantityType quantityType) {
            Unit<?> itemUnit = getUnit(dimension, true);
            Unit<?> stateUnit = quantityType.getUnit();
            if (itemUnit != null && (!stateUnit.getSystemUnit().equals(itemUnit.getSystemUnit())
                    || UnitUtils.isDifferentMeasurementSystem(itemUnit, stateUnit))) {
                QuantityType<?> convertedState = quantityType.toInvertibleUnit(itemUnit);
                if (convertedState != null) {
                    super.setState(convertedState);
                    return;
                }

                // the state could not be converted to an accepted unit.
                return;
            }
        }

        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            super.setState(state);
        } else {
            logSetTypeError(state);
        }
    }

    /**
     * Returns the optional unit symbol for this {@link NumberItem}.
     *
     * @return the optional unit symbol for this {@link NumberItem}.
     */
    public @Nullable String getUnitSymbol() {
        Unit<?> unit = getUnit(dimension, true);
        return unit != null ? unit.toString() : null;
    }

    /**
     * Derive the unit for this item by the following priority:
     * <ul>
     * <li>the unit parsed from the state description</li>
     * <li>no unit if state description contains <code>%unit%</code></li>
     * <li>the default system unit from the item's dimension</li>
     * </ul>
     *
     * @return the {@link Unit} for this item if available, {@code null} otherwise.
     */
    public @Nullable Unit<? extends Quantity<?>> getUnit() {
        return getUnit(dimension, true);
    }

    /**
     * Try to convert a {@link DecimalType} into a new {@link QuantityType}. The unit for the new
     * type is derived either from the state description (which might also give a hint on items w/o dimension) or from
     * the system default unit of the given dimension.
     *
     * @param originalType the source {@link DecimalType}.
     * @param dimension the dimension to which the new {@link QuantityType} should adhere.
     * @return the new {@link QuantityType} from the given originalType, {@code null} if a unit could not be calculated.
     */
    public @Nullable QuantityType<?> toQuantityType(DecimalType originalType,
            @Nullable Class<? extends Quantity<?>> dimension) {
        Unit<? extends Quantity<?>> itemUnit = getUnit(dimension, false);
        if (itemUnit != null) {
            return new QuantityType<>(originalType.toBigDecimal(), itemUnit);
        }

        return null;
    }

    /**
     * Derive the unit for this item by the following priority:
     * <ul>
     * <li>the unit parsed from the state description</li>
     * <li>the unit from the value if <code>hasUnit = true</code> and state description has unit
     * <code>%unit%</code></li>
     * <li>the default system unit from the (optional) dimension parameter</li>
     * </ul>
     *
     * @param dimension the (optional) dimension
     * @param hasUnit if the value has a unit
     * @return the {@link Unit} for this item if available, {@code null} otherwise.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private @Nullable Unit<? extends Quantity<?>> getUnit(@Nullable Class<? extends Quantity<?>> dimension,
            boolean hasUnit) {
        if (dimension == null) {
            // if it is a plain number without dimension, we do not have a unit.
            return null;
        }
        StateDescription stateDescription = getStateDescription();
        if (stateDescription != null) {
            String pattern = stateDescription.getPattern();
            if (pattern != null) {
                if (hasUnit && pattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                    // use provided unit if present
                    return null;
                }
                Unit<?> stateDescriptionUnit = UnitUtils.parseUnit(pattern);
                if (stateDescriptionUnit != null) {
                    return stateDescriptionUnit;
                }
            }
        }

        if (unitProvider != null) {
            // explicit cast to Class<? extends Quantity> as JDK compiler complains
            return unitProvider.getUnit((Class<? extends Quantity>) dimension);
        }

        return null;
    }
}
